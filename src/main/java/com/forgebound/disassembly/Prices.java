package com.forgebound.disassembly;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Sell prices from Eldoria RPG's Coin Trader (read through reflection; 0 without Eldoria RPG). Used only to pick the
 * cheapest recipe when an item has several, so no recipe can be played against another.
 */
final class Prices {
    private static boolean looked;
    private static Method get, price;

    private Prices() {
    }

    static long of(ItemStack stack) {
        try {
            if (!looked) {
                looked = true;
                if (ModList.get().isLoaded("eldoria_rpg")) {
                    Class<?> c = Class.forName("com.eldoria.rpg.trade.SellPrices");
                    get = c.getMethod("get");
                    price = c.getMethod("price", ItemStack.class);
                }
            }
            return price == null ? 0 : (Long) price.invoke(get.invoke(null), stack); // get() each time: the config can be reloaded
        } catch (ReflectiveOperationException | RuntimeException e) {
            price = null;
            return 0;
        }
    }
}
