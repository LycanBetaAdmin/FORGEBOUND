package com.forgebound.disassembly;

import com.forgebound.ForgeboundConfig;
import com.forgebound.content.ModContent;
import com.forgebound.tier.Tiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the Ruinforged Crucible gives back for a piece of gear: its recipe's core material ({@link CoreMaterials}) plus
 * the FORGEBOUND upgrade materials paid to raise it to its tier (metals, Arcane Shards, Crystal Dust, Ascension Cores,
 * at their base costs), all at the same share: up to 50% at full durability, down to nothing when broken. Fractions
 * round by chance. Gear without a crafting or smithing recipe can't be recovered at all.
 */
public final class Recovery {
    public record Part(Item item, double expected) {
        public int most() {
            return (int) Math.ceil(expected - 1.0e-9);
        }

        public int roll(RandomSource random) {
            int whole = (int) Math.floor(expected + 1.0e-9);
            return whole + (random.nextDouble() < expected - whole ? 1 : 0);
        }
    }

    private Recovery() {
    }

    /** Empty when the item can't be recovered (not gear, or no recipe). */
    public static List<Part> of(ItemStack stack, Level level) {
        Optional<CoreMaterials.Core> core = CoreMaterials.core(stack, level);
        if (core.isEmpty()) return List.of();
        double share = CoreMaterials.MAX_SHARE * CoreMaterials.condition(stack);
        Map<Item, Double> amounts = new LinkedHashMap<>();
        amounts.merge(core.get().item(), core.get().count() * share, Double::sum);
        int tier = Tiers.get(stack).level();
        ForgeboundConfig cfg = ForgeboundConfig.get();
        for (int t = 1; t < tier; t++) {
            ForgeboundConfig.Upgrade u = cfg.upgrade(t);
            if (u == null) continue;
            Item metal = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(u.metal)).orElse(Items.AIR);
            if (metal != Items.AIR) amounts.merge(metal, u.metalCount * share, Double::sum);
            amounts.merge(ModContent.ARCANE_SHARD.get(), u.arcaneShard * share, Double::sum);
            if (u.crystalDust > 0) amounts.merge(ModContent.CRYSTAL_DUST.get(), u.crystalDust * share, Double::sum);
            if (u.ascensionCore > 0) amounts.merge(ModContent.ASCENSION_CORE.get(), u.ascensionCore * share, Double::sum);
        }
        List<Part> out = new ArrayList<>();
        amounts.forEach((item, e) -> {
            if (e > 1.0e-6) out.add(new Part(item, e));
        });
        return out;
    }
}
