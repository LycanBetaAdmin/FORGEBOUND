package com.forgebound.tier;

import com.forgebound.Forgebound;
import com.forgebound.ForgeboundConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a tier does: {@code tier_power} more of the item's own attack damage, armor and armor toughness (as extra
 * modifiers in the same slot), faster mining with a tiered tool, and stronger arrows from a tiered bow or crossbow.
 * Durability is scaled when the tier is set ({@link Tiers#set}). Also, on players' inventories: converts items tiered
 * by the removed RPG Items Tiers mod and gives boss weapons their starting tier.
 */
public final class TierEffects {
    private static final List<Holder<Attribute>> SCALED = List.of(Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS);

    private TierEffects() {
    }

    private static ResourceLocation id(Holder<Attribute> attr, EquipmentSlotGroup slot) {
        String a = attr.unwrapKey().map(k -> k.location().getPath()).orElse("attr").replace('.', '_');
        return ResourceLocation.fromNamespaceAndPath(Forgebound.MODID, "tier_" + a + "_" + slot.getSerializedName());
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!Tiers.hasTier(stack)) return;
        double power = ForgeboundConfig.get().power(Tiers.get(stack).level());
        if (power <= 0) return;
        // sum the item's own flat bonuses per (attribute, slot) and add the tier's share of them
        Map<Holder<Attribute>, Map<EquipmentSlotGroup, Double>> sums = new LinkedHashMap<>();
        for (ItemAttributeModifiers.Entry e : event.getDefaultModifiers().modifiers()) {
            if (!SCALED.contains(e.attribute()) || e.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) continue;
            if (e.modifier().id().getNamespace().equals(Forgebound.MODID)) continue;
            sums.computeIfAbsent(e.attribute(), k -> new LinkedHashMap<>()).merge(e.slot(), e.modifier().amount(), Double::sum);
        }
        sums.forEach((attr, bySlot) -> bySlot.forEach((slot, amount) -> {
            if (amount > 0) {
                event.addModifier(attr, new AttributeModifier(id(attr, slot), amount * power, AttributeModifier.Operation.ADD_VALUE), slot);
            }
        }));
    }

    /** Tiered tools dig faster at blocks they are right for. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (!Tiers.hasTier(tool) || !tool.isCorrectToolForDrops(event.getState())) return;
        double power = ForgeboundConfig.get().power(Tiers.get(tool).level());
        if (power > 0) event.setNewSpeed((float) (event.getNewSpeed() * (1 + power)));
    }

    /** Arrows (and bolts) from a tiered bow or crossbow hit harder. */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || !Tiers.hasTier(weapon)) return;
        double power = ForgeboundConfig.get().power(Tiers.get(weapon).level());
        if (power > 0) arrow.setBaseDamage(arrow.getBaseDamage() * (1 + power));
    }

    /** Every 2 seconds: convert old RPG Items Tiers items and give boss weapons their starting tier. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 40 != 0) return;
        Inventory inv = player.getInventory();
        for (List<ItemStack> list : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack stack : list) {
                if (stack.isEmpty()) continue;
                LegacyTiers.convert(stack);
                if (!Tiers.hasTier(stack)) {
                    Tier start = Tiers.startTier(stack);
                    if (start != null) Tiers.set(stack, start);
                }
            }
        }
    }
}
