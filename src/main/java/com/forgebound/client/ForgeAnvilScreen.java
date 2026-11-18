package com.forgebound.client;

import com.forgebound.ForgeboundConfig;
import com.forgebound.anvil.ForgeAnvilMenu;
import com.forgebound.content.ModContent;
import com.forgebound.tier.Tier;
import com.forgebound.tier.Tiers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Locale;

import static com.forgebound.client.ForgeUi.*;

/**
 * The Forge Anvil, Reforge tab: the item in a violet rune circle, "»", the reforged result beside it with both tiers
 * named underneath; Required Materials with counts; the Reforge button. On the right: Preview (After) with each stat
 * old ▸ new (+gain) and the Tier Progression track.
 */
public class ForgeAnvilScreen extends AbstractContainerScreen<ForgeAnvilMenu> {
    static final ItemStack[] TAB_ICONS = {new ItemStack(Items.ANVIL), new ItemStack(Items.ENDER_EYE)};
    static final Component[] TAB_LABELS = {Component.translatable("screen.forgebound.tab.reforge.short"), Component.translatable("screen.forgebound.tab.reroll.short")};
    private static final int BTN_X = 98, BTN_Y = 130, BTN_W = 64, BTN_H = 18;
    private static final int RESULT_X = 147, RESULT_Y = 44;

    public ForgeAnvilScreen(ForgeAnvilMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = W;
        imageHeight = H;
        inventoryLabelX = INV_X;
        inventoryLabelY = INV_Y - 11;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (tabAt(mx, my, leftPos, topPos, 2) == 1) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ForgeAnvilMenu.BUTTON_REROLL_TAB);
                return true;
            }
            if (over(mx, my, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H) && menu.blocked() == null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SMITHING_TABLE_USE, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int tab = tabAt(mx, my, leftPos, topPos, 2);
        if (tab >= 0) {
            g.renderTooltip(font, Component.translatable(tab == 0 ? "screen.forgebound.tab.reforge" : "screen.forgebound.tab.reroll"), mx, my);
        } else if (over(mx, my, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H)) {
            Component why = menu.blocked();
            if (why != null) g.renderTooltip(font, why.copy().withColor(0xFF6B6B), mx, my);
            else g.renderTooltip(font, Component.translatable("screen.forgebound.anvil.reforge_to", menu.cost().to().label()), mx, my);
        } else if (over(mx, my, leftPos + RESULT_X, topPos + RESULT_Y, 16, 16) && !menu.gear().isEmpty() && menu.cost() != null) {
            g.renderTooltip(font, preview(), mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    /** The item as it will be after reforging. */
    private ItemStack preview() {
        ItemStack s = menu.gear().copy();
        ForgeAnvilMenu.Cost c = menu.cost();
        if (c != null) Tiers.set(s, c.to());
        return s;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LAVENDER, false);
        ItemStack gear = menu.gear();
        ForgeAnvilMenu.Cost cost = menu.cost();
        Tier cur = gear.isEmpty() ? null : Tiers.get(gear);
        Tier next = cost == null ? cur : cost.to();
        int ccx = CENTER_X + CENTER_W / 2;
        // tier names under the item and the result
        if (cur != null) {
            tierLabel(g, cur, 105, 69);
            if (cost != null) tierLabel(g, next, 155, 69);
            else smallCentered(g, Component.translatable("screen.forgebound.anvil.max_short"), 155, 69, GOLD_LIGHT & 0xFFFFFF);
        } else {
            smallCentered(g, Component.translatable("screen.forgebound.anvil.place_short"), ccx, 72, MUTED);
        }
        g.drawString(font, "»", 126, 48, cost != null ? GOLD_LIGHT & 0xFFFFFF : MUTED, false);
        header(g, font, CENTER_X, CENTER_W, 86, Component.translatable("screen.forgebound.anvil.required"));
        // material counts: green when the slot holds enough
        for (int s = ForgeAnvilMenu.SHARD; s <= ForgeAnvilMenu.CORE; s++) {
            Slot slot = menu.slots.get(s);
            int need = cost == null ? 0 : cost.need(s);
            String txt = need <= 0 ? "-" : count(s, cost) + "/" + need;
            int col = need <= 0 ? MUTED : menu.hasMaterial(cost, s) ? GREEN : RED;
            tinyCentered(g, Component.literal(txt), slot.x + 8, slot.y + 19, col);
        }
        // right: preview and progression
        header(g, font, RIGHT_X, RIGHT_W, 28, Component.translatable("screen.forgebound.anvil.preview"));
        if (cur != null) {
            ForgeboundConfig cfg = ForgeboundConfig.get();
            int y = 43;
            double dmg = sum(gear, true), armor = sum(gear, false);
            if (dmg > 0) y = stat(g, y, "ATK", 1 + dmg * (1 + cfg.power(cur.level())), 1 + dmg * (1 + cfg.power(next.level())), "%.1f");
            if (armor > 0) y = stat(g, y, "DEF", armor * (1 + cfg.power(cur.level())), armor * (1 + cfg.power(next.level())), "%.1f");
            int dur = Tiers.maxDurability(gear, cur), durNext = Tiers.maxDurability(gear, next);
            if (dur > 0) y = stat(g, y, "DUR", dur, durNext, "%.0f");
            stat(g, y, "PWR", cfg.power(cur.level()) * 100, cfg.power(next.level()) * 100, "+%.0f%%");
        } else {
            wrapped(g, font, Component.translatable("screen.forgebound.anvil.hint"), RIGHT_X + 6, 44, RIGHT_W - 12, 4, MUTED);
        }
        header(g, font, RIGHT_X, RIGHT_W, 90, Component.translatable("screen.forgebound.anvil.progression"));
        for (int i = 0; i < Tier.MAX; i++) {
            Tier t = Tier.of(i + 1);
            int x = RIGHT_X + 5 + i * 14, y = 104, w = 13;
            boolean isCur = cur == t, isNext = cost != null && next == t;
            boolean beyond = cur != null && menu.cap() > 0 && t.level() > menu.cap();
            int c = 0xFF000000 | t.color();
            g.fill(x, y, x + w, y + 12, isCur ? darker(c, 0.5) : 0xFF1A1424);
            frame(g, x, y, w, 12, isNext ? GOLD_LIGHT : isCur ? c : beyond ? 0xFF3A3046 : darker(c, 0.6));
            String r = t.roman();
            g.pose().pushPose();
            g.pose().translate(x + w / 2F - font.width(r) * 0.75F / 2, y + 3, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, r, 0, 0, beyond ? 0x5A5068 : isCur ? 0xFFFFFF : t.color(), false);
            g.pose().popPose();
        }
        Component why = menu.blocked();
        if (cur != null) {
            Component note = why != null ? why : menu.multiplier() > 1 ? Component.translatable("screen.forgebound.anvil.non_smith_cost", menu.multiplier())
                    : Component.translatable("screen.forgebound.anvil.ready");
            wrapped(g, font, note, RIGHT_X + 6, 124, RIGHT_W - 12, 4, why != null ? 0xFF8A80 : menu.multiplier() > 1 ? 0xFFB347 : GREEN);
        }
    }

    private void tierLabel(GuiGraphics g, Tier t, int cx, int y) {
        centerLabel(g, Component.literal("Tier " + t.roman()), cx, y, t.color());
        centerLabel(g, Component.literal("(").append(t.displayName()).append(")"), cx, y + 8, t.color());
    }

    private int count(int slot, ForgeAnvilMenu.Cost cost) {
        ItemStack s = menu.slots.get(slot).getItem();
        if (slot == ForgeAnvilMenu.METAL && !s.is(cost.metal())) return 0;
        return s.getCount();
    }

    private int stat(GuiGraphics g, int y, String name, double from, double to, String fmt) {
        String a = String.format(Locale.ROOT, fmt, from), b = String.format(Locale.ROOT, fmt, to);
        int x = RIGHT_X + 6;
        small(g, font, Component.literal(name), x, y, LAVENDER);
        small(g, font, Component.literal(a), x + 22, y, IVORY);
        if (Math.abs(to - from) > 1e-6) {
            int ax = x + 22 + (int) (font.width(a) * 0.75F) + 2;
            small(g, font, Component.literal("▸ " + b), ax, y, GREEN);
        }
        return y + 10;
    }

    /** The item's own flat attack damage (true) or armor (false). */
    static double sum(ItemStack stack, boolean damage) {
        double total = 0;
        for (ItemAttributeModifiers.Entry e : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
            if (e.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) continue;
            if (damage ? e.attribute().equals(Attributes.ATTACK_DAMAGE) : e.attribute().equals(Attributes.ARMOR)) total += e.modifier().amount();
        }
        return total;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        panel(g, font, x, y, title);
        tabs(g, font, x, y, 0, mx, my, TAB_ICONS, TAB_LABELS);
        // rune circles: the item and the result
        runeCircle(g, x + 105, y + 52, 14, VIOLET);
        runeCircle(g, x + 155, y + 52, 14, menu.cost() != null && !menu.gear().isEmpty() ? GOLD_LIGHT : 0xFF4A3A60);
        slot(g, x + RESULT_X - 1, y + RESULT_Y - 1, true);
        if (!menu.gear().isEmpty() && menu.cost() != null) {
            g.renderItem(preview(), x + RESULT_X, y + RESULT_Y);
            g.fill(x + RESULT_X, y + RESULT_Y, x + RESULT_X + 16, y + RESULT_Y + 16, 0x30FFD15C);
        }
        for (Slot s : menu.slots) {
            if (s.index < ForgeAnvilMenu.SIZE) slot(g, x + s.x - 1, y + s.y - 1, s.index == ForgeAnvilMenu.GEAR);
            else slot(g, x + s.x - 1, y + s.y - 1, false);
        }
        ForgeAnvilMenu.Cost cost = menu.cost();
        if (!menu.slots.get(ForgeAnvilMenu.GEAR).hasItem()) ghost(g, x + 97, y + 44, new ItemStack(Items.IRON_SWORD));
        hint(g, ForgeAnvilMenu.SHARD, new ItemStack(ModContent.ARCANE_SHARD.get()));
        hint(g, ForgeAnvilMenu.METAL, new ItemStack(cost != null && cost.metal() != Items.AIR ? cost.metal() : ModContent.VERDANTITE_INGOT.get()));
        hint(g, ForgeAnvilMenu.DUST, new ItemStack(ModContent.CRYSTAL_DUST.get()));
        hint(g, ForgeAnvilMenu.CORE, new ItemStack(ModContent.ASCENSION_CORE.get()));
        button(g, font, x + BTN_X, y + BTN_Y, BTN_W, BTN_H, Component.translatable("screen.forgebound.anvil.reforge"), menu.blocked() == null,
                over(mx, my, x + BTN_X, y + BTN_Y, BTN_W, BTN_H));
    }

    private void hint(GuiGraphics g, int index, ItemStack stack) {
        Slot s = menu.slots.get(index);
        if (!s.hasItem()) ghost(g, leftPos + s.x, topPos + s.y, stack);
    }
}
