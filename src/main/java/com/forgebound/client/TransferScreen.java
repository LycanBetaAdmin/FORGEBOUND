package com.forgebound.client;

import com.forgebound.content.ModContent;
import com.forgebound.enhance.Enhancement;
import com.forgebound.enhance.Enhancements;
import com.forgebound.enhance.TransferMenu;
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

import static com.forgebound.client.ForgeAnvilScreen.*;

/** The Rune Engraving Table's Transfer tab: source item, the enhancements that will move, target item, cost. */
public class TransferScreen extends AbstractContainerScreen<TransferMenu> {
    private static final int BTN_X = 100, BTN_Y = 80, BTN_W = 68, BTN_H = 18;
    private static final int MID_X = 50, MID_Y = 22;

    public TransferScreen(TransferMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = TransferMenu.INV_Y + 82;
        inventoryLabelY = TransferMenu.INV_Y - 11;
        titleLabelX = 8;
    }

    private boolean overButton(double mx, double my) {
        int x = (int) mx - leftPos, y = (int) my - topPos;
        return x >= BTN_X && x < BTN_X + BTN_W && y >= BTN_Y && y < BTN_Y + BTN_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (tabAt(mx, my, leftPos, topPos) == 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TransferMenu.BUTTON_ENGRAVE_TAB);
                return true;
            }
            if (overButton(mx, my) && menu.blocked() == null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TransferMenu.BUTTON_TRANSFER);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int tab = tabAt(mx, my, leftPos, topPos);
        if (tab >= 0) {
            g.renderTooltip(font, Component.translatable(tab == 0 ? "screen.forgebound.tab.enhance" : "screen.forgebound.tab.transfer"), mx, my);
        } else if (overButton(mx, my)) {
            Component why = menu.blocked();
            g.renderTooltip(font, why == null ? Component.translatable("screen.forgebound.transfer.go", menu.moving().size()).withColor(0xFFD15C)
                    : why.copy().withColor(0xFF6B6B), mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, 6, GOLD_LIGHT & 0xFFFFFF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB9A6D6, false);
        g.pose().pushPose();
        g.pose().scale(0.75F, 0.75F, 1);
        drawCentered(g, Component.translatable("screen.forgebound.transfer.from"), 34, 60);
        drawCentered(g, Component.translatable("screen.forgebound.transfer.to"), 142, 60);
        g.pose().popPose();
        List<Enhancement> move = menu.moving();
        if (menu.source().isEmpty() || menu.target().isEmpty()) {
            g.drawWordWrap(font, Component.translatable("screen.forgebound.transfer.hint"), MID_X, MID_Y, 78, 0x9C88BC);
        } else {
            int y = MID_Y;
            for (Enhancement e : move.subList(0, Math.min(5, move.size()))) {
                g.pose().pushPose();
                g.pose().scale(0.75F, 0.75F, 1);
                g.drawString(font, font.plainSubstrByWidth(e.line().getString(), 104), (int) (MID_X / 0.75F), (int) (y / 0.75F), 0x80FF20, false);
                g.pose().popPose();
                y += 8;
            }
            if (move.isEmpty()) g.drawWordWrap(font, Component.translatable("screen.forgebound.transfer.nothing"), MID_X, MID_Y, 78, 0x9C88BC);
            String free = Component.translatable("screen.forgebound.transfer.free", Enhancements.free(menu.target())).getString();
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, free, (int) (MID_X / 0.75F), (int) (64 / 0.75F), Enhancements.free(menu.target()) >= move.size() ? 0xB9A6D6 : 0xFF5050, false);
            g.pose().popPose();
        }
        // material needs
        for (int s = TransferMenu.SHARD; s <= TransferMenu.DUST; s++) {
            Slot slot = menu.slots.get(s);
            int need = s == TransferMenu.SHARD ? menu.needShards() : menu.needDust();
            String txt = need <= 0 ? "-" : "x" + need;
            int col = need <= 0 ? 0x5A5068 : menu.has(s) ? 0x80FF20 : 0xFF5050;
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, txt, (int) ((slot.x + 8) / 0.75F) - font.width(txt) / 2, (int) ((slot.y + 18) / 0.75F), col, false);
            g.pose().popPose();
        }
        boolean ok = menu.blocked() == null;
        String b = Component.translatable("screen.forgebound.transfer.button").getString();
        g.drawString(font, b, BTN_X + (BTN_W - font.width(b)) / 2, BTN_Y + 5, ok ? 0x2A1A05 : 0x6A6070, false);
    }

    private void drawCentered(GuiGraphics g, Component c, int cx, int y) {
        g.drawString(font, c, (int) (cx / 0.75F) - font.width(c) / 2, (int) (y / 0.75F), 0xB9A6D6, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        drawTabs(g, x, y, 1, mx, my, EngraveScreen.TABS);
        EngraveScreen.panel(g, x, y, w, h, TransferMenu.INV_Y);
        for (int idx : new int[]{TransferMenu.SOURCE, TransferMenu.TARGET}) {
            Slot s = menu.slots.get(idx);
            int cx = x + s.x + 8, cy = y + s.y + 8;
            ring(g, cx, cy, 15, RUNE & 0x9FFFFFFF);
        }
        // arrow from source to target
        int ay = y + 75;
        g.fill(x + 44, ay, x + 128, ay + 2, menu.blocked() == null ? GOLD_LIGHT : 0xFF3A3046);
        for (int i = 0; i < 4; i++) g.fill(x + 128 + i, ay - 3 + i, x + 129 + i, ay + 5 - i, menu.blocked() == null ? GOLD_LIGHT : 0xFF3A3046);
        for (Slot s : menu.slots) slot(g, x + s.x - 1, y + s.y - 1, s.index <= TransferMenu.TARGET);
        ghost(g, menu.slots.get(TransferMenu.SOURCE), new ItemStack(Items.IRON_SWORD));
        ghost(g, menu.slots.get(TransferMenu.TARGET), new ItemStack(Items.DIAMOND_SWORD));
        ghost(g, menu.slots.get(TransferMenu.SHARD), new ItemStack(ModContent.ARCANE_SHARD.get()));
        ghost(g, menu.slots.get(TransferMenu.DUST), new ItemStack(ModContent.CRYSTAL_DUST.get()));
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
