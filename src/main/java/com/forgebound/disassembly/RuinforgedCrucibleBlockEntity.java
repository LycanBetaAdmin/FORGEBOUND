package com.forgebound.disassembly;

import com.forgebound.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The Crucible's furnace logic: one piece of coal or charcoal burns {@link #FUEL_TICKS}; while it burns, the gear in
 * the input melts over {@link #MELT_TICKS} and its {@link Recovery} parts land in the six output slots. It only lights
 * when the gear can be recovered and everything it could give fits in the output.
 */
public class RuinforgedCrucibleBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUT = 0, FUEL = 1, OUT_FIRST = 2, OUTPUTS = 6, SIZE = OUT_FIRST + OUTPUTS;
    public static final int FUEL_TICKS = 1600, MELT_TICKS = 200, PREVIEW = 6;

    /**
     * Synced to the screen: burn left/total, melt progress, status, then for up to {@link #PREVIEW} recovered parts:
     * item id + 1 (low 15 bits, 0 = none), the rest of the id, and the expected amount x100.
     */
    public static final int D_BURN = 0, D_BURN_TOTAL = 1, D_PROGRESS = 2, D_STATUS = 3, D_PARTS = 4, D_COUNT = D_PARTS + PREVIEW * 3;
    public static final int S_EMPTY = 0, S_OK = 1, S_UNRECOVERABLE = 2, S_OUTPUT_BLOCKED = 3;

    private static final int[] TOP = {INPUT}, SIDES = {FUEL}, BOTTOM = {2, 3, 4, 5, 6, 7};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int burn, burnTotal, progress, status;
    private final int[] preview = new int[PREVIEW * 3];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case D_BURN -> burn;
                case D_BURN_TOTAL -> burnTotal;
                case D_PROGRESS -> progress;
                case D_STATUS -> status;
                default -> i - D_PARTS < preview.length ? preview[i - D_PARTS] : 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return D_COUNT;
        }
    };

    public RuinforgedCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.CRUCIBLE_BE.get(), pos, state);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(ItemTags.COALS);
    }

    void serverTick() {
        if (level == null) return;
        boolean wasLit = burn > 0;
        if (burn > 0) burn--;

        ItemStack in = items.get(INPUT);
        List<Recovery.Part> parts = in.isEmpty() ? List.of() : Recovery.of(in, level);
        boolean canMelt = false;
        java.util.Arrays.fill(preview, 0);
        if (in.isEmpty()) {
            status = S_EMPTY;
        } else if (parts.isEmpty()) {
            status = S_UNRECOVERABLE;
        } else {
            for (int i = 0; i < Math.min(PREVIEW, parts.size()); i++) {
                int id = BuiltInRegistries.ITEM.getId(parts.get(i).item());
                preview[i * 3] = (id & 0x7FFF) + 1;
                preview[i * 3 + 1] = id >> 15;
                preview[i * 3 + 2] = (int) Math.min(30000, Math.round(parts.get(i).expected() * 100));
            }
            canMelt = fits(parts);
            status = canMelt ? S_OK : S_OUTPUT_BLOCKED;
        }

        if (burn == 0 && canMelt && isFuel(items.get(FUEL))) {
            items.get(FUEL).shrink(1);
            burn = burnTotal = FUEL_TICKS;
        }
        if (burn > 0 && canMelt) {
            if (++progress >= MELT_TICKS) {
                progress = 0;
                melt(parts);
            }
        } else if (!canMelt) {
            progress = 0;
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2); // cools down without fuel
        }

        boolean lit = burn > 0;
        BlockState state = getBlockState();
        if (wasLit != lit && state.getValue(RuinforgedCrucibleBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(RuinforgedCrucibleBlock.LIT, lit), 3);
        }
        if (lit || wasLit) setChanged();
    }

    /** True when the most every part could give fits into the output slots together. */
    private boolean fits(List<Recovery.Part> parts) {
        List<ItemStack> sim = new ArrayList<>();
        for (int i = 0; i < OUTPUTS; i++) sim.add(items.get(OUT_FIRST + i).copy());
        for (Recovery.Part p : parts) {
            if (insert(sim, p.item(), p.most()) > 0) return false;
        }
        return true;
    }

    /** Puts {@code n} of an item into the slots (existing stacks first); returns what didn't fit. */
    private static int insert(List<ItemStack> slots, net.minecraft.world.item.Item item, int n) {
        for (int i = 0; i < slots.size() && n > 0; i++) {
            ItemStack s = slots.get(i);
            if (!s.isEmpty() && s.is(item)) {
                int add = Math.min(n, s.getMaxStackSize() - s.getCount());
                s.grow(add);
                n -= add;
            }
        }
        for (int i = 0; i < slots.size() && n > 0; i++) {
            if (slots.get(i).isEmpty()) {
                int put = Math.min(n, item.getDefaultMaxStackSize());
                slots.set(i, new ItemStack(item, put));
                n -= put;
            }
        }
        return n;
    }

    private void melt(List<Recovery.Part> parts) {
        items.set(INPUT, ItemStack.EMPTY);
        List<ItemStack> out = items.subList(OUT_FIRST, SIZE);
        for (Recovery.Part p : parts) {
            int n = p.roll(level.random);
            if (n > 0) insert(out, p.item(), n);
        }
        level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.7F + level.random.nextFloat() * 0.2F);
        setChanged();
    }

    // ------------------------------------------------------------------ container

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.forgebound.ruinforged_crucible");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new CrucibleMenu(id, inv, this, data);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> CoreMaterials.isGear(stack);
            case FUEL -> isFuel(stack);
            default -> false;
        };
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        boolean newInput = slot == INPUT && !ItemStack.isSameItemSameComponents(stack, items.get(INPUT));
        super.setItem(slot, stack);
        if (newInput) progress = 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : side == Direction.UP ? TOP : SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack) && (slot != INPUT || items.get(INPUT).isEmpty());
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUT_FIRST;
    }

    // ------------------------------------------------------------------ save (a Crucible from Eldoria RPG had input, fuel, one output: slots 0-2 still line up)

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        burn = tag.getInt("Burn");
        burnTotal = tag.getInt("BurnTotal");
        progress = tag.getInt("Progress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Burn", burn);
        tag.putInt("BurnTotal", burnTotal);
        tag.putInt("Progress", progress);
    }
}
