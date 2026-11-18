package com.forgebound.anvil;

import com.forgebound.ForgeboundConfig;
import com.forgebound.content.ModContent;
import com.forgebound.tier.Blacksmith;
import com.forgebound.tier.GearType;
import com.forgebound.tier.Tier;
import com.forgebound.tier.Tiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Forge Anvil: equipment in the top slot, the upgrade materials below (Arcane Shard, the tier's metal, Crystal
 * Dust, Ascension Core). Reforge (button 0) raises the tier by one and consumes the cost; the item keeps everything
 * else it carries (enchantments, Apotheosis affixes and gems, names). Who may forge how far, and at what cost, is
 * {@link Blacksmith}'s call (server side; synced here as {@link #cap()} and {@link #multiplier()}).
 */
public class ForgeAnvilMenu extends AbstractContainerMenu {
    public static final int GEAR = 0, SHARD = 1, METAL = 2, DUST = 3, CORE = 4, SIZE = 5;
    public static final int INV_Y = 178, INV_X = 70;

    private final ContainerLevelAccess access;
    private final Player player;
    private final Container container = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    /** 0 = cap (max tier this player may forge this item to), 1 = material multiplier, 2 = Blacksmith (1/0). */
    private final ContainerData data = new SimpleContainerData(3);

    /** Client side. */
    public ForgeAnvilMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public ForgeAnvilMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModContent.FORGE_ANVIL_MENU.get(), id);
        this.access = access;
        this.player = inv.player;
        addSlot(new Slot(container, GEAR, 97, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GearType.isGear(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(material(SHARD, 82, 100));
        addSlot(material(METAL, 106, 100));
        addSlot(material(DUST, 130, 100));
        addSlot(material(CORE, 154, 100));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 70 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 70 + c * 18, INV_Y + 58));
        addDataSlots(data);
        refresh();
    }

    private Slot material(int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return slotFor(stack) == index;
            }
        };
    }

    public static boolean isMetal(ItemStack stack) {
        return stack.is(ModContent.VERDANTITE_INGOT.get()) || stack.is(ModContent.INFERNALITE_INGOT.get()) || stack.is(ModContent.VOIDSTEEL_INGOT.get());
    }

    /** Which material slot takes this stack, or -1. */
    public static int slotFor(ItemStack stack) {
        if (stack.is(ModContent.ARCANE_SHARD.get())) return SHARD;
        if (isMetal(stack)) return METAL;
        if (stack.is(ModContent.CRYSTAL_DUST.get())) return DUST;
        if (stack.is(ModContent.ASCENSION_CORE.get())) return CORE;
        return -1;
    }

    /** Server: work out the cap and the multiplier for this player and item. */
    private void refresh() {
        if (player.level().isClientSide()) return;
        ItemStack gear = container.getItem(GEAR);
        data.set(0, gear.isEmpty() ? 0 : Blacksmith.anvilCap(player, gear));
        data.set(1, Blacksmith.costMultiplier(player));
        data.set(2, Blacksmith.isBlacksmith(player) ? 1 : 0);
    }

    @Override
    public void slotsChanged(Container changed) {
        super.slotsChanged(changed);
        refresh();
    }

    public ItemStack gear() {
        return container.getItem(GEAR);
    }

    public int cap() {
        return data.get(0);
    }

    public int multiplier() {
        return Math.max(1, data.get(1));
    }

    public boolean blacksmith() {
        return data.get(2) != 0;
    }

    /** What the next upgrade costs this player (counts already multiplied), or null at T7 / no gear. */
    public Cost cost() {
        ItemStack gear = gear();
        if (gear.isEmpty()) return null;
        Tier tier = Tiers.get(gear);
        ForgeboundConfig.Upgrade u = ForgeboundConfig.get().upgrade(tier.level());
        if (u == null) return null;
        int m = multiplier();
        Item metal = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(u.metal)).orElse(Items.AIR);
        return new Cost(tier.next(), u.arcaneShard * m, metal, u.metalCount * m, u.crystalDust * m, u.ascensionCore * m);
    }

    public record Cost(Tier to, int shard, Item metal, int metalCount, int dust, int core) {
        public int need(int slot) {
            return switch (slot) {
                case SHARD -> shard;
                case METAL -> metalCount;
                case DUST -> dust;
                case CORE -> core;
                default -> 0;
            };
        }
    }

    public boolean hasMaterial(Cost cost, int slot) {
        int need = cost.need(slot);
        if (need <= 0) return true;
        ItemStack s = container.getItem(slot);
        if (slot == METAL && !s.is(cost.metal())) return false;
        return s.getCount() >= need;
    }

    /** Why Reforge can't run now (null = it can). */
    public Component blocked() {
        ItemStack gear = gear();
        if (gear.isEmpty()) return Component.translatable("screen.forgebound.anvil.place");
        Cost cost = cost();
        if (cost == null) return Component.translatable("screen.forgebound.anvil.max");
        if (cost.to().level() > cap()) {
            return blacksmith() ? Component.translatable("screen.forgebound.anvil.rank_cap", Tier.of(cap()).roman())
                    : Component.translatable("screen.forgebound.anvil.non_smith_cap", Tier.of(cap()).roman());
        }
        for (int slot = SHARD; slot <= CORE; slot++) {
            if (!hasMaterial(cost, slot)) return Component.translatable("screen.forgebound.anvil.materials");
        }
        return null;
    }

    public static final int BUTTON_REROLL_TAB = 101;

    /** Opens the Reforge tab (the anvil's default). */
    public static net.minecraft.world.MenuProvider provider(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new ForgeAnvilMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.forgebound.forge_anvil"));
    }

    /** Opens the Reroll tab. */
    public static net.minecraft.world.MenuProvider rerollProvider(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new RerollMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.forgebound.forge_anvil"));
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id == BUTTON_REROLL_TAB && p instanceof ServerPlayer sp) {
            access.execute((level, pos) -> sp.openMenu(rerollProvider(level, pos)));
            return true;
        }
        if (id != 0 || !(p instanceof ServerPlayer)) return false;
        refresh();
        if (blocked() != null) return false;
        Cost cost = cost();
        for (int slot = SHARD; slot <= CORE; slot++) {
            int need = cost.need(slot);
            if (need > 0) container.removeItem(slot, need);
        }
        ItemStack gear = container.getItem(GEAR);
        Tiers.set(gear, cost.to());
        container.setItem(GEAR, gear);
        access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.9F + level.random.nextFloat() * 0.2F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        });
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
