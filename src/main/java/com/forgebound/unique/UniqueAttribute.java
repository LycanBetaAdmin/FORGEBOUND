package com.forgebound.unique;

import com.forgebound.tier.GearType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Unique Attributes: one random special effect per item (two from T6), rerolled at the Forge Anvil. Each has a
 * base value (its strength at T1, scaled up with the tier: {@link Uniques#scale}), a category (weapon / armor /
 * tool) and a pool: BASE (any tier), RARE (T3+) and TOP (T5+). Effects live in {@link UniqueEffects}.
 */
public enum UniqueAttribute {
    // ---- weapons: offensive effects
    SOUL_REND(Category.WEAPON, Pool.TOP, 0.08),
    BLOODTHIRSTY(Category.WEAPON, Pool.BASE, 0.05),
    EXECUTIONER(Category.WEAPON, Pool.RARE, 0.15),
    TITAN_SLAYER(Category.WEAPON, Pool.TOP, 0.15),
    KILLER_EDGE(Category.WEAPON, Pool.RARE, 0.12),
    FORTUNES_EDGE(Category.WEAPON, Pool.BASE, 0.05),
    DEVASTATOR(Category.WEAPON, Pool.BASE, 0.15),
    PHANTOM_STRIKE(Category.WEAPON, Pool.RARE, 0.10),
    COMBO_MASTER(Category.WEAPON, Pool.RARE, 0.50),
    SOUL_HARVEST(Category.WEAPON, Pool.BASE, 0.03),
    OVERCHARGE(Category.WEAPON, Pool.TOP, 0.05),
    ARMOR_BREAKER(Category.WEAPON, Pool.RARE, 0.05),
    MANA_REAVER(Category.WEAPON, Pool.BASE, 0.02),
    // ---- armor (and shields): defence, survival, recovery
    UNBREAKABLE(Category.ARMOR, Pool.BASE, 0.15),
    GUARDIAN_SOUL(Category.ARMOR, Pool.RARE, 0.05),
    SECOND_CHANCE(Category.ARMOR, Pool.TOP, 0.20),
    IRON_WILL(Category.ARMOR, Pool.BASE, 0.30),
    REGENERATION(Category.ARMOR, Pool.BASE, 0.01),
    FORTIFIED(Category.ARMOR, Pool.BASE, 0.08),
    VAMPIRIC_BARRIER(Category.ARMOR, Pool.RARE, 0.05),
    LAST_STAND(Category.ARMOR, Pool.RARE, 0.15),
    PURIFICATION(Category.ARMOR, Pool.BASE, 0.10),
    GUARDIANS_RESOLVE(Category.ARMOR, Pool.TOP, 0.10),
    THORNED_ARMOR(Category.ARMOR, Pool.RARE, 0.10),
    // ---- tools: mining, harvesting, durability
    FORTUNE_SEEKER(Category.TOOL, Pool.TOP, 0.05),
    DEEP_MINER(Category.TOOL, Pool.BASE, 0.15),
    EARTH_BREAKER(Category.TOOL, Pool.RARE, 0.10),
    TREASURE_HUNTER(Category.TOOL, Pool.RARE, 0.05),
    DURABLE_CRAFT(Category.TOOL, Pool.BASE, 0.20),
    LUMBERJACK_SOUL(Category.TOOL, Pool.BASE, 0.15),
    BOTANICAL_TOUCH(Category.TOOL, Pool.RARE, 0.10),
    EXCAVATOR(Category.TOOL, Pool.BASE, 0.15),
    GEM_FINDER(Category.TOOL, Pool.TOP, 0.08),
    EFFICIENT_HARVEST(Category.TOOL, Pool.BASE, 0.10);

    public enum Category {
        WEAPON, ARMOR, TOOL;

        public static Category of(GearType type) {
            if (type == null) return null;
            return switch (type) {
                case WEAPON, RANGED -> WEAPON;
                case ARMOR, SHIELD -> ARMOR;
                case TOOL -> TOOL;
            };
        }
    }

    public enum Pool {
        BASE(1), RARE(3), TOP(5);

        /** Lowest tier whose rerolls can draw from this pool. */
        public final int minTier;

        Pool(int minTier) {
            this.minTier = minTier;
        }
    }

    private static final Map<String, UniqueAttribute> BY_ID = new LinkedHashMap<>();

    static {
        for (UniqueAttribute a : values()) BY_ID.put(a.id(), a);
    }

    public final Category category;
    public final Pool pool;
    public final double base;

    UniqueAttribute(Category category, Pool pool, double base) {
        this.category = category;
        this.pool = pool;
        this.base = base;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static UniqueAttribute byId(String id) {
        return BY_ID.get(id);
    }

    public static List<UniqueAttribute> of(Category category, Pool pool) {
        List<UniqueAttribute> out = new ArrayList<>();
        for (UniqueAttribute a : values()) if (a.category == category && a.pool == pool) out.add(a);
        return Collections.unmodifiableList(out);
    }

    /** "Soul Rend" in gold. */
    public MutableComponent displayName() {
        return Component.translatable("unique.forgebound." + id()).withColor(0xFFD15C);
    }

    /** The effect at a tier, e.g. "8.8% chance to ignore 50% of enemy armor." */
    public MutableComponent description(int tier) {
        double v = base * Uniques.scale(tier) * 100;
        String value = (Math.abs(v - Math.rint(v)) < 0.05 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.1f", v)) + "%";
        return Component.translatable("unique.forgebound." + id() + ".desc", value).withStyle(ChatFormatting.GRAY);
    }
}
