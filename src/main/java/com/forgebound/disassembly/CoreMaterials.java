package com.forgebound.disassembly;

import com.forgebound.tier.GearType;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Works out what the Ruinforged Crucible gives back for a piece of gear: the "core material" of its recipe (the
 * ingredient used most, sticks and other gear left out; for a smithing upgrade, the addition such as the netherite
 * ingot), times {@link #MAX_SHARE} at full durability, falling linearly to nothing as it wears out. Gear without a
 * crafting or smithing recipe (boss drops, loot-only items) can't be recovered. Works for every mod's recipes.
 * Server side; the index is rebuilt when the recipe manager changes (/reload).
 */
public final class CoreMaterials {
    /** Share of the core material returned at full durability. */
    public static final double MAX_SHARE = 0.5;

    /** The core material of an item's recipe: {@code count} of {@code item} per crafted item. */
    public record Core(Item item, double count) {}

    private static RecipeManager indexedFor;
    private static final Map<Item, List<Recipe<?>>> BY_RESULT = new HashMap<>();
    private static final Map<Item, Optional<Core>> CORES = new HashMap<>();
    private static Field additionField;
    private static boolean additionLooked;

    private CoreMaterials() {
    }

    /** True when the crucible accepts this item at all (weapon, tool or armor). */
    public static boolean isGear(ItemStack stack) {
        return GearType.isGear(stack);
    }

    /** Durability left, 0..1 (1 for gear that never wears). */
    public static double condition(ItemStack stack) {
        int max = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
        if (max <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - (double) stack.getDamageValue() / max));
    }

    /** Expected amount of the core material back for this stack (fractional), or 0. */
    public static double expected(Core core, ItemStack stack) {
        return core.count() * MAX_SHARE * condition(stack);
    }

    /** The amount actually returned: the expected amount, its fraction rounded up by chance. */
    public static int roll(Core core, ItemStack stack, RandomSource random) {
        double e = expected(core, stack);
        int whole = (int) Math.floor(e + 1.0e-9);
        return whole + (random.nextDouble() < e - whole ? 1 : 0);
    }

    /** Most that one stack can return (for checking the output slot has room). */
    public static int most(Core core, ItemStack stack) {
        return (int) Math.ceil(expected(core, stack) - 1.0e-9);
    }

    public static synchronized Optional<Core> core(ItemStack stack, Level level) {
        if (stack.isEmpty() || !isGear(stack)) return Optional.empty();
        RecipeManager rm = level.getRecipeManager();
        if (rm != indexedFor) {
            indexedFor = rm;
            BY_RESULT.clear();
            CORES.clear();
            for (RecipeHolder<?> h : rm.getRecipes()) {
                Recipe<?> r = h.value();
                if (!(r instanceof CraftingRecipe) && !(r instanceof SmithingTransformRecipe)) continue;
                ItemStack out;
                try {
                    out = r.getResultItem(level.registryAccess());
                } catch (Throwable t) {
                    continue;
                }
                if (out == null || out.isEmpty()) continue;
                BY_RESULT.computeIfAbsent(out.getItem(), k -> new ArrayList<>()).add(r);
            }
        }
        return CORES.computeIfAbsent(stack.getItem(), item -> find(item, level));
    }

    /** Of all recipes for {@code item}, the one whose core is worth least (so no recipe can be played against another). */
    private static Optional<Core> find(Item item, Level level) {
        HolderLookup.Provider registries = level.registryAccess();
        Core best = null;
        double bestValue = Double.MAX_VALUE;
        for (Recipe<?> r : BY_RESULT.getOrDefault(item, List.of())) {
            Core c = coreOf(r, item, registries);
            if (c == null) continue;
            double value = c.count() * Math.max(1, Prices.of(new ItemStack(c.item())));
            if (value < bestValue) {
                bestValue = value;
                best = c;
            }
        }
        return Optional.ofNullable(best);
    }

    private static Core coreOf(Recipe<?> r, Item result, HolderLookup.Provider registries) {
        int made = Math.max(1, r.getResultItem(registries).getCount());
        if (r instanceof SmithingTransformRecipe smithing) {
            Item add = first(addition(smithing));
            return add == null || add == result ? null : new Core(add, 1.0 / made);
        }
        // count the ingredients by the item they stand for
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ing : r.getIngredients()) {
            if (ing.isEmpty()) continue;
            Item it = first(ing);
            if (it == null || it == result) continue;
            ItemStack probe = new ItemStack(it);
            if (probe.hasCraftingRemainingItem() || isGear(probe)) continue; // buckets come back; gear is not a material
            counts.merge(it, 1, Integer::sum);
        }
        if (counts.isEmpty()) return null;
        Item pick = null;
        int pickCount = 0;
        long pickPrice = -1;
        boolean pickStick = true;
        for (Map.Entry<Item, Integer> e : counts.entrySet()) {
            boolean stick = isStick(e.getKey());
            long price = Prices.of(new ItemStack(e.getKey()));
            boolean better;
            if (pick == null) better = true;
            else if (stick != pickStick) better = !stick; // sticks only when there's nothing else
            else if (e.getValue() != pickCount) better = e.getValue() > pickCount;
            else better = price > pickPrice;
            if (better) {
                pick = e.getKey();
                pickCount = e.getValue();
                pickPrice = price;
                pickStick = stick;
            }
        }
        return new Core(pick, (double) pickCount / made);
    }

    private static boolean isStick(Item item) {
        return item == Items.STICK || new ItemStack(item).is(Tags.Items.RODS_WOODEN);
    }

    private static Item first(Ingredient ing) {
        if (ing == null) return null;
        for (ItemStack s : ing.getItems()) if (!s.isEmpty()) return s.getItem();
        return null;
    }

    /** The smithing recipe's addition (netherite ingot); its field isn't public. */
    private static Ingredient addition(SmithingTransformRecipe r) {
        if (!additionLooked) {
            additionLooked = true;
            try {
                additionField = SmithingTransformRecipe.class.getDeclaredField("addition");
            } catch (NoSuchFieldException e) {
                List<Field> ings = new ArrayList<>();
                for (Field f : SmithingTransformRecipe.class.getDeclaredFields()) if (f.getType() == Ingredient.class) ings.add(f);
                additionField = ings.size() == 3 ? ings.get(2) : null; // template, base, addition
            }
            if (additionField != null) additionField.setAccessible(true);
        }
        try {
            return additionField == null ? null : (Ingredient) additionField.get(r);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
