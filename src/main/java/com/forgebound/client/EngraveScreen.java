package com.forgebound.client;

import com.forgebound.content.ModContent;
import com.forgebound.enhance.EngraveMenu;
import com.forgebound.enhance.Enhancement;
import com.forgebound.enhance.Enhancements;
import com.forgebound.tier.Tier;
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
 * The Rune Engraving Table's Enhance tab: the item in a blue rune ring, the enhancements for its kind of gear as a
 * checklist (engraved ones ticked in gold), "Enhancements n / max", and the Arcane Shard slot. Click a row to engrave
 * it, click a ticked one to remove it.
 */
public class EngraveScreen extends AbstractContainerScreen<EngraveMenu> {
    static final ItemStack[] TABS = {new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.ENDER_EYE)};
    private static final int LIST_X = 56, LIST_Y = 42, ROW_H = 10, BOX = 7;

    public EngraveScreen(EngraveMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = EngraveMenu.INV_Y + 82;
        inventoryLabelY = EngraveMenu.INV_Y - 11;
        titleLabelX = 8;
    }

    private int rowAt(double mx, double my) {
        int x = (int) mx - leftPos, y = (int) my - topPos;
        if (x < LIST_X || x >= imageWidth - 8 || y < LIST_Y) return -1;
        int i = (y - LIST_Y) / ROW_H;
        return i < menu.offered().size() ? i : -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            if (tabAt(mx, my, leftPos, topPos) == 1) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, EngraveMenu.BUTTON_TRANSFER_TAB);
                return true;
            }
            int row = rowAt(mx, my);
            if (row >= 0 && menu.blocked(minecraft.player, menu.offered().get(row)) == null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, row);
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
            tip.add(Component.translatable(tab == 0 ? "screen.forgebound.tab.enhance" : "screen.forgebound.tab.transfer"));
        } else if (row >= 0 && minecraft != null && minecraft.player != null) {
            Enhancement e = menu.offered().get(row);
            Tier tier = menu.tier();
            boolean has = Enhancements.get(menu.gear()).contains(e);
            tip.add(e.line());
            Component why = menu.blocked(minecraft.player, e);
            if (why != null) tip.add(why.copy().withColor(0xFF6B6B));
            else if (has) tip.add(Component.translatable("screen.forgebound.engrave.remove", Enhancements.removeShards(tier)).withColor(0xFFB347));
            else tip.add(Component.translatable("screen.forgebound.engrave.apply", Enhancements.applyShards(tier), Enhancements.applyLevels(tier)).withColor(0xFFD15C));
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
            g.drawWordWrap(font, Component.translatable("screen.forgebound.engrave.hint"), LIST_X, 20, 110, 0x9C88BC);
            return;
        }
        Tier tier = menu.tier();
        fitted(g, font, tier.label(), LIST_X, 18, imageWidth - 8 - LIST_X);
        List<Enhancement> have = Enhancements.get(gear);
        int max = Enhancements.slots(tier);
        g.drawString(font, Component.translatable("screen.forgebound.engrave.count", have.size(), max), LIST_X, 29,
                have.size() >= max ? 0xFFB347 : 0xB9A6D6, false);
        List<Enhancement> offered = menu.offered();
        int hover = rowAt(mx, my);
        for (int i = 0; i < offered.size(); i++) {
            Enhancement e = offered.get(i);
            int y = LIST_Y + i * ROW_H;
            boolean on = have.contains(e);
            boolean ok = minecraft != null && minecraft.player != null && menu.blocked(minecraft.player, e) == null;
            if (i == hover) g.fill(LIST_X - 2, y - 1, imageWidth - 8, y + ROW_H - 1, 0x40FFD15C);
            int name = on ? 0x80FF20 : ok ? 0xE8E0ED : 0x6E6480;
            String v = e.value();
            int nx = LIST_X + BOX + 4, avail = imageWidth - 13 - font.width(v) - nx;
            int nw = font.width(e.displayName());
            if (nw <= avail) {
                g.drawString(font, e.displayName(), nx, y, name, false);
            } else { // shrink long names to fit beside the value
                float s = Math.max(0.6F, (float) avail / nw);
                g.pose().pushPose();
                g.pose().translate(nx, y + (1 - s) * 4, 0);
                g.pose().scale(s, s, 1);
                g.drawString(font, e.displayName(), 0, 0, name, false);
                g.pose().popPose();
            }
            g.drawString(font, v, imageWidth - 10 - font.width(v), y, on ? 0x80FF20 : ok ? 0xFFD15C : 0x6E6480, false);
        }
        // shard need under the shard slot
        Slot shard = menu.slots.get(EngraveMenu.SHARD);
        String txt = "x" + Enhancements.applyShards(tier);
        int col = menu.shards() >= Enhancements.applyShards(tier) ? 0x80FF20 : 0xFF5050;
        g.pose().pushPose();
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, txt, (int) ((shard.x + 8) / 0.75F) - font.width(txt) / 2, (int) ((shard.y + 18) / 0.75F), col, false);
        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        drawTabs(g, x, y, 0, mx, my, TABS);
        panel(g, x, y, w, h, EngraveMenu.INV_Y);
        Slot gear = menu.slots.get(EngraveMenu.GEAR);
        int cx = x + gear.x + 8, cy = y + gear.y + 8;
        ring(g, cx, cy, 17, RUNE & 0x9FFFFFFF);
        ring(g, cx, cy, 15, 0x5F2A90C8);
        for (int[] d : new int[][]{{0, -18}, {0, 18}, {-18, 0}, {18, 0}}) g.fill(cx + d[0] - 1, cy + d[1] - 1, cx + d[0] + 2, cy + d[1] + 2, RUNE);
        // checklist boxes
        List<Enhancement> have = Enhancements.get(menu.gear());
        List<Enhancement> offered = menu.offered();
        for (int i = 0; i < offered.size(); i++) {
            int bx = x + LIST_X, by = y + LIST_Y + i * ROW_H;
            boolean on = have.contains(offered.get(i));
            g.fill(bx, by, bx + BOX, by + BOX, on ? 0xFF6A4A12 : 0xFF1A1424);
            frame(g, bx, by, BOX, BOX, on ? GOLD_LIGHT : STEEL);
            if (on) g.fill(bx + 2, by + 2, bx + BOX - 2, by + BOX - 2, GOLD_LIGHT);
        }
        for (Slot s : menu.slots) slot(g, x + s.x - 1, y + s.y - 1, s.index == EngraveMenu.GEAR);
        ghost(g, menu.slots.get(EngraveMenu.GEAR), new ItemStack(Items.IRON_SWORD));
        ghost(g, menu.slots.get(EngraveMenu.SHARD), new ItemStack(ModContent.ARCANE_SHARD.get()));
    }

    private void ghost(GuiGraphics g, Slot s, ItemStack hint) {
        if (s.hasItem()) return;
        g.renderFakeItem(hint, leftPos + s.x, topPos + s.y);
        g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xB8201A2A);
    }

    /** The shared obsidian panel: gold double frame with corner studs, steel inset above the inventory. */
    static void panel(GuiGraphics g, int x, int y, int w, int h, int invY) {
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
}
