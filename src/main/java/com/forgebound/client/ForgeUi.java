package com.forgebound.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The FORGEBOUND screen kit (Forge Anvil and Rune Engraving Table): a 300 x 262 obsidian panel with a double antique
 * gold frame and corner studs, a gold title banner, a labelled tab column on the left, a centre work panel and a right
 * info panel with gold-cornered steel insets, and the player inventory centred below. Drawn without textures.
 */
public final class ForgeUi {
    public static final int W = 300, H = 262, INV_Y = 178, INV_X = 70;
    public static final int TABS_X = 8, TABS_W = 66, CENTER_X = 80, CENTER_W = 100, RIGHT_X = 186, RIGHT_W = 106, PANEL_Y = 24, PANEL_H = 136;
    public static final int TAB_H = 22;
    public static final int OBSIDIAN = 0xFF100D18, OBSIDIAN_2 = 0xFF1B1526, STEEL = 0xFF51465F, STEEL_DARK = 0xFF221B2C, INSET = 0xFF16111F;
    public static final int GOLD = 0xFFD6A84B, GOLD_LIGHT = 0xFFFFD15C, GOLD_DARK = 0xFF7A5A22, VIOLET = 0xFFB45CFF, VIOLET_DIM = 0x8F7A3FC0;
    public static final int IVORY = 0xE8E0ED, LAVENDER = 0xB9A6D6, MUTED = 0x6E6480, GREEN = 0x80FF20, RED = 0xFF5050;

    private ForgeUi() {
    }

    // ------------------------------------------------------------------ primitives

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static int darker(int argb, double f) {
        int r = (int) (((argb >> 16) & 255) * f), gg = (int) (((argb >> 8) & 255) * f), b = (int) ((argb & 255) * f);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    /** Gold L-brackets in the four corners of a box. */
    public static void corners(GuiGraphics g, int x, int y, int w, int h, int color) {
        int l = 5;
        g.fill(x, y, x + l, y + 1, color);
        g.fill(x, y, x + 1, y + l, color);
        g.fill(x + w - l, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + l, color);
        g.fill(x, y + h - 1, x + l, y + h, color);
        g.fill(x, y + h - l, x + 1, y + h, color);
        g.fill(x + w - l, y + h - 1, x + w, y + h, color);
        g.fill(x + w - 1, y + h - l, x + w, y + h, color);
    }

    public static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int a = 0; a < 360; a += 4) {
            double rad = Math.toRadians(a);
            int px = cx + (int) Math.round(Math.cos(rad) * r), py = cy + (int) Math.round(Math.sin(rad) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** A rune circle round a slot: two rings and four studs. */
    public static void runeCircle(GuiGraphics g, int cx, int cy, int outer, int color) {
        ring(g, cx, cy, outer, color);
        ring(g, cx, cy, outer - 2, (color & 0x00FFFFFF) | 0x60000000);
        for (int[] d : new int[][]{{0, -outer - 1}, {0, outer + 1}, {-outer - 1, 0}, {outer + 1, 0}}) {
            g.fill(cx + d[0] - 1, cy + d[1] - 1, cx + d[0] + 2, cy + d[1] + 2, color | 0xFF000000);
        }
    }

    /** An inventory slot frame (inner 16 x 16 at x+1, y+1); {@code big} gets a gold bevel. */
    public static void slot(GuiGraphics g, int x, int y, boolean big) {
        int grow = big ? 2 : 0;
        int x0 = x - grow, y0 = y - grow, x1 = x + 18 + grow, y1 = y + 18 + grow;
        g.fill(x0, y0, x1, y1, big ? 0xFF3A2A52 : 0xFF2E2638);
        g.fill(x0, y0, x1 - 1, y0 + 1, 0xFF0A0710);
        g.fill(x0, y0, x0 + 1, y1 - 1, 0xFF0A0710);
        g.fill(x0 + 1, y1 - 1, x1, y1, big ? GOLD : 0xFF6E5E84);
        g.fill(x1 - 1, y0 + 1, x1, y1, big ? GOLD : 0xFF6E5E84);
    }

    /** A faded hint item in an empty slot. */
    public static void ghost(GuiGraphics g, int x, int y, ItemStack hint) {
        if (hint.isEmpty()) return;
        g.renderFakeItem(hint, x, y);
        g.fill(x, y, x + 16, y + 16, 0xB8201A2A);
    }

    // ------------------------------------------------------------------ the panel

    /** The whole background: frame, title banner, tab column, centre and right insets, inventory divider. */
    public static void panel(GuiGraphics g, Font font, int x, int y, Component title) {
        g.fill(x, y, x + W, y + H, 0xFF06040A);
        g.fillGradient(x + 1, y + 1, x + W - 1, y + H - 1, OBSIDIAN_2, OBSIDIAN);
        frame(g, x + 1, y + 1, W - 2, H - 2, GOLD_DARK);
        frame(g, x + 3, y + 3, W - 6, H - 6, GOLD);
        for (int[] c : new int[][]{{x, y}, {x + W - 7, y}, {x, y + H - 7}, {x + W - 7, y + H - 7}}) {
            g.fill(c[0], c[1], c[0] + 7, c[1] + 7, GOLD_LIGHT);
            g.fill(c[0] + 2, c[1] + 2, c[0] + 5, c[1] + 5, GOLD_DARK);
            g.fill(c[0] + 3, c[1] + 3, c[0] + 4, c[1] + 4, VIOLET);
        }
        titleBanner(g, font, x + W / 2, y + 4, title);
        inset(g, x + TABS_X, y + PANEL_Y, TABS_W, PANEL_H, false);
        inset(g, x + CENTER_X, y + PANEL_Y, CENTER_W, PANEL_H, true);
        inset(g, x + RIGHT_X, y + PANEL_Y, RIGHT_W, PANEL_H, true);
        int dy = y + INV_Y - 14;
        g.fill(x + 12, dy, x + W - 12, dy + 1, GOLD_DARK);
        g.fill(x + W / 2 - 3, dy - 1, x + W / 2 + 3, dy + 2, GOLD_LIGHT);
        g.fill(x + W / 2 - 1, dy - 2, x + W / 2 + 1, dy + 3, VIOLET);
    }

    /** The compact single-column panel (the Ruinforged Crucible): gold double frame, steel inset above the inventory. */
    public static void compactPanel(GuiGraphics g, int x, int y, int w, int h, int invY) {
        g.fill(x, y, x + w, y + h, 0xFF06040A);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, OBSIDIAN_2, OBSIDIAN);
        frame(g, x + 1, y + 1, w - 2, h - 2, GOLD_DARK);
        frame(g, x + 2, y + 2, w - 4, h - 4, GOLD);
        for (int[] c : new int[][]{{x + 1, y + 1}, {x + w - 6, y + 1}, {x + 1, y + h - 6}, {x + w - 6, y + h - 6}}) {
            g.fill(c[0], c[1], c[0] + 5, c[1] + 5, GOLD_LIGHT);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 4, c[1] + 4, GOLD_DARK);
        }
        g.fill(x + 6, y + 15, x + w - 6, y + invY - 14, STEEL_DARK);
        frame(g, x + 6, y + 15, w - 12, invY - 29, STEEL);
        g.fill(x + 10, y + invY - 13, x + w - 10, y + invY - 12, GOLD_DARK);
        g.fill(x + w / 2 - 2, y + invY - 14, x + w / 2 + 2, y + invY - 11, GOLD_LIGHT);
    }

    private static void inset(GuiGraphics g, int x, int y, int w, int h, boolean cornered) {
        g.fill(x, y, x + w, y + h, INSET);
        frame(g, x, y, w, h, STEEL_DARK);
        frame(g, x + 1, y + 1, w - 2, h - 2, 0xFF2E2540);
        if (cornered) corners(g, x, y, w, h, GOLD);
    }

    /** Gold title box centred on cx, with ornament wings. */
    public static void titleBanner(GuiGraphics g, Font font, int cx, int y, Component title) {
        int tw = font.width(title);
        int x0 = cx - tw / 2 - 10, x1 = cx + tw / 2 + 10;
        g.fill(x0 - 30, y + 8, x0 - 2, y + 9, GOLD_DARK);
        g.fill(x1 + 2, y + 8, x1 + 30, y + 9, GOLD_DARK);
        g.fill(x0 - 32, y + 7, x0 - 30, y + 10, GOLD);
        g.fill(x1 + 30, y + 7, x1 + 32, y + 10, GOLD);
        g.fill(x0, y + 2, x1, y + 15, 0xFF2A1F12);
        frame(g, x0, y + 2, x1 - x0, 13, GOLD);
        g.fill(x0 + 1, y + 3, x1 - 1, y + 4, GOLD_DARK);
        g.drawString(font, title, cx - tw / 2, y + 5, GOLD_LIGHT & 0xFFFFFF, false);
    }

    /** A small section caption with a gold underline, centred in a panel column. */
    public static void header(GuiGraphics g, Font font, int x, int w, int y, Component text) {
        int tw = font.width(text);
        g.drawString(font, text, x + (w - tw) / 2, y, GOLD_LIGHT & 0xFFFFFF, false);
        g.fill(x + 6, y + 10, x + w - 6, y + 11, GOLD_DARK);
        g.fill(x + w / 2 - 1, y + 9, x + w / 2 + 2, y + 12, GOLD);
    }

    // ------------------------------------------------------------------ tabs

    /** Tab index under the mouse, or -1. */
    public static int tabAt(double mx, double my, int left, int top, int count) {
        for (int i = 0; i < count; i++) {
            int x = left + TABS_X + 3, y = top + PANEL_Y + 4 + i * (TAB_H + 3);
            if (mx >= x && mx < x + TABS_W - 6 && my >= y && my < y + TAB_H) return i;
        }
        return -1;
    }

    public static void tabs(GuiGraphics g, Font font, int left, int top, int active, int mx, int my, ItemStack[] icons, Component[] labels) {
        for (int i = 0; i < icons.length; i++) {
            int x = left + TABS_X + 3, y = top + PANEL_Y + 4 + i * (TAB_H + 3), w = TABS_W - 6;
            boolean on = i == active, hover = tabAt(mx, my, left, top, icons.length) == i;
            g.fill(x, y, x + w, y + TAB_H, on ? 0xFF3A2A1A : hover ? 0xFF2A2236 : 0xFF1C1626);
            frame(g, x, y, w, TAB_H, on ? GOLD : hover ? 0xFF6E5E84 : STEEL_DARK);
            if (on) {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, GOLD_LIGHT);
                g.fill(x + w, y + TAB_H / 2 - 2, x + w + 2, y + TAB_H / 2 + 2, GOLD); // arrow nub towards the work panel
            }
            g.renderItem(icons[i], x + 3, y + 3);
            fitted(g, font, labels[i], x + 21, y + 7, w - 24, on ? GOLD_LIGHT & 0xFFFFFF : LAVENDER);
        }
    }

    // ------------------------------------------------------------------ button

    public static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, boolean enabled, boolean hover) {
        g.fill(x, y, x + w, y + h, enabled ? GOLD_DARK : 0xFF2A2333);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, enabled ? (hover ? 0xFFFFE08A : GOLD_LIGHT) : 0xFF3A3046, enabled ? GOLD : 0xFF2A2333);
        frame(g, x, y, w, h, enabled ? 0xFFFFF0C0 : STEEL);
        g.fill(x + 2, y + 2, x + 4, y + 3, enabled ? 0xFFFFF6D8 : STEEL);
        int tw = font.width(label);
        g.drawString(font, label, x + (w - tw) / 2, y + (h - 8) / 2, enabled ? 0x2A1A05 : 0x6A6070, false);
    }

    /** Text shrunk to fit {@code maxW} when wider; returns the width drawn. */
    public static int fitted(GuiGraphics g, Font font, Component text, int x, int y, int maxW, int color) {
        int w = font.width(text);
        if (w <= maxW) {
            g.drawString(font, text, x, y, color, false);
            return w;
        }
        float s = Math.max(0.5F, (float) maxW / w);
        g.pose().pushPose();
        g.pose().translate(x, y + (1 - s) * 4, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
        return (int) (w * s);
    }

    /** Small text (scale 0.75). */
    public static void small(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Small text centred on cx. */
    public static void smallCentered(GuiGraphics g, Font font, Component text, int cx, int y, int color) {
        small(g, font, text, cx - (int) (font.width(text) * 0.75F / 2), y, color);
    }

    /** Small text centred on cx but kept inside the centre panel. */
    public static void centerLabel(GuiGraphics g, Component text, int cx, int y, int color) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        int half = (int) (font.width(text) * 0.75F / 2) + 1;
        cx = Math.max(CENTER_X + 3 + half, Math.min(CENTER_X + CENTER_W - 3 - half, cx));
        smallCentered(g, font, text, cx, y, color);
    }

    /** Tiny text (scale 0.65) centred on cx: material counts under slots. */
    public static void tinyCentered(GuiGraphics g, Component text, int cx, int y, int color) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(cx - font.width(text) * 0.65F / 2, y, 0);
        g.pose().scale(0.65F, 0.65F, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Small text centred on cx, with the game font. */
    public static void smallCentered(GuiGraphics g, Component text, int cx, int y, int color) {
        smallCentered(g, net.minecraft.client.Minecraft.getInstance().font, text, cx, y, color);
    }

    /** Small wrapped text, up to {@code lines} lines. */
    public static void wrapped(GuiGraphics g, Font font, Component text, int x, int y, int w, int lines, int color) {
        var split = font.split(text, (int) (w / 0.75F));
        for (int i = 0; i < Math.min(lines, split.size()); i++) {
            g.pose().pushPose();
            g.pose().translate(x, y + i * 7, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, split.get(i), 0, 0, color, false);
            g.pose().popPose();
        }
    }
}
