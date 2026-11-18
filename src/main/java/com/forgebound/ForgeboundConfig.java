package com.forgebound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Balance numbers, read from {@code config/forgebound/forgebound.json} (written with the defaults when missing;
 * missing keys fall back to the defaults). Arrays indexed by tier hold T1..T7 at 0..6.
 */
public final class ForgeboundConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static ForgeboundConfig instance = new ForgeboundConfig();

    public static final class Upgrade {
        @SerializedName("arcane_shard") public int arcaneShard;
        @SerializedName("metal") public String metal;
        @SerializedName("metal_count") public int metalCount;
        @SerializedName("crystal_dust") public int crystalDust;
        @SerializedName("ascension_core") public int ascensionCore;

        public Upgrade() {
        }

        Upgrade(int shard, String metal, int metalCount, int dust, int core) {
            this.arcaneShard = shard;
            this.metal = metal;
            this.metalCount = metalCount;
            this.crystalDust = dust;
            this.ascensionCore = core;
        }
    }

    @SerializedName("_comment")
    public String comment = "Tier arrays are T1..T7. tier_power: bonus to the item's own attack damage, armor, armor toughness and "
            + "mining speed. tier_durability: max durability multiplier. loot_tier_weights: chance weights for gear found in "
            + "chests. upgrades[i]: cost to go from tier i+1 to i+2. Non-Blacksmiths pay non_blacksmith_cost_multiplier times the "
            + "materials and can only reach non_blacksmith_max_tier at the Forge Anvil. start_tiers: gear from these mod ids "
            + "(namespaces) starts at this tier (weapons only).";

    @SerializedName("tier_power")
    public double[] tierPower = {0.0, 0.05, 0.10, 0.16, 0.23, 0.31, 0.40};

    @SerializedName("tier_durability")
    public double[] tierDurability = {1.0, 1.15, 1.30, 1.45, 1.60, 1.80, 2.0};

    @SerializedName("loot_tier_weights")
    public double[] lootTierWeights = {40, 25, 17, 10, 5, 2.5, 0.5};

    @SerializedName("upgrades")
    public List<Upgrade> upgrades = new ArrayList<>(List.of(
            new Upgrade(4, "forgebound:verdantite_ingot", 4, 0, 0),
            new Upgrade(8, "forgebound:verdantite_ingot", 6, 2, 0),
            new Upgrade(12, "forgebound:verdantite_ingot", 8, 4, 1),
            new Upgrade(16, "forgebound:infernalite_ingot", 6, 8, 1),
            new Upgrade(24, "forgebound:infernalite_ingot", 8, 12, 2),
            new Upgrade(32, "forgebound:voidsteel_ingot", 8, 16, 4)));

    @SerializedName("non_blacksmith_cost_multiplier")
    public int nonBlacksmithCostMultiplier = 2;

    @SerializedName("non_blacksmith_max_tier")
    public int nonBlacksmithMaxTier = 5;

    @SerializedName("start_tiers")
    public Map<String, Integer> startTiers = new LinkedHashMap<>(Map.of("eldoria", 5));

    public static ForgeboundConfig get() {
        return instance;
    }

    public double power(int tier) {
        return at(tierPower, tier, 0.0);
    }

    public double durability(int tier) {
        return at(tierDurability, tier, 1.0);
    }

    /** Cost to raise an item from {@code fromTier} to the next one, or null at T7. */
    public Upgrade upgrade(int fromTier) {
        int i = fromTier - 1;
        return i >= 0 && i < upgrades.size() ? upgrades.get(i) : null;
    }

    private static double at(double[] a, int tier, double def) {
        int i = tier - 1;
        return a != null && i >= 0 && i < a.length ? a[i] : def;
    }

    public static void load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("forgebound").resolve("forgebound.json");
        ForgeboundConfig cfg = new ForgeboundConfig();
        try {
            if (Files.exists(file)) {
                ForgeboundConfig read = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), ForgeboundConfig.class);
                if (read != null) cfg = fill(read);
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(cfg), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            Forgebound.LOGGER.error("[FORGEBOUND] Could not read {}: {} (using defaults)", file, e.toString());
            cfg = new ForgeboundConfig();
        }
        instance = cfg;
    }

    /** Keys missing from the file keep their defaults. */
    private static ForgeboundConfig fill(ForgeboundConfig read) {
        ForgeboundConfig d = new ForgeboundConfig();
        if (read.tierPower == null || read.tierPower.length < 7) read.tierPower = d.tierPower;
        if (read.tierDurability == null || read.tierDurability.length < 7) read.tierDurability = d.tierDurability;
        if (read.lootTierWeights == null || read.lootTierWeights.length < 7) read.lootTierWeights = d.lootTierWeights;
        if (read.upgrades == null || read.upgrades.size() < 6) read.upgrades = d.upgrades;
        if (read.startTiers == null) read.startTiers = d.startTiers;
        if (read.nonBlacksmithCostMultiplier < 1) read.nonBlacksmithCostMultiplier = d.nonBlacksmithCostMultiplier;
        if (read.nonBlacksmithMaxTier < 1) read.nonBlacksmithMaxTier = d.nonBlacksmithMaxTier;
        return read;
    }
}
