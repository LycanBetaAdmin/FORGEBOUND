# FORGEBOUND

Equipment evolution for NeoForge 1.21.1: every weapon, tool and armor piece has a tier (T1-T7), raised at the
Forge Anvil with dimensional metals and reforge materials.

| Tier | Name | Colour |
|---|---|---|
| T1 | Common | Silver |
| T2 | Uncommon | Green |
| T3 | Rare | Blue |
| T4 | Epic | Purple |
| T5 | Legendary | Gold |
| T6 | Mythical Divine | Crimson |
| T7 | Relic | Magenta |

## Phase 1 (this version)
- **Tiers**: stored on the item (`forgebound:tier`). Each tier adds a share of the item's own attack damage, armor
  and toughness, mines faster, shoots harder arrows and multiplies max durability (`config/forgebound/forgebound.json`).
- **Starting tier**: crafted gear is T1; Eldoria RPG Blacksmiths roll up to their rank (Beginner T1 ... Master T5,
  T6 for the gear their specialization covers). Chest loot rolls a random tier. Eldoria boss weapons start at T5.
- **Forge Anvil**: raises a tier by one. Costs (T1>T2 ... T6>T7) use Arcane Shard, the tier's metal, Crystal Dust and
  Ascension Cores. Non-Blacksmiths pay double and stop at T5; Blacksmiths reach T5, Masters T6, specializations T7.
  Enchantments, Apotheosis affixes and gems are kept.
- **Metals**: Verdantite (Overworld ore, T1-T4), Infernalite (Nether ore, fortress chests, T4-T6), Voidsteel (End ore,
  End City chests, T6-T7). Raw metal smelts only in a blast furnace.
- **Materials**: Arcane Shard (amethyst clusters, dungeon chests), Crystal Dust (amethyst shard in a crafting grid),
  Ascension Core (bosses: `forgebound:bosses` entity tag).
- **RPG Items Tiers**: replaced. Its items are converted on load (Common T1, Rare T3, Epic T4, Legendary T5,
  Mythic T6, Divine T7).

## Phase 2
- **Unique Attributes**: 34 random effects (13 weapon, 11 armor, 10 tool) stored on the item (`forgebound:unique`); one
  slot, two at T6-T7. Strength scales with tier (+10% per tier). Pools: base (any tier), rarer (T3+), top (T5+), 60/30/10.
- **Reroll** (Forge Anvil, Reroll tab): Fortune Dice + Soul Essence by tier; a Fortune Crystal skips the base pool;
  one Sealing Rune per attribute kept.
- **Materials**: Fortune Dice (crafted: gold, bones, Arcane Shard), Soul Essence (hostile mobs, 2% + Looting; bosses 4-8),
  Fortune Crystal (bosses, 35%), Sealing Rune (stronghold library, ancient city, End City, bastion, mansion, ominous vaults).

## Phase 3
- **Rune Engraving Table** (crafted: Arcane Shards, book, amethyst blocks, enchanting table, obsidian).
- **Enhance tab**: chosen bonuses (`forgebound:enhancements`). Slots by tier: 1 / 2 / 2 / 3 / 3 / 4 / 4. Engraving costs
  2 Arcane Shards per tier + (2 + tier) XP levels; removing costs half the shards, no refund. Melee: attack damage, attack
  speed, crit rate, crit damage, life steal, fire damage, armor pierce. Ranged: arrow damage, draw speed, crit. Armor:
  armor, toughness, max health, knockback resistance, speed, dodge, healing received. Tools: mining speed, luck, reach,
  experience. Apothic Attributes ones only appear when that mod is installed.
- **Transfer tab**: moves every enhancement to another item of the same kind (free slots needed); 2 Arcane Shards and
  2 Crystal Dust per enhancement; the source loses them.

## Phase 4
- **Ruinforged Crucible = the Disassembly Forge** (moved here from Eldoria RPG; `eldoria_rpg:ruinforged_crucible` is
  aliased, so placed Crucibles carry over). Burns gear on coal or charcoal: up to 50% back at full durability, falling
  with wear, of the recipe's core material **and** of the FORGEBOUND upgrade materials paid to reach its tier (metals,
  Arcane Shards, Crystal Dust, Ascension Cores). Six output slots; empty ones preview what comes back. Gear without a
  recipe is refused.
- **API** (`com.forgebound.api.ForgeboundApi`): gear type, tier, and the shop tier roll used by Eldoria RPG's Armsmaster
  (T5-T7 by default, `shop_min_tier`). Animal armor is not FORGEBOUND gear.

## Phase 5
- **Tooltip frame** for every weapon, tool and armor piece: obsidian background, outer border in the tier colour,
  dark silver inner line, gold corner ornaments and rune diamonds. Inside: name, tier badge (`FORGEBOUND · T7 RELIC`) and
  the Apotheosis rarity, stats (bonuses green, penalties red), then bannered sections: Enhancements (blue), Apotheosis
  Affixes (purple: its special affixes and gem sockets) and Unique Attribute (gold), with durability at the foot.
  Other items keep the normal tooltip. (In packs with Simply Tooltips, turn its `enableTooltipRendering` off.)
- **Apotheosis**: reforging, augmenting and gems keep the FORGEBOUND tier, Unique Attributes and enhancements (they
  work on a copy of the item); FORGEBOUND never touches affixes, rarity or sockets, and the two progressions stay separate.

## Building
`./gradlew build` (Java 21); the jar lands in `build/libs/`. Art is generated: `tools/texgen/gen_forgebound.ps1` (item icons),
`gen_forgebound_blocks.ps1` (ores, ingots, Forge Anvil and Rune Engraving Table textures), `gen_forgebound_models.ps1`
(their models) and `gen_ruinforged_crucible*.ps1` (the Crucible).
