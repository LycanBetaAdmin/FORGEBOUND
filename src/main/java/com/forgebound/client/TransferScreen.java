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

import static com.forgebound.client.ForgeUi.*;

/**
 * The Rune Engraving Table, Transfer tab: source and target in rune circles with a gold arrow between them, the cost
 * (Arcane Shards and Crystal Dust per enhancement) and the Transfer button. On the right: what will move and the
 * target's free slots.
 */
public class TransferScreen extends AbstractContainerScreen<TransferMenu> {
    private static final int BTN_X = 98, BTN_Y = 130, BTN_W = 64, BTN_H = 18;

    public TransferScreen(TransferMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = W;
        imageHeight = H;
        inventoryLabelX = INV_X;
        inventoryLabelY = INV_Y - 11;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (tabAt(mx, my, leftPos, topPos, 2) == 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TransferMenu.BUTTON_ENGRAVE_TAB);
                return true;
            }
            if (over(mx, my, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H) && menu.blocked() == null) {
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
        int tab = tabAt(mx, my, leftPos, topPos, 2);
        if (tab >= 0) {
            g.renderTooltip(font, Component.translatable(tab == 0 ? "screen.forgebound.tab.enhance" : "screen.forgebound.tab.transfer"), mx, my);
        } else if (over(mx, my, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H)) {
            Component why = menu.blocked();
            g.renderTooltip(font, why == null ? Component.translatable("screen.forgebound.transfer.go", menu.moving().size()).withColor(0xFFD15C)
                    : why.copy().withColor(0xFF6B6B), mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LAVENDER, false);
        smallCentered(g, Component.translatable("screen.forgebound.transfer.from"), 96, 69, LAVENDER);
        smallCentered(g, Component.translatable("screen.forgebound.transfer.to"), 158, 69, LAVENDER);
        header(g, font, CENTER_X, CENTER_W, 86, Component.translatable("screen.forgebound.engrave.cost"));
        for (int s = TransferMenu.SHARD; s <= TransferMenu.DUST; s++) {
            Slot slot = menu.slots.get(s);
            int need = s == TransferMenu.SHARD ? menu.needShards() : menu.needDust();
            String txt = need <= 0 ? "-" : menu.slots.get(s).getItem().getCount() + "/" + need;
            tinyCentered(g, Component.literal(txt), slot.x + 8, slot.y + 19, need <= 0 ? MUTED : menu.has(s) ? GREEN : RED);
        }
        header(g, font, RIGHT_X, RIGHT_W, 28, Component.translatable("screen.forgebound.transfer.moving"));
        int x = RIGHT_X + 6;
        if (menu.source().isEmpty() || menu.target().isEmpty()) {
            wrapped(g, font, Component.translatable("screen.forgebound.transfer.hint"), x, 44, RIGHT_W - 12, 6, MUTED);
            return;
        }
        List<Enhancement> move = menu.moving();
        int y = 44;
        for (Enhancement e : move.subList(0, Math.min(6, move.size()))) {
            fitted(g, font, Component.literal("◇ ").append(e.line()), x, y, RIGHT_W - 12, GREEN);
            y += 11;
        }
        if (move.isEmpty()) wrapped(g, font, Component.translatable("screen.forgebound.transfer.nothing"), x, 44, RIGHT_W - 12, 4, MUTED);
        int free = Enhancements.free(menu.target());
        small(g, font, Component.translatable("screen.forgebound.transfer.free", free), x, 116, free >= move.size() ? LAVENDER : RED);
        Component why = menu.blocked();
        if (why != null) wrapped(g, font, why, x, 128, RIGHT_W - 12, 4, 0xFF8A80);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        panel(g, font, x, y, title);
        tabs(g, font, x, y, 1, mx, my, EngraveScreen.TAB_ICONS, EngraveScreen.TAB_LABELS);
        runeCircle(g, x + 96, y + 52, 14, VIOLET);
        runeCircle(g, x + 158, y + 52, 14, VIOLET);
        boolean ok = menu.blocked() == null;
        int c = ok ? GOLD_LIGHT : 0xFF3A3046;
        g.fill(x + 114, y + 51, x + 136, y + 53, c);
        for (int i = 0; i < 4; i++) g.fill(x + 136 + i, y + 48 + i, x + 137 + i, y + 56 - i, c);
        for (Slot s : menu.slots) slot(g, x + s.x - 1, y + s.y - 1, s.index <= TransferMenu.TARGET);
        hint(g, TransferMenu.SOURCE, new ItemStack(Items.IRON_SWORD));
        hint(g, TransferMenu.TARGET, new ItemStack(Items.DIAMOND_SWORD));
        hint(g, TransferMenu.SHARD, new ItemStack(ModContent.ARCANE_SHARD.get()));
        hint(g, TransferMenu.DUST, new ItemStack(ModContent.CRYSTAL_DUST.get()));
        button(g, font, x + BTN_X, y + BTN_Y, BTN_W, BTN_H, Component.translatable("screen.forgebound.transfer.button"), ok,
                over(mx, my, x + BTN_X, y + BTN_Y, BTN_W, BTN_H));
    }

    private void hint(GuiGraphics g, int index, ItemStack stack) {
        Slot s = menu.slots.get(index);
        if (!s.hasItem()) ghost(g, leftPos + s.x, topPos + s.y, stack);
    }
}
