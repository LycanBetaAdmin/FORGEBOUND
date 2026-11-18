package com.forgebound.unique;

import com.forgebound.Forgebound;
import com.forgebound.ForgeboundConfig;
import com.forgebound.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** What each Unique Attribute does. Values come from {@link Uniques#level} (base x tier scale, summed over gear). */
public final class UniqueEffects {
    private static final TagKey<Item> GEMS = Tags.Items.GEMS;
    private static final ResourceLocation FORTIFIED_ID = Forgebound.id("unique_fortified");
    private static final ResourceLocation CRIT_CHANCE_ID = Forgebound.id("unique_fortunes_edge");
    private static final ResourceLocation CRIT_DAMAGE_ID = Forgebound.id("unique_devastator");
    private static final String SECOND_CHANCE_TAG = "forgebound_second_chance";
    /** Combo Master: attacker -> (hits in a row, game time of the last hit). */
    private static final Map<UUID, long[]> COMBO = new ConcurrentHashMap<>();

    private UniqueEffects() {
    }

    private static boolean roll(RandomSource random, double chance) {
        return chance > 0 && random.nextDouble() < chance;
    }

    private static LivingEntity attacker(DamageSource source) {
        return source.getEntity() instanceof LivingEntity l ? l : null;
    }

    // ------------------------------------------------------------------ attributes on the item itself

    /** Fortified (+armor of that piece), Fortune's Edge (+crit chance), Devastator (+crit damage). */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.has(Uniques.UNIQUE.get())) return;
        double fortified = Uniques.value(stack, UniqueAttribute.FORTIFIED);
        if (fortified > 0) {
            for (ItemAttributeModifiers.Entry e : event.getDefaultModifiers().modifiers()) {
                if (e.attribute().equals(Attributes.ARMOR) && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE
                        && !e.modifier().id().getNamespace().equals(Forgebound.MODID) && e.modifier().amount() > 0) {
                    event.addModifier(Attributes.ARMOR, new AttributeModifier(FORTIFIED_ID, e.modifier().amount() * fortified,
                            AttributeModifier.Operation.ADD_VALUE), e.slot());
                    break;
                }
            }
        }
        addOptional(event, "apothic_attributes:crit_chance", CRIT_CHANCE_ID, Uniques.value(stack, UniqueAttribute.FORTUNES_EDGE));
        addOptional(event, "apothic_attributes:crit_damage", CRIT_DAMAGE_ID, Uniques.value(stack, UniqueAttribute.DEVASTATOR));
    }

    private static void addOptional(ItemAttributeModifierEvent event, String attr, ResourceLocation id, double amount) {
        if (amount <= 0) return;
        Optional<Holder.Reference<Attribute>> h = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attr));
        h.ifPresent(holder -> event.addModifier(holder, new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND));
    }

    // ------------------------------------------------------------------ damage dealt and taken

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        RandomSource rng = victim.getRandom();
        float amount = event.getAmount();
        LivingEntity att = attacker(event.getSource());
        if (att != null && att != victim) {
            double mult = 1;
            mult += victim.getHealth() < victim.getMaxHealth() * 0.2F ? Uniques.level(att, UniqueAttribute.EXECUTIONER) : 0;
            mult += victim.getType().is(ModContent.BOSSES) ? Uniques.level(att, UniqueAttribute.TITAN_SLAYER) : 0;
            mult += victim instanceof Player ? Uniques.level(att, UniqueAttribute.KILLER_EDGE) : 0;
            if (roll(rng, Uniques.level(att, UniqueAttribute.PHANTOM_STRIKE))) mult += 0.30;
            if (roll(rng, Uniques.level(att, UniqueAttribute.OVERCHARGE))) mult += 1.0;
            double combo = Uniques.level(att, UniqueAttribute.COMBO_MASTER);
            if (combo > 0 && combo(att, victim.level().getGameTime())) mult += combo;
            if (roll(rng, Uniques.level(att, UniqueAttribute.SOUL_REND))) {
                event.getContainer().addModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * 0.5F);
            }
            amount *= (float) mult;
        }
        double cut = Uniques.level(victim, UniqueAttribute.GUARDIAN_SOUL);
        if (victim.getHealth() < victim.getMaxHealth() * 0.25F) cut += Uniques.level(victim, UniqueAttribute.LAST_STAND);
        if (att != null && att.getType().is(ModContent.BOSSES)) cut += Uniques.level(victim, UniqueAttribute.GUARDIANS_RESOLVE);
        if (cut > 0) amount *= (float) Math.max(0.2, 1 - cut);
        event.setAmount(amount);
    }

    /** Combo Master: true on every fifth hit in a row (a pause of 3 seconds breaks the chain). */
    private static boolean combo(LivingEntity att, long now) {
        long[] c = COMBO.computeIfAbsent(att.getUUID(), k -> new long[2]);
        if (now - c[1] > 60) c[0] = 0;
        c[1] = now;
        if (++c[0] >= 5) {
            c[0] = 0;
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        float dealt = event.getNewDamage();
        if (victim.level().isClientSide() || dealt <= 0) return;
        LivingEntity att = attacker(event.getSource());
        if (att != null && att != victim) {
            double steal = Uniques.level(att, UniqueAttribute.BLOODTHIRSTY);
            if (steal > 0) att.heal((float) (dealt * steal));
            double barrier = Uniques.level(att, UniqueAttribute.VAMPIRIC_BARRIER);
            if (barrier > 0) att.setAbsorptionAmount((float) Math.min(att.getMaxHealth() * 0.5, att.getAbsorptionAmount() + dealt * barrier));
            double breaker = Uniques.level(att, UniqueAttribute.ARMOR_BREAKER);
            if (breaker > 0) {
                int amp = Math.max(0, (int) Math.round(breaker * 100) - 1); // 1% armor per level
                victim.addEffect(new MobEffectInstance(ModContent.SUNDERED, 80, amp, false, true, true), att);
            }
            double reaver = Uniques.level(att, UniqueAttribute.MANA_REAVER);
            if (reaver > 0) Mana.restoreFraction(att, reaver);
            // Thorned Armor: melee only (the hit came from the attacker itself, not a projectile)
            double thorns = Uniques.level(victim, UniqueAttribute.THORNED_ARMOR);
            if (thorns > 0 && event.getSource().getDirectEntity() == att && !event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
                att.hurt(victim.damageSources().thorns(victim), (float) (dealt * thorns));
            }
        }
    }

    /** Soul Harvest (heal on kill) and Second Chance (cheat death, long cooldown). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dying = event.getEntity();
        if (dying.level().isClientSide()) return;
        double chance = Uniques.level(dying, UniqueAttribute.SECOND_CHANCE);
        if (chance > 0) {
            long now = dying.level().getGameTime();
            long ready = dying.getPersistentData().getLong(SECOND_CHANCE_TAG);
            if (now >= ready) {
                event.setCanceled(true);
                dying.setHealth((float) Math.max(1, dying.getMaxHealth() * chance));
                dying.getPersistentData().putLong(SECOND_CHANCE_TAG, now + ForgeboundConfig.get().secondChanceCooldown * 20L);
                dying.level().playSound(null, dying.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
                if (dying.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, dying.getX(), dying.getY() + 1, dying.getZ(), 40, 0.4, 0.6, 0.4, 0.4);
                }
                return;
            }
        }
        LivingEntity killer = attacker(event.getSource());
        if (killer != null && killer != dying) {
            double harvest = Uniques.level(killer, UniqueAttribute.SOUL_HARVEST);
            if (harvest > 0) killer.heal((float) (killer.getMaxHealth() * harvest));
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        double will = Uniques.level(event.getEntity(), UniqueAttribute.IRON_WILL);
        if (will > 0) event.setStrength((float) (event.getStrength() * Math.max(0, 1 - will)));
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide() || event.getEffectInstance().getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return;
        if (roll(e.getRandom(), Uniques.level(e, UniqueAttribute.PURIFICATION))) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    /** Regeneration: heal a share of max HP every 5 seconds. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide() || p.tickCount % 100 != 0 || p.getHealth() >= p.getMaxHealth()) return;
        double regen = Uniques.level(p, UniqueAttribute.REGENERATION);
        if (regen > 0) p.heal((float) (p.getMaxHealth() * regen));
    }

    // ------------------------------------------------------------------ tools

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        ItemStack tool = p.getMainHandItem();
        if (!tool.has(Uniques.UNIQUE.get())) return;
        BlockState state = event.getState();
        if (!tool.isCorrectToolForDrops(state)) return;
        double bonus = 0;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) bonus += Uniques.value(tool, UniqueAttribute.DEEP_MINER);
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) bonus += Uniques.value(tool, UniqueAttribute.EXCAVATOR);
        if (state.is(BlockTags.LOGS)) bonus += Uniques.value(tool, UniqueAttribute.LUMBERJACK_SOUL);
        float speed = (float) (event.getNewSpeed() * (1 + bonus));
        // Earth Breaker: some blocks shatter at once. Decided per block position and player so client and server agree.
        double brittle = Uniques.value(tool, UniqueAttribute.EARTH_BREAKER);
        Optional<BlockPos> pos = event.getPosition();
        if (brittle > 0 && pos.isPresent() && state.getDestroySpeed(p.level(), pos.get()) >= 0) {
            long h = pos.get().asLong() * 0x9E3779B97F4A7C15L ^ p.getUUID().getLeastSignificantBits();
            h ^= h >>> 29;
            if ((h & 0xFFFF) / 65536.0 < brittle) speed = Math.max(speed, 10000F);
        }
        event.setNewSpeed(speed);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player p) || event.getDrops().isEmpty()) return;
        ItemStack tool = event.getTool();
        if (tool == null || !tool.has(Uniques.UNIQUE.get())) return;
        RandomSource rng = event.getLevel().getRandom();
        BlockState state = event.getState();
        List<ItemEntity> drops = event.getDrops();
        boolean crop = state.getBlock() instanceof CropBlock c && c.isMaxAge(state);
        boolean ore = state.is(Tags.Blocks.ORES);
        boolean dup = (ore && roll(rng, Uniques.value(tool, UniqueAttribute.FORTUNE_SEEKER)))
                || (crop && roll(rng, Uniques.value(tool, UniqueAttribute.BOTANICAL_TOUCH)));
        if (dup) {
            for (ItemEntity e : List.copyOf(drops)) drops.add(copy(e, e.getItem().copy()));
        }
        if (roll(rng, Uniques.value(tool, UniqueAttribute.TREASURE_HUNTER))) {
            ItemEntity first = drops.get(0);
            drops.add(copy(first, first.getItem().copyWithCount(1)));
        }
        if (roll(rng, Uniques.value(tool, UniqueAttribute.GEM_FINDER))) {
            for (ItemEntity e : List.copyOf(drops)) {
                if (e.getItem().is(GEMS)) {
                    drops.add(copy(e, e.getItem().copyWithCount(1)));
                    break;
                }
            }
        }
    }

    private static ItemEntity copy(ItemEntity like, ItemStack stack) {
        ItemEntity e = new ItemEntity(like.level(), like.getX(), like.getY(), like.getZ(), stack);
        e.setDefaultPickUpDelay();
        return e;
    }

    // ------------------------------------------------------------------ durability (ItemStackDurabilityMixin)

    /** Unbreakable / Durable Craft: each point of wear may be skipped; Efficient Harvest: the whole hit may be. */
    public static int durability(ItemStack stack, int amount, RandomSource random) {
        if (amount <= 0 || !stack.has(Uniques.UNIQUE.get())) return amount;
        if (roll(random, Uniques.value(stack, UniqueAttribute.EFFICIENT_HARVEST))) return 0;
        double skip = Uniques.value(stack, UniqueAttribute.UNBREAKABLE) + Uniques.value(stack, UniqueAttribute.DURABLE_CRAFT);
        if (skip <= 0) return amount;
        int out = 0;
        for (int i = 0; i < amount; i++) if (random.nextDouble() >= skip) out++;
        return out;
    }

    // ------------------------------------------------------------------ Iron's Spellbooks mana (optional)

    static final class Mana {
        private static boolean looked;
        private static Method getData, getMana, setMana;

        private Mana() {
        }

        static void restoreFraction(LivingEntity e, double fraction) {
            if (!(e instanceof ServerPlayer)) return;
            try {
                if (!looked) {
                    looked = true;
                    Class<?> c = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                    getData = c.getMethod("getPlayerMagicData", LivingEntity.class);
                    getMana = c.getMethod("getMana");
                    setMana = c.getMethod("setMana", float.class);
                }
                if (getData == null) return;
                Optional<Holder.Reference<Attribute>> max = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse("irons_spellbooks:max_mana"));
                if (max.isEmpty() || e.getAttribute(max.get()) == null) return;
                double cap = e.getAttributeValue(max.get());
                Object data = getData.invoke(null, e);
                float now = (Float) getMana.invoke(data);
                setMana.invoke(data, (float) Math.min(cap, now + cap * fraction));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                getData = null; // Iron's Spellbooks missing or changed: Mana Reaver does nothing
            }
        }
    }
}
