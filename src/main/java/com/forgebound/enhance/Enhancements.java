package com.forgebound.enhance;

import com.forgebound.Forgebound;
import com.forgebound.ForgeboundConfig;
import com.forgebound.tier.GearType;
import com.forgebound.tier.Tier;
import com.forgebound.tier.Tiers;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhancements on an item ({@code forgebound:enhancements}, a list of ids). How many fit depends on the tier
 * ({@code enhancement_slots}); each enhancement is an attribute modifier while the item is used in its slot.
 */
public final class Enhancements {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Forgebound.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> ENHANCEMENTS = COMPONENTS.registerComponentType("enhancements",
            b -> b.persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16))));

    private Enhancements() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    public static Enhancement.Kind kind(ItemStack stack) {
        return Enhancement.Kind.of(GearType.of(stack));
    }

    public static int slots(Tier tier) {
        int[] s = ForgeboundConfig.get().enhancementSlots;
        int i = tier.level() - 1;
        return s != null && i < s.length ? s[i] : 1;
    }

    public static List<Enhancement> get(ItemStack stack) {
        List<String> ids = stack.get(ENHANCEMENTS.get());
        if (ids == null || ids.isEmpty()) return List.of();
        List<Enhancement> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            Enhancement e = Enhancement.byId(id);
            if (e != null) out.add(e);
        }
        return out;
    }

    public static void set(ItemStack stack, List<Enhancement> list) {
        if (list.isEmpty()) stack.remove(ENHANCEMENTS.get());
        else stack.set(ENHANCEMENTS.get(), list.stream().map(Enhancement::id).toList());
    }

    public static int free(ItemStack stack) {
        return Math.max(0, slots(Tiers.get(stack)) - get(stack).size());
    }

    // ---- costs (config)

    public static int applyShards(Tier tier) {
        return ForgeboundConfig.get().enhanceShardsPerTier * tier.level();
    }

    public static int applyLevels(Tier tier) {
        return ForgeboundConfig.get().enhanceLevelsBase + tier.level();
    }

    public static int removeShards(Tier tier) {
        return Math.max(1, applyShards(tier) / 2);
    }

    // ---- the bonuses

    private static EquipmentSlotGroup group(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armor) return EquipmentSlotGroup.bySlot(armor.getEquipmentSlot());
        GearType type = GearType.of(stack);
        if (type == GearType.ARMOR) return EquipmentSlotGroup.CHEST; // elytra
        if (type == GearType.SHIELD) return EquipmentSlotGroup.HAND;
        return EquipmentSlotGroup.MAINHAND;
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.has(ENHANCEMENTS.get())) return;
        EquipmentSlotGroup slot = group(stack);
        for (Enhancement e : get(stack)) {
            e.holder().ifPresent(h -> event.addModifier(h,
                    new AttributeModifier(Forgebound.id("enh_" + e.id()), e.amount, e.operation), slot));
        }
    }
}
