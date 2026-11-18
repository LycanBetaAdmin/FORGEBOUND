package com.forgebound.enhance;

import com.forgebound.tier.GearType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Enhancements engraved at the Rune Engraving Table: fixed bonuses the player chooses (unlike Unique Attributes,
 * which are random). Each is an attribute modifier carried by the item; the ones whose attribute isn't installed
 * (Apothic Attributes) are not offered.
 */
public enum Enhancement {
    // ---- melee weapons
    SHARPENED(Kind.WEAPON, "minecraft:generic.attack_damage", 0.06, Operation.ADD_MULTIPLIED_TOTAL, true),
    SWIFT(Kind.WEAPON, "minecraft:generic.attack_speed", 0.06, Operation.ADD_MULTIPLIED_TOTAL, true),
    KEEN(Kind.WEAPON, "apothic_attributes:crit_chance", 0.04, Operation.ADD_VALUE, true),
    BRUTAL(Kind.WEAPON, "apothic_attributes:crit_damage", 0.10, Operation.ADD_VALUE, true),
    LEECHING(Kind.WEAPON, "apothic_attributes:life_steal", 0.03, Operation.ADD_VALUE, true),
    SEARING(Kind.WEAPON, "apothic_attributes:fire_damage", 1.5, Operation.ADD_VALUE, false),
    PIERCING(Kind.WEAPON, "apothic_attributes:armor_pierce", 2.0, Operation.ADD_VALUE, false),
    // ---- bows and crossbows
    TRUE_SHOT(Kind.RANGED, "apothic_attributes:arrow_damage", 0.08, Operation.ADD_VALUE, true),
    QUICKDRAW(Kind.RANGED, "apothic_attributes:draw_speed", 0.08, Operation.ADD_VALUE, true),
    EAGLE_EYE(Kind.RANGED, "apothic_attributes:crit_chance", 0.04, Operation.ADD_VALUE, true),
    DEADLY(Kind.RANGED, "apothic_attributes:crit_damage", 0.10, Operation.ADD_VALUE, true),
    // ---- armor and shields
    REINFORCED(Kind.ARMOR, "minecraft:generic.armor", 1.5, Operation.ADD_VALUE, false),
    HARDENED(Kind.ARMOR, "minecraft:generic.armor_toughness", 1.0, Operation.ADD_VALUE, false),
    VITAL(Kind.ARMOR, "minecraft:generic.max_health", 2.0, Operation.ADD_VALUE, false),
    STEADFAST(Kind.ARMOR, "minecraft:generic.knockback_resistance", 0.05, Operation.ADD_VALUE, true),
    FLEET(Kind.ARMOR, "minecraft:generic.movement_speed", 0.03, Operation.ADD_MULTIPLIED_BASE, true),
    EVASIVE(Kind.ARMOR, "apothic_attributes:dodge_chance", 0.02, Operation.ADD_VALUE, true),
    RESTORING(Kind.ARMOR, "apothic_attributes:healing_received", 0.05, Operation.ADD_VALUE, true),
    // ---- tools
    EFFICIENT(Kind.TOOL, "minecraft:player.block_break_speed", 0.10, Operation.ADD_MULTIPLIED_BASE, true),
    LUCKY(Kind.TOOL, "minecraft:generic.luck", 1.0, Operation.ADD_VALUE, false),
    REACHING(Kind.TOOL, "minecraft:player.block_interaction_range", 0.5, Operation.ADD_VALUE, false),
    INSIGHTFUL(Kind.TOOL, "apothic_attributes:experience_gained", 0.10, Operation.ADD_VALUE, true);

    /** Which gear can take it. Shields count as armor. */
    public enum Kind {
        WEAPON, RANGED, ARMOR, TOOL;

        public static Kind of(GearType type) {
            if (type == null) return null;
            return switch (type) {
                case WEAPON -> WEAPON;
                case RANGED -> RANGED;
                case ARMOR, SHIELD -> ARMOR;
                case TOOL -> TOOL;
            };
        }
    }

    private static final Map<String, Enhancement> BY_ID = new LinkedHashMap<>();

    static {
        for (Enhancement e : values()) BY_ID.put(e.id(), e);
    }

    public final Kind kind;
    public final ResourceLocation attribute;
    public final double amount;
    public final Operation operation;
    /** Shown as a percentage. */
    public final boolean percent;

    Enhancement(Kind kind, String attribute, double amount, Operation operation, boolean percent) {
        this.kind = kind;
        this.attribute = ResourceLocation.parse(attribute);
        this.amount = amount;
        this.operation = operation;
        this.percent = percent;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Enhancement byId(String id) {
        return BY_ID.get(id);
    }

    public Optional<Holder.Reference<Attribute>> holder() {
        return BuiltInRegistries.ATTRIBUTE.getHolder(attribute);
    }

    /** Offered only when its attribute exists in this pack. */
    public boolean available() {
        return holder().isPresent();
    }

    public static List<Enhancement> forKind(Kind kind) {
        List<Enhancement> out = new ArrayList<>();
        for (Enhancement e : values()) if (e.kind == kind && e.available()) out.add(e);
        return out;
    }

    /** "+6%" / "+1.5". */
    public String value() {
        double v = percent ? amount * 100 : amount;
        String n = Math.abs(v - Math.rint(v)) < 1e-6 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.1f", v);
        return "+" + n + (percent ? "%" : "");
    }

    /** "Attack Damage". */
    public MutableComponent displayName() {
        return Component.translatable("enhancement.forgebound." + id());
    }

    /** "+6% Attack Damage" in green. */
    public MutableComponent line() {
        return Component.literal(value() + " ").append(displayName()).withColor(0x80FF20);
    }
}
