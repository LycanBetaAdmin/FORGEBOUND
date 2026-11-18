package com.forgebound.tier;

import com.forgebound.Forgebound;
import com.forgebound.ForgeboundConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.List;

/**
 * The Eldoria RPG Blacksmith job (read through reflection, so FORGEBOUND works without Eldoria RPG: then nobody is a
 * Blacksmith).
 * <ul>
 *   <li>Crafting: non-Blacksmiths always craft T1. A Blacksmith rolls up to their rank: Beginner T1, Apprentice T2,
 *       Journeyman T3, Expert T4, Master T5, and T6 for the gear their specialization covers (Master Blacksmith: all;
 *       Weaponmaster: weapons and ranged; Armorer: armor and shields; Toolsmith: tools). Higher tiers are rarer.</li>
 *   <li>Forge Anvil: anyone can upgrade, but non-Blacksmiths pay {@code non_blacksmith_cost_multiplier} times the
 *       materials and stop at {@code non_blacksmith_max_tier} (T5). Blacksmiths pay the normal cost and reach T5, a
 *       Master T6, and T7 with a specialization for that gear.</li>
 * </ul>
 */
public final class Blacksmith {
    private static final String JOB = "blacksmith";
    private static boolean looked, warned;
    private static Method jobM, specsM, tierM;

    private Blacksmith() {
    }

    private static boolean ready() {
        if (!looked) {
            looked = true;
            if (ModList.get().isLoaded("eldoria_rpg")) {
                try {
                    Class<?> c = Class.forName("com.eldoria.rpg.JobRules");
                    jobM = c.getMethod("job", Player.class);
                    specsM = c.getMethod("specs", Player.class);
                    tierM = c.getMethod("tier", Player.class, String.class);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    warn(e);
                    jobM = null;
                }
            }
        }
        return jobM != null;
    }

    private static void warn(Throwable t) {
        if (!warned) {
            warned = true;
            Forgebound.LOGGER.warn("[FORGEBOUND] Eldoria RPG Blacksmith link unavailable: {}", t.toString());
        }
    }

    public static boolean isBlacksmith(Player player) {
        if (player == null || !ready()) return false;
        try {
            return JOB.equals(jobM.invoke(null, player));
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
            return false;
        }
    }

    /** Blacksmith rank 0..5 (none, Beginner .. Master). Server side only (it reads the job tree tags). */
    public static int rank(Player player) {
        if (player == null || !ready()) return 0;
        try {
            return (Integer) tierM.invoke(null, player, JOB);
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
            return 0;
        }
    }

    /** True when one of the player's specializations covers this kind of gear. */
    public static boolean specialized(Player player, GearType type) {
        if (player == null || type == null || !ready()) return false;
        try {
            @SuppressWarnings("unchecked")
            List<String> specs = (List<String>) specsM.invoke(null, player);
            for (String s : specs) {
                boolean covers = switch (s) {
                    case "master_blacksmith" -> true;
                    case "weaponmaster" -> type.offensive();
                    case "armorer" -> type.defensive();
                    case "toolsmith" -> type == GearType.TOOL;
                    default -> false;
                };
                if (covers) return true;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
        return false;
    }

    /** Highest tier this player can craft this item at. */
    public static int craftCap(Player player, ItemStack stack) {
        if (!isBlacksmith(player)) return Tier.MIN;
        if (specialized(player, GearType.of(stack))) return 6;
        return Math.max(Tier.MIN, Math.min(5, rank(player)));
    }

    /** A crafted item's tier: weighted towards the low end of what the crafter can make (the cap is the rarest). */
    public static Tier rollCrafted(Player player, ItemStack stack, RandomSource random) {
        int cap = craftCap(player, stack);
        if (cap <= 1) return Tier.T1;
        int[] w = new int[cap];
        int total = 0;
        for (int t = 1; t <= cap; t++) {
            int below = cap - t; // 0 for the cap itself
            w[t - 1] = Math.min(4, below + 1);
            total += w[t - 1];
        }
        int r = random.nextInt(total);
        for (int t = 1; t <= cap; t++) {
            r -= w[t - 1];
            if (r < 0) return Tier.of(t);
        }
        return Tier.T1;
    }

    /** Highest tier this player can reforge this item to at the Forge Anvil. Server side. */
    public static int anvilCap(Player player, ItemStack stack) {
        int anyone = Math.max(Tier.MIN, Math.min(Tier.MAX, ForgeboundConfig.get().nonBlacksmithMaxTier));
        if (!isBlacksmith(player)) return anyone;
        if (specialized(player, GearType.of(stack))) return Tier.MAX;
        return Math.max(anyone, rank(player) >= 5 ? 6 : 5);
    }

    /** Material multiplier at the Forge Anvil. */
    public static int costMultiplier(Player player) {
        return isBlacksmith(player) ? 1 : Math.max(1, ForgeboundConfig.get().nonBlacksmithCostMultiplier);
    }
}
