package com.forgebound.mixin;

import com.forgebound.unique.UniqueEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/** Unbreakable, Durable Craft and Efficient Harvest: less wear on items that carry them. */
@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {
    @ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int forgebound$wear(int amount, int amountAgain, ServerLevel level, ServerPlayer player, Consumer<Item> onBreak) {
        return UniqueEffects.durability((ItemStack) (Object) this, amount, level.getRandom());
    }
}
