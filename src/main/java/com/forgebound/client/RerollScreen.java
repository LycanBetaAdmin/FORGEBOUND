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

import static com.forgebound.client.ForgeUi.*;

/**
 * The Forge Anvil, Reroll tab: the item in a gold rune circle with its tier and the attribute pools it can draw from,
 * the reroll materials (Fortune Dice, Soul Essence, optional Fortune Crystal and Sealing Runes) and the Reroll button.
 * On the right: the Unique Attributes, each with a seal box when the item holds two.
 */
public class RerollScreen extends AbstractContainerScreen<RerollMenu> {
    private static final int BTN_X = 98, BTN_Y = 130, BTN_W = 64, BTN_H = 18;
    private static final int LIST_X = RIGHT_X + 6, LIST_Y = 42, ROW_H = 30, BOX = 8;

    public RerollScreen(RerollMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = W;
        imageHeight = H;
        inventoryLabelX = INV_X;
        inventoryLabelY = INV_Y - 11;
    }

    private boolean canSeal() {
        return !menu.gear().isEmpty() && Uniques.slots(menu.tier()) > 1;
    }

    private int rowAt(double mx, double my) {
        int x = (int) mx - leftPos, y = (int) my - topPos;
        if (x < LIST_X || x >= RIGHT_X + RIGHT_W - 4 || y < LIST_Y) return -1;
        int i = (y - LIST_Y) / ROW_H;
        return i < Uniques.get(menu.gear()).size() ? i : -1;
    }

    private boolean overBox(double mx, double my, int row) {
        return over(mx, my, leftPos + LIST_X, topPos + LIST_Y + row * ROW_H, BOX, BOX);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (tabAt(mx, my, leftPos, topPos, 2) == 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RerollMenu.BUTTON_REFORGE_TAB);
                return true;
            }
            int row = rowAt(mx, my);
            if (row >= 0 && canSeal() && overBox(mx, my, row)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RerollMenu.BUTTON_SEAL + row);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            if (over(mx, my, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H) && menu.blocked() == null) {
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
        int tab = tabAt(mx, my, leftPos, topPos, 2);
        int row = rowAt(mx, my);
        if (tab >= 0) {
            tip.add(Component.translatable(tab == 0 ? "screen.forgebound.tab.reforge" : "screen.forgebound.tab.reroll"));
        } else if (over(mx, my, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H)) {
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
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LAVENDER, false);
        ItemStack gear = menu.gear();
        // centre: tier and the pools this tier can draw from
        if (gear.isEmpty()) {
            smallCentered(g, Component.translatable("screen.forgebound.anvil.place_short"), 105, 72, MUTED);
        } else {
            Tier tier = menu.tier();
            centerLabel(g, Component.literal("Tier " + tier.roman()), 105, 69, tier.color());
            centerLabel(g, Component.literal("(").append(tier.displayName()).append(")"), 105, 77, tier.color());
        }
        small(g, font, Component.translatable("screen.forgebound.reroll.pools"), 128, 34, GOLD_LIGHT & 0xFFFFFF);
        int lvl = gear.isEmpty() ? 0 : menu.tier().level();
        int y = 44;
        for (UniqueAttribute.Pool p : UniqueAttribute.Pool.values()) {
            boolean open = lvl >= p.minTier && !(menu.premium() && p == UniqueAttribute.Pool.BASE && lvl >= UniqueAttribute.Pool.RARE.minTier);
            small(g, font, Component.translatable("screen.forgebound.reroll.pool." + p.name().toLowerCase(java.util.Locale.ROOT)), 132, y, open ? GREEN : MUTED);
            y += 9;
        }
        header(g, font, CENTER_X, CENTER_W, 86, Component.translatable("screen.forgebound.anvil.required"));
        for (int s = RerollMenu.DICE; s <= RerollMenu.RUNE; s++) {
            Slot slot = menu.slots.get(s);
            int need = menu.need(s);
            String txt = need <= 0 ? (s == RerollMenu.CRYSTAL || s == RerollMenu.RUNE ? "opt" : "-") : menu.slots.get(s).getItem().getCount() + "/" + need;
            tinyCentered(g, Component.literal(txt), slot.x + 8, slot.y + 19, need <= 0 ? MUTED : menu.has(s) ? GREEN : RED);
        }
        // right: the attributes
        header(g, font, RIGHT_X, RIGHT_W, 28, Component.translatable("screen.forgebound.section.unique"));
        if (gear.isEmpty()) {
            wrapped(g, font, Component.translatable("screen.forgebound.reroll.hint"), LIST_X, 44, RIGHT_W - 12, 5, MUTED);
            return;
        }
        Tier tier = menu.tier();
        List<UniqueAttribute> attrs = Uniques.get(gear);
        int slots = Uniques.slots(tier);
        for (int i = 0; i < slots; i++) {
            int ry = LIST_Y + i * ROW_H;
            int tx = LIST_X + (slots > 1 ? BOX + 3 : 0);
            if (i < attrs.size()) {
                UniqueAttribute a = attrs.get(i);
                fitted(g, font, a.displayName(), tx, ry, RIGHT_X + RIGHT_W - 6 - tx, 0xFFD15C);
                wrapped(g, font, a.description(tier.level()), tx, ry + 10, RIGHT_X + RIGHT_W - 6 - tx, 2, IVORY);
            } else {
                small(g, font, Component.translatable("screen.forgebound.reroll.empty"), tx, ry + 2, MUTED);
            }
        }
        Component why = menu.blocked();
        wrapped(g, font, why != null ? why : Component.translatable(slots > 1 ? "screen.forgebound.reroll.seal_hint" : "screen.forgebound.reroll.ready"),
                LIST_X, 106, RIGHT_W - 12, 6, why != null ? 0xFF8A80 : LAVENDER);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        panel(g, font, x, y, title);
        tabs(g, font, x, y, 1, mx, my, ForgeAnvilScreen.TAB_ICONS, ForgeAnvilScreen.TAB_LABELS);
        runeCircle(g, x + 105, y + 52, 14, GOLD_LIGHT);
        if (canSeal()) {
            int n = Uniques.get(menu.gear()).size();
            for (int i = 0; i < n; i++) {
                int bx = x + LIST_X, by = y + LIST_Y + i * ROW_H;
                boolean on = menu.sealed(i);
                g.fill(bx, by, bx + BOX, by + BOX, on ? 0xFF6A4A12 : 0xFF2A2333);
                frame(g, bx, by, BOX, BOX, on ? GOLD_LIGHT : STEEL);
                if (on) g.fill(bx + 2, by + 2, bx + BOX - 2, by + BOX - 2, GOLD_LIGHT);
            }
        }
        for (Slot s : menu.slots) slot(g, x + s.x - 1, y + s.y - 1, s.index == RerollMenu.GEAR);
        if (!menu.slots.get(RerollMenu.GEAR).hasItem()) ghost(g, x + 97, y + 44, new ItemStack(Items.IRON_SWORD));
        hint(g, RerollMenu.DICE, new ItemStack(ModContent.FORTUNE_DICE.get()));
        hint(g, RerollMenu.ESSENCE, new ItemStack(ModContent.SOUL_ESSENCE.get()));
        hint(g, RerollMenu.CRYSTAL, new ItemStack(ModContent.FORTUNE_CRYSTAL.get()));
        hint(g, RerollMenu.RUNE, new ItemStack(ModContent.SEALING_RUNE.get()));
        button(g, font, x + BTN_X, y + BTN_Y, BTN_W, BTN_H, Component.translatable("screen.forgebound.reroll.button"), menu.blocked() == null,
                over(mx, my, x + BTN_X, y + BTN_Y, BTN_W, BTN_H));
    }

    private void hint(GuiGraphics g, int index, ItemStack stack) {
        Slot s = menu.slots.get(index);
        if (!s.hasItem()) ghost(g, leftPos + s.x, topPos + s.y, stack);
    }
}
