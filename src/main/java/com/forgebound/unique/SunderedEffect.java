package com.forgebound.unique;

import com.forgebound.Forgebound;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Armor Breaker's debuff: -1% armor per level (the level carries the attribute's tier-scaled strength). */
public class SunderedEffect extends MobEffect {
    public SunderedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A7FA0);
        addAttributeModifier(Attributes.ARMOR, Forgebound.id("sundered"), -0.01, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
