package com.forgebound.tier;

import com.forgebound.Forgebound;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FORGEBOUND replaces RPG Items Tiers ({@code legendarytiers}). Items that mod rolled carry its data components;
 * without the mod those items would fail to load and vanish. When it is not installed, this registers the same
 * component ids with a codec that accepts anything, so the items load, and {@link #convert} turns the old rarity into
 * a FORGEBOUND tier: Common T1, Rare T3, Epic T4, Legendary T5, Mythic T6, Divine T7.
 */
public final class LegacyTiers {
    private static final String NS = "legendarytiers";
    private static final Codec<Tag> ANY = Codec.PASSTHROUGH.xmap(d -> d.convert(NbtOps.INSTANCE).getValue(), t -> new Dynamic<>(NbtOps.INSTANCE, t));
    private static final List<DeferredHolder<DataComponentType<?>, DataComponentType<Tag>>> OLD = new ArrayList<>();
    private static DeferredHolder<DataComponentType<?>, DataComponentType<Tag>> tierData;

    private LegacyTiers() {
    }

    public static void register(IEventBus modBus) {
        if (ModList.get().isLoaded(NS)) return;
        DeferredRegister.DataComponents reg = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NS);
        for (String id : List.of("tier_data", "experience", "original_item_id", "reforge_attempts")) {
            DeferredHolder<DataComponentType<?>, DataComponentType<Tag>> holder = reg.<Tag>registerComponentType(id, b -> b.persistent(ANY).networkSynchronized(ByteBufCodecs.TAG));
            OLD.add(holder);
            if (id.equals("tier_data")) tierData = holder;
        }
        reg.register(modBus);
        Forgebound.LOGGER.info("[FORGEBOUND] RPG Items Tiers is not installed: its items will be converted to FORGEBOUND tiers");
    }

    /** Converts an old RPG Items Tiers item in place (no-op otherwise). */
    public static void convert(ItemStack stack) {
        if (tierData == null || OLD.isEmpty()) return;
        boolean old = false;
        for (var h : OLD) old |= stack.has(h.get());
        if (!old) return;
        Tag data = stack.get(tierData.get());
        for (var h : OLD) stack.remove(h.get());
        if (Tiers.hasTier(stack) || !GearType.isGear(stack)) return;
        Tiers.set(stack, fromRarity(rarity(data)));
    }

    private static String rarity(Tag data) {
        if (data instanceof CompoundTag c && c.contains("rarity")) return c.getString("rarity");
        if (data instanceof StringTag s) return s.getAsString();
        return "";
    }

    static Tier fromRarity(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "rare" -> Tier.T3;
            case "epic" -> Tier.T4;
            case "legendary" -> Tier.T5;
            case "mythic" -> Tier.T6;
            case "divine" -> Tier.T7;
            default -> Tier.T1;
        };
    }
}
