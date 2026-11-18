package com.forgebound.enhance;

import com.forgebound.content.ModContent;
import com.forgebound.tier.GearType;
import com.forgebound.tier.Tier;
import com.forgebound.tier.Tiers;
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
 * The Rune Engraving Table's Enhance tab: equipment on top, Arcane Shards below. Button i (0..) engraves the i-th
 * enhancement offered for this kind of gear, or removes it when the item already carries it. Engraving costs Arcane
 * Shards and XP levels by tier; removing costs half the shards. 101 opens the Transfer tab.
 */
public class EngraveMenu extends AbstractContainerMenu {
    public static final int GEAR = 0, SHARD = 1, SIZE = 2;
    public static final int INV_Y = 132;
    public static final int BUTTON_TRANSFER_TAB = 101;

    private final ContainerLevelAccess access;
    private final Player player;
    private final Container container = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    public EngraveMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public EngraveMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModContent.ENGRAVE_MENU.get(), id);
        this.access = access;
        this.player = inv.player;
        addSlot(new Slot(container, GEAR, 26, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GearType.isGear(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, SHARD, 26, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModContent.ARCANE_SHARD.get());
            }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
    }

    public static MenuProvider provider(Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new EngraveMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.forgebound.rune_engraving_table"));
    }

    public ItemStack gear() {
        return container.getItem(GEAR);
    }

    public int shards() {
        return container.getItem(SHARD).getCount();
    }

    /** Enhancements offered for the item in the slot (fixed order, same on both sides). */
    public List<Enhancement> offered() {
        Enhancement.Kind kind = Enhancements.kind(gear());
        return kind == null ? List.of() : Enhancement.forKind(kind);
    }

    public Tier tier() {
        return Tiers.get(gear());
    }

    /** Why clicking this enhancement does nothing (null = it works). */
    public Component blocked(Player p, Enhancement e) {
        ItemStack gear = gear();
        if (gear.isEmpty()) return Component.translatable("screen.forgebound.engrave.place");
        boolean has = Enhancements.get(gear).contains(e);
        Tier tier = tier();
        if (has) {
            return shards() < Enhancements.removeShards(tier) ? Component.translatable("screen.forgebound.engrave.need_shards", Enhancements.removeShards(tier)) : null;
        }
        if (Enhancements.free(gear) <= 0) return Component.translatable("screen.forgebound.engrave.full", Enhancements.slots(tier));
        if (shards() < Enhancements.applyShards(tier)) return Component.translatable("screen.forgebound.engrave.need_shards", Enhancements.applyShards(tier));
        if (!p.getAbilities().instabuild && p.experienceLevel < Enhancements.applyLevels(tier)) {
            return Component.translatable("screen.forgebound.engrave.need_levels", Enhancements.applyLevels(tier));
        }
        return null;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return false;
        if (id == BUTTON_TRANSFER_TAB) {
            access.execute((level, pos) -> sp.openMenu(TransferMenu.provider(level, pos)));
            return true;
        }
        List<Enhancement> offered = offered();
        if (id < 0 || id >= offered.size()) return false;
        Enhancement e = offered.get(id);
        if (blocked(p, e) != null) return false;
        ItemStack gear = container.getItem(GEAR);
        Tier tier = tier();
        List<Enhancement> list = new ArrayList<>(Enhancements.get(gear));
        boolean removing = list.contains(e);
        if (removing) {
            list.remove(e);
            container.removeItem(SHARD, Enhancements.removeShards(tier));
        } else {
            list.add(e);
            container.removeItem(SHARD, Enhancements.applyShards(tier));
            if (!p.getAbilities().instabuild) p.giveExperienceLevels(-Enhancements.applyLevels(tier));
        }
        Enhancements.set(gear, list);
        container.setItem(GEAR, gear);
        access.execute((level, pos) -> level.playSound(null, pos, removing ? SoundEvents.GRINDSTONE_USE : SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, removing ? 1.0F : 1.2F));
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
        } else {
            int target = GearType.isGear(stack) ? GEAR : stack.is(ModContent.ARCANE_SHARD.get()) ? SHARD : -1;
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
