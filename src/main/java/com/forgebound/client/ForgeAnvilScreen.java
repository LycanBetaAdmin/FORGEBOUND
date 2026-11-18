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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Forge Anvil screen, drawn without a texture: dark obsidian panel, antique gold frame, steel insets, a blue rune
 * ring around the equipment, the I-VII tier track, a before/after stat preview and the material row with live counts.
 */
public class ForgeAnvilScreen extends AbstractContainerScreen<ForgeAnvilMenu> {
    static final int OBSIDIAN = 0xFF100D18, OBSIDIAN_2 = 0xFF1B1526, STEEL = 0xFF51465F, STEEL_DARK = 0xFF2A2333;
    static final int GOLD = 0xFFD6A84B, GOLD_LIGHT = 0xFFFFD15C, GOLD_DARK = 0xFF7A5A22, RUNE = 0xFF4FC3F7, IVORY = 0xE8E0ED;
    private static final int BTN_X = 116, BTN_Y = 78, BTN_W = 52, BTN_H = 18;
    private static final int TRACK_X = 56, TRACK_Y = 18, BADGE = 15;
    private static final int PREVIEW_X = 56, PREVIEW_Y = 38;

    public ForgeAnvilScreen(ForgeAnvilMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = ForgeAnvilMenu.INV_Y + 82;
        inventoryLabelY = ForgeAnvilMenu.INV_Y - 11;
        titleLabelX = 8;
    }

    private boolean overButton(double mx, double my) {
        int x = (int) mx - leftPos, y = (int) my - topPos;
        return x >= BTN_X && x < BTN_X + BTN_W && y >= BTN_Y && y < BTN_Y + BTN_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && tabAt(mx, my, leftPos, topPos) == 1 && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ForgeAnvilMenu.BUTTON_REROLL_TAB);
            return true;
        }
        if (button == 0 && overButton(mx, my) && menu.blocked() == null && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SMITHING_TABLE_USE, 1.0F));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        if (overButton(mx, my)) {
            Component why = menu.blocked();
            List<Component> lines = new ArrayList<>();
            if (why == null) {
                ForgeAnvilMenu.Cost c = menu.cost();
                lines.add(Component.translatable("screen.forgebound.anvil.reforge_to", c.to().label()));
                if (menu.multiplier() > 1) lines.add(Component.translatable("screen.forgebound.anvil.non_smith_cost", menu.multiplier()).withColor(0xFF8A80));
            } else {
                lines.add(why.copy().withColor(0xFF6B6B));
            }
            g.renderComponentTooltip(font, lines, mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    // ------------------------------------------------------------------ labels and preview

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, 6, GOLD_LIGHT & 0xFFFFFF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB9A6D6, false);
        ItemStack gear = menu.gear();
        Tier cur = gear.isEmpty() ? null : Tiers.get(gear);
        // tier track I..VII: current lit, next outlined in gold, beyond the cap dimmed
        for (int i = 0; i < Tier.MAX; i++) {
            Tier t = Tier.of(i + 1);
            int x = TRACK_X + i * (BADGE + 1), y = TRACK_Y;
            boolean isCur = cur == t, isNext = cur != null && cur.next() == t && cur != t;
            boolean beyond = cur != null && menu.cap() > 0 && t.level() > menu.cap();
            int c = 0xFF000000 | t.color();
            g.fill(x, y, x + BADGE, y + 11, isCur ? darker(c, 0.55) : 0xFF1A1424);
            frame(g, x, y, BADGE, 11, isNext ? GOLD_LIGHT : isCur ? c : beyond ? 0xFF3A3046 : darker(c, 0.6));
            String r = t.roman();
            g.drawString(font, r, x + (BADGE - font.width(r)) / 2 + 1, y + 2, beyond ? 0x5A5068 : isCur ? 0xFFFFFF : t.color(), false);
        }
        if (gear.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("screen.forgebound.anvil.hint"), PREVIEW_X, PREVIEW_Y + 2, 110, 0x9C88BC);
            return;
        }
        ForgeAnvilMenu.Cost cost = menu.cost();
        Tier next = cost == null ? cur : cost.to();
        int lw = fitted(g, font, cur.label(), PREVIEW_X, PREVIEW_Y, imageWidth - 18 - PREVIEW_X);
        if (cost != null) g.drawString(font, "»", PREVIEW_X + lw + 3, PREVIEW_Y, GOLD_LIGHT & 0xFFFFFF, false);
        int y = PREVIEW_Y + 11;
        double dmg = sum(gear, true), armor = sum(gear, false);
        ForgeboundConfig cfg = ForgeboundConfig.get();
        if (dmg > 0) y = stat(g, y, "ATK", 1 + dmg * (1 + cfg.power(cur.level())), 1 + dmg * (1 + cfg.power(next.level())), "%.1f");
        if (armor > 0) y = stat(g, y, "DEF", armor * (1 + cfg.power(cur.level())), armor * (1 + cfg.power(next.level())), "%.1f");
        int dur = Tiers.maxDurability(gear, cur), durNext = Tiers.maxDurability(gear, next);
        if (dur > 0) y = stat(g, y, "DUR", dur, durNext, "%.0f");
        if (dmg <= 0 && armor <= 0) stat(g, y, "PWR", cfg.power(cur.level()) * 100, cfg.power(next.level()) * 100, "+%.0f%%");
        // material counts under each material slot
        if (cost != null) {
            for (int s = ForgeAnvilMenu.SHARD; s <= ForgeAnvilMenu.CORE; s++) {
                Slot slot = menu.slots.get(s);
                int need = cost.need(s);
                String txt = need <= 0 ? "-" : "x" + need; // green when the slot holds enough, red when not
                int col = need <= 0 ? 0x5A5068 : menu.hasMaterial(cost, s) ? 0x80FF20 : 0xFF5050;
                g.pose().pushPose();
                g.pose().scale(0.75F, 0.75F, 1);
                g.drawString(font, txt, (int) ((slot.x + 8) / 0.75F) - font.width(txt) / 2, (int) ((slot.y + 18) / 0.75F), col, false);
                g.pose().popPose();
            }
        }
        // button label
        boolean ok = menu.blocked() == null;
        String b = Component.translatable("screen.forgebound.anvil.reforge").getString();
        g.drawString(font, b, BTN_X + (BTN_W - font.width(b)) / 2, BTN_Y + 5, ok ? 0x2A1A05 : 0x6A6070, false);
    }

    private int stat(GuiGraphics g, int y, String name, double from, double to, String fmt) {
        g.drawString(font, name, PREVIEW_X, y, 0xB9A6D6, false);
        String a = String.format(Locale.ROOT, fmt, from), b = String.format(Locale.ROOT, fmt, to);
        int x = PREVIEW_X + 26;
        g.drawString(font, a, x, y, IVORY, false);
        if (Math.abs(to - from) > 1e-6) {
            x += font.width(a) + 3;
            g.drawString(font, "▸", x, y, GOLD_LIGHT & 0xFFFFFF, false);
            g.drawString(font, b, x + 8, y, 0x80FF20, false);
        }
        return y + 10;
    }

    /** The item's own flat attack damage (true) or armor (false), from its attribute modifiers. */
    private static double sum(ItemStack stack, boolean damage) {
        double total = 0;
        for (ItemAttributeModifiers.Entry e : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
            if (e.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) continue;
            if (damage ? e.attribute().equals(Attributes.ATTACK_DAMAGE) : e.attribute().equals(Attributes.ARMOR)) total += e.modifier().amount();
        }
        return total;
    }

    // ------------------------------------------------------------------ background

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        drawTabs(g, x, y, 0, mx, my);
        // obsidian body, double gold frame with corner studs
        g.fill(x, y, x + w, y + h, 0xFF06040A);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, OBSIDIAN_2, OBSIDIAN);
        frame(g, x + 1, y + 1, w - 2, h - 2, GOLD_DARK);
        frame(g, x + 2, y + 2, w - 4, h - 4, GOLD);
        for (int[] c : new int[][]{{x + 1, y + 1}, {x + w - 6, y + 1}, {x + 1, y + h - 6}, {x + w - 6, y + h - 6}}) {
            g.fill(c[0], c[1], c[0] + 5, c[1] + 5, GOLD_LIGHT);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 4, c[1] + 4, GOLD_DARK);
        }
        // forge panel inset and a gold divider above the inventory
        g.fill(x + 6, y + 15, x + w - 6, y + ForgeAnvilMenu.INV_Y - 14, STEEL_DARK);
        frame(g, x + 6, y + 15, w - 12, ForgeAnvilMenu.INV_Y - 29, STEEL);
        g.fill(x + 10, y + ForgeAnvilMenu.INV_Y - 13, x + w - 10, y + ForgeAnvilMenu.INV_Y - 12, GOLD_DARK);
        g.fill(x + w / 2 - 2, y + ForgeAnvilMenu.INV_Y - 14, x + w / 2 + 2, y + ForgeAnvilMenu.INV_Y - 11, GOLD_LIGHT);
        // blue rune ring around the equipment slot
        Slot gear = menu.slots.get(ForgeAnvilMenu.GEAR);
        int cx = x + gear.x + 8, cy = y + gear.y + 8;
        ring(g, cx, cy, 17, RUNE & 0x9FFFFFFF);
        ring(g, cx, cy, 15, 0x5F2A90C8);
        for (int[] d : new int[][]{{0, -18}, {0, 18}, {-18, 0}, {18, 0}}) g.fill(cx + d[0] - 1, cy + d[1] - 1, cx + d[0] + 2, cy + d[1] + 2, RUNE);
        // material row caption and slots
        for (Slot s : menu.slots) slot(g, x + s.x - 1, y + s.y - 1, s.index == ForgeAnvilMenu.GEAR);
        ghost(g, menu.slots.get(ForgeAnvilMenu.GEAR), new ItemStack(Items.IRON_SWORD));
        ForgeAnvilMenu.Cost cost = menu.cost();
        ghost(g, menu.slots.get(ForgeAnvilMenu.SHARD), new ItemStack(ModContent.ARCANE_SHARD.get()));
        ghost(g, menu.slots.get(ForgeAnvilMenu.METAL), new ItemStack(cost != null && cost.metal() != Items.AIR ? cost.metal() : ModContent.VERDANTITE_INGOT.get()));
        ghost(g, menu.slots.get(ForgeAnvilMenu.DUST), new ItemStack(ModContent.CRYSTAL_DUST.get()));
        ghost(g, menu.slots.get(ForgeAnvilMenu.CORE), new ItemStack(ModContent.ASCENSION_CORE.get()));
        // Reforge button: gold when ready
        boolean ok = menu.blocked() == null, hover = overButton(mx, my);
        int bx = x + BTN_X, by = y + BTN_Y;
        g.fill(bx, by, bx + BTN_W, by + BTN_H, ok ? GOLD_DARK : 0xFF2A2333);
        g.fillGradient(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, ok ? (hover ? 0xFFFFE08A : GOLD_LIGHT) : 0xFF3A3046, ok ? GOLD : 0xFF2A2333);
        frame(g, bx, by, BTN_W, BTN_H, ok ? 0xFFFFF0C0 : STEEL);
    }

    private void ghost(GuiGraphics g, Slot s, ItemStack hint) {
        if (s.hasItem()) return;
        g.renderFakeItem(hint, leftPos + s.x, topPos + s.y);
        g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xB8201A2A);
    }

    static void slot(GuiGraphics g, int x, int y, boolean big) {
        int grow = big ? 2 : 0;
        int x0 = x - grow, y0 = y - grow, x1 = x + 18 + grow, y1 = y + 18 + grow;
        g.fill(x0, y0, x1, y1, big ? 0xFF3A2A52 : 0xFF2E2638);
        g.fill(x0, y0, x1 - 1, y0 + 1, 0xFF0A0710);
        g.fill(x0, y0, x0 + 1, y1 - 1, 0xFF0A0710);
        g.fill(x0 + 1, y1 - 1, x1, y1, big ? GOLD : 0xFF6E5E84);
        g.fill(x1 - 1, y0 + 1, x1, y1, big ? GOLD : 0xFF6E5E84);
    }

    // ------------------------------------------------------------------ tabs (Reforge / Reroll)

    static final int TAB_W = 26, TAB_H = 20;

    /** Tab index under the mouse (0 = Reforge, 1 = Reroll), or -1. Tabs hang off the left edge of the panel. */
    static int tabAt(double mx, double my, int left, int top) {
        for (int i = 0; i < 2; i++) {
            int x = left - TAB_W + 2, y = top + 18 + i * (TAB_H + 3);
            if (mx >= x && mx < x + TAB_W && my >= y && my < y + TAB_H) return i;
        }
        return -1;
    }

    static void drawTabs(GuiGraphics g, int left, int top, int active, int mx, int my) {
        drawTabs(g, left, top, active, mx, my, new ItemStack[]{new ItemStack(ModContent.FORGE_ANVIL_ITEM.get()), new ItemStack(ModContent.FORTUNE_DICE.get())});
    }

    static void drawTabs(GuiGraphics g, int left, int top, int active, int mx, int my, ItemStack[] icons) {
        for (int i = 0; i < 2; i++) {
            int x = left - TAB_W + 2, y = top + 18 + i * (TAB_H + 3);
            boolean on = i == active, hover = tabAt(mx, my, left, top) == i;
            g.fill(x, y, x + TAB_W, y + TAB_H, on ? OBSIDIAN_2 : hover ? 0xFF241C30 : 0xFF140F1C);
            frame(g, x, y, TAB_W, TAB_H, on ? GOLD : GOLD_DARK);
            if (on) g.fill(x + TAB_W - 2, y + 1, x + TAB_W, y + TAB_H - 1, OBSIDIAN_2);
            g.renderItem(icons[i], x + 6, y + 2);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics g, int mx, int my) {
        int tab = tabAt(mx, my, leftPos, topPos);
        if (tab >= 0) {
            g.renderTooltip(font, Component.translatable(tab == 0 ? "screen.forgebound.tab.reforge" : "screen.forgebound.tab.reroll"), mx, my);
            return;
        }
        super.renderTooltip(g, mx, my);
    }

    /** Draws text, shrunk to fit {@code maxW} when it is wider; returns the width it took. */
    static int fitted(GuiGraphics g, net.minecraft.client.gui.Font font, Component text, int x, int y, int maxW) {
        int w = font.width(text);
        if (w <= maxW) {
            g.drawString(font, text, x, y, 0xFFFFFF, false);
            return w;
        }
        float s = Math.max(0.5F, (float) maxW / w);
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - s) * 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, text, 0, 0, 0xFFFFFF, false);
        g.pose().popPose();
        return (int) (w * s);
    }

    static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int a = 0; a < 360; a += 6) {
            double rad = Math.toRadians(a);
            int px = cx + (int) Math.round(Math.cos(rad) * r), py = cy + (int) Math.round(Math.sin(rad) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    static int darker(int argb, double f) {
        int r = (int) (((argb >> 16) & 255) * f), gg = (int) (((argb >> 8) & 255) * f), b = (int) ((argb & 255) * f);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }
}
