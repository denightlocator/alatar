package dev.atlas.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Atlas's client-only visual customization dashboard. */
public final class AtlasScreen extends Screen {
    private static final int PANEL = 0xEE0C1427;
    private static final int TEXT = 0xFFE8F2FF;
    private static final int MUTED = 0xFF8D9CB7;
    private static final int DIM = 0xFF64728C;
    private static final int DEFAULT_ACCENT = 0xFF55B8FF;
    private static final int DEFAULT_BRIGHT = 0xFF83D4FF;
    private static final int DEFAULT_DARK = 0xFF2462B1;

    private static final String[] NAV = {"Overview", "Settings", "Visuals", "HUD", "Profiles", "Configs", "About"};
    private static final String[] NAV_GLYPHS = {"O", "S", "V", "H", "P", "C", "i"};
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
        graphics.fillGradient(0, 0, width, height, 0xF5090D19, 0xF5091022);
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
        g.fillGradient(0, 0, width, 2, 0x004D9BFF, 0xD957BFFF);
        if (ambientGlow) {
            int glowAlpha = Math.min(120, 28 + intensityOf("Ambient glow", 65));
            g.fill(width - 2, 0, width, height, withAlpha(accent, glowAlpha));
            g.fillGradient(Math.min(120, width), Math.max(0, height - 30), width, height,
                    0x002B76D9, withAlpha(accentDark, glowAlpha / 2));
        }
    }

    private void drawHeader(GuiGraphics g, Font font, Layout l) {
        g.fillGradient(0, 0, width, l.headerH, 0xF10A0E1B, 0xEA0B1222);
        g.fill(0, l.headerH - 1, width, l.headerH, withAlpha(accent, 170));
        int logoX = l.sidebarW + 12;
        panel(g, logoX, 12, 36, 36, true);
        g.fillGradient(logoX + 1, 13, logoX + 35, 47, accentBright, accentDark);
        g.drawCenteredString(font, Component.literal("A"), logoX + 18, 22, 0xFFFFFFFF);
        g.drawString(font, "Atlas Client", logoX + 47, 13, headingColor(), true);
        g.drawString(font, "VISUAL INTERFACE  ·  1.21.11", logoX + 48, 30, captionColor(), false);

        int searchW = Math.max(88, width - (logoX + 250) - 28);
        int searchX = logoX + 250;
        if (searchX + searchW > width - 14) searchX = Math.max(logoX + 46, width - searchW - 14);
        panel(g, searchX, 15, searchW, 30, false);
        g.drawString(font, ">", searchX + 10, 21, accentBright, true);
        String query = search.isEmpty() ? "Search visual settings..." : search;
        g.drawString(font, fit(font, query, Math.max(16, searchW - 38)), searchX + 29, 24,
                search.isEmpty() ? DIM : TEXT, false);
        if (searchX + searchW < width - 64) g.drawString(font, "RIGHT SHIFT", width - 87, 24, DIM, false);
    }

    private void drawSidebar(GuiGraphics g, Font font, Layout l, int mouseX, int mouseY) {
        panel(g, 8, l.headerH + 10, l.sidebarW - 8, height - l.headerH - 20, false);
        int y = l.headerH + 23;
        for (int i = 0; i < NAV.length; i++) {
            String item = NAV[i];
            boolean active = item.equals(activePage);
            int rowY = y + i * 38;
            if (active) {
                g.fillGradient(15, rowY, l.sidebarW - 7, rowY + 31, withAlpha(accent, 203), withAlpha(accentDark, 178));
                g.fill(15, rowY + 4, 17, rowY + 27, accentBright);
            } else if (inside(mouseX, mouseY, 15, rowY, l.sidebarW - 23, 31)) {
                g.fill(15, rowY, l.sidebarW - 7, rowY + 31, 0x4A29446B);
            }
            g.drawString(font, NAV_GLYPHS[i], 25, rowY + 10, active ? 0xFFFFFFFF : accent, true);
            if (sidebarLabels && l.sidebarW >= 106) g.drawString(font, item, 43, rowY + 10, active ? TEXT : captionColor(), false);
        }
        int footerY = height - 57;
        g.fill(17, footerY - 8, l.sidebarW - 16, footerY - 7, 0x66324B72);
        g.drawString(font, "ATLAS", 22, footerY, accentBright, true);
        if (l.sidebarW >= 106) g.drawString(font, "build 1.0.0", 22, footerY + 14, DIM, false);
    }

    private void drawOverview(GuiGraphics g, Font font, Layout l, int mouseX, int mouseY) {
        int gap = 8;
        int cardW = (l.contentW - gap * (l.columns - 1)) / l.columns;
        int cardH = (l.contentH - gap) / 2;
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
        g.fillGradient(x + 1, y + 1, x + w - 1, y + 31, 0xB9202D46, 0x88202D46);
        g.fill(x + 1, y + 31, x + w - 1, y + 32, 0x882D456A);
        g.drawString(font, groupGlyph(title), x + 9, y + 11, accentBright, true);
        g.drawString(font, fit(font, title, Math.max(24, w - 44)), x + 25, y + 11, headingColor(), true);
        if (sectionCounts) g.drawString(font, Integer.toString(options.size()), x + w - 20, y + 11, DIM, false);

        int rowHeight = compactSpacing || compactLabels ? 18 : 21;
        int scroll = scrollByGroup.getOrDefault(title, 0);
        int rows = Math.max(0, (h - 42) / rowHeight);
        int maxScroll = Math.max(0, options.size() - rows);
        scroll = Math.min(scroll, maxScroll);
        scrollByGroup.put(title, scroll);
        for (int i = 0; i < rows; i++) {
            int optionIndex = scroll + i;
            if (optionIndex >= options.size()) break;
            VisualOption option = options.get(optionIndex);
            int rowY = y + 39 + i * rowHeight;
            boolean hover = hoverFeedback && inside(mouseX, mouseY, x + 5, rowY - 3, w - 10, rowHeight);
            if (focusHighlight && option == selectedOption) g.fill(x + 5, rowY - 3, x + w - 5, rowY + rowHeight - 1, withAlpha(accent, 20 + selectedOption.intensity));
            else if (hover) g.fill(x + 5, rowY - 3, x + w - 5, rowY + rowHeight - 1, 0x322A4264);
            drawCheck(g, x + 10, rowY + 2, option.enabled);
            int labelX = x + 27;
            int labelWidth = Math.max(16, w - 39);
            if (option.palette) {
                g.fill(x + 28, rowY + 3, x + 39, rowY + 13, option.accent);
                g.fill(x + 28, rowY + 3, x + 39, rowY + 4, option.accentBright);
                labelX = x + 44;
                labelWidth = Math.max(16, w - 56);
            }
            g.drawString(font, fit(font, option.name, labelWidth), labelX, rowY + 3,
                    option == selectedOption ? TEXT : (option.enabled ? 0xFFBEDCFA : (highContrast ? TEXT : captionColor())), false);
        }
        if (options.size() > rows && rows > 0) {
            int trackX = x + w - 5;
            int trackY = y + 38;
            int trackH = Math.max(10, h - 47);
            g.fill(trackX, trackY, trackX + 2, trackY + trackH, 0x66304768);
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
        g.fillGradient(x + 1, y + 1, x + w - 1, y + 36, 0xD81B2A43, 0xA8152034);
        g.drawString(font, "APPEARANCE", x + 13, y + 9, accentBright, true);
        g.drawString(font, fit(font, selectedOption.name, w - 24), x + 13, y + 22, headingColor(), true);
        int cursorY = y + 51;
        g.drawString(font, selectedOption.group.toUpperCase(Locale.ROOT), x + 13, cursorY, captionColor(), false);
        cursorY += 29;
        g.drawString(font, "Active", x + 13, cursorY + 3, TEXT, false);
        drawSwitch(g, x + w - 48, cursorY - 2, selectedOption.enabled);
        cursorY += 35;
        g.fill(x + 12, cursorY, x + w - 12, cursorY + 1, 0x66354D70);
        cursorY += 14;
        g.drawString(font, "Intensity", x + 13, cursorY, TEXT, false);
        g.drawString(font, selectedOption.intensity + "%", x + w - 42, cursorY, captionColor(), false);
        drawSlider(g, x + 13, cursorY + 15, w - 26, selectedOption.intensity / 100.0);
        cursorY += 48;
        g.drawString(font, "Treatment", x + 13, cursorY, TEXT, false);
        g.drawString(font, selectedOption.treatment, x + w - 64, cursorY, accentBright, false);
        cursorY += 32;
        int noteH = Math.max(42, Math.min(68, h - (cursorY - y) - 58));
        if (selectedOption.treatment.equals("Soft")) {
            g.fillGradient(x + 11, cursorY, x + w - 11, cursorY + noteH, withAlpha(accent, 58), 0x7017243B);
        } else {
            g.fill(x + 11, cursorY, x + w - 11, cursorY + noteH, 0xB017243B);
        }
        g.drawString(font, "STYLE NOTE", x + 20, cursorY + 8, accentBright, true);
        drawWrapped(g, font, selectedOption.description, x + 20, cursorY + 22, w - 42, noteH - 24, captionColor());
        g.drawString(font, "VISUAL CONTROLS ONLY", x + 12, y + h - 29, DIM, false);
    }

    private void drawUtilityPage(GuiGraphics g, Font font, Layout l) {
        int x = l.contentX;
        int y = l.contentY;
        int w = l.contentW + l.detailW + (l.detailW > 0 ? 10 : 0);
        panel(g, x, y, w, l.contentH, false);
        String subtitle = switch (activePage) {
            case "Settings" -> "Tune the Atlas interface to your preference.";
            case "Visuals" -> "Personalize colors, panels, lighting, and visual density.";
            case "HUD" -> "Choose which interface elements are shown.";
            case "Profiles" -> "Visual profiles are planned for a later pass.";
            case "Configs" -> "Theme saving and import/export are planned for later.";
            default -> "Atlas Client · a visual-first interface for Minecraft 1.21.11.";
        };
        g.drawString(font, activePage.toUpperCase(Locale.ROOT), x + 18, y + 18, accentBright, true);
        g.drawString(font, subtitle, x + 18, y + 36, captionColor(), false);
        g.fill(x + 18, y + 57, x + w - 18, y + 58, 0x66354D70);

        if (activePage.equals("Profiles") || activePage.equals("Configs") || activePage.equals("About")) {
            String[] lines = activePage.equals("About")
                    ? new String[]{"Atlas Client", "Fabric client interface for Minecraft 1.21.11.", "Right Shift opens and closes this dashboard.", "This build focuses on visual styling; no gameplay features are included."}
                    : new String[]{"Coming soon", "Current appearance choices stay in memory for this game session.", "No files are written and no external config packs are loaded."};
            int lineY = y + 81;
            for (String line : lines) {
                g.drawString(font, line, x + 18, lineY, lineY == y + 81 ? TEXT : captionColor(), false);
                lineY += 22;
            }
            return;
        }

        String[] options = utilityOptions(activePage);
        int rowY = y + 75;
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            int col = i % 2;
            int row = i / 2;
            int cellX = x + 18 + col * ((w - 46) / 2);
            int cellY = rowY + row * 55;
            int cellW = (w - 54) / 2;
            g.fillGradient(cellX, cellY, cellX + cellW, cellY + 42,
                    0xA8172439, gradientPanels ? 0x88111B2D : 0xAA172439);
            g.drawString(font, fit(font, option, cellW - 55), cellX + 11, cellY + 15, TEXT, false);
            drawSwitch(g, cellX + cellW - 42, cellY + 10, utilityValue(option));
        }
        g.drawString(font, "Appearance adjustments are local to this game session.", x + 18, y + l.contentH - 12, DIM, false);
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

    private void drawCheck(GuiGraphics g, int x, int y, boolean checked) {
        g.fill(x, y, x + 13, y + 13, checked ? accent : 0xFF101A2B);
        g.fill(x, y, x + 13, y + 1, checked ? accentBright : 0xFF43506A);
        if (checked) {
            g.fill(x + 3, y + 6, x + 5, y + 9, 0xFFFFFFFF);
            g.fill(x + 5, y + 8, x + 10, y + 10, 0xFFFFFFFF);
        } else {
            g.fill(x, y + 12, x + 13, y + 13, 0xFF31415A);
        }
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean on) {
        g.fillGradient(x, y, x + 39, y + 20, on ? accent : 0xFF263348, on ? accentDark : 0xFF1A2638);
        int knobX = on ? x + 22 : x + 3;
        g.fill(knobX, y + 3, knobX + 14, y + 17, 0xFFF1F7FF);
        g.fill(knobX + 2, y + 4, knobX + 12, y + 5, 0x66FFFFFF);
    }

    private void drawSlider(GuiGraphics g, int x, int y, int w, double amount) {
        g.fill(x, y + 4, x + w, y + 7, 0xFF23314A);
        int fillW = Math.max(0, Math.min(w, (int) (w * amount)));
        g.fillGradient(x, y + 4, x + fillW, y + 7, accentBright, accentDark);
        int knobX = x + fillW - 4;
        g.fill(knobX, y, knobX + 9, y + 15, 0xFFECF6FF);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, boolean accentPanel) {
        if (w <= 0 || h <= 0) return;
        if (quietShadows) g.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x50000000);
        int surface = accentPanel ? (glassOverlay ? 0xC8111D32 : 0xF1111D32) : (glassOverlay ? 0xC80C1427 : PANEL);
        int surfaceBottom = glassOverlay ? 0xA00A1020 : 0xF00A1020;
        if (gradientPanels) g.fillGradient(x, y, x + w, y + h, surface, surfaceBottom);
        else g.fill(x, y, x + w, y + h, surface);
        int edgeAlpha = Math.min(225, 28 + intensityOf("Edge glow", 65) * 2);
        int border = accentPanel || edgeGlow ? withAlpha(accent, accentPanel ? 225 : edgeAlpha) : 0x8830476C;
        if (softEdges) border = withAlpha(border, 166);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, withAlpha(accentDark, softEdges ? 145 : 205));
        g.fill(x, y + 1, x + 1, y + h - 1, border);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
        if (glassOverlay && h > 6) g.fill(x + 3, y + 2, x + w - 3, y + 3, withAlpha(accentBright, 28));
    }

    private static void drawWrapped(GuiGraphics g, Font font, String text, int x, int y, int maxW, int maxH, int color) {
        StringBuilder line = new StringBuilder();
        int lineY = y;
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) > maxW && line.length() > 0) {
                g.drawString(font, line.toString(), x, lineY, color, false);
                lineY += 10;
                line.setLength(0);
                if (lineY > y + maxH - 8) return;
                line.append(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0 && lineY <= y + maxH - 8) g.drawString(font, line.toString(), x, lineY, color, false);
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (maxWidth <= 0 || font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > maxWidth) end--;
        return end == 0 ? "" : text.substring(0, end) + ellipsis;
    }

    private Layout layout() {
        int side = width < 460 ? 66 : width < 720 ? 96 : 132;
        int header = 59;
        int top = header + 11;
        int bottom = height - 12;
        int detailW = width >= 680 ? Math.max(164, Math.min(205, width / 5)) : 0;
        int detailGap = detailW > 0 ? 10 : 0;
        int contentX = side + 13;
        int rightMargin = 13;
        int contentW = Math.max(54, width - contentX - rightMargin - detailW - detailGap);
        int columns = contentW >= 600 ? (wideCards ? 3 : 4) : contentW >= 410 ? (wideCards ? 2 : 3) : contentW >= 250 ? 2 : 1;
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
        int navTop = l.headerH + 23;
        for (int i = 0; i < NAV.length; i++) {
            int rowY = navTop + i * 38;
            if (inside(mouseX, mouseY, 15, rowY, l.sidebarW - 22, 31)) {
                activePage = NAV[i];
                return true;
            }
        }
        int logoX = l.sidebarW + 12;
        int searchW = Math.max(88, width - (logoX + 250) - 28);
        int searchX = logoX + 250;
        if (searchX + searchW > width - 14) searchX = Math.max(logoX + 46, width - searchW - 14);
        if (inside(mouseX, mouseY, searchX, 15, searchW, 30)) return true;

        if (activePage.equals("Overview")) {
            if (l.detailW > 0) {
                int x = l.detailX;
                int y = l.contentY;
                int w = l.detailW;
                int activeY = y + 51 + 29;
                if (inside(mouseX, mouseY, x + w - 54, activeY - 7, 48, 28)) {
                    setVisualOption(selectedOption, !selectedOption.enabled);
                    return true;
                }
                int sliderY = activeY + 35 + 14 + 15;
                if (inside(mouseX, mouseY, x + 10, sliderY - 3, w - 20, 19)) {
                    selectedOption.intensity = (int) Math.round(Math.max(0, Math.min(1, (mouseX - (x + 13)) / (w - 26.0))) * 100);
                    return true;
                }
                int treatmentY = sliderY + 33;
                if (inside(mouseX, mouseY, x + 10, treatmentY - 4, w - 20, 25)) {
                    selectedOption.treatment = selectedOption.treatment.equals("Soft") ? "Crisp" : "Soft";
                    return true;
                }
            }
            int gap = 8;
            int cardW = (l.contentW - gap * (l.columns - 1)) / l.columns;
            int cardH = (l.contentH - gap) / 2;
            int rowHeight = compactSpacing || compactLabels ? 18 : 21;
            int groupIndex = 0;
            for (Map.Entry<String, List<VisualOption>> group : GROUPS.entrySet()) {
                int x = l.contentX + (groupIndex % l.columns) * (cardW + gap);
                int y = l.contentY + (groupIndex / l.columns) * (cardH + gap);
                if (inside(mouseX, mouseY, x, y + 38, cardW, cardH - 39)) {
                    List<VisualOption> options = visibleOptions(group.getValue());
                    int rows = Math.max(0, (cardH - 42) / rowHeight);
                    int clickedIndex = scrollByGroup.getOrDefault(group.getKey(), 0) + (int) ((mouseY - (y + 39)) / rowHeight);
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
            int w = l.contentW + l.detailW + (l.detailW > 0 ? 10 : 0);
            for (int index = 0; index < options.length; index++) {
                int cellX = x + 18 + (index % 2) * ((w - 46) / 2);
                int cellY = y + 75 + (index / 2) * 55;
                if (inside(mouseX, mouseY, cellX, cellY, (w - 54) / 2, 42)) {
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
        int gap = 8;
        int cardW = (l.contentW - gap * (l.columns - 1)) / l.columns;
        int cardH = (l.contentH - gap) / 2;
        int groupIndex = 0;
        int rowHeight = compactSpacing || compactLabels ? 18 : 21;
        for (String group : GROUPS.keySet()) {
            int x = l.contentX + (groupIndex % l.columns) * (cardW + gap);
            int y = l.contentY + (groupIndex / l.columns) * (cardH + gap);
            if (inside(mouseX, mouseY, x, y, cardW, cardH)) {
                int rows = Math.max(0, (cardH - 42) / rowHeight);
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
        palettes.add(VisualOption.palette("Ocean blue", "Deep navy panels with cool cyan highlights.", DEFAULT_ACCENT, DEFAULT_BRIGHT, DEFAULT_DARK, true));
        palettes.add(VisualOption.palette("Glacier", "Icy blue surfaces with bright frosted accents.", 0xFF6ED9F5, 0xFFB3F0FF, 0xFF3285B8, false));
        palettes.add(VisualOption.palette("Indigo", "A deeper violet-blue accent with a calm glow.", 0xFF7989FF, 0xFFB5BEFF, 0xFF3E4EB7, false));
        palettes.add(VisualOption.palette("Sapphire", "Saturated royal blue with a crisp highlight.", 0xFF3986FF, 0xFF8EB8FF, 0xFF214DAB, false));
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
