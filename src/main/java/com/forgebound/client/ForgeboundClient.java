package com.forgebound.client;

import com.forgebound.Forgebound;
import com.forgebound.content.ModContent;
import com.forgebound.tier.GearType;
import com.forgebound.tier.Tiers;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Client hooks: the Forge Anvil screen and the tier line on equipment tooltips (the full frame comes in phase 5). */
public final class ForgeboundClient {
    private ForgeboundClient() {
    }

    @EventBusSubscriber(modid = Forgebound.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(ModContent.FORGE_ANVIL_MENU.get(), ForgeAnvilScreen::new);
        }
    }

    @EventBusSubscriber(modid = Forgebound.MODID, value = Dist.CLIENT)
    public static final class GameEvents {

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            if (!GearType.isGear(event.getItemStack())) return;
            List<Component> lines = event.getToolTip();
            lines.add(Math.min(1, lines.size()), Tiers.get(event.getItemStack()).label());
        }
    }
}
