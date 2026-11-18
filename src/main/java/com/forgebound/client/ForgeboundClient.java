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
            event.register(ModContent.REROLL_MENU.get(), RerollScreen::new);
            event.register(ModContent.ENGRAVE_MENU.get(), EngraveScreen::new);
            event.register(ModContent.TRANSFER_MENU.get(), TransferScreen::new);
        }
    }

    @EventBusSubscriber(modid = Forgebound.MODID, value = Dist.CLIENT)
    public static final class GameEvents {

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            if (!GearType.isGear(event.getItemStack())) return;
            List<Component> lines = event.getToolTip();
            var stack = event.getItemStack();
            var tier = Tiers.get(stack);
            int at = Math.min(1, lines.size());
            lines.add(at++, tier.label());
            var attrs = com.forgebound.unique.Uniques.get(stack);
            if (!attrs.isEmpty()) {
                lines.add(at++, Component.translatable("tooltip.forgebound.unique").withColor(0xD6A84B));
                for (var a : attrs) {
                    lines.add(at++, Component.literal(" ").append(a.displayName()));
                    lines.add(at++, Component.literal("   ").append(a.description(tier.level())));
                }
            }
            var enh = com.forgebound.enhance.Enhancements.get(stack);
            if (!enh.isEmpty()) {
                lines.add(at++, Component.translatable("tooltip.forgebound.enhancements", enh.size(), com.forgebound.enhance.Enhancements.slots(tier)).withColor(0x55AAFF));
                for (var e : enh) lines.add(at++, Component.literal(" ").append(e.line()));
            }
        }
    }
}
