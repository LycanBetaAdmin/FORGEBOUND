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
import net.minecraft.world.entity.EntityType;
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

    // ---- Forge Anvil
    public static final DeferredBlock<ForgeAnvilBlock> FORGE_ANVIL = BLOCKS.register("forge_anvil", () -> new ForgeAnvilBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F, 1200.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.ANVIL).lightLevel(s -> 6).noOcclusion()));
    public static final DeferredItem<BlockItem> FORGE_ANVIL_ITEM = ITEMS.registerSimpleBlockItem(FORGE_ANVIL);
    public static final DeferredHolder<MenuType<?>, MenuType<ForgeAnvilMenu>> FORGE_ANVIL_MENU =
            MENUS.register("forge_anvil", () -> new MenuType<>(ForgeAnvilMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<ChestTierModifier>> CHEST_TIERS =
            LOOT_MODIFIERS.register("chest_tiers", () -> ChestTierModifier.CODEC);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("forgebound", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.forgebound"))
            .icon(() -> new ItemStack(FORGE_ANVIL_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(FORGE_ANVIL_ITEM.get());
                for (Supplier<? extends ItemLike> s : List.<Supplier<? extends ItemLike>>of(VERDANTITE_ORE, DEEPSLATE_VERDANTITE_ORE, INFERNALITE_ORE, VOIDSTEEL_ORE,
                        RAW_VERDANTITE, VERDANTITE_INGOT, RAW_INFERNALITE, INFERNALITE_INGOT, RAW_VOIDSTEEL, VOIDSTEEL_INGOT,
                        ARCANE_SHARD, CRYSTAL_DUST, ASCENSION_CORE)) {
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
    }

    /** Bosses drop 1-2 Ascension Cores. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getType().is(BOSSES)) return;
        int n = 1 + entity.getRandom().nextInt(2);
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(), new ItemStack(ASCENSION_CORE.get(), n)));
    }
}
