package com.forgebound.anvil;

import com.forgebound.ForgeboundConfig;
import com.forgebound.content.ModContent;
import com.forgebound.tier.GearType;
import com.forgebound.tier.Tier;
import com.forgebound.tier.Tiers;
import com.forgebound.unique.UniqueAttribute;
import com.forgebound.unique.Uniques;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Forge Anvil's Reroll tab: equipment on top, Fortune Dice and Soul Essence below (cost by tier), an optional
 * Fortune Crystal (premium roll: skips the base pool) and Sealing Runes (one per attribute kept). Buttons: 1 = reroll,
 * 20/21 = seal or unseal the first/second Unique Attribute, 100 = back to the Reforge tab.
 */
public class RerollMenu extends AbstractContainerMenu {
    public static final int GEAR = 0, DICE = 1, ESSENCE = 2, CRYSTAL = 3, RUNE = 4, SIZE = 5;
    public static final int INV_Y = ForgeAnvilMenu.INV_Y;
    public static final int BUTTON_REROLL = 1, BUTTON_SEAL = 20, BUTTON_REFORGE_TAB = 100;

    private final ContainerLevelAccess access;
    private final Player player;
    private final Container container = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    /** Seal flags for attribute slot 0 and 1. */
    private final ContainerData seals = new SimpleContainerData(2);

    public RerollMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public RerollMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModContent.REROLL_MENU.get(), id);
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
        addSlot(material(DICE, 26, 78));
        addSlot(material(ESSENCE, 48, 78));
        addSlot(material(CRYSTAL, 70, 78));
        addSlot(material(RUNE, 92, 78));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
        addDataSlots(seals);
    }

    private Slot material(int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return slotFor(stack) == index;
            }
        };
    }

    public static int slotFor(ItemStack stack) {
        if (stack.is(ModContent.FORTUNE_DICE.get())) return DICE;
        if (stack.is(ModContent.SOUL_ESSENCE.get())) return ESSENCE;
        if (stack.is(ModContent.FORTUNE_CRYSTAL.get())) return CRYSTAL;
        if (stack.is(ModContent.SEALING_RUNE.get())) return RUNE;
        return -1;
    }

    @Override
    public void slotsChanged(Container changed) {
        super.slotsChanged(changed);
        // seals only make sense for attributes the item has
        List<UniqueAttribute> cur = Uniques.get(gear());
        for (int i = 0; i < 2; i++) if (i >= cur.size()) seals.set(i, 0);
    }

    public ItemStack gear() {
        return container.getItem(GEAR);
    }

    public boolean sealed(int i) {
        return seals.get(i) != 0;
    }

    public int sealedCount() {
        return (sealed(0) ? 1 : 0) + (sealed(1) ? 1 : 0);
    }

    public boolean premium() {
        return !container.getItem(CRYSTAL).isEmpty();
    }

    public int needDice() {
        return gear().isEmpty() ? 0 : ForgeboundConfig.get().rerollDice(Tiers.get(gear()).level());
    }

    public int needEssence() {
        return gear().isEmpty() ? 0 : ForgeboundConfig.get().rerollEssence(Tiers.get(gear()).level());
    }

    public int need(int slot) {
        return switch (slot) {
            case DICE -> needDice();
            case ESSENCE -> needEssence();
            case CRYSTAL -> premium() ? 1 : 0;
            case RUNE -> sealedCount();
            default -> 0;
        };
    }

    public boolean has(int slot) {
        return container.getItem(slot).getCount() >= need(slot);
    }

    /** Why the reroll can't run (null = it can). */
    public Component blocked() {
        ItemStack gear = gear();
        if (gear.isEmpty()) return Component.translatable("screen.forgebound.reroll.place");
        if (UniqueAttribute.Category.of(GearType.of(gear)) == null) return Component.translatable("screen.forgebound.reroll.place");
        int slots = Uniques.slots(Tiers.get(gear));
        if (sealedCount() >= slots) return Component.translatable("screen.forgebound.reroll.all_sealed");
        for (int s = DICE; s <= RUNE; s++) {
            if (!has(s)) return Component.translatable(s == RUNE ? "screen.forgebound.reroll.need_rune" : "screen.forgebound.reroll.materials");
        }
        return null;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return false;
        if (id == BUTTON_REFORGE_TAB) {
            access.execute((level, pos) -> sp.openMenu(ForgeAnvilMenu.provider(level, pos)));
            return true;
        }
        if (id == BUTTON_SEAL || id == BUTTON_SEAL + 1) {
            int i = id - BUTTON_SEAL;
            if (i < Uniques.get(gear()).size() && Uniques.slots(Tiers.get(gear())) > 1) seals.set(i, sealed(i) ? 0 : 1);
            broadcastChanges();
            return true;
        }
        if (id != BUTTON_REROLL || blocked() != null) return false;
        int dice = needDice(), essence = needEssence(), runes = sealedCount();
        boolean premium = premium();
        ItemStack gear = container.getItem(GEAR);
        if (!Uniques.reroll(gear, p.getRandom(), premium, new boolean[]{sealed(0), sealed(1)})) return false;
        container.removeItem(DICE, dice);
        container.removeItem(ESSENCE, essence);
        if (premium) container.removeItem(CRYSTAL, 1);
        if (runes > 0) container.removeItem(RUNE, runes);
        container.setItem(GEAR, gear);
        seals.set(0, 0);
        seals.set(1, 0);
        access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.1F);
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.6F, 1.4F);
        });
        broadcastChanges();
        return true;
    }

    /** Tier this item's rerolls can draw from (for the screen). */
    public Tier tier() {
        return Tiers.get(gear());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int target = GearType.isGear(stack) ? GEAR : slotFor(stack);
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
        return stillValid(access, p, ModContent.FORGE_ANVIL.get());
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((level, pos) -> clearContainer(p, container));
    }
}
