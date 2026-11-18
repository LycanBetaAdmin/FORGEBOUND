package com.forgebound.tier;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** The seven FORGEBOUND equipment tiers. Gear without a tier counts as {@link #T1}. */
public enum Tier {
    T1(1, "I", "common", 0xC0C0C8),
    T2(2, "II", "uncommon", 0x55FF55),
    T3(3, "III", "rare", 0x55AAFF),
    T4(4, "IV", "epic", 0xAA55FF),
    T5(5, "V", "legendary", 0xFFAA00),
    T6(6, "VI", "mythical_divine", 0xFF5555),
    T7(7, "VII", "relic", 0xFF55FF);

    public static final int MIN = 1, MAX = 7;

    private final int level;
    private final String roman;
    private final String key;
    private final int color;

    Tier(int level, String roman, String key, int color) {
        this.level = level;
        this.roman = roman;
        this.key = key;
        this.color = color;
    }

    public int level() {
        return level;
    }

    public String roman() {
        return roman;
    }

    public int color() {
        return color;
    }

    /** "Legendary" etc. */
    public MutableComponent displayName() {
        return Component.translatable("tier.forgebound." + key).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    /** "Tier V · Legendary" in the tier colour. */
    public MutableComponent label() {
        return Component.translatable("tooltip.forgebound.tier", roman, Component.translatable("tier.forgebound." + key))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    public static Tier of(int level) {
        return values()[Math.max(MIN, Math.min(MAX, level)) - 1];
    }

    public Tier next() {
        return level >= MAX ? this : of(level + 1);
    }
}
