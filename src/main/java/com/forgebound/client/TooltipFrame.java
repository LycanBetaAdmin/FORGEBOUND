package com.forgebound.client;

import com.forgebound.enhance.Enhancement;
import com.forgebound.enhance.Enhancements;
import com.forgebound.tier.GearType;
import com.forgebound.tier.Tier;
import com.forgebound.tier.Tiers;
import com.forgebound.unique.UniqueAttribute;
import com.forgebound.unique.Uniques;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The FORGEBOUND tooltip for equipment: a dark obsidian frame whose outer border carries the tier colour, a dark silver
 * inner line, gold corner ornaments and a rune diamond top and bottom; inside, the name, a tier badge (and the
 * Apotheosis rarity), the stats (bonuses green, penalties red), then Enhancements, Apotheosis Affixes (purple, with the
 * gem sockets) and the Unique Attribute (gold) as bannered sections, and the durability at the foot. Other items keep
 * the normal tooltip.
 */
public final class TooltipFrame {
    static final int OBSIDIAN = 0xF0100D18, OBSIDIAN_2 = 0xF01A1424, SILVER = 0xFF51465F, GOLD = 0xFFD6A84B, GOLD_DARK = 0xFF7A5A22;
    static final int PURPLE = 0xC45BFF, ROYAL_GOLD = 0xFFD15C, IVORY = 0xE8E0ED, GREEN = 0x80FF20, RED = 0xFF5555, STEEL_BLUE = 0x55AAFF;
    private static final Pattern RARITY = Pattern.compile("([a-z0-9_.-]+):([a-z0-9_/.-]+)");

    private TooltipFrame() {
    }

    // ------------------------------------------------------------------ custom tooltip parts

    /** Tier badge + optional Apotheosis rarity line. */
    public record Header(Tier tier, String rarity) implements TooltipComponent {
    }

    /** A section banner: a framed title between two ornament lines. */
    public record Banner(Component title, int color, int width) implements TooltipComponent {
    }

    /** A thin divider with a small diamond. */
    public record Divider(int color, int width) implements TooltipComponent {
    }

    // called from ForgeboundClient
    public static void onFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(Header.class, ClientHeader::new);
        event.register(Banner.class, ClientBanner::new);
        event.register(Divider.class, ClientDivider::new);
    }

    // ------------------------------------------------------------------ building the content

    private static boolean ours(Component c) {
        if (c.getContents() instanceof TranslatableContents tc) {
            String k = tc.getKey();
            if (k.startsWith("tooltip.forgebound.") || k.startsWith("unique.forgebound.") || k.startsWith("enhancement.forgebound.")) return true;
        }
        for (Component s : c.getSiblings()) if (ours(s)) return true;
        return false;
    }

    private static String key(Component c) {
        if (c.getContents() instanceof TranslatableContents tc) return tc.getKey();
        for (Component s : c.getSiblings()) {
            String k = key(s);
            if (!k.isEmpty()) return k;
        }
        return "";
    }

    private static boolean blank(Either<FormattedText, TooltipComponent> e) {
        return e.left().map(t -> t.getString().isBlank()).orElse(false);
    }

    /** Positive attribute lines green, negative ones red (the frame's colour rules). */
    private static FormattedText recolor(Component c) {
        String k = key(c);
        if (k.startsWith("neoforge.modifier.plus") || k.startsWith("attribute.modifier.plus")) return c.copy().withStyle(s -> s.withColor(GREEN));
        if (k.startsWith("neoforge.modifier.take") || k.startsWith("attribute.modifier.take")) return c.copy().withStyle(s -> s.withColor(RED));
        return c;
    }

    /** "Epic" from the item's apotheosis:rarity component (read generically: no Apotheosis classes needed). */
    private static String apothRarity(ItemStack stack) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("apotheosis", "rarity"));
        if (type == null || !stack.has(type)) return "";
        Matcher m = RARITY.matcher(String.valueOf(stack.get(type)));
        if (!m.find()) return "";
        String path = m.group(2);
        path = path.substring(path.lastIndexOf('/') + 1);
        return path.isEmpty() ? "" : Character.toUpperCase(path.charAt(0)) + path.substring(1).toLowerCase(Locale.ROOT);
    }

    /** Rebuilds an equipment tooltip into the FORGEBOUND layout. */
    // called from ForgeboundClient (LOWEST priority there)
    public static void onGather(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!GearType.isGear(stack)) return;
        List<Either<FormattedText, TooltipComponent>> in = event.getTooltipElements();
        if (in.isEmpty()) return;
        Tier tier = Tiers.get(stack);
        int tierColor = tier.color();

        List<Either<FormattedText, TooltipComponent>> stats = new ArrayList<>();
        List<Either<FormattedText, TooltipComponent>> apoth = new ArrayList<>();
        for (int i = 1; i < in.size(); i++) {
            Either<FormattedText, TooltipComponent> e = in.get(i);
            if (e.right().isPresent()) {
                // Apotheosis' gem sockets (and any other picture) go to the affix section when it's Apotheosis'
                if (e.right().get().getClass().getName().startsWith("dev.shadowsoffire.apotheosis")) apoth.add(e);
                else stats.add(e);
                continue;
            }
            FormattedText t = e.left().get();
            if (t instanceof Component c) {
                if (ours(c)) continue;
                if (key(c).startsWith("text.apotheosis.dot_prefix")) {
                    apoth.add(Either.left(c.copy().withStyle(s -> s.withColor(0xE3B8FF))));
                    continue;
                }
                stats.add(Either.left(recolor(c)));
            } else {
                stats.add(e);
            }
        }
        while (!stats.isEmpty() && blank(stats.get(0))) stats.remove(0);
        while (!stats.isEmpty() && blank(stats.get(stats.size() - 1))) stats.remove(stats.size() - 1);

        // every section line and banner spans the same width: the widest text, at least 150
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        int width = 150;
        for (Either<FormattedText, TooltipComponent> e : in) width = Math.max(width, e.left().map(font::width).orElse(0));
        for (Enhancement x : Enhancements.get(stack)) width = Math.max(width, font.width(x.line()) + 18);
        for (UniqueAttribute a : Uniques.get(stack)) width = Math.max(width, font.width(a.description(tier.level())) + 24);
        width = Math.min(width, 400);

        List<Either<FormattedText, TooltipComponent>> out = new ArrayList<>();
        out.add(in.get(0)); // the name
        out.add(Either.right(new Header(tier, apothRarity(stack))));
        out.add(Either.right(new Divider(tierColor, width)));
        out.addAll(stats);

        List<Enhancement> enh = Enhancements.get(stack);
        if (!enh.isEmpty()) {
            out.add(Either.right(new Banner(Component.translatable("tooltip.forgebound.section.enhancements", enh.size(), Enhancements.slots(tier)), STEEL_BLUE, width)));
            for (Enhancement x : enh) out.add(Either.left(Component.literal(" ◇ ").withColor(STEEL_BLUE).append(x.line())));
        }
        if (!apoth.isEmpty()) {
            out.add(Either.right(new Banner(Component.translatable("tooltip.forgebound.section.affixes"), PURPLE, width)));
            out.addAll(apoth);
        }
        List<UniqueAttribute> uniques = Uniques.get(stack);
        if (!uniques.isEmpty()) {
            out.add(Either.right(new Banner(Component.translatable("tooltip.forgebound.section.unique"), ROYAL_GOLD, width)));
            for (UniqueAttribute a : uniques) {
                out.add(Either.left(Component.literal(" ✦ ").withColor(ROYAL_GOLD).append(a.displayName().withColor(ROYAL_GOLD))));
                out.add(Either.left(Component.literal("    ").append(a.description(tier.level()).withColor(IVORY))));
            }
        }
        if (stack.isDamageableItem()) {
            out.add(Either.right(new Divider(tierColor, width)));
            int max = stack.getMaxDamage(), left = max - stack.getDamageValue();
            out.add(Either.left(Component.translatable("tooltip.forgebound.durability",
                    String.format(Locale.ROOT, "%,d", left), String.format(Locale.ROOT, "%,d", max)).withColor(IVORY)));
        }
        in.clear();
        in.addAll(out);
    }

    // ------------------------------------------------------------------ the frame

    /** Draws the frame and hides the vanilla background and border (made transparent). */
    // called from ForgeboundClient (LOWEST priority there)
    public static void onColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        if (!GearType.isGear(stack)) return;
        event.setBackgroundStart(0);
        event.setBackgroundEnd(0);
        event.setBorderStart(0);
        event.setBorderEnd(0);
        Font font = event.getFont();
        List<ClientTooltipComponent> comps = event.getComponents();
        int w = 0, h = comps.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent c : comps) {
            w = Math.max(w, c.getWidth(font));
            h += c.getHeight();
        }
        draw(event.getGraphics(), event.getX(), event.getY(), w, h, Tiers.get(stack).color());
    }

    static void draw(GuiGraphics g, int x, int y, int w, int h, int tierRgb) {
        int tierC = 0xFF000000 | tierRgb;
        int tierDark = ForgeUi.darker(tierC, 0.55);
        int x0 = x - 5, y0 = y - 5, x1 = x + w + 5, y1 = y + h + 5;
        g.pose().pushPose();
        g.pose().translate(0, 0, 399);
        g.fillGradient(x0, y0, x1, y1, OBSIDIAN_2, OBSIDIAN);
        // outer border in the tier colour (brighter at the top), inner dark silver line
        g.fillGradient(x0, y0, x0 + 1, y1, tierC, tierDark);
        g.fillGradient(x1 - 1, y0, x1, y1, tierC, tierDark);
        g.fill(x0, y0, x1, y0 + 1, tierC);
        g.fill(x0, y1 - 1, x1, y1, tierDark);
        ForgeUi.frame(g, x0 + 2, y0 + 2, x1 - x0 - 4, y1 - y0 - 4, SILVER);
        // corner ornaments: gold L-brackets with a tier-coloured stud, reaching a pixel outside the frame
        corner(g, x0 - 1, y0 - 1, 1, 1, tierC);
        corner(g, x1, y0 - 1, -1, 1, tierC);
        corner(g, x0 - 1, y1, 1, -1, tierC);
        corner(g, x1, y1, -1, -1, tierC);
        // rune diamonds at the top and bottom centre
        int cx = (x0 + x1) / 2;
        diamond(g, cx, y0, tierC);
        diamond(g, cx, y1 - 1, tierC);
        g.pose().popPose();
    }

    private static void corner(GuiGraphics g, int x, int y, int dx, int dy, int tierC) {
        for (int i = 0; i < 7; i++) {
            px(g, x + i * dx, y, GOLD);
            px(g, x, y + i * dy, GOLD);
        }
        for (int i = 1; i < 5; i++) {
            px(g, x + i * dx, y + dy, GOLD_DARK);
            px(g, x + dx, y + i * dy, GOLD_DARK);
        }
        px(g, x + 2 * dx, y + 2 * dy, tierC);
    }

    private static void diamond(GuiGraphics g, int cx, int cy, int tierC) {
        for (int dy = -3; dy <= 3; dy++) {
            int half = 3 - Math.abs(dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, Math.abs(dy) == 3 ? GOLD : tierC);
            px(g, cx - half, cy + dy, GOLD);
            px(g, cx + half, cy + dy, GOLD);
        }
        px(g, cx, cy, 0xFFFFFFFF);
    }

    private static void px(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 1, y + 1, c);
    }

    // ------------------------------------------------------------------ renderers

    static final class ClientHeader implements ClientTooltipComponent {
        private final Header h;

        ClientHeader(Header h) {
            this.h = h;
        }

        private Component badge() {
            return Component.translatable("tooltip.forgebound.badge", "T" + h.tier().level(), h.tier().displayName().getString().toUpperCase(Locale.ROOT));
        }

        private Component rarity() {
            return Component.translatable("tooltip.forgebound.apotheosis", h.rarity());
        }

        @Override
        public int getHeight() {
            return h.rarity().isEmpty() ? 14 : 24;
        }

        @Override
        public int getWidth(Font font) {
            int w = font.width(badge()) + 12;
            if (!h.rarity().isEmpty()) w = Math.max(w, font.width(rarity()) + 2);
            return w;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics g) {
            int c = 0xFF000000 | h.tier().color();
            int bw = font.width(badge()) + 10;
            g.fill(x, y + 1, x + bw, y + 12, ForgeUi.darker(c, 0.25) | 0xFF000000);
            ForgeUi.frame(g, x, y + 1, bw, 11, c);
            g.fill(x + 1, y + 2, x + 2, y + 3, GOLD);
            g.fill(x + bw - 2, y + 10, x + bw - 1, y + 11, GOLD);
            g.drawString(font, badge(), x + 5, y + 3, h.tier().color(), false);
            if (!h.rarity().isEmpty()) g.drawString(font, rarity(), x + 1, y + 15, PURPLE, false);
        }
    }

    static final class ClientBanner implements ClientTooltipComponent {
        private final Banner b;

        ClientBanner(Banner b) {
            this.b = b;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public int getWidth(Font font) {
            return Math.max(b.width(), font.width(b.title()) + 44);
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics g) {
            int c = 0xFF000000 | b.color();
            int dim = ForgeUi.darker(c, 0.5);
            int w = getWidth(font), tw = font.width(b.title());
            int bx0 = x + (w - tw) / 2 - 7, bx1 = x + (w + tw) / 2 + 7, my = y + 8;
            // ornament lines to either side, with a small diamond on each
            g.fill(x, my, bx0 - 2, my + 1, dim);
            g.fill(bx1 + 2, my, x + w, my + 1, dim);
            g.fill(bx0 - 4, my - 1, bx0 - 3, my + 2, c);
            g.fill(bx1 + 3, my - 1, bx1 + 4, my + 2, c);
            // the banner box
            g.fill(bx0, y + 3, bx1, y + 14, ForgeUi.darker(c, 0.18) | 0xFF000000);
            ForgeUi.frame(g, bx0, y + 3, bx1 - bx0, 11, c);
            g.drawString(font, b.title(), x + (w - tw) / 2, y + 5, b.color(), false);
        }
    }

    static final class ClientDivider implements ClientTooltipComponent {
        private final Divider d;

        ClientDivider(Divider d) {
            this.d = d;
        }

        @Override
        public int getHeight() {
            return 6;
        }

        @Override
        public int getWidth(Font font) {
            return d.width();
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics g) {
            int c = 0xFF000000 | d.color();
            int w = d.width(), mid = x + w / 2;
            g.fill(x, y + 2, x + w, y + 3, ForgeUi.darker(c, 0.45));
            g.fill(mid - 1, y + 1, mid + 2, y + 4, c);
            g.fill(mid, y, mid + 1, y + 5, c);
        }
    }
}
