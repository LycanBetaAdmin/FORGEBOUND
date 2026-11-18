package com.forgebound.loot;

import com.forgebound.tier.Tiers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Gear generated in any chest loot table ({@code <ns>:chests/...}) gets a random tier (loot_tier_weights). */
public class ChestTierModifier extends LootModifier {
    public static final MapCodec<ChestTierModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ChestTierModifier::new));

    public ChestTierModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!context.getQueriedLootTableId().getPath().startsWith("chests/")) return loot;
        for (ItemStack stack : loot) Tiers.onLooted(stack, context.getRandom());
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
