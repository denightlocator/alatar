package dev.atlas.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Atlas's client-only visual customization dashboard. */
public final class AtlasScreen extends Screen {
    private static final int PANEL = 0xEE0B1427;
    private static final int TEXT = 0xFFF0F6FF;
    private static final int MUTED = 0xFF9AAAC7;
    private static final int DIM = 0xFF687A9B;
    private static final int DEFAULT_ACCENT = 0xFF4CA8FF;
    private static final int DEFAULT_BRIGHT = 0xFF9BE1FF;
    private static final int DEFAULT_DARK = 0xFF1D58A2;
    private static final FontDescription UI_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("atlas", "ui"));

    private static final String[] NAV = {"Overview", "Settings", "Visuals", "HUD", "Profiles", "Configs", "About"};
    private static final LinkedHashMap<String, List<VisualOption>> GROUPS = makeVisualGroups();

    private String activePage = "Overview";
    private String search = "";
    private VisualOption selectedOption = GROUPS.get("Palette").get(0);
    private final Map<String, Integer> scrollByGroup = new LinkedHashMap<>();
    private final Map<String, Boolean> pageToggles = new LinkedHashMap<>();

    private int accent = DEFAULT_ACCENT;
    private int accentBright = DEFAULT_BRIGHT;
    private int accentDark = DEFAULT_DARK;
    private boolean gradientPanels = true;
    private boolean ambientGlow = true;
    private boolean edgeGlow = true;
    private boolean softEdges = true;
    private boolean glassOverlay;
    private boolean quietShadows = true;
    private boolean focusHighlight = true;
    private boolean compactSpacing;
    private boolean compactLabels;
    private boolean brightHeadings = true;
    private boolean mutedCaptions;
    private boolean sidebarLabels = true;
    private boolean sectionCounts = true;
    private boolean wideCards;
    private boolean highContrast;
    private boolean hoverFeedback = true;

    public AtlasScreen() {
        super(Component.literal("Atlas Client"));
        pageToggles.put("Show status chips", true);
        pageToggles.put("Show coordinates", false);
        pageToggles.put("Show section counts", true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        graphics.fillGradient(0, 0, width, height, 0xFF070B14, 0xFF0B1427);
        drawAmbient(graphics);

        Layout layout = layout();
        drawHeader(graphics, font, layout);
        drawSidebar(graphics, font, layout, mouseX, mouseY);
        if (activePage.equals("Overview")) {
            drawOverview(graphics, font, layout, mouseX, mouseY);
        } else {
            drawUtilityPage(graphics, font, layout);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawAmbient(GuiGraphics g) {
        g.fillGradient(0, 0, width, 2, withAlpha(accentBright, 0), withAlpha(accentBright, 230));
        drawSlash(g, width * 2 / 3, height * 3 / 5, width / 2, height / 5, -width / 7,
                withAlpha(accentDark, ambientGlow ? 36 : 16));
        drawSlash(g, width * 4 / 5, -height / 8, width / 3, height / 2, -width / 5,
                withAlpha(accent, ambientGlow ? 24 : 11));
        if (ambientGlow) {
            int glowAlpha = Math.min(105, 18 + intensityOf("Ambient glow", 65));
            g.fillGradient(0, height - Math.max(80, height / 5), width, height,
                    withAlpha(accentDark, 0), withAlpha(accentDark, glowAlpha));
            g.fillGradient(width - 2, 0, width, height, withAlpha(accent, 0), withAlpha(accent, glowAlpha));
        }
    }

    private void drawSlash(GuiGraphics g, int x, int y, int length, int thickness, int drift, int color) {
        if (length <= 0 || thickness <= 0) return;
        for (int row = 0; row < thickness; row++) {
            int startX = x + drift * row / thickness;
            int rowY = y + row;
            if (rowY < 0 || rowY >= height) continue;
            int left = Math.max(0, startX);
            int right = Math.min(width, startX + length);
            if (right > left) g.fill(left, rowY, right, Math.min(height, rowY + 1), color);
        }
    }

    private void drawHeader(GuiGraphics g, Font font, Layout l) {
        g.fillGradient(0, 0, width, l.headerH, 0xF1080D19, 0xEA0A1526);
        g.fill(0, l.headerH - 1, width, l.headerH, withAlpha(accent, 150));
        int logoX = headerLogoX();
        panel(g, logoX, 10, 46, 46, true);
        fillRoundedGradient(g, logoX + 2, 12, 42, 42, accentBright, accentDark, 10);
        drawText(g, font, "A", logoX + 17, 22, 0xFFFFFFFF, true);
        drawText(g, font, "Atlas Client", logoX + 94, 15, headingColor(), true);
        drawText(g, font, "VISUAL INTERFACE  ·  1.21.11", logoX + 95, 35, captionColor(), false);

        int searchX = headerSearchX();
        int searchW = headerSearchWidth();
        panel(g, searchX, 18, searchW, 34, false);
        drawSearchIcon(g, searchX + 13, 30, accentBright);
        String query = search.isEmpty() ? "Search visual settings..." : search;
        drawText(g, font, fit(font, query, Math.max(16, searchW - 48)), searchX + 36, 30,
                search.isEmpty() ? DIM : TEXT, false);
        if (searchX + searchW < width - 94) drawText(g, font, "RIGHT SHIFT", width - 91, 30, accentBright, false);
    }

    private int headerLogoX() {
        return width >= 720 ? 48 : 12;
    }

    private int headerSearchX() {
        return Math.max(headerLogoX() + 320, width * 32 / 100);
    }

    private int headerSearchWidth() {
        return Math.max(88, width - headerSearchX() - 116);
    }

    private void drawSidebar(GuiGraphics g, Font font, Layout l, int mouseX, int mouseY) {
        panel(g, 8, l.headerH + 10, l.sidebarW - 8, height - l.headerH - 20, false);
        int y = l.headerH + 25;
        for (int i = 0; i < NAV.length; i++) {
            String item = NAV[i];
            boolean active = item.equals(activePage);
            int rowY = y + i * 43;
            if (active) {
                fillRoundedGradient(g, 14, rowY, l.sidebarW - 24, 36,
                        withAlpha(accent, 220), withAlpha(accentDark, 190), 10);
                fillRounded(g, 14, rowY + 6, 3, 24, accentBright, 2);
            } else if (inside(mouseX, mouseY, 14, rowY, l.sidebarW - 24, 36)) {
                fillRounded(g, 14, rowY, l.sidebarW - 24, 36, withAlpha(accent, 46), 10);
            }
            drawNavIcon(g, i, 26, rowY + 12, active ? 0xFFFFFFFF : accent);
            if (sidebarLabels && l.sidebarW >= 106) drawText(g, font, item, 49, rowY + 13, active ? TEXT : captionColor(), false);
        }
        int footerY = height - 57;
        g.fill(17, footerY - 8, l.sidebarW - 16, footerY - 7, 0x66405D86);
        drawText(g, font, "ATLAS", 22, footerY, accentBright, true);
        if (l.sidebarW >= 106) drawText(g, font, "build 1.0.0", 22, footerY + 14, DIM, false);
    }

    private void drawOverview(GuiGraphics g, Font font, Layout l, int mouseX, int mouseY) {
        int gap = 12;
        int cardW = (l.contentW - gap * (l.columns - 1)) / l.columns;
        int groupRows = (GROUPS.size() + l.columns - 1) / l.columns;
        int cardH = (l.contentH - gap * (groupRows - 1)) / groupRows;
        int groupIndex = 0;
        for (Map.Entry<String, List<VisualOption>> group : GROUPS.entrySet()) {
            int x = l.contentX + (groupIndex % l.columns) * (cardW + gap);
            int y = l.contentY + (groupIndex / l.columns) * (cardH + gap);
            drawVisualGroup(g, font, group.getKey(), visibleOptions(group.getValue()), x, y, cardW, cardH, mouseX, mouseY);
            groupIndex++;
        }
        if (l.detailW > 0) drawDetails(g, font, l);
    }

    private void drawVisualGroup(GuiGraphics g, Font font, String title, List<VisualOption> options,
                                 int x, int y, int w, int h, int mouseX, int mouseY) {
        panel(g, x, y, w, h, false);
        fillRoundedTopGradient(g, x + 1, y + 1, w - 2, 36, 0xB9233A5D, 0x88213A5E, 10);
        g.fill(x + 13, y + 36, x + w - 13, y + 37, withAlpha(accent, 82));
        drawText(g, font, groupGlyph(title), x + 13, y + 12, accentBright, true);
        drawText(g, font, fit(font, title, Math.max(24, w - 50)), x + 31, y + 12, headingColor(), true);
        if (sectionCounts) drawText(g, font, Integer.toString(options.size()), x + w - 23, y + 12, DIM, false);

        int rowHeight = compactSpacing || compactLabels ? 20 : 24;
        int scroll = scrollByGroup.getOrDefault(title, 0);
        int rows = Math.max(0, (h - 52) / rowHeight);
        int maxScroll = Math.max(0, options.size() - rows);
        scroll = Math.min(scroll, maxScroll);
        scrollByGroup.put(title, scroll);
        for (int i = 0; i < rows; i++) {
            int optionIndex = scroll + i;
            if (optionIndex >= options.size()) break;
            VisualOption option = options.get(optionIndex);
            int rowY = y + 45 + i * rowHeight;
            boolean hover = hoverFeedback && inside(mouseX, mouseY, x + 8, rowY - 2, w - 16, rowHeight - 1);
            if (focusHighlight && option == selectedOption) {
                fillRounded(g, x + 8, rowY - 2, w - 16, rowHeight - 1, withAlpha(accent, 24 + selectedOption.intensity / 2), 6);
            } else if (hover) {
                fillRounded(g, x + 8, rowY - 2, w - 16, rowHeight - 1, withAlpha(accent, 34), 6);
            }
            drawCheck(g, x + 13, rowY + 5, option.enabled);
            int labelX = x + 34;
            int labelWidth = Math.max(16, w - 46);
            if (option.palette) {
                fillRounded(g, x + 35, rowY + 7, 13, 11, option.accent, 4);
                g.fill(x + 38, rowY + 8, x + 45, rowY + 9, option.accentBright);
                labelX = x + 55;
                labelWidth = Math.max(16, w - 68);
            }
            drawText(g, font, fit(font, option.name, labelWidth), labelX, rowY + 7,
                    option == selectedOption ? TEXT : (option.enabled ? 0xFFD9ECFF : (highContrast ? TEXT : captionColor())), false);
        }
        if (options.size() > rows && rows > 0) {
            int trackX = x + w - 5;
            int trackY = y + 44;
            int trackH = Math.max(10, h - 54);
            g.fill(trackX, trackY, trackX + 2, trackY + trackH, 0x66405B7D);
            int thumbH = Math.max(13, trackH * rows / options.size());
            int thumbY = trackY + (trackH - thumbH) * scroll / Math.max(1, maxScroll);
            g.fillGradient(trackX, thumbY, trackX + 2, thumbY + thumbH, accentBright, accentDark);
        }
    }

    private void drawDetails(GuiGraphics g, Font font, Layout l) {
        int x = l.detailX;
        int y = l.contentY;
        int w = l.detailW;
        int h = l.contentH;
        panel(g, x, y, w, h, true);
        fillRoundedTopGradient(g, x + 1, y + 1, w - 2, 44, 0xD8203B5F, 0xA8172942, 12);
        drawText(g, font, "APPEARANCE", x + 16, y + 11, accentBright, true);
        drawText(g, font, fit(font, selectedOption.name, w - 30), x + 16, y + 27, headingColor(), true);
        int cursorY = y + 60;
        drawText(g, font, selectedOption.group.toUpperCase(Locale.ROOT), x + 16, cursorY, captionColor(), false);
        cursorY += 32;
        drawText(g, font, "Active", x + 16, cursorY + 4, TEXT, false);
        drawSwitch(g, x + w - 55, cursorY - 1, selectedOption.enabled);
        cursorY += 38;
        g.fill(x + 15, cursorY, x + w - 15, cursorY + 1, 0x66405D86);
        cursorY += 17;
        drawText(g, font, "Intensity", x + 16, cursorY, TEXT, false);
        drawText(g, font, selectedOption.intensity + "%", x + w - 48, cursorY, captionColor(), false);
        drawSlider(g, x + 16, cursorY + 17, w - 32, selectedOption.intensity / 100.0);
        cursorY += 52;
        drawText(g, font, "Treatment", x + 16, cursorY, TEXT, false);
        drawText(g, font, selectedOption.treatment, x + w - 70, cursorY, accentBright, false);
        cursorY += 36;
        int noteH = Math.max(48, Math.min(76, h - (cursorY - y) - 66));
        if (selectedOption.treatment.equals("Soft")) {
            fillRoundedGradient(g, x + 13, cursorY, w - 26, noteH, withAlpha(accent, 64), 0x70243C5E, 10);
        } else {
            fillRounded(g, x + 13, cursorY, w - 26, noteH, 0xB0172942, 10);
        }
        drawText(g, font, "STYLE NOTE", x + 22, cursorY + 10, accentBright, true);
        drawWrapped(g, font, selectedOption.description, x + 22, cursorY + 26, w - 44, noteH - 28, captionColor());
        drawText(g, font, "VISUAL CONTROLS ONLY", x + 12, y + h - 29, DIM, false);
    }

    private void drawUtilityPage(GuiGraphics g, Font font, Layout l) {
        int x = l.contentX;
        int y = l.contentY;
        int w = l.contentW + l.detailW + (l.detailW > 0 ? 14 : 0);
        panel(g, x, y, w, l.contentH, false);
        String subtitle = switch (activePage) {
            case "Settings" -> "Tune the Atlas interface to your preference.";
            case "Visuals" -> "Personalize colors, panels, lighting, and visual density.";
            case "HUD" -> "Choose which interface elements are shown.";
            case "Profiles" -> "Visual profiles are planned for a later pass.";
            case "Configs" -> "Theme saving and import/export are planned for later.";
            default -> "Atlas Client · a visual-first interface for Minecraft 1.21.11.";
        };
        drawText(g, font, activePage.toUpperCase(Locale.ROOT), x + 18, y + 18, accentBright, true);
        drawText(g, font, subtitle, x + 18, y + 36, captionColor(), false);
        g.fill(x + 18, y + 57, x + w - 18, y + 58, 0x66405D86);

        if (activePage.equals("Profiles") || activePage.equals("Configs") || activePage.equals("About")) {
            String[] lines = activePage.equals("About")
                    ? new String[]{"Atlas Client", "Fabric client interface for Minecraft 1.21.11.", "Right Shift opens and closes this dashboard.", "This build focuses on visual styling; no gameplay features are included."}
                    : new String[]{"Coming soon", "Current appearance choices stay in memory for this game session.", "No files are written and no external config packs are loaded."};
            int lineY = y + 81;
            for (String line : lines) {
                drawText(g, font, line, x + 18, lineY, lineY == y + 81 ? TEXT : captionColor(), false);
                lineY += 22;
            }
            return;
        }

        String[] options = utilityOptions(activePage);
        int rowY = y + 84;
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            int col = i % 2;
            int row = i / 2;
            int cellX = x + 20 + col * ((w - 52) / 2);
            int cellY = rowY + row * 62;
            int cellW = (w - 60) / 2;
            fillRoundedGradient(g, cellX, cellY, cellW, 48,
                    0xA8192B45, gradientPanels ? 0x88152A47 : 0xAA19304D, 10);
            drawRoundedBorder(g, cellX, cellY, cellW, 48, withAlpha(accent, 80), 10);
            drawText(g, font, fit(font, option, cellW - 64), cellX + 14, cellY + 17, TEXT, false);
            drawSwitch(g, cellX + cellW - 52, cellY + 14, utilityValue(option));
        }
        drawText(g, font, "Appearance adjustments are local to this game session.", x + 18, y + l.contentH - 12, DIM, false);
    }

    private String[] utilityOptions(String page) {
        return switch (page) {
            case "Settings" -> new String[]{"Compact spacing", "Wide cards", "Quiet shadows"};
            case "HUD" -> new String[]{"Show status chips", "Show coordinates", "Section counts"};
            default -> new String[]{"Ambient glow", "Glass overlay", "Gradient panels"};
        };
    }

    private boolean utilityValue(String option) {
        return switch (option) {
            case "Compact spacing" -> compactSpacing;
            case "Wide cards" -> wideCards;
            case "Quiet shadows" -> quietShadows;
            case "Gradient panels" -> gradientPanels;
            case "Ambient glow" -> ambientGlow;
            case "Glass overlay" -> glassOverlay;
            case "Section counts" -> sectionCounts;
            default -> pageToggles.getOrDefault(option, true);
        };
    }

    private void toggleUtilityOption(String option) {
        boolean value = !utilityValue(option);
        switch (option) {
            case "Compact spacing" -> compactSpacing = value;
            case "Wide cards" -> wideCards = value;
            case "Quiet shadows" -> quietShadows = value;
            case "Gradient panels" -> gradientPanels = value;
            case "Ambient glow" -> ambientGlow = value;
            case "Glass overlay" -> glassOverlay = value;
            case "Section counts" -> sectionCounts = value;
            default -> pageToggles.put(option, value);
        }
        syncOptionState(option, value);
    }

    private void setVisualOption(VisualOption option, boolean enabled) {
        if (option.palette) {
            if (!enabled) return;
            for (VisualOption palette : GROUPS.get("Palette")) palette.enabled = palette == option;
            accent = option.accent;
            accentBright = option.accentBright;
            accentDark = option.accentDark;
            return;
        }
        option.enabled = enabled;
        switch (option.name) {
            case "Gradient panels" -> gradientPanels = enabled;
            case "Glass overlay" -> glassOverlay = enabled;
            case "Soft edges" -> softEdges = enabled;
            case "Quiet shadows" -> quietShadows = enabled;
            case "Ambient glow" -> ambientGlow = enabled;
            case "Edge glow" -> edgeGlow = enabled;
            case "Focus highlight" -> focusHighlight = enabled;
            case "Compact labels" -> compactLabels = enabled;
            case "Bright headings" -> brightHeadings = enabled;
            case "Muted captions" -> mutedCaptions = enabled;
            case "High contrast" -> highContrast = enabled;
            case "Reduced glare" -> mutedCaptions = enabled;
            case "Compact spacing" -> compactSpacing = enabled;
            case "Wide cards" -> wideCards = enabled;
            case "Sidebar labels" -> sidebarLabels = enabled;
            case "Section counts" -> sectionCounts = enabled;
            case "Hover feedback" -> hoverFeedback = enabled;
            default -> { }
        }
        syncOptionState(option.name, enabled);
    }

    private void syncOptionState(String name, boolean enabled) {
        for (List<VisualOption> options : GROUPS.values()) {
            for (VisualOption option : options) {
                if (!option.palette && option.name.equals(name)) option.enabled = enabled;
            }
        }
    }

    private static Component uiText(String value, boolean emphasize) {
        return Component.literal(value).withStyle(Style.EMPTY.withFont(UI_FONT).withBold(emphasize));
    }

    private static void drawText(GuiGraphics graphics, Font font, String value, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, uiText(value, shadow), x, y, color, shadow);
    }

    private void drawSearchIcon(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 1, y, x + 7, y + 1, color);
        g.fill(x, y + 1, x + 1, y + 7, color);
        g.fill(x + 7, y + 1, x + 8, y + 6, color);
        g.fill(x + 1, y + 7, x + 6, y + 8, color);
        g.fill(x + 2, y + 2, x + 6, y + 6, 0xFF0B1427);
        g.fill(x + 7, y + 7, x + 11, y + 9, color);
    }

    private void drawNavIcon(GuiGraphics g, int index, int x, int y, int color) {
        switch (index) {
            case 0 -> {
                g.fill(x, y, x + 5, y + 5, color);
                g.fill(x + 7, y, x + 12, y + 5, color);
                g.fill(x, y + 7, x + 5, y + 12, color);
                g.fill(x + 7, y + 7, x + 12, y + 12, color);
            }
            case 1 -> {
                g.fill(x + 4, y, x + 8, y + 12, color);
                g.fill(x, y + 4, x + 12, y + 8, color);
                g.fill(x + 2, y + 2, x + 10, y + 10, withAlpha(color, 190));
                g.fill(x + 5, y + 5, x + 7, y + 7, 0xFF0B1427);
            }
            case 2 -> {
                g.fill(x + 1, y + 5, x + 11, y + 7, color);
                g.fill(x + 3, y + 3, x + 9, y + 9, color);
                g.fill(x + 5, y + 5, x + 7, y + 7, 0xFF0B1427);
            }
            case 3 -> {
                g.fill(x, y + 7, x + 3, y + 12, color);
                g.fill(x + 5, y + 4, x + 8, y + 12, color);
                g.fill(x + 10, y, x + 13, y + 12, color);
            }
            case 4 -> {
                g.fill(x + 4, y, x + 9, y + 5, color);
                g.fill(x + 2, y + 6, x + 11, y + 12, color);
            }
            case 5 -> {
                g.fill(x + 1, y + 1, x + 12, y + 3, color);
                g.fill(x + 1, y + 5, x + 10, y + 7, color);
                g.fill(x + 1, y + 9, x + 12, y + 11, color);
            }
            default -> {
                g.fill(x + 1, y + 1, x + 12, y + 12, color);
                g.fill(x + 3, y + 3, x + 10, y + 10, 0xFF0B1427);
                g.fill(x + 6, y + 5, x + 8, y + 9, color);
                g.fill(x + 6, y + 3, x + 8, y + 4, color);
            }
        }
    }

    private void drawCheck(GuiGraphics g, int x, int y, boolean checked) {
        fillRounded(g, x, y, 15, 15, checked ? accent : 0xFF101B2E, 4);
        drawRoundedBorder(g, x, y, 15, 15, checked ? withAlpha(accentBright, 230) : 0xFF405B7D, 4);
        if (checked) {
            g.fill(x + 3, y + 7, x + 6, y + 10, 0xFFFFFFFF);
            g.fill(x + 5, y + 9, x + 11, y + 11, 0xFFFFFFFF);
        }
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean on) {
        fillRounded(g, x, y, 39, 20, on ? accent : 0xFF203752, 10);
        int knobX = on ? x + 22 : x + 3;
        fillRounded(g, knobX, y + 3, 14, 14, 0xFFFFF7FD, 7);
        g.fill(knobX + 3, y + 4, knobX + 11, y + 5, 0x66FFFFFF);
    }

    private static void fillRounded(GuiGraphics g, int x, int y, int w, int h, int color, int radius) {
        fillRoundedGradient(g, x, y, w, h, color, color, radius);
    }

    private static int cornerInset(int row, int height, int radius) {
        int edgeDistance = row < radius ? radius - row - 1 : row >= height - radius ? row - (height - radius) : -1;
        return edgeDistance < 0 ? 0 : radius - (int) Math.sqrt(radius * radius - edgeDistance * edgeDistance);
    }

    private static int blendColor(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int a = Math.round(((from >>> 24) & 255) * (1.0f - t) + ((to >>> 24) & 255) * t);
        int r = Math.round(((from >>> 16) & 255) * (1.0f - t) + ((to >>> 16) & 255) * t);
        int g = Math.round(((from >>> 8) & 255) * (1.0f - t) + ((to >>> 8) & 255) * t);
        int b = Math.round((from & 255) * (1.0f - t) + (to & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void fillRoundedGradient(GuiGraphics g, int x, int y, int w, int h,
                                            int top, int bottom, int radius) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            g.fillGradient(x, y, x + w, y + h, top, bottom);
            return;
        }
        int middleTop = y + r;
        int middleBottom = y + h - r;
        if (middleBottom > middleTop) {
            float topMix = (middleTop - y) / (float) Math.max(1, h - 1);
            float bottomMix = (middleBottom - 1 - y) / (float) Math.max(1, h - 1);
            g.fillGradient(x, middleTop, x + w, middleBottom,
                    blendColor(top, bottom, topMix), blendColor(top, bottom, bottomMix));
        }
        for (int row = 0; row < r; row++) {
            int inset = cornerInset(row, h, r);
            int topColor = blendColor(top, bottom, row / (float) Math.max(1, h - 1));
            int bottomRow = h - 1 - row;
            int bottomColor = blendColor(top, bottom, bottomRow / (float) Math.max(1, h - 1));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, topColor);
            g.fill(x + inset, y + bottomRow, x + w - inset, y + bottomRow + 1, bottomColor);
        }
    }

    private static void fillRoundedTopGradient(GuiGraphics g, int x, int y, int w, int h,
                                               int top, int bottom, int radius) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        int middleTop = y + r;
        if (h > r) g.fillGradient(x, middleTop, x + w, y + h, blendColor(top, bottom, r / (float) h), bottom);
        for (int row = 0; row < r; row++) {
            int inset = r - (int) Math.sqrt(r * r - (r - row - 1) * (r - row - 1));
            int color = blendColor(top, bottom, row / (float) Math.max(1, h - 1));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private static void drawRoundedBorder(GuiGraphics g, int x, int y, int w, int h, int color, int radius) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            g.fill(x, y, x + w, y + 1, color);
            g.fill(x, y + h - 1, x + w, y + h, color);
            g.fill(x, y, x + 1, y + h, color);
            g.fill(x + w - 1, y, x + w, y + h, color);
            return;
        }
        g.fill(x + r, y, x + w - r, y + 1, color);
        g.fill(x + r, y + h - 1, x + w - r, y + h, color);
        if (h > 2 * r) {
            g.fill(x, y + r, x + 1, y + h - r, color);
            g.fill(x + w - 1, y + r, x + w, y + h - r, color);
        }
        for (int row = 0; row < r; row++) {
            int inset = cornerInset(row, h, r);
            g.fill(x + inset, y + row, x + inset + 1, y + row + 1, color);
            g.fill(x + w - inset - 1, y + row, x + w - inset, y + row + 1, color);
            int bottomRow = h - 1 - row;
            g.fill(x + inset, y + bottomRow, x + inset + 1, y + bottomRow + 1, color);
            g.fill(x + w - inset - 1, y + bottomRow, x + w - inset, y + bottomRow + 1, color);
        }
    }

    private void drawSlider(GuiGraphics g, int x, int y, int w, double amount) {
        fillRounded(g, x, y + 5, w, 4, 0xFF2A456A, 2);
        int fillW = Math.max(0, Math.min(w, (int) (w * amount)));
        if (fillW > 0) fillRoundedGradient(g, x, y + 5, fillW, 4, accentBright, accentDark, 2);
        int knobX = Math.max(x - 2, Math.min(x + w - 8, x + fillW - 4));
        fillRounded(g, knobX, y, 10, 16, 0xFFEAF5FF, 5);
        g.fill(knobX + 2, y + 2, knobX + 8, y + 3, 0x66FFFFFF);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, boolean accentPanel) {
        if (w <= 0 || h <= 0) return;
        int radius = Math.min(12, Math.min(w, h) / 3);
        if (edgeGlow) {
            int aura = withAlpha(accent, accentPanel ? 34 : 23);
            drawRoundedBorder(g, x - 1, y - 1, w + 2, h + 2, aura, radius + 1);
        }
        if (quietShadows) fillRounded(g, x + 2, y + 4, w, h, 0x50000000, radius);
        int surface = accentPanel ? (glassOverlay ? 0xC8121E35 : 0xF1101C31) : (glassOverlay ? 0xC80C172A : PANEL);
        int surfaceBottom = glassOverlay ? 0xA0091427 : 0xF0091427;
        if (gradientPanels) fillRoundedGradient(g, x, y, w, h, surface, surfaceBottom, radius);
        else fillRounded(g, x, y, w, h, surface, radius);
        int edgeAlpha = Math.min(235, 55 + intensityOf("Edge glow", 65) * 3);
        int border = accentPanel || edgeGlow ? withAlpha(accent, accentPanel ? 235 : edgeAlpha) : 0x8845699B;
        if (softEdges) border = withAlpha(border, 215);
        drawRoundedBorder(g, x, y, w, h, border, radius);
        if (glassOverlay && h > 6) fillRoundedTopGradient(g, x + 2, y + 2, w - 4, Math.min(18, h - 4),
                withAlpha(accentBright, 34), withAlpha(accentBright, 0), Math.max(2, radius - 2));
    }

    private static void drawWrapped(GuiGraphics g, Font font, String text, int x, int y, int maxW, int maxH, int color) {
        StringBuilder line = new StringBuilder();
        int lineY = y;
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(uiText(candidate, false)) > maxW && line.length() > 0) {
                drawText(g, font, line.toString(), x, lineY, color, false);
                lineY += 13;
                line.setLength(0);
                if (lineY > y + maxH - 8) return;
                line.append(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0 && lineY <= y + maxH - 8) drawText(g, font, line.toString(), x, lineY, color, false);
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (maxWidth <= 0 || font.width(uiText(text, false)) <= maxWidth) return text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && font.width(uiText(text.substring(0, end) + ellipsis, false)) > maxWidth) end--;
        return end == 0 ? "" : text.substring(0, end) + ellipsis;
    }

    private Layout layout() {
        int side = width < 460 ? 74 : width < 720 ? 112 : 148;
        int header = 68;
        int top = header + 14;
        int bottom = height - 15;
        int detailW = width >= 740 ? Math.max(190, Math.min(220, width / 6)) : 0;
        int detailGap = detailW > 0 ? 14 : 0;
        int contentX = side + 16;
        int rightMargin = 16;
        int contentW = Math.max(54, width - contentX - rightMargin - detailW - detailGap);
        int columns = contentW >= 840 ? (wideCards ? 4 : 6)
                : contentW >= 600 ? (wideCards ? 3 : 4)
                : contentW >= 410 ? (wideCards ? 2 : 3)
                : contentW >= 250 ? 2 : 1;
        int contentH = Math.max(84, bottom - top);
        int detailX = width - rightMargin - detailW;
        return new Layout(side, header, contentX, top, contentW, contentH, columns, detailX, detailW);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button != 0) return super.mouseClicked(event, doubleClick);
        Layout l = layout();
        int navTop = l.headerH + 25;
        for (int i = 0; i < NAV.length; i++) {
            int rowY = navTop + i * 43;
            if (inside(mouseX, mouseY, 14, rowY, l.sidebarW - 24, 36)) {
                activePage = NAV[i];
                return true;
            }
        }
        int searchX = headerSearchX();
        int searchW = headerSearchWidth();
        if (inside(mouseX, mouseY, searchX, 18, searchW, 34)) return true;

        if (activePage.equals("Overview")) {
            if (l.detailW > 0) {
                int x = l.detailX;
                int y = l.contentY;
                int w = l.detailW;
                int activeY = y + 60 + 32;
                if (inside(mouseX, mouseY, x + w - 54, activeY - 7, 48, 28)) {
                    setVisualOption(selectedOption, !selectedOption.enabled);
                    return true;
                }
                int sliderY = activeY + 72;
                if (inside(mouseX, mouseY, x + 10, sliderY - 3, w - 20, 19)) {
                    selectedOption.intensity = (int) Math.round(Math.max(0, Math.min(1, (mouseX - (x + 13)) / (w - 26.0))) * 100);
                    return true;
                }
                int treatmentY = sliderY + 35;
                if (inside(mouseX, mouseY, x + 10, treatmentY - 4, w - 20, 25)) {
                    selectedOption.treatment = selectedOption.treatment.equals("Soft") ? "Crisp" : "Soft";
                    return true;
                }
            }
            int gap = 12;
            int cardW = (l.contentW - gap * (l.columns - 1)) / l.columns;
            int groupRows = (GROUPS.size() + l.columns - 1) / l.columns;
            int cardH = (l.contentH - gap * (groupRows - 1)) / groupRows;
            int rowHeight = compactSpacing || compactLabels ? 20 : 24;
            int groupIndex = 0;
            for (Map.Entry<String, List<VisualOption>> group : GROUPS.entrySet()) {
                int x = l.contentX + (groupIndex % l.columns) * (cardW + gap);
                int y = l.contentY + (groupIndex / l.columns) * (cardH + gap);
                if (inside(mouseX, mouseY, x, y + 44, cardW, cardH - 45)) {
                    List<VisualOption> options = visibleOptions(group.getValue());
                    int rows = Math.max(0, (cardH - 52) / rowHeight);
                    int clickedIndex = scrollByGroup.getOrDefault(group.getKey(), 0) + (int) ((mouseY - (y + 45)) / rowHeight);
                    if (clickedIndex >= 0 && clickedIndex < options.size()
                            && clickedIndex < scrollByGroup.getOrDefault(group.getKey(), 0) + rows) {
                        selectedOption = options.get(clickedIndex);
                        setVisualOption(selectedOption, selectedOption.palette || !selectedOption.enabled);
                        return true;
                    }
                }
                groupIndex++;
            }
        } else {
            if (!activePage.equals("Settings") && !activePage.equals("Visuals") && !activePage.equals("HUD")) {
                return super.mouseClicked(event, doubleClick);
            }
            String[] options = utilityOptions(activePage);
            int x = l.contentX;
            int y = l.contentY;
            int w = l.contentW + l.detailW + (l.detailW > 0 ? 14 : 0);
            for (int index = 0; index < options.length; index++) {
                int cellX = x + 20 + (index % 2) * ((w - 52) / 2);
                int cellY = y + 84 + (index / 2) * 62;
                if (inside(mouseX, mouseY, cellX, cellY, (w - 60) / 2, 48)) {
                    toggleUtilityOption(options[index]);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!activePage.equals("Overview")) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        Layout l = layout();
        int gap = 12;
        int cardW = (l.contentW - gap * (l.columns - 1)) / l.columns;
        int groupRows = (GROUPS.size() + l.columns - 1) / l.columns;
        int cardH = (l.contentH - gap * (groupRows - 1)) / groupRows;
        int groupIndex = 0;
        int rowHeight = compactSpacing || compactLabels ? 20 : 24;
        for (String group : GROUPS.keySet()) {
            int x = l.contentX + (groupIndex % l.columns) * (cardW + gap);
            int y = l.contentY + (groupIndex / l.columns) * (cardH + gap);
            if (inside(mouseX, mouseY, x, y, cardW, cardH)) {
                int rows = Math.max(0, (cardH - 52) / rowHeight);
                int maxScroll = Math.max(0, visibleOptions(GROUPS.get(group)).size() - rows);
                int current = scrollByGroup.getOrDefault(group, 0);
                scrollByGroup.put(group, Math.max(0, Math.min(maxScroll, current - (int) Math.signum(verticalAmount) * 2)));
                return true;
            }
            groupIndex++;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codePoint = event.codepoint();
        if (!Character.isISOControl(codePoint) && search.codePointCount(0, search.length()) < 40) {
            search += new String(Character.toChars(codePoint));
            for (String group : GROUPS.keySet()) scrollByGroup.put(group, 0);
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            int lastCodePointStart = search.offsetByCodePoints(search.length(), -1);
            search = search.substring(0, lastCodePointStart);
            for (String group : GROUPS.keySet()) scrollByGroup.put(group, 0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            search = "";
            for (String group : GROUPS.keySet()) scrollByGroup.put(group, 0);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private List<VisualOption> visibleOptions(List<VisualOption> options) {
        if (search.isBlank()) return options;
        String query = search.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.name.toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && y >= top && x < left + w && y < top + h;
    }

    private static String groupGlyph(String group) {
        return switch (group) {
            case "Palette" -> "P";
            case "Surfaces" -> "S";
            case "Lighting" -> "L";
            case "Typography" -> "T";
            case "Layout" -> "G";
            default -> "F";
        };
    }

    private static LinkedHashMap<String, List<VisualOption>> makeVisualGroups() {
        LinkedHashMap<String, List<VisualOption>> groups = new LinkedHashMap<>();
        List<VisualOption> palettes = new ArrayList<>();
        palettes.add(VisualOption.palette("Ocean blue", "Deep navy panels with bright electric-blue accents.", DEFAULT_ACCENT, DEFAULT_BRIGHT, DEFAULT_DARK, true));
        palettes.add(VisualOption.palette("Glacier", "Icy blue surfaces with a soft cyan highlight.", 0xFF4CCCF3, 0xFFB5F0FF, 0xFF267BAF, false));
        palettes.add(VisualOption.palette("Sapphire", "A saturated royal blue with crisp pale highlights.", 0xFF4D78FF, 0xFFA9BCFF, 0xFF294BAA, false));
        palettes.add(VisualOption.palette("Midnight cyan", "Cool cyan glow over deep midnight-blue surfaces.", 0xFF25BFE4, 0xFF8DEAFF, 0xFF12628B, false));
        groups.put("Palette", palettes);
        groups.put("Surfaces", options("Surfaces",
                "Gradient panels", "Glass overlay", "Soft edges", "Quiet shadows"));
        groups.put("Lighting", options("Lighting",
                "Ambient glow", "Edge glow", "Focus highlight"));
        groups.put("Typography", options("Typography",
                "Compact labels", "Bright headings", "Muted captions", "High contrast"));
        groups.put("Layout", options("Layout",
                "Compact spacing", "Wide cards", "Sidebar labels", "Section counts"));
        groups.put("Feedback", options("Feedback",
                "Hover feedback", "Focus highlight", "Reduced glare", "Quiet shadows"));
        for (VisualOption option : groups.get("Surfaces")) {
            if (option.name.equals("Gradient panels") || option.name.equals("Soft edges") || option.name.equals("Quiet shadows")) option.enabled = true;
        }
        for (VisualOption option : groups.get("Lighting")) {
            if (option.name.equals("Ambient glow") || option.name.equals("Edge glow") || option.name.equals("Focus highlight")) option.enabled = true;
        }
        for (VisualOption option : groups.get("Typography")) if (option.name.equals("Bright headings")) option.enabled = true;
        for (VisualOption option : groups.get("Layout")) if (option.name.equals("Sidebar labels") || option.name.equals("Section counts")) option.enabled = true;
        for (VisualOption option : groups.get("Feedback")) if (option.name.equals("Hover feedback")) option.enabled = true;
        return groups;
    }

    private static List<VisualOption> options(String group, String... names) {
        List<VisualOption> options = new ArrayList<>();
        for (String name : names) options.add(new VisualOption(group, name, name + " adjusts the appearance of the Atlas interface."));
        return options;
    }

    private int intensityOf(String name, int fallback) {
        for (List<VisualOption> options : GROUPS.values()) {
            for (VisualOption option : options) {
                if (option.name.equals(name)) return option.intensity;
            }
        }
        return fallback;
    }

    private int captionColor() {
        return mutedCaptions ? DIM : MUTED;
    }

    private int headingColor() {
        return brightHeadings ? accentBright : TEXT;
    }

    private static int withAlpha(int rgb, int alpha) {
        return (rgb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private record Layout(int sidebarW, int headerH, int contentX, int contentY, int contentW, int contentH,
                          int columns, int detailX, int detailW) {}

    private static final class VisualOption {
        private final String group;
        private final String name;
        private final String description;
        private final boolean palette;
        private final int accent;
        private final int accentBright;
        private final int accentDark;
        private boolean enabled;
        private int intensity = 65;
        private String treatment = "Soft";

        private VisualOption(String group, String name, String description) {
            this(group, name, description, false, 0, 0, 0, false);
        }

        private VisualOption(String group, String name, String description, boolean palette,
                             int accent, int accentBright, int accentDark, boolean enabled) {
            this.group = group;
            this.name = name;
            this.description = description;
            this.palette = palette;
            this.accent = accent;
            this.accentBright = accentBright;
            this.accentDark = accentDark;
            this.enabled = enabled;
        }

        private static VisualOption palette(String name, String description, int accent, int bright, int dark, boolean enabled) {
            return new VisualOption("Palette", name, description, true, accent, bright, dark, enabled);
        }
    }
}
