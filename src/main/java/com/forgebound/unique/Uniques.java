package com.forgebound.unique;

import com.forgebound.Forgebound;
import com.forgebound.ForgeboundConfig;
import com.forgebound.tier.GearType;
import com.forgebound.tier.Tier;
import com.forgebound.tier.Tiers;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * The Unique Attributes on an item ({@code forgebound:unique}: up to two attribute ids). Items start without one;
 * the Forge Anvil's Reroll rolls them. One slot below T6, two at T6 and T7.
 */
public final class Uniques {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Forgebound.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> UNIQUE = COMPONENTS.registerComponentType("unique",
            b -> b.persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(2))));

    private Uniques() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    /** Strength multiplier at a tier: 1 at T1, +unique_scale_per_tier per tier above. */
    public static double scale(int tier) {
        return 1 + ForgeboundConfig.get().uniqueScalePerTier * (Math.max(1, tier) - 1);
    }

    /** How many Unique Attributes an item of this tier can hold. */
    public static int slots(Tier tier) {
        return tier.level() >= 6 ? 2 : 1;
    }

    public static List<UniqueAttribute> get(ItemStack stack) {
        List<String> ids = stack.get(UNIQUE.get());
        if (ids == null || ids.isEmpty()) return List.of();
        List<UniqueAttribute> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            UniqueAttribute a = UniqueAttribute.byId(id);
            if (a != null) out.add(a);
        }
        return out;
    }

    public static void set(ItemStack stack, List<UniqueAttribute> attrs) {
        if (attrs.isEmpty()) {
            stack.remove(UNIQUE.get());
            return;
        }
        stack.set(UNIQUE.get(), attrs.stream().map(UniqueAttribute::id).toList());
    }

    /** The item's value for this attribute (base x tier scale), or 0 if it doesn't carry it. */
    public static double value(ItemStack stack, UniqueAttribute attr) {
        if (stack.isEmpty() || !stack.has(UNIQUE.get())) return 0;
        for (UniqueAttribute a : get(stack)) {
            if (a == attr) return attr.base * scale(Tiers.get(stack).level());
        }
        return 0;
    }

    /**
     * Total value of an attribute on what this entity is using: weapons and tools count in the main hand, armor in the
     * armor slots, shields in either hand.
     */
    public static double level(LivingEntity entity, UniqueAttribute attr) {
        if (entity == null) return 0;
        double total = 0;
        switch (attr.category) {
            case WEAPON, TOOL -> total += value(entity.getMainHandItem(), attr);
            case ARMOR -> {
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    total += value(entity.getItemBySlot(slot), attr);
                }
                for (ItemStack hand : new ItemStack[]{entity.getMainHandItem(), entity.getOffhandItem()}) {
                    if (GearType.of(hand) == GearType.SHIELD) total += value(hand, attr);
                }
            }
        }
        return total;
    }

    /**
     * Rerolls the item's Unique Attributes: every slot not in {@code sealed} gets a new attribute (never the one it had,
     * never a duplicate). {@code premium} (Fortune Crystal) skips the base pool when a better one is open.
     * Returns false for items that can't hold Unique Attributes.
     */
    public static boolean reroll(ItemStack stack, RandomSource random, boolean premium, boolean[] sealed) {
        UniqueAttribute.Category cat = UniqueAttribute.Category.of(GearType.of(stack));
        if (cat == null) return false;
        Tier tier = Tiers.get(stack);
        int n = slots(tier);
        List<UniqueAttribute> cur = new ArrayList<>(get(stack));
        while (cur.size() > n) cur.remove(cur.size() - 1);
        List<UniqueAttribute> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            boolean keep = i < cur.size() && sealed != null && i < sealed.length && sealed[i];
            out.add(keep ? cur.get(i) : null);
        }
        for (int i = 0; i < n; i++) {
            if (out.get(i) != null) continue;
            UniqueAttribute previous = i < cur.size() ? cur.get(i) : null;
            UniqueAttribute pick = pick(cat, tier, premium, random, out, previous);
            if (pick == null) pick = pick(cat, tier, premium, random, out, null); // tiny pools: allow the same one again
            out.set(i, pick);
        }
        out.removeIf(java.util.Objects::isNull);
        set(stack, out);
        return true;
    }

    private static UniqueAttribute pick(UniqueAttribute.Category cat, Tier tier, boolean premium, RandomSource random,
                                        List<UniqueAttribute> taken, UniqueAttribute previous) {
        double[] w = ForgeboundConfig.get().poolWeights;
        UniqueAttribute.Pool[] pools = UniqueAttribute.Pool.values();
        List<List<UniqueAttribute>> candidates = new ArrayList<>();
        double total = 0;
        double[] weights = new double[pools.length];
        boolean betterOpen = tier.level() >= UniqueAttribute.Pool.RARE.minTier;
        for (int p = 0; p < pools.length; p++) {
            List<UniqueAttribute> list = new ArrayList<>();
            boolean open = tier.level() >= pools[p].minTier && !(premium && betterOpen && pools[p] == UniqueAttribute.Pool.BASE);
            if (open) {
                for (UniqueAttribute a : UniqueAttribute.of(cat, pools[p])) if (!taken.contains(a) && a != previous) list.add(a);
            }
            candidates.add(list);
            weights[p] = list.isEmpty() ? 0 : Math.max(0, w[p]);
            total += weights[p];
        }
        if (total <= 0) return null;
        double r = random.nextDouble() * total;
        for (int p = 0; p < pools.length; p++) {
            r -= weights[p];
            if (r < 0 || p == pools.length - 1 && weights[p] > 0) {
                List<UniqueAttribute> list = candidates.get(p);
                if (!list.isEmpty()) return list.get(random.nextInt(list.size()));
            }
        }
        return null;
    }
}
