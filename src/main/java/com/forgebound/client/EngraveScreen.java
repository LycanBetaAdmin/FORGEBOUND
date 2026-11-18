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

import java.util.List;

import static com.forgebound.client.ForgeUi.*;

/**
 * The Rune Engraving Table, Enhance tab: the item in a violet rune circle with its tier and "Enhancements n/max", the
 * cost (Arcane Shards and XP levels) and the Engrave / Remove button. On the right: Available Enhancements, a checklist
 * (engraved ones ticked in gold); click one to select it, then press the button.
 */
public class EngraveScreen extends AbstractContainerScreen<EngraveMenu> {
    static final ItemStack[] TAB_ICONS = {new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.ENDER_EYE)};
    static final Component[] TAB_LABELS = {Component.translatable("screen.forgebound.tab.enhance.short"), Component.translatable("screen.forgebound.tab.transfer.short")};
    private static final int BTN_X = 98, BTN_Y = 130, BTN_W = 64, BTN_H = 18;
    private static final int LIST_X = RIGHT_X + 5, LIST_Y = 42, ROW_H = 11, BOX = 7;
    private int selected = -1;

    public EngraveScreen(EngraveMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = W;
        imageHeight = H;
        inventoryLabelX = INV_X;
        inventoryLabelY = INV_Y - 11;
    }

    private int rowAt(double mx, double my) {
        int x = (int) mx - leftPos, y = (int) my - topPos;
        if (x < LIST_X || x >= RIGHT_X + RIGHT_W - 3 || y < LIST_Y) return -1;
        int i = (y - LIST_Y) / ROW_H;
        return i < menu.offered().size() ? i : -1;
    }

    private Enhancement selection() {
        List<Enhancement> offered = menu.offered();
        return selected >= 0 && selected < offered.size() ? offered.get(selected) : null;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (selected >= menu.offered().size()) selected = -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            if (tabAt(mx, my, leftPos, topPos, 2) == 1) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, EngraveMenu.BUTTON_TRANSFER_TAB);
                return true;
            }
            int row = rowAt(mx, my);
            if (row >= 0) {
                selected = row;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8F));
                return true;
            }
            Enhancement e = selection();
            if (e != null && over(mx, my, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H) && menu.blocked(minecraft.player, e) == null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean engraved(Enhancement e) {
        return e != null && Enhancements.get(menu.gear()).contains(e);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int tab = tabAt(mx, my, leftPos, topPos, 2);
        int row = rowAt(mx, my);
        if (tab >= 0) {
            g.renderTooltip(font, Component.translatable(tab == 0 ? "screen.forgebound.tab.enhance" : "screen.forgebound.tab.transfer"), mx, my);
        } else if (row >= 0) {
            g.renderTooltip(font, menu.offered().get(row).line(), mx, my);
        } else if (over(mx, my, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H) && minecraft != null && minecraft.player != null) {
            Enhancement e = selection();
            Component why = e == null ? Component.translatable("screen.forgebound.engrave.select") : menu.blocked(minecraft.player, e);
            if (why != null) g.renderTooltip(font, why.copy().withColor(0xFF6B6B), mx, my);
            else g.renderTooltip(font, e.line(), mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LAVENDER, false);
        ItemStack gear = menu.gear();
        Enhancement sel = selection();
        if (gear.isEmpty()) {
            smallCentered(g, Component.translatable("screen.forgebound.engrave.place"), 130, 72, MUTED);
        } else {
            Tier tier = menu.tier();
            smallCentered(g, Component.literal("Tier " + tier.roman() + " · ").append(tier.displayName()), 130, 69, tier.color());
            int have = Enhancements.get(gear).size(), max = Enhancements.slots(tier);
            smallCentered(g, Component.translatable("screen.forgebound.engrave.count", have, max), 130, 77, have >= max ? 0xFFB347 : LAVENDER);
        }
        header(g, font, CENTER_X, CENTER_W, 86, Component.translatable("screen.forgebound.engrave.cost"));
        if (!gear.isEmpty()) {
            Tier tier = menu.tier();
            boolean removing = engraved(sel);
            int shards = removing ? Enhancements.removeShards(tier) : Enhancements.applyShards(tier);
            Slot shard = menu.slots.get(EngraveMenu.SHARD);
            smallCentered(g, Component.literal(menu.shards() + "/" + shards), shard.x + 8, shard.y + 19, menu.shards() >= shards ? GREEN : RED);
            int levels = removing ? 0 : Enhancements.applyLevels(tier);
            boolean ok = levels == 0 || minecraft == null || minecraft.player == null || minecraft.player.getAbilities().instabuild || minecraft.player.experienceLevel >= levels;
            small(g, font, Component.translatable("screen.forgebound.engrave.levels", levels), 112, 104, ok ? GREEN : RED);
            small(g, font, Component.translatable(removing ? "screen.forgebound.engrave.no_refund" : "screen.forgebound.engrave.per"), 112, 112, MUTED);
        }
        // right: the checklist
        header(g, font, RIGHT_X, RIGHT_W, 28, Component.translatable("screen.forgebound.engrave.available"));
        List<Enhancement> offered = menu.offered();
        if (offered.isEmpty()) {
            wrapped(g, font, Component.translatable("screen.forgebound.engrave.hint"), LIST_X + 1, 44, RIGHT_W - 12, 6, MUTED);
            return;
        }
        List<Enhancement> have = Enhancements.get(gear);
        int hover = rowAt(mx, my);
        for (int i = 0; i < offered.size(); i++) {
            Enhancement e = offered.get(i);
            int y = LIST_Y + i * ROW_H;
            boolean on = have.contains(e);
            if (i == selected) g.fill(LIST_X - 2, y - 2, RIGHT_X + RIGHT_W - 3, y + ROW_H - 2, 0x50FFD15C);
            else if (i == hover) g.fill(LIST_X - 2, y - 2, RIGHT_X + RIGHT_W - 3, y + ROW_H - 2, 0x25FFFFFF);
            String v = e.value();
            int vx = RIGHT_X + RIGHT_W - 6 - (int) (font.width(v) * 0.75F);
            fitted(g, font, e.displayName(), LIST_X + BOX + 3, y, vx - (LIST_X + BOX + 5), on ? GREEN : IVORY);
            small(g, font, Component.literal(v), vx, y + 1, on ? GREEN : GOLD_LIGHT & 0xFFFFFF);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        panel(g, font, x, y, title);
        tabs(g, font, x, y, 0, mx, my, TAB_ICONS, TAB_LABELS);
        runeCircle(g, x + 105, y + 52, 14, VIOLET);
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
        if (!menu.slots.get(EngraveMenu.GEAR).hasItem()) ghost(g, x + 97, y + 44, new ItemStack(Items.IRON_SWORD));
        if (!menu.slots.get(EngraveMenu.SHARD).hasItem()) ghost(g, x + 88, y + 100, new ItemStack(ModContent.ARCANE_SHARD.get()));
        Enhancement sel = selection();
        boolean ok = sel != null && minecraft != null && minecraft.player != null && menu.blocked(minecraft.player, sel) == null;
        Component label = Component.translatable(engraved(sel) ? "screen.forgebound.engrave.remove_btn" : "screen.forgebound.engrave.apply_btn");
        button(g, font, x + BTN_X, y + BTN_Y, BTN_W, BTN_H, label, ok, over(mx, my, x + BTN_X, y + BTN_Y, BTN_W, BTN_H));
    }
}
