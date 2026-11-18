package com.forgebound.tier;

import com.forgebound.Forgebound;
import com.forgebound.ForgeboundConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


/**
 * The tier stored on a piece of equipment ({@code forgebound:tier}, 1..7). Gear without it is T1. Setting a tier
 * also scales the item's max durability from its base value; every other component (Apotheosis affixes, gems,
 * enchantments, names) is left as it is.
 */
public final class Tiers {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Forgebound.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TIER = COMPONENTS.registerComponentType("tier",
            b -> b.persistent(Codec.intRange(Tier.MIN, Tier.MAX)).networkSynchronized(ByteBufCodecs.VAR_INT));

    private Tiers() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    public static Tier get(ItemStack stack) {
        return Tier.of(stack.getOrDefault(TIER.get(), Tier.MIN));
    }

    public static boolean hasTier(ItemStack stack) {
        return stack.has(TIER.get());
    }

    /** Sets the tier and rescales max durability (damage taken so far is kept). */
    public static void set(ItemStack stack, Tier tier) {
        stack.set(TIER.get(), tier.level());
        int base = stack.getItem().components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
        if (base > 0) {
            int max = Math.max(1, (int) Math.round(base * ForgeboundConfig.get().durability(tier.level())));
            stack.set(DataComponents.MAX_DAMAGE, max);
            if (stack.getDamageValue() >= max) stack.setDamageValue(max - 1);
        }
    }

    /** Max durability at a tier (for previews). */
    public static int maxDurability(ItemStack stack, Tier tier) {
        int base = stack.getItem().components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
        return base <= 0 ? 0 : Math.max(1, (int) Math.round(base * ForgeboundConfig.get().durability(tier.level())));
    }

    /** Tier this item starts at because of where it comes from (start_tiers by mod id), or null. */
    public static Tier startTier(ItemStack stack) {
        GearType type = GearType.of(stack);
        if (type == null || !type.offensive()) return null;
        String ns = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        Integer t = ForgeboundConfig.get().startTiers.get(ns);
        return t == null ? null : Tier.of(t);
    }

    /** A freshly crafted piece of gear (any crafting station): its starting tier. Server side. */
    public static void onCrafted(ItemStack stack, Level level, Player player) {
        if (level == null || level.isClientSide() || stack.isEmpty() || hasTier(stack) || !GearType.isGear(stack)) return;
        Tier start = startTier(stack);
        set(stack, start != null ? start : Blacksmith.rollCrafted(player, stack, level.random));
    }

    /** A random tier with the configured loot weights. */
    public static Tier rollLoot(RandomSource random) {
        double[] w = ForgeboundConfig.get().lootTierWeights;
        double total = 0;
        for (int i = 0; i < Tier.MAX; i++) total += Math.max(0, w[i]);
        double r = random.nextDouble() * total;
        for (int i = 0; i < Tier.MAX; i++) {
            r -= Math.max(0, w[i]);
            if (r < 0) return Tier.of(i + 1);
        }
        return Tier.T1;
    }

    /** Gives looted gear a random tier unless it already has one. */
    public static void onLooted(ItemStack stack, RandomSource random) {
        if (stack.isEmpty() || hasTier(stack) || !GearType.isGear(stack)) return;
        Tier start = startTier(stack);
        set(stack, start != null ? start : rollLoot(random));
    }

}
