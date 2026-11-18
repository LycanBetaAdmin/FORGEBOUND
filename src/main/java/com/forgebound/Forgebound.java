package com.forgebound;

import com.forgebound.content.ModContent;
import com.forgebound.tier.LegacyTiers;
import com.forgebound.tier.TierEffects;
import com.forgebound.tier.Tiers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * FORGEBOUND: equipment evolution. Phase 1: tiers T1-T7 on every weapon, tool and armor piece ({@link Tiers}), the
 * Forge Anvil to raise them, dimensional metals and reforge materials, and the Eldoria RPG Blacksmith job link.
 */
@Mod(Forgebound.MODID)
public final class Forgebound {
    public static final String MODID = "forgebound";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Forgebound(IEventBus modBus, ModContainer container) {
        ForgeboundConfig.load();
        Tiers.register(modBus);
        LegacyTiers.register(modBus);
        ModContent.register(modBus);
        NeoForge.EVENT_BUS.register(TierEffects.class);
        NeoForge.EVENT_BUS.register(ModContent.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
