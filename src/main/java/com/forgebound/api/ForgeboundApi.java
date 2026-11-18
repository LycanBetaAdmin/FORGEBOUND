package com.forgebound.api;

import com.forgebound.ForgeboundConfig;
import com.forgebound.tier.GearType;
import com.forgebound.tier.Tier;
import com.forgebound.tier.Tiers;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Calls for other mods (Eldoria RPG reaches these through reflection, so neither mod needs the other to compile).
 * Keep the signatures stable.
 */
public final class ForgeboundApi {
    private ForgeboundApi() {
    }

    /** "weapon", "ranged", "armor", "shield", "tool", or "" for anything that isn't equipment. */
    public static String gearType(ItemStack stack) {
        GearType t = GearType.of(stack);
        return t == null ? "" : t.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The item's tier, 1..7 (gear without one is 1). */
    public static int tier(ItemStack stack) {
        return Tiers.get(stack).level();
    }

    /**
     * Gives a shop item its tier: random from {@code shop_min_tier} up to T7, weighted like chest loot (so the lowest
     * allowed tier is the most common). Returns the tier set, or 0 for items that aren't equipment.
     */
    public static int rollShopTier(ItemStack stack, RandomSource random) {
        if (!GearType.isGear(stack)) return 0;
        ForgeboundConfig cfg = ForgeboundConfig.get();
        int min = Math.max(Tier.MIN, Math.min(Tier.MAX, cfg.shopMinTier));
        double total = 0;
        for (int t = min; t <= Tier.MAX; t++) total += Math.max(0, cfg.lootTierWeights[t - 1]);
        Tier pick = Tier.of(min);
        if (total > 0) {
            double r = random.nextDouble() * total;
            for (int t = min; t <= Tier.MAX; t++) {
                r -= Math.max(0, cfg.lootTierWeights[t - 1]);
                if (r < 0) {
                    pick = Tier.of(t);
                    break;
                }
            }
        }
        Tiers.set(stack, pick);
        return pick.level();
    }
}
