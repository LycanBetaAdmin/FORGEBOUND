package com.forgebound.client;

import com.forgebound.anvil.RerollMenu;
import com.forgebound.content.ModContent;
import com.forgebound.tier.Tier;
import com.forgebound.unique.UniqueAttribute;
import com.forgebound.unique.Uniques;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static com.forgebound.client.ForgeAnvilScreen.*;

/**
 * The Forge Anvil's Reroll tab, in the same obsidian-and-gold style: the item's Unique Attributes (gold, with a seal
 * toggle on each when the item has two slots), the open pools, and the dice / essence / crystal / rune row.
 */
public class RerollScreen extends AbstractContainerScreen<RerollMenu> {
    private static final int BTN_X = 116, BTN_Y = 78, BTN_W = 52, BTN_H = 18;
    private static final int LIST_X = 56, LIST_Y = 30, ROW_H = 22, LOCK = 9;

    public RerollScreen(RerollMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = RerollMenu.INV_Y + 82;
        inventoryLabelY = RerollMenu.INV_Y - 11;
        titleLabelX = 8;
    }

    private boolean overButton(double mx, double my) {
        int x = (int) mx - leftPos, y = (int) my - topPos;
        return x >= BTN_X && x < BTN_X + BTN_W && y >= BTN_Y && y < BTN_Y + BTN_H;
    }

    /** Attribute row under the mouse (0/1), or -1. */
    private int rowAt(double mx, double my) {
        int x = (int) mx - leftPos, y = (int) my - topPos;
        if (x < LIST_X || x >= imageWidth - 8 || y < LIST_Y) return -1;
        int i = (y - LIST_Y) / ROW_H;
        return i < Uniques.get(menu.gear()).size() ? i : -1;
    }

    private boolean overLock(double mx, double my, int row) {
        int x = (int) mx - leftPos, y = (int) my - topPos, ly = LIST_Y + row * ROW_H;
        return x >= LIST_X && x < LIST_X + LOCK && y >= ly && y < ly + LOCK;
    }

    private boolean canSeal() {
        return !menu.gear().isEmpty() && Uniques.slots(menu.tier()) > 1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (tabAt(mx, my, leftPos, topPos) == 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RerollMenu.BUTTON_REFORGE_TAB);
                return true;
            }
            int row = rowAt(mx, my);
            if (row >= 0 && canSeal() && overLock(mx, my, row)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RerollMenu.BUTTON_SEAL + row);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            if (overButton(mx, my) && menu.blocked() == null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RerollMenu.BUTTON_REROLL);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        List<Component> tip = new ArrayList<>();
        int tab = tabAt(mx, my, leftPos, topPos);
        int row = rowAt(mx, my);
        if (tab >= 0) {
            tip.add(Component.translatable(tab == 0 ? "screen.forgebound.tab.reforge" : "screen.forgebound.tab.reroll"));
        } else if (overButton(mx, my)) {
            Component why = menu.blocked();
            tip.add(why == null ? Component.translatable("screen.forgebound.reroll.go").withColor(0xFFD15C) : why.copy().withColor(0xFF6B6B));
            if (why == null && menu.premium()) tip.add(Component.translatable("screen.forgebound.reroll.premium").withColor(0xE3B8FF));
        } else if (row >= 0) {
            UniqueAttribute a = Uniques.get(menu.gear()).get(row);
            tip.add(a.displayName());
            tip.add(a.description(menu.tier().level()));
            if (canSeal()) tip.add(Component.translatable(menu.sealed(row) ? "screen.forgebound.reroll.unseal" : "screen.forgebound.reroll.seal").withColor(0x9C88BC));
        }
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mx, my);
        else renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, 6, GOLD_LIGHT & 0xFFFFFF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB9A6D6, false);
        ItemStack gear = menu.gear();
        if (gear.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("screen.forgebound.reroll.hint"), LIST_X, 20, 110, 0x9C88BC);
            return;
        }
        Tier tier = menu.tier();
        fitted(g, font, tier.label(), LIST_X, 19, imageWidth - 8 - LIST_X);
        List<UniqueAttribute> attrs = Uniques.get(gear);
        int slots = Uniques.slots(tier);
        for (int i = 0; i < slots; i++) {
            int y = LIST_Y + i * ROW_H;
            int tx = LIST_X + (slots > 1 ? LOCK + 3 : 0);
            if (i < attrs.size()) {
                UniqueAttribute a = attrs.get(i);
                g.drawString(font, a.displayName(), tx, y, 0xFFD15C, false);
                // the description, wrapped onto up to two small lines (the full text is in the hover tooltip)
                g.pose().pushPose();
                g.pose().scale(0.6F, 0.6F, 1);
                var lines = font.split(a.description(tier.level()), (int) ((imageWidth - 8 - tx) / 0.6F));
                for (int l = 0; l < Math.min(2, lines.size()); l++) {
                    g.drawString(font, lines.get(l), (int) (tx / 0.6F), (int) ((y + 9.5F + l * 5.5F) / 0.6F), 0xB9A6D6, false);
                }
                g.pose().popPose();
            } else {
                g.drawString(font, Component.translatable("screen.forgebound.reroll.empty"), tx, y, 0x6E5E84, false);
            }
        }
        // material needs under each slot, green when the slot holds enough
        for (int s = RerollMenu.DICE; s <= RerollMenu.RUNE; s++) {
            Slot slot = menu.slots.get(s);
            int need = menu.need(s);
            String txt = need <= 0 ? (s == RerollMenu.CRYSTAL || s == RerollMenu.RUNE ? "opt" : "-") : "x" + need;
            int col = need <= 0 ? 0x5A5068 : menu.has(s) ? 0x80FF20 : 0xFF5050;
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, txt, (int) ((slot.x + 8) / 0.75F) - font.width(txt) / 2, (int) ((slot.y + 18) / 0.75F), col, false);
            g.pose().popPose();
        }
        boolean ok = menu.blocked() == null;
        String b = Component.translatable("screen.forgebound.reroll.button").getString();
        g.drawString(font, b, BTN_X + (BTN_W - font.width(b)) / 2, BTN_Y + 5, ok ? 0x2A1A05 : 0x6A6070, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        drawTabs(g, x, y, 1, mx, my);
        g.fill(x, y, x + w, y + h, 0xFF06040A);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, OBSIDIAN_2, OBSIDIAN);
        frame(g, x + 1, y + 1, w - 2, h - 2, GOLD_DARK);
        frame(g, x + 2, y + 2, w - 4, h - 4, GOLD);
        for (int[] c : new int[][]{{x + 1, y + 1}, {x + w - 6, y + 1}, {x + 1, y + h - 6}, {x + w - 6, y + h - 6}}) {
            g.fill(c[0], c[1], c[0] + 5, c[1] + 5, GOLD_LIGHT);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 4, c[1] + 4, GOLD_DARK);
        }
        g.fill(x + 6, y + 15, x + w - 6, y + RerollMenu.INV_Y - 14, STEEL_DARK);
        frame(g, x + 6, y + 15, w - 12, RerollMenu.INV_Y - 29, STEEL);
        g.fill(x + 10, y + RerollMenu.INV_Y - 13, x + w - 10, y + RerollMenu.INV_Y - 12, GOLD_DARK);
        g.fill(x + w / 2 - 2, y + RerollMenu.INV_Y - 14, x + w / 2 + 2, y + RerollMenu.INV_Y - 11, GOLD_LIGHT);
        // gold "Unique Attribute" banner behind the list
        g.fill(x + LIST_X - 3, y + LIST_Y - 2, x + w - 8, y + LIST_Y - 1, GOLD_DARK);
        // golden ring around the equipment (blue on the Reforge tab)
        Slot gear = menu.slots.get(RerollMenu.GEAR);
        int cx = x + gear.x + 8, cy = y + gear.y + 8;
        ring(g, cx, cy, 17, 0x9FFFD15C);
        ring(g, cx, cy, 15, 0x5FB8862B);
        for (int[] d : new int[][]{{0, -18}, {0, 18}, {-18, 0}, {18, 0}}) g.fill(cx + d[0] - 1, cy + d[1] - 1, cx + d[0] + 2, cy + d[1] + 2, GOLD_LIGHT);
        // seal toggles
        if (canSeal()) {
            int n = Uniques.get(menu.gear()).size();
            for (int i = 0; i < n; i++) {
                int lx = x + LIST_X, ly = y + LIST_Y + i * ROW_H;
                boolean on = menu.sealed(i);
                g.fill(lx, ly, lx + LOCK, ly + LOCK, on ? 0xFF6A4A12 : 0xFF2A2333);
                frame(g, lx, ly, LOCK, LOCK, on ? GOLD_LIGHT : STEEL);
                if (on) g.fill(lx + 3, ly + 3, lx + LOCK - 3, ly + LOCK - 3, GOLD_LIGHT);
            }
        }
        for (Slot s : menu.slots) slot(g, x + s.x - 1, y + s.y - 1, s.index == RerollMenu.GEAR);
        ghost(g, menu.slots.get(RerollMenu.GEAR), new ItemStack(Items.IRON_SWORD));
        ghost(g, menu.slots.get(RerollMenu.DICE), new ItemStack(ModContent.FORTUNE_DICE.get()));
        ghost(g, menu.slots.get(RerollMenu.ESSENCE), new ItemStack(ModContent.SOUL_ESSENCE.get()));
        ghost(g, menu.slots.get(RerollMenu.CRYSTAL), new ItemStack(ModContent.FORTUNE_CRYSTAL.get()));
        ghost(g, menu.slots.get(RerollMenu.RUNE), new ItemStack(ModContent.SEALING_RUNE.get()));
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
}
