package com.forgebound.mixin;

import com.forgebound.tier.Tiers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item.onCraftedBy runs for every crafting station (grid, 2x2, smithing, stonecutter, furnaces) and also on
 * shift-click crafts, before the stack is moved: the crafted gear gets its starting tier there.
 */
@Mixin(Item.class)
public abstract class ItemCraftedMixin {
    @Inject(method = "onCraftedBy", at = @At("HEAD"))
    private void forgebound$onCraftedBy(ItemStack stack, Level level, Player player, CallbackInfo ci) {
        Tiers.onCrafted(stack, level, player);
    }
}
