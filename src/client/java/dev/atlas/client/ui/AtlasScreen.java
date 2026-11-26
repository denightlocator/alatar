package dev.atlas.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Atlas's client-only visual customization dashboard. */
public final class AtlasScreen extends Screen {
    private static final int PANEL = 0xEE0B1427;
    private static final int TEXT = 0xFFF0F6FF;
    private static final int MUTED = 0xFF9AAAC7;
    private static final int DIM = 0xFF687A9B;
    private static final int DEFAULT_ACCENT = 0xFF4CA8FF;
    private static final int DEFAULT_BRIGHT = 0xFF9BE1FF;
    private static final int DEFAULT_DARK = 0xFF1D58A2;

    private static final String[] NAV = {"Overview", "Settings", "Visuals", "HUD", "Profiles", "Configs", "About"};
    private static final LinkedHashMap<String, List<VisualOption>> GROUPS = makeVisualGroups();
    private static final String[] CATEGORY_NAMES = GROUPS.keySet().toArray(String[]::new);

    private String activePage = "Overview";
    private String search = "";
    private String selectedUtilityOption;
    private boolean closeRequested;
    private double categoryScrollTarget;
    private double categoryScrollPosition;
    private long lastCategoryAnimationNanos;
    private double utilityScrollTarget;
    private double utilityScrollPosition;
    private long lastUtilityAnimationNanos;
    private VisualOption selectedOption = GROUPS.get("Palette").get(0);
    private final Map<String, Integer> scrollByGroup = new LinkedHashMap<>();
    private final Map<String, Boolean> pageToggles = new LinkedHashMap<>();
    private final Set<String> collapsedUtilityCategories = new HashSet<>();

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
    public void tick() {
        super.tick();
        if (closeRequested) {
            closeRequested = false;
            Minecraft.getInstance().setScreen(null);
        }
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
            drawOverview(graphics, font, layout, animateCategoryScroll());
        } else {
            drawUtilityPage(graphics, font, layout, animateUtilityScroll());
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
        fillRounded(g, width - 35, 22, 24, 24, 0x66263E5C, 8);
        drawText(g, font, "×", width - 28, 26, captionColor(), true);
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
        g.fill(l.sidebarW + 3, l.headerH + 12, l.sidebarW + 4, height - 14, 0x55405D86);
        int y = l.headerH + 25;
        for (int i = 0; i < NAV.length; i++) {
            String item = NAV[i];
            boolean active = item.equals(activePage);
            int rowY = y + i * 43;
            if (active) {
                fillRoundedGradient(g, 10, rowY, l.sidebarW - 20, 36,
                        withAlpha(accent, 220), withAlpha(accentDark, 190), 10);
                fillRounded(g, 10, rowY + 6, 3, 24, accentBright, 2);
            } else if (inside(mouseX, mouseY, 10, rowY, l.sidebarW - 20, 36)) {
                fillRounded(g, 10, rowY, l.sidebarW - 20, 36, withAlpha(accent, 46), 10);
            }
            drawNavIcon(g, i, 23, rowY + 12, active ? 0xFFFFFFFF : accent);
            if (sidebarLabels && l.sidebarW >= 106) drawText(g, font, item, 47, rowY + 13, active ? TEXT : captionColor(), false);
        }
        int footerY = height - 96;
        panel(g, 10, footerY, l.sidebarW - 20, 80, true);
        fillRoundedGradient(g, l.sidebarW / 2 - 16, footerY + 8, 32, 30, accentBright, accentDark, 9);
        drawText(g, font, "A", l.sidebarW / 2 - 4, footerY + 16, 0xFFFFFFFF, true);
        if (l.sidebarW >= 106) {
            drawText(g, font, "Atlas Client", Math.max(12, l.sidebarW / 2 - 37), footerY + 44, headingColor(), true);
            drawText(g, font, "build 1.0.0", Math.max(12, l.sidebarW / 2 - 30), footerY + 61, DIM, false);
        } else {
            drawText(g, font, "1.0.0", 17, footerY + 53, DIM, false);
        }
    }

    private double animateCategoryScroll() {
        long now = System.nanoTime();
        if (lastCategoryAnimationNanos == 0) lastCategoryAnimationNanos = now;
        double deltaSeconds = Math.min(0.05, (now - lastCategoryAnimationNanos) / 1_000_000_000.0);
        lastCategoryAnimationNanos = now;
        double easing = 1.0 - Math.exp(-deltaSeconds / 0.12);
        categoryScrollPosition += (categoryScrollTarget - categoryScrollPosition) * easing;
        if (Math.abs(categoryScrollTarget - categoryScrollPosition) < 0.001) {
            categoryScrollPosition = categoryScrollTarget;
        }
        return categoryScrollPosition;
    }

    private void drawOverview(GuiGraphics g, Font font, Layout l, double scrollPosition) {
        // Category cards echo the reference layout, while their module lists intentionally stay empty.
        int columns = Math.min(CATEGORY_NAMES.length, l.columns);
        int maxScroll = Math.max(0, CATEGORY_NAMES.length - columns);
        categoryScrollTarget = Math.min(categoryScrollTarget, maxScroll);
        categoryScrollPosition = Math.min(categoryScrollPosition, maxScroll);
        scrollPosition = Math.max(0, Math.min(maxScroll, scrollPosition));
        int gap = 12;
        int cardWidth = (l.contentW - gap * (columns - 1)) / columns;
        int first = Math.max(0, (int) Math.floor(scrollPosition));
        int last = Math.min(CATEGORY_NAMES.length - 1, (int) Math.ceil(scrollPosition + columns - 1));
        g.enableScissor(l.contentX, l.contentY, l.contentX + l.contentW, l.contentY + l.contentH);
        for (int index = first; index <= last; index++) {
            int x = l.contentX + (int) Math.round((index - scrollPosition) * (cardWidth + gap));
            drawEmptyCategory(g, font, CATEGORY_NAMES[index], x, l.contentY, cardWidth, l.contentH);
        }
        g.disableScissor();
        drawOverviewDetails(g, font, l);
    }

    private void drawEmptyCategory(GuiGraphics g, Font font, String category, int x, int y, int w, int h) {
        panel(g, x, y, w, h, false);
        fillRoundedTopGradient(g, x + 1, y + 1, w - 2, 42, 0xB9233A5D, 0x88213A5E, 10);
        g.fill(x + 12, y + 42, x + w - 12, y + 43, withAlpha(accent, 82));
        drawCategoryIcon(g, category, x + 12, y + 13, accentBright);
        drawText(g, font, fit(font, category, w - 36), x + 32, y + 14, headingColor(), true);
    }

    private void drawOverviewDetails(GuiGraphics g, Font font, Layout l) {
        if (l.detailW <= 0) return;
        panel(g, l.detailX, l.contentY, l.detailW, l.contentH, true);
        fillRoundedTopGradient(g, l.detailX + 1, l.contentY + 1, l.detailW - 2, 42,
                0xD8203B5F, 0xA8172942, 12);
        drawText(g, font, "OVERVIEW", l.detailX + 14, l.contentY + 14, accentBright, true);
        drawText(g, font, "EMPTY LIST", l.detailX + 16, l.contentY + 68, headingColor(), true);
        drawText(g, font, "No module rows are shown.", l.detailX + 16, l.contentY + 90, captionColor(), false);
        drawWrapped(g, font, "Scroll the center to browse categories.", l.detailX + 16, l.contentY + 112,
                l.detailW - 32, 48, captionColor());
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

    private double animateUtilityScroll() {
        long now = System.nanoTime();
        if (lastUtilityAnimationNanos == 0) lastUtilityAnimationNanos = now;
        double deltaSeconds = Math.min(0.05, (now - lastUtilityAnimationNanos) / 1_000_000_000.0);
        lastUtilityAnimationNanos = now;
        double easing = 1.0 - Math.exp(-deltaSeconds / 0.12);
        utilityScrollPosition += (utilityScrollTarget - utilityScrollPosition) * easing;
        if (Math.abs(utilityScrollTarget - utilityScrollPosition) < 0.5) {
            utilityScrollPosition = utilityScrollTarget;
        }
        return utilityScrollPosition;
    }

    private void drawUtilityPage(GuiGraphics g, Font font, Layout l, double scrollOffset) {
        int maxScroll = utilityScrollMaximum(l);
        utilityScrollTarget = Math.min(utilityScrollTarget, maxScroll);
        utilityScrollPosition = Math.min(utilityScrollPosition, maxScroll);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        List<UtilityCard> cards = utilityCards(l, utilityCategories(activePage), scrollOffset);
        g.enableScissor(l.contentX, l.contentY, l.contentX + l.contentW, l.contentY + l.contentH);
        for (UtilityCard card : cards) {
            int x = card.x;
            int y = card.y;
            int w = card.width;
            int h = card.height;
            UtilityCategory category = card.category;
            panel(g, x, y, w, h, false);
            fillRoundedTopGradient(g, x + 1, y + 1, w - 2, 38, 0xB9233A5D, 0x88213A5E, 10);
            drawCategoryIcon(g, category.title, x + 12, y + 11, accentBright);
            drawText(g, font, fit(font, category.title, w - 56), x + 33, y + 12, headingColor(), true);
            drawChevron(g, x + w - 20, y + 17, card.collapsed, accentBright);
            g.fill(x + 12, y + 38, x + w - 12, y + 39, withAlpha(accent, 82));
            if (!card.collapsed) for (int i = 0; i < category.items.size(); i++) {
                String item = category.items.get(i);
                int rowY = y + 46 + i * 30;
                if (category.toggles) {
                    drawCheck(g, x + 13, rowY + 5, utilityValue(item));
                    drawText(g, font, fit(font, item, w - 48), x + 36, rowY + 8, TEXT, false);
                } else {
                    fillRounded(g, x + 16, rowY + 9, 7, 7, accentBright, 4);
                    drawText(g, font, fit(font, item, w - 44), x + 31, rowY + 7, i == 0 ? TEXT : captionColor(), i == 0);
                }
            }
        }
        g.disableScissor();
        drawUtilityDetails(g, font, l);
    }

    private void drawUtilityDetails(GuiGraphics g, Font font, Layout l) {
        if (l.detailW <= 0) return;
        int x = l.detailX;
        int y = l.contentY;
        int w = l.detailW;
        int h = l.contentH;
        panel(g, x, y, w, h, true);
        fillRoundedTopGradient(g, x + 1, y + 1, w - 2, 42, 0xD8203B5F, 0xA8172942, 12);
        drawText(g, font, "DETAILS", x + 14, y + 14, accentBright, true);
        drawText(g, font, "×", x + w - 22, y + 12, captionColor(), true);

        UtilityCategory selectedCategory = null;
        if (selectedUtilityOption != null) {
            for (UtilityCategory category : utilityCategories(activePage)) {
                if (category.items.contains(selectedUtilityOption)) {
                    selectedCategory = category;
                    break;
                }
            }
        }
        if (selectedCategory == null) {
            String detailTitle = switch (activePage) {
                case "About" -> "ATLAS CLIENT";
                case "Profiles" -> "NO PROFILES";
                case "Configs" -> "LOCAL SESSION";
                default -> "SELECT AN OPTION";
            };
            String detailText = switch (activePage) {
                case "About" -> "A visual interface for Minecraft 1.21.11. No gameplay modules are included.";
                case "Profiles" -> "Visual profile presets are planned for a later update.";
                case "Configs" -> "Appearance settings stay in memory and are not written to files.";
                default -> "Choose a row from one of the center categories to inspect its current state.";
            };
            drawText(g, font, detailTitle, x + 16, y + 66, headingColor(), true);
            drawWrapped(g, font, detailText, x + 16, y + 88, w - 32, 110, captionColor());
            return;
        }

        drawText(g, font, selectedCategory.title.toUpperCase(Locale.ROOT), x + 16, y + 62, captionColor(), false);
        drawText(g, font, fit(font, selectedUtilityOption, w - 32), x + 16, y + 81, headingColor(), true);
        if (selectedCategory.toggles) {
            VisualOption detail = findVisualOption(selectedUtilityOption);
            if (detail != null && detail.palette) {
                drawText(g, font, "ACTIVE PALETTE", x + 16, y + 124, accentBright, true);
                fillRounded(g, x + 16, y + 151, w - 32, 18, detail.accent, 8);
                fillRounded(g, x + 16, y + 175, w - 32, 8, detail.accentBright, 4);
                fillRounded(g, x + 16, y + 188, w - 32, 5, detail.accentDark, 3);
                drawWrapped(g, font, detail.description, x + 16, y + 210, w - 32, 76, captionColor());
            } else {
                int toggleY = y + 124;
                drawText(g, font, "Enabled", x + 16, toggleY + 4, TEXT, false);
                drawSwitch(g, x + w - 54, toggleY, utilityValue(selectedUtilityOption));
                g.fill(x + 15, toggleY + 38, x + w - 15, toggleY + 39, 0x66405D86);
                if (detail != null) {
                    int controlY = toggleY + 56;
                    boolean hasIntensity = selectedUtilityOption.equals("Ambient glow") || selectedUtilityOption.equals("Edge glow");
                    boolean hasTreatment = selectedUtilityOption.equals("Soft edges");
                    if (hasIntensity) {
                        drawText(g, font, "Intensity", x + 16, controlY, TEXT, false);
                        drawText(g, font, detail.intensity + "%", x + w - 48, controlY, captionColor(), false);
                        drawSlider(g, x + 16, controlY + 17, w - 32, detail.intensity / 100.0);
                        controlY += 52;
                    }
                    if (hasTreatment) {
                        drawText(g, font, "Treatment", x + 16, controlY, TEXT, false);
                        drawText(g, font, detail.treatment, x + w - 68, controlY, accentBright, false);
                        drawChevron(g, x + w - 21, controlY + 3, false, accentBright);
                        controlY += 24;
                    }
                    fillRounded(g, x + 13, controlY + 5, w - 26, 66, 0x70172942, 9);
                    drawText(g, font, "STYLE NOTE", x + 22, controlY + 14, accentBright, true);
                    drawWrapped(g, font, detail.description, x + 22, controlY + 31, w - 44, 36, captionColor());
                } else {
                    drawText(g, font, "VISUAL CONTROL", x + 16, toggleY + 56, accentBright, true);
                    drawWrapped(g, font, selectedUtilityOption + " changes the Atlas interface only.",
                            x + 16, toggleY + 75, w - 32, 90, captionColor());
                }
            }
        }
        drawText(g, font, "SESSION ONLY", x + 14, y + h - 25, DIM, false);
    }

    private List<UtilityCategory> utilityCategories(String page) {
        return switch (page) {
            case "Settings" -> List.of(
                    new UtilityCategory("Layout", List.of("Compact spacing", "Wide cards"), true),
                    new UtilityCategory("Panels", List.of("Gradient panels", "Glass overlay", "Quiet shadows", "Soft edges"), true),
                    new UtilityCategory("Lighting", List.of("Ambient glow", "Edge glow"), true),
                    new UtilityCategory("Typography", List.of("Bright headings", "Muted captions", "High contrast"), true),
                    new UtilityCategory("Feedback", List.of("Hover feedback", "Focus highlight"), true),
                    new UtilityCategory("Navigation", List.of("Sidebar labels", "Section counts"), true));
            case "Visuals" -> List.of(
                    new UtilityCategory("Palette", List.of("Ocean blue", "Glacier", "Sapphire", "Midnight cyan"), true),
                    new UtilityCategory("Lighting", List.of("Ambient glow", "Edge glow", "Focus highlight"), true),
                    new UtilityCategory("Surfaces", List.of("Gradient panels", "Glass overlay", "Quiet shadows", "Soft edges"), true),
                    new UtilityCategory("Typography", List.of("Bright headings", "Muted captions", "Compact labels"), true),
                    new UtilityCategory("Contrast", List.of("High contrast", "Reduced glare"), true),
                    new UtilityCategory("Feedback", List.of("Hover feedback", "Focus highlight"), true));
            case "HUD" -> List.of(
                    new UtilityCategory("Status", List.of("Show status chips"), true),
                    new UtilityCategory("Coordinates", List.of("Show coordinates"), true),
                    new UtilityCategory("Indicators", List.of("Section counts"), true));
            case "Profiles" -> List.of(
                    new UtilityCategory("Profiles", List.of("No saved visual profiles", "Profile presets planned for a later update"), false),
                    new UtilityCategory("Session", List.of("Appearance stays active for this game session"), false));
            case "Configs" -> List.of(
                    new UtilityCategory("Storage", List.of("No configuration files loaded", "Atlas does not write config files in this build"), false),
                    new UtilityCategory("Tools", List.of("Import and export planned for a later update"), false));
            default -> List.of(
                    new UtilityCategory("Atlas Client", List.of("Fabric interface for Minecraft 1.21.11", "Right Shift opens and closes Atlas"), false),
                    new UtilityCategory("Scope", List.of("Visual interface only", "No gameplay modules"), false),
                    new UtilityCategory("Build", List.of("Version 1.0.0", "Client-side only"), false));
        };
    }

    private List<UtilityCard> utilityCards(Layout l, List<UtilityCategory> categories, double scrollOffset) {
        int x = l.contentX;
        int width = l.contentW;
        int gap = 12;
        int columns = Math.min(categories.size(), Math.max(1, l.columns));
        int cardWidth = (width - gap * (columns - 1)) / columns;
        List<UtilityCard> cards = new ArrayList<>();
        int y = l.contentY - (int) Math.round(scrollOffset);
        for (int start = 0; start < categories.size(); start += columns) {
            int rowCount = Math.min(columns, categories.size() - start);
            int rowHeight = 0;
            for (int i = 0; i < rowCount; i++) {
                UtilityCategory category = categories.get(start + i);
                int cardHeight = isUtilityCategoryCollapsed(activePage, category.title)
                        ? 50 : 50 + category.items.size() * 30;
                rowHeight = Math.max(rowHeight, cardHeight);
            }
            for (int i = 0; i < rowCount; i++) {
                UtilityCategory category = categories.get(start + i);
                boolean collapsed = isUtilityCategoryCollapsed(activePage, category.title);
                int cardHeight = collapsed ? 50 : 50 + category.items.size() * 30;
                cards.add(new UtilityCard(category, x + i * (cardWidth + gap), y, cardWidth, cardHeight, collapsed));
            }
            y += rowHeight + gap;
        }
        return cards;
    }

    private String utilityCategoryKey(String page, String category) {
        return page + ":" + category;
    }

    private boolean isUtilityCategoryCollapsed(String page, String category) {
        return collapsedUtilityCategories.contains(utilityCategoryKey(page, category));
    }

    private int utilityScrollMaximum(Layout l) {
        int contentBottom = utilityCards(l, utilityCategories(activePage), 0).stream()
                .mapToInt(card -> card.y + card.height).max().orElse(l.contentY);
        return Math.max(0, contentBottom - (l.contentY + l.contentH));
    }

    private boolean utilityValue(String option) {
        VisualOption visual = findVisualOption(option);
        if (visual != null && visual.palette) return visual.enabled;
        return switch (option) {
            case "Compact spacing" -> compactSpacing;
            case "Wide cards" -> wideCards;
            case "Quiet shadows" -> quietShadows;
            case "Gradient panels" -> gradientPanels;
            case "Ambient glow" -> ambientGlow;
            case "Edge glow" -> edgeGlow;
            case "Focus highlight" -> focusHighlight;
            case "Glass overlay" -> glassOverlay;
            case "Soft edges" -> softEdges;
            case "Compact labels" -> compactLabels;
            case "Bright headings" -> brightHeadings;
            case "Muted captions", "Reduced glare" -> mutedCaptions;
            case "High contrast" -> highContrast;
            case "Sidebar labels" -> sidebarLabels;
            case "Hover feedback" -> hoverFeedback;
            case "Section counts" -> sectionCounts;
            default -> pageToggles.getOrDefault(option, true);
        };
    }

    private VisualOption findVisualOption(String name) {
        for (List<VisualOption> options : GROUPS.values()) {
            for (VisualOption option : options) {
                if (option.name.equals(name)) return option;
            }
        }
        return null;
    }

    private void toggleUtilityOption(String option) {
        VisualOption visual = findVisualOption(option);
        if (visual != null && visual.palette) {
            setVisualOption(visual, true);
            return;
        }
        boolean value = !utilityValue(option);
        switch (option) {
            case "Compact spacing" -> compactSpacing = value;
            case "Wide cards" -> wideCards = value;
            case "Quiet shadows" -> quietShadows = value;
            case "Gradient panels" -> gradientPanels = value;
            case "Ambient glow" -> ambientGlow = value;
            case "Edge glow" -> edgeGlow = value;
            case "Focus highlight" -> focusHighlight = value;
            case "Glass overlay" -> glassOverlay = value;
            case "Soft edges" -> softEdges = value;
            case "Compact labels" -> compactLabels = value;
            case "Bright headings" -> brightHeadings = value;
            case "Muted captions", "Reduced glare" -> mutedCaptions = value;
            case "High contrast" -> highContrast = value;
            case "Sidebar labels" -> sidebarLabels = value;
            case "Hover feedback" -> hoverFeedback = value;
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
        // Use Minecraft's bundled font for readable fallback glyphs on every client.
        return Component.literal(value).withStyle(Style.EMPTY.withBold(emphasize));
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
                fillRounded(g, x + 1, y + 2, 12, 11, color, 6);
                g.fill(x + 8, y + 8, x + 14, y + 14, 0xFF0B1427);
                g.fill(x + 3, y + 4, x + 5, y + 6, 0xFF0B1427);
                g.fill(x + 7, y + 3, x + 9, y + 5, 0xFF0B1427);
                g.fill(x + 4, y + 9, x + 6, y + 11, 0xFF0B1427);
            }
            case 3 -> {
                g.fill(x, y + 7, x + 3, y + 12, color);
                g.fill(x + 5, y + 4, x + 8, y + 12, color);
                g.fill(x + 10, y, x + 13, y + 12, color);
            }
            case 4 -> {
                g.fill(x + 2, y + 4, x + 7, y + 9, color);
                g.fill(x + 7, y + 4, x + 12, y + 9, color);
                g.fill(x + 4, y + 1, x + 10, y + 5, color);
                g.fill(x + 4, y + 8, x + 10, y + 13, color);
                g.fill(x + 5, y + 5, x + 9, y + 8, 0xFF0B1427);
            }
            case 5 -> {
                g.fill(x + 2, y + 5, x + 12, y + 12, color);
                g.fill(x + 3, y + 2, x + 8, y + 10, color);
                g.fill(x + 7, y, x + 12, y + 9, color);
                g.fill(x + 4, y + 8, x + 10, y + 10, 0xFF0B1427);
            }
            default -> {
                g.fill(x + 1, y + 1, x + 12, y + 12, color);
                g.fill(x + 3, y + 3, x + 10, y + 10, 0xFF0B1427);
                g.fill(x + 6, y + 5, x + 8, y + 9, color);
                g.fill(x + 6, y + 3, x + 8, y + 4, color);
            }
        }
    }

    private void drawChevron(GuiGraphics g, int x, int y, boolean collapsed, int color) {
        if (collapsed) {
            g.fill(x, y, x + 2, y + 2, color);
            g.fill(x + 2, y + 2, x + 4, y + 4, color);
            g.fill(x, y + 4, x + 2, y + 6, color);
        } else {
            g.fill(x, y, x + 2, y + 2, color);
            g.fill(x + 2, y + 2, x + 4, y + 4, color);
            g.fill(x + 4, y, x + 6, y + 2, color);
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
                if (!activePage.equals(NAV[i])) {
                    activePage = NAV[i];
                    selectedUtilityOption = null;
                    for (UtilityCategory category : utilityCategories(activePage)) {
                        if (category.toggles && !category.items.isEmpty()) {
                            selectedUtilityOption = category.items.get(0);
                            selectedOption = findVisualOption(selectedUtilityOption);
                            break;
                        }
                    }
                    utilityScrollTarget = 0;
                    utilityScrollPosition = 0;
                }
                return true;
            }
        }
        int searchX = headerSearchX();
        int searchW = headerSearchWidth();
        if (inside(mouseX, mouseY, searchX, 18, searchW, 34)) return true;
        if (inside(mouseX, mouseY, width - 37, 18, 28, 32)) {
            closeRequested = true;
            return true;
        }

        if (activePage.equals("Overview")) return super.mouseClicked(event, doubleClick);
        for (UtilityCard card : utilityCards(l, utilityCategories(activePage), utilityScrollPosition)) {
            if (inside(mouseX, mouseY, card.x + card.width - 30, card.y + 3, 26, 32)) {
                String key = utilityCategoryKey(activePage, card.category.title);
                if (!collapsedUtilityCategories.remove(key)) collapsedUtilityCategories.add(key);
                return true;
            }
            if (!card.category.toggles || card.collapsed) continue;
            for (int index = 0; index < card.category.items.size(); index++) {
                int rowY = card.y + 46 + index * 30;
                if (inside(mouseX, mouseY, card.x + 8, rowY, card.width - 16, 28)) {
                    selectedUtilityOption = card.category.items.get(index);
                    VisualOption visualOption = findVisualOption(selectedUtilityOption);
                    if (visualOption != null) selectedOption = visualOption;
                    toggleUtilityOption(selectedUtilityOption);
                    return true;
                }
            }
        }
        if (l.detailW > 0) {
            if (inside(mouseX, mouseY, l.detailX + l.detailW - 34, l.contentY + 6, 26, 28)) {
                selectedUtilityOption = null;
                return true;
            }
            if (selectedUtilityOption != null) {
                for (UtilityCategory category : utilityCategories(activePage)) {
                    if (!category.toggles || !category.items.contains(selectedUtilityOption)) continue;
                    VisualOption detail = findVisualOption(selectedUtilityOption);
                    if ((detail == null || !detail.palette)
                            && inside(mouseX, mouseY, l.detailX + l.detailW - 60, l.contentY + 112, 50, 36)) {
                        toggleUtilityOption(selectedUtilityOption);
                        return true;
                    }
                    if (detail != null && !detail.palette
                            && (selectedUtilityOption.equals("Ambient glow") || selectedUtilityOption.equals("Edge glow"))
                            && inside(mouseX, mouseY, l.detailX + 10, l.contentY + 194, l.detailW - 20, 22)) {
                        detail.intensity = (int) Math.round(Math.max(0, Math.min(1,
                                (mouseX - (l.detailX + 16)) / (l.detailW - 32.0))) * 100);
                        return true;
                    }
                    if (detail != null && !detail.palette && selectedUtilityOption.equals("Soft edges")
                            && inside(mouseX, mouseY, l.detailX + 12, l.contentY + 172, l.detailW - 24, 30)) {
                        detail.treatment = detail.treatment.equals("Soft") ? "Crisp" : "Soft";
                        softEdges = detail.treatment.equals("Soft");
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            Layout l = layout();
            if (inside(mouseX, mouseY, l.contentX, l.contentY, l.contentW, l.contentH)) {
                if (activePage.equals("Overview")) {
                    int columns = Math.min(CATEGORY_NAMES.length, l.columns);
                    int maxCategoryScroll = Math.max(0, CATEGORY_NAMES.length - columns);
                    categoryScrollTarget = Math.max(0, Math.min(maxCategoryScroll,
                            categoryScrollTarget - verticalAmount));
                    return true;
                }
                int maxScroll = utilityScrollMaximum(l);
                if (maxScroll > 0) {
                    utilityScrollTarget = Math.max(0, Math.min(maxScroll, utilityScrollTarget - verticalAmount * 36));
                    return true;
                }
            }
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
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // Close on the next screen tick so Escape cannot also reach the game behind Atlas.
            closeRequested = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            int lastCodePointStart = search.offsetByCodePoints(search.length(), -1);
            search = search.substring(0, lastCodePointStart);
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            search = "";
        }
        // Consume otherwise-unhandled keys instead of forwarding them to Minecraft key bindings.
        return true;
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

    private void drawCategoryIcon(GuiGraphics g, String category, int x, int y, int color) {
        switch (category) {
            case "Palette" -> {
                fillRounded(g, x, y + 1, 14, 12, color, 7);
                g.fill(x + 8, y + 8, x + 14, y + 14, 0xFF0B1427);
                g.fill(x + 3, y + 3, x + 5, y + 5, 0xFF0B1427);
                g.fill(x + 7, y + 2, x + 9, y + 4, 0xFF0B1427);
                g.fill(x + 3, y + 8, x + 5, y + 10, 0xFF0B1427);
            }
            case "Layout" -> {
                g.fill(x, y, x + 5, y + 5, color);
                g.fill(x + 8, y, x + 13, y + 5, color);
                g.fill(x, y + 8, x + 5, y + 13, color);
                g.fill(x + 8, y + 8, x + 13, y + 13, color);
            }
            case "Panels", "Surfaces" -> {
                g.fill(x, y, x + 14, y + 2, color);
                g.fill(x, y + 2, x + 2, y + 12, color);
                g.fill(x + 12, y + 2, x + 14, y + 12, color);
                g.fill(x + 2, y + 12, x + 12, y + 14, color);
                g.fill(x + 4, y + 5, x + 10, y + 7, withAlpha(color, 180));
            }
            case "Lighting" -> {
                g.fill(x + 4, y + 4, x + 10, y + 10, color);
                g.fill(x + 6, y, x + 8, y + 3, color);
                g.fill(x + 6, y + 11, x + 8, y + 14, color);
                g.fill(x, y + 6, x + 3, y + 8, color);
                g.fill(x + 11, y + 6, x + 14, y + 8, color);
            }
            case "Typography" -> {
                g.fill(x + 1, y + 1, x + 13, y + 3, color);
                g.fill(x + 6, y + 3, x + 8, y + 12, color);
                g.fill(x + 3, y + 12, x + 11, y + 14, color);
            }
            case "Navigation" -> {
                for (int row = 0; row < 3; row++) {
                    g.fill(x + 4, y + 2 + row * 4, x + 14, y + 4 + row * 4, color);
                    g.fill(x, y + 2 + row * 4, x + 2, y + 4 + row * 4, color);
                }
            }
            case "Feedback" -> {
                g.fill(x + 1, y + 6, x + 5, y + 9, color);
                g.fill(x + 4, y + 8, x + 7, y + 12, color);
                g.fill(x + 6, y + 5, x + 14, y + 8, color);
            }
            case "Status", "Indicators" -> {
                g.fill(x, y + 9, x + 3, y + 14, color);
                g.fill(x + 5, y + 5, x + 8, y + 14, color);
                g.fill(x + 10, y, x + 13, y + 14, color);
            }
            case "Coordinates" -> {
                g.fill(x + 6, y, x + 8, y + 14, color);
                g.fill(x, y + 6, x + 14, y + 8, color);
                g.fill(x + 4, y + 4, x + 10, y + 10, 0xFF0B1427);
            }
            case "Profiles" -> {
                g.fill(x + 4, y, x + 10, y + 6, color);
                g.fill(x + 2, y + 7, x + 12, y + 14, color);
            }
            case "Storage", "Tools" -> {
                g.fill(x + 1, y + 5, x + 13, y + 13, color);
                g.fill(x + 3, y + 2, x + 9, y + 9, color);
                g.fill(x + 7, y, x + 12, y + 8, color);
            }
            default -> {
                g.fill(x + 1, y + 1, x + 13, y + 13, color);
                g.fill(x + 3, y + 3, x + 11, y + 11, 0xFF0B1427);
                g.fill(x + 6, y + 4, x + 8, y + 9, color);
                g.fill(x + 6, y + 10, x + 8, y + 12, color);
            }
        }
    }

    private static String groupGlyph(String group) {
        return switch (group) {
            case "Palette" -> "P";
            case "Surfaces" -> "S";
            case "Lighting" -> "L";
            case "Typography" -> "T";
            case "Layout" -> "G";
            default -> group.isEmpty() ? "" : group.substring(0, 1).toUpperCase(Locale.ROOT);
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

    private record UtilityCategory(String title, List<String> items, boolean toggles) {}
    private record UtilityCard(UtilityCategory category, int x, int y, int width, int height, boolean collapsed) {}

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
