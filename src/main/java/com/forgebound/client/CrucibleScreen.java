package com.forgebound.client;

import com.forgebound.disassembly.CoreMaterials;
import com.forgebound.disassembly.CrucibleMenu;
import com.forgebound.disassembly.RuinforgedCrucibleBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

import static com.forgebound.client.ForgeUi.*;

/**
 * The Ruinforged Crucible (FORGEBOUND's Disassembly Forge): gear over coal, a violet flame and progress arrow, and a
 * 3x2 recovery grid. Before melting, the empty grid slots show what will come back (faded, with the expected amount).
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    private static final int FLAME_X = 41, FLAME_Y = 36, FLAME = 14, ARROW_X = 64, ARROW_Y = 33, ARROW_W = 38, INFO_Y = 76;

    public CrucibleScreen(CrucibleMenu menu, Inventory inv, Component name) {
        super(menu, inv, name);
        imageWidth = 176;
        imageHeight = CrucibleMenu.INV_Y + 82;
        inventoryLabelY = CrucibleMenu.INV_Y - 11;
        titleLabelX = 8;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, 6, GOLD_LIGHT & 0xFFFFFF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB3A3C4, false);
        Component line1, line2 = null;
        int color1 = 0xB3A3C4, color2 = 0x8F80A6;
        ItemStack in = menu.input();
        switch (menu.status()) {
            case RuinforgedCrucibleBlockEntity.S_UNRECOVERABLE -> {
                line1 = Component.translatable("screen.forgebound.crucible.unrecoverable");
                line2 = Component.translatable("screen.forgebound.crucible.unrecoverable2");
                color1 = 0xFF6B6B;
            }
            case RuinforgedCrucibleBlockEntity.S_OUTPUT_BLOCKED -> {
                line1 = Component.translatable("screen.forgebound.crucible.output_blocked");
                color1 = 0xFFB347;
            }
            case RuinforgedCrucibleBlockEntity.S_OK -> {
                int pct = (int) Math.round(CoreMaterials.condition(in) * 100);
                line1 = Component.translatable("screen.forgebound.crucible.condition", pct, Math.round(pct * CoreMaterials.MAX_SHARE));
                color1 = 0xD9B8FF;
                if (!menu.lit() && !menu.hasFuel()) {
                    line2 = Component.translatable("screen.forgebound.crucible.need_fuel");
                    color2 = 0xFF6B6B;
                } else {
                    line2 = Component.translatable("screen.forgebound.crucible.chance");
                }
            }
            default -> {
                line1 = Component.translatable("screen.forgebound.crucible.hint");
                line2 = Component.translatable("screen.forgebound.crucible.hint2");
            }
        }
        centered(g, line1, INFO_Y, color1);
        if (line2 != null) centered(g, line2, INFO_Y + 10, color2);
        g.pose().pushPose();
        g.pose().scale(0.75F, 0.75F, 1);
        String head = Component.translatable("screen.forgebound.crucible.output").getString();
        g.drawString(font, head, (int) ((CrucibleMenu.OUT_X + 26) / 0.75F) - font.width(head) / 2, (int) (17 / 0.75F), 0xB9A6D6, false);
        g.pose().popPose();
    }

    private void centered(GuiGraphics g, Component text, int y, int color) {
        String s = font.plainSubstrByWidth(text.getString(), imageWidth - 12);
        g.drawString(font, s, (imageWidth - font.width(s)) / 2, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        compactPanel(g, x, y, w, h, CrucibleMenu.INV_Y);
        g.fill(x + 6, y + INFO_Y - 3, x + w - 6, y + INFO_Y - 2, 0xFF5B3F7E);
        // furnace mouth glow and violet flame, burning down as the fuel runs out
        if (menu.lit()) g.fillGradient(x + FLAME_X - 3, y + FLAME_Y - 2, x + FLAME_X + FLAME + 3, y + FLAME_Y + FLAME + 1, 0x00000000, 0x60B060FF);
        g.fill(x + FLAME_X, y + FLAME_Y, x + FLAME_X + FLAME, y + FLAME_Y + FLAME, 0xFF2A1C38);
        if (menu.lit()) {
            int fh = Math.max(1, Math.round(FLAME * menu.burnFraction()));
            int top = y + FLAME_Y + FLAME - fh;
            g.fillGradient(x + FLAME_X, top, x + FLAME_X + FLAME, y + FLAME_Y + FLAME, 0xFF9B4DE6, 0xFFF0D4FF);
            for (int i = 0; i < FLAME; i += 3) {
                int tip = (i * 7) % 4;
                g.fill(x + FLAME_X + i, top, x + FLAME_X + i + 1, Math.min(y + FLAME_Y + FLAME, top + tip), 0xFF2A1C38);
            }
        }
        // progress arrow
        int ay = y + ARROW_Y + 4;
        g.fill(x + ARROW_X, ay, x + ARROW_X + ARROW_W - 6, ay + 4, 0xFF2A1C38);
        for (int i = 0; i < 6; i++) g.fill(x + ARROW_X + ARROW_W - 6 + i, ay - 4 + i, x + ARROW_X + ARROW_W - 5 + i, ay + 8 - i, 0xFF2A1C38);
        int done = Math.round(ARROW_W * menu.progressFraction());
        if (done > 0) {
            g.fill(x + ARROW_X, ay, x + ARROW_X + Math.min(done, ARROW_W - 6), ay + 4, 0xFFC784FF);
            for (int i = 0; i < 6 && ARROW_W - 6 + i < done; i++) {
                g.fill(x + ARROW_X + ARROW_W - 6 + i, ay - 4 + i, x + ARROW_X + ARROW_W - 5 + i, ay + 8 - i, 0xFFC784FF);
            }
        }
        for (Slot s : menu.slots) slot(g, x + s.x - 1, y + s.y - 1, false);
        ghost(g, menu.slots.get(RuinforgedCrucibleBlockEntity.INPUT), new ItemStack(Items.IRON_SWORD), null);
        ghost(g, menu.slots.get(RuinforgedCrucibleBlockEntity.FUEL), new ItemStack(Items.COAL), null);
        // recovery preview in the empty output slots
        List<CrucibleMenu.Preview> preview = menu.preview();
        int p = 0;
        for (int i = 0; i < RuinforgedCrucibleBlockEntity.OUTPUTS && p < preview.size(); i++) {
            Slot s = menu.slots.get(RuinforgedCrucibleBlockEntity.OUT_FIRST + i);
            if (s.hasItem()) continue;
            CrucibleMenu.Preview pv = preview.get(p++);
            ghost(g, s, new ItemStack(pv.item()), String.format(Locale.ROOT, "~%.1f", pv.expected()));
        }
    }

    private void ghost(GuiGraphics g, Slot s, ItemStack hint, String amount) {
        if (s.hasItem()) return;
        g.renderFakeItem(hint, leftPos + s.x, topPos + s.y);
        g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0x90201A2A);
        if (amount != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.pose().scale(0.5F, 0.5F, 1);
            g.drawString(font, amount, (int) ((leftPos + s.x + 17) / 0.5F) - font.width(amount), (int) ((topPos + s.y + 11) / 0.5F), 0xE9C8FF, true);
            g.pose().popPose();
        }
    }
}
