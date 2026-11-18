package com.forgebound.enhance;

import com.forgebound.ForgeboundConfig;
import com.forgebound.content.ModContent;
import com.forgebound.tier.GearType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rune Engraving Table's Transfer tab: moves every enhancement from the source item to the target item (same kind
 * of gear; ones the target already has stay behind). The target needs a free slot for each; it costs Arcane Shards
 * and Crystal Dust per enhancement moved. Button 1 transfers, 100 opens the Enhance tab.
 */
public class TransferMenu extends AbstractContainerMenu {
    public static final int SOURCE = 0, TARGET = 1, SHARD = 2, DUST = 3, SIZE = 4;
    public static final int INV_Y = EngraveMenu.INV_Y;
    public static final int BUTTON_TRANSFER = 1, BUTTON_ENGRAVE_TAB = 100;

    private final ContainerLevelAccess access;
    private final Container container = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    public TransferMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public TransferMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModContent.TRANSFER_MENU.get(), id);
        this.access = access;
        addSlot(gearSlot(SOURCE, 26, 38));
        addSlot(gearSlot(TARGET, 134, 38));
        addSlot(new Slot(container, SHARD, 26, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModContent.ARCANE_SHARD.get());
            }
        });
        addSlot(new Slot(container, DUST, 48, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModContent.CRYSTAL_DUST.get());
            }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
    }

    private Slot gearSlot(int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GearType.isGear(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
    }

    public static MenuProvider provider(Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new TransferMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.forgebound.rune_engraving_table"));
    }

    public ItemStack source() {
        return container.getItem(SOURCE);
    }

    public ItemStack target() {
        return container.getItem(TARGET);
    }

    /** The source's enhancements the target doesn't have yet (what a transfer moves). */
    public List<Enhancement> moving() {
        List<Enhancement> out = new ArrayList<>();
        if (source().isEmpty() || target().isEmpty()) return out;
        List<Enhancement> have = Enhancements.get(target());
        for (Enhancement e : Enhancements.get(source())) if (!have.contains(e)) out.add(e);
        return out;
    }

    public int needShards() {
        return moving().size() * ForgeboundConfig.get().transferShards;
    }

    public int needDust() {
        return moving().size() * ForgeboundConfig.get().transferDust;
    }

    public boolean has(int slot) {
        int need = slot == SHARD ? needShards() : slot == DUST ? needDust() : 0;
        return container.getItem(slot).getCount() >= need;
    }

    public Component blocked() {
        if (source().isEmpty() || target().isEmpty()) return Component.translatable("screen.forgebound.transfer.place");
        if (Enhancements.kind(source()) != Enhancements.kind(target())) return Component.translatable("screen.forgebound.transfer.kind");
        List<Enhancement> move = moving();
        if (move.isEmpty()) return Component.translatable("screen.forgebound.transfer.nothing");
        if (Enhancements.free(target()) < move.size()) return Component.translatable("screen.forgebound.transfer.slots", move.size());
        if (!has(SHARD) || !has(DUST)) return Component.translatable("screen.forgebound.transfer.materials");
        return null;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return false;
        if (id == BUTTON_ENGRAVE_TAB) {
            access.execute((level, pos) -> sp.openMenu(EngraveMenu.provider(level, pos)));
            return true;
        }
        if (id != BUTTON_TRANSFER || blocked() != null) return false;
        List<Enhancement> move = moving();
        int shards = needShards(), dust = needDust();
        ItemStack src = container.getItem(SOURCE), dst = container.getItem(TARGET);
        List<Enhancement> to = new ArrayList<>(Enhancements.get(dst));
        to.addAll(move);
        Enhancements.set(dst, to);
        Enhancements.set(src, List.of());
        container.removeItem(SHARD, shards);
        container.removeItem(DUST, dust);
        container.setItem(SOURCE, src);
        container.setItem(TARGET, dst);
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F));
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else if (GearType.isGear(stack)) {
            if (!moveItemStackTo(stack, SOURCE, TARGET + 1, false)) return ItemStack.EMPTY;
        } else {
            int target = stack.is(ModContent.ARCANE_SHARD.get()) ? SHARD : stack.is(ModContent.CRYSTAL_DUST.get()) ? DUST : -1;
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModContent.RUNE_TABLE.get());
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((level, pos) -> clearContainer(p, container));
    }
}
