package com.forgebound.tier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** What kind of equipment an item is (null = not equipment). Works for any mod's gear. */
public enum GearType {
    WEAPON, RANGED, ARMOR, SHIELD, TOOL;

    public static GearType of(ItemStack stack) {
        return stack.isEmpty() ? null : of(stack.getItem());
    }

    public static GearType of(Item item) {
        if (item instanceof ArmorItem || item instanceof ElytraItem) return ARMOR;
        if (item instanceof ShieldItem) return SHIELD;
        if (item instanceof ProjectileWeaponItem) return RANGED;
        if (item instanceof SwordItem || item instanceof TridentItem || item instanceof MaceItem) return WEAPON;
        if (item instanceof DiggerItem) return TOOL;
        if (!item.components().has(DataComponents.MAX_DAMAGE)) return null;
        // other damageable items: a weapon if it carries attack damage, otherwise a tool (shears, fishing rods ...)
        ItemAttributeModifiers mods = item.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute().equals(Attributes.ATTACK_DAMAGE) && e.modifier().amount() > 0) return WEAPON;
        }
        return TOOL;
    }

    public static boolean isGear(ItemStack stack) {
        return of(stack) != null;
    }

    public boolean offensive() {
        return this == WEAPON || this == RANGED;
    }

    public boolean defensive() {
        return this == ARMOR || this == SHIELD;
    }
}
