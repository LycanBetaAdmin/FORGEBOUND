package com.forgebound.content;

import com.forgebound.Forgebound;
import com.forgebound.anvil.ForgeAnvilBlock;
import com.forgebound.anvil.ForgeAnvilMenu;
import com.forgebound.loot.ChestTierModifier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/**
 * Blocks, items and the Forge Anvil menu. Dimensional metals: Verdantite (Overworld ore, T1-T4 upgrades), Infernalite
 * (Nether ore and fortress chests, T4-T6), Voidsteel (End ore and End City chests, T6-T7); raw metal only smelts in a
 * blast furnace. Reforge materials: Arcane Shard (amethyst clusters, dungeon chests), Crystal Dust (ground amethyst),
 * Ascension Core (bosses, see {@link #onDrops}).
 */
public final class ModContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Forgebound.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Forgebound.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Forgebound.MODID);
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Forgebound.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Forgebound.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Forgebound.MODID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Forgebound.MODID);

    /** Entities that drop an Ascension Core (data/forgebound/tags/entity_type/bosses.json). */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Forgebound.id("bosses"));

    // ---- ores
    public static final DeferredBlock<Block> VERDANTITE_ORE = ore("verdantite_ore", Blocks.IRON_ORE, 3.5F, UniformInt.of(2, 5));
    public static final DeferredBlock<Block> DEEPSLATE_VERDANTITE_ORE = ore("deepslate_verdantite_ore", Blocks.DEEPSLATE_IRON_ORE, 5.0F, UniformInt.of(2, 5));
    public static final DeferredBlock<Block> INFERNALITE_ORE = ore("infernalite_ore", Blocks.NETHER_GOLD_ORE, 4.0F, UniformInt.of(3, 7));
    public static final DeferredBlock<Block> VOIDSTEEL_ORE = ore("voidsteel_ore", Blocks.END_STONE, 6.0F, UniformInt.of(4, 9));

    // ---- metals and materials
    public static final DeferredItem<Item> RAW_VERDANTITE = item("raw_verdantite", Rarity.COMMON);
    public static final DeferredItem<Item> RAW_INFERNALITE = item("raw_infernalite", Rarity.UNCOMMON);
    public static final DeferredItem<Item> RAW_VOIDSTEEL = item("raw_voidsteel", Rarity.RARE);
    public static final DeferredItem<Item> VERDANTITE_INGOT = item("verdantite_ingot", Rarity.COMMON);
    public static final DeferredItem<Item> INFERNALITE_INGOT = item("infernalite_ingot", Rarity.UNCOMMON);
    public static final DeferredItem<Item> VOIDSTEEL_INGOT = item("voidsteel_ingot", Rarity.RARE);
    public static final DeferredItem<Item> ARCANE_SHARD = item("arcane_shard", Rarity.COMMON);
    public static final DeferredItem<Item> CRYSTAL_DUST = item("crystal_dust", Rarity.COMMON);
    public static final DeferredItem<Item> ASCENSION_CORE = ITEMS.register("ascension_core",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    // ---- reroll materials (phase 2)
    /** Main reroll catalyst: crafted. */
    public static final DeferredItem<Item> FORTUNE_DICE = item("fortune_dice", Rarity.UNCOMMON);
    /** Required for every reroll: rare drop from hostile mobs, several from bosses. */
    public static final DeferredItem<Item> SOUL_ESSENCE = item("soul_essence", Rarity.UNCOMMON);
    /** Premium reroll (skips the base pool): bosses sometimes drop one. */
    public static final DeferredItem<Item> FORTUNE_CRYSTAL = ITEMS.register("fortune_crystal",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });
    /** Seals one Unique Attribute during a reroll: high-level dungeon chests. */
    public static final DeferredItem<Item> SEALING_RUNE = item("sealing_rune", Rarity.RARE);

    /** Armor Breaker's debuff. */
    public static final DeferredHolder<MobEffect, MobEffect> SUNDERED = EFFECTS.register("sundered", com.forgebound.unique.SunderedEffect::new);

    // ---- Forge Anvil
    public static final DeferredBlock<ForgeAnvilBlock> FORGE_ANVIL = BLOCKS.register("forge_anvil", () -> new ForgeAnvilBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F, 1200.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.ANVIL).lightLevel(s -> 6).noOcclusion()));
    public static final DeferredItem<BlockItem> FORGE_ANVIL_ITEM = ITEMS.registerSimpleBlockItem(FORGE_ANVIL);
    public static final DeferredHolder<MenuType<?>, MenuType<ForgeAnvilMenu>> FORGE_ANVIL_MENU =
            MENUS.register("forge_anvil", () -> new MenuType<>(ForgeAnvilMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<com.forgebound.anvil.RerollMenu>> REROLL_MENU =
            MENUS.register("forge_anvil_reroll", () -> new MenuType<>(com.forgebound.anvil.RerollMenu::new, FeatureFlags.VANILLA_SET));

    // ---- Rune Engraving Table (phase 3)
    public static final DeferredBlock<com.forgebound.enhance.RuneTableBlock> RUNE_TABLE = BLOCKS.register("rune_engraving_table",
            () -> new com.forgebound.enhance.RuneTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 1200.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.WOOD).lightLevel(s -> 7).noOcclusion()));
    public static final DeferredItem<BlockItem> RUNE_TABLE_ITEM = ITEMS.registerSimpleBlockItem(RUNE_TABLE);
    public static final DeferredHolder<MenuType<?>, MenuType<com.forgebound.enhance.EngraveMenu>> ENGRAVE_MENU =
            MENUS.register("rune_engraving_table", () -> new MenuType<>(com.forgebound.enhance.EngraveMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<com.forgebound.enhance.TransferMenu>> TRANSFER_MENU =
            MENUS.register("rune_engraving_table_transfer", () -> new MenuType<>(com.forgebound.enhance.TransferMenu::new, FeatureFlags.VANILLA_SET));

    // ---- Ruinforged Crucible: the Disassembly Forge (phase 4; moved here from Eldoria RPG)
    public static final DeferredBlock<com.forgebound.disassembly.RuinforgedCrucibleBlock> CRUCIBLE = BLOCKS.register("ruinforged_crucible",
            () -> new com.forgebound.disassembly.RuinforgedCrucibleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(s -> s.getValue(com.forgebound.disassembly.RuinforgedCrucibleBlock.LIT) ? 13 : 8).noOcclusion()));
    public static final DeferredItem<BlockItem> CRUCIBLE_ITEM = ITEMS.registerSimpleBlockItem(CRUCIBLE);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<com.forgebound.disassembly.RuinforgedCrucibleBlockEntity>> CRUCIBLE_BE =
            BLOCK_ENTITIES.register("ruinforged_crucible", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    com.forgebound.disassembly.RuinforgedCrucibleBlockEntity::new, CRUCIBLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<com.forgebound.disassembly.CrucibleMenu>> CRUCIBLE_MENU =
            MENUS.register("ruinforged_crucible", () -> new MenuType<>(com.forgebound.disassembly.CrucibleMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<ChestTierModifier>> CHEST_TIERS =
            LOOT_MODIFIERS.register("chest_tiers", () -> ChestTierModifier.CODEC);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("forgebound", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.forgebound"))
            .icon(() -> new ItemStack(FORGE_ANVIL_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(FORGE_ANVIL_ITEM.get());
                out.accept(RUNE_TABLE_ITEM.get());
                out.accept(CRUCIBLE_ITEM.get());
                for (Supplier<? extends ItemLike> s : List.<Supplier<? extends ItemLike>>of(VERDANTITE_ORE, DEEPSLATE_VERDANTITE_ORE, INFERNALITE_ORE, VOIDSTEEL_ORE,
                        RAW_VERDANTITE, VERDANTITE_INGOT, RAW_INFERNALITE, INFERNALITE_INGOT, RAW_VOIDSTEEL, VOIDSTEEL_INGOT,
                        ARCANE_SHARD, CRYSTAL_DUST, ASCENSION_CORE, FORTUNE_DICE, SOUL_ESSENCE, FORTUNE_CRYSTAL, SEALING_RUNE)) {
                    out.accept(s.get());
                }
            }).build());

    private ModContent() {
    }

    private static DeferredBlock<Block> ore(String name, Block like, float hardness, UniformInt xp) {
        DeferredBlock<Block> block = BLOCKS.register(name, () -> new DropExperienceBlock(xp,
                BlockBehaviour.Properties.ofFullCopy(like).strength(hardness, 3.0F).requiresCorrectToolForDrops()));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    private static DeferredItem<Item> item(String name, Rarity rarity) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().rarity(rarity)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        EFFECTS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        // Crucibles placed while Eldoria RPG owned them load as FORGEBOUND's (Eldoria RPG skips its own when FORGEBOUND is installed)
        var old = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eldoria_rpg", "ruinforged_crucible");
        var now = Forgebound.id("ruinforged_crucible");
        BLOCKS.addAlias(old, now);
        ITEMS.addAlias(old, now);
        BLOCK_ENTITIES.addAlias(old, now);
        MENUS.addAlias(old, now);
    }

    /**
     * Bosses drop 1-2 Ascension Cores, 4-8 Soul Essence and sometimes a Fortune Crystal; other hostile mobs killed by a
     * player sometimes drop a Soul Essence (Looting raises the chance).
     */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        var rng = entity.getRandom();
        var cfg = com.forgebound.ForgeboundConfig.get();
        if (entity.getType().is(BOSSES)) {
            drop(event, new ItemStack(ASCENSION_CORE.get(), 1 + rng.nextInt(2)));
            drop(event, new ItemStack(SOUL_ESSENCE.get(), 4 + rng.nextInt(5)));
            if (rng.nextDouble() < cfg.bossCrystalChance) drop(event, new ItemStack(FORTUNE_CRYSTAL.get()));
            return;
        }
        if (entity instanceof Enemy && event.getSource().getEntity() instanceof Player) {
            int looting = 0;
            var reg = entity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            var holder = reg.getHolder(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
            if (holder.isPresent() && event.getSource().getEntity() instanceof Player p) {
                looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder.get(), p.getMainHandItem());
            }
            if (rng.nextDouble() < cfg.soulEssenceChance + cfg.soulEssenceLootingBonus * looting) drop(event, new ItemStack(SOUL_ESSENCE.get()));
        }
    }

    private static void drop(LivingDropsEvent event, ItemStack stack) {
        var e = event.getEntity();
        event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(), stack));
    }
}
