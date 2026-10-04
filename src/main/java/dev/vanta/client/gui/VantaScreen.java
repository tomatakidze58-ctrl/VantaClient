package dev.vanta.client.gui;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Vanta's Right-Shift menu. The layout deliberately mirrors the visual mockup:
 * categories on the left, module cards in the middle and the selected module
 * on the right. It only uses Minecraft/Fabric GUI abstractions, so it works
 * with both the OpenGL and Vulkan renderer paths in 26.2.
 */
public final class VantaScreen extends Screen {
    private Module.Category selectedCategory;
    private String selectedModuleName;

    public VantaScreen() {
        this(Module.Category.HUD, "FPS Counter");
    }

    private VantaScreen(Module.Category category, String selectedName) {
        super(Component.literal("Vanta Client"));
        this.selectedCategory = category;
        this.selectedModuleName = selectedName;
    }

    @Override
    protected void init() {
        int panelW = Math.min(1080, width - 24);
        int panelH = Math.min(650, height - 24);
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;

        final int sidebarW = 164;
        final int rightW = 246;
        final int gap = 12;
        int contentX = px + sidebarW + 24;
        int rightX = px + panelW - rightW - 14;
        int centerW = rightX - contentX - gap;

        // Category navigation.
        int cy = py + 90;
        for (Module.Category category : Module.Category.values()) {
            String prefix = category == selectedCategory ? "◆  " : "◇  ";
            this.addRenderableWidget(Button.builder(
                    Component.literal(prefix + label(category)),
                    b -> this.minecraft.gui.setScreen(new VantaScreen(category, firstModuleName(category)))
            ).bounds(px + 16, cy, sidebarW - 30, 22).build());
            cy += 29;
        }

        // Module cards. Clicking the main card selects it; the compact button toggles it.
        List<Module> mods = VantaClient.MODULES.in(selectedCategory);
        int cardGap = 8;
        int cardW = Math.max(150, (centerW - cardGap) / 2);
        int startY = py + 92;

        for (int i = 0; i < mods.size(); i++) {
            Module module = mods.get(i);
            int col = i % 2;
            int row = i / 2;
            int x = contentX + col * (cardW + cardGap);
            int y = startY + row * 34;
            if (y > py + panelH - 55) break;

            int toggleW = 48;
            this.addRenderableWidget(Button.builder(
                    Component.literal(module.name),
                    b -> this.minecraft.gui.setScreen(new VantaScreen(selectedCategory, module.name))
            ).bounds(x, y, cardW - toggleW - 4, 26).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal(module.enabled ? "ON" : "OFF"),
                    b -> {
                        VantaClient.MODULES.toggle(module);
                        this.minecraft.gui.setScreen(new VantaScreen(selectedCategory, module.name));
                    }
            ).bounds(x + cardW - toggleW, y, toggleW, 26).build());
        }

        // Right-side selected module toggle.
        Module selected = selectedModule();
        if (selected != null) {
            this.addRenderableWidget(Button.builder(
                    Component.literal(selected.enabled ? "Enabled" : "Disabled"),
                    b -> {
                        VantaClient.MODULES.toggle(selected);
                        this.minecraft.gui.setScreen(new VantaScreen(selectedCategory, selected.name));
                    }
            ).bounds(rightX + 14, py + 176, rightW - 28, 24).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        int panelW = Math.min(1080, width - 24);
        int panelH = Math.min(650, height - 24);
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        final int sidebarW = 164;
        final int rightW = 246;
        int rightX = px + panelW - rightW - 14;
        int contentX = px + sidebarW + 24;

        // Main shell.
        g.fill(px, py, px + panelW, py + panelH, 0xF4070610);
        g.outline(px, py, panelW, panelH, 0xFF6D28D9);
        g.fillGradient(px, py, px + panelW, py + 68, 0xF0180A2B, 0xF00B0713);

        // Sidebar and selected-module panel.
        g.fill(px + 10, py + 76, px + sidebarW, py + panelH - 10, 0xE80C0913);
        g.outline(px + 10, py + 76, sidebarW - 10, panelH - 86, 0x553B206C);
        g.fill(rightX, py + 78, px + panelW - 12, py + panelH - 12, 0xE80C0913);
        g.outline(rightX, py + 78, rightW - 2, panelH - 90, 0x663B206C);

        // Header.
        g.text(font, "V", px + 20, py + 20, 0xFFB469FF, true);
        g.text(font, "VANTA CLIENT", px + 39, py + 20, 0xFFF2EAFF, true);
        g.text(font, "PLAY BEYOND LIMITS", px + 39, py + 38, 0xFF7E6D93, false);
        g.text(font, "Minecraft 26.2", px + panelW - 112, py + 22, 0xFFC7B6D8, false);
        g.text(font, "Right Shift", px + panelW - 112, py + 39, 0xFF8F7AA8, false);

        // Center title.
        g.text(font, label(selectedCategory), contentX, py + 74, 0xFFEBDDFF, true);
        g.text(font, categoryDescription(selectedCategory), contentX, py + 88, 0xFF8D7B9E, false);

        // Selected module details.
        Module selected = selectedModule();
        int sx = rightX + 14;
        int sy = py + 96;
        if (selected != null) {
            g.text(font, selected.name, sx, sy, 0xFFF2EAFF, true);
            g.text(font, selected.enabled ? "● ENABLED" : "○ DISABLED", sx, sy + 18,
                    selected.enabled ? 0xFFB469FF : 0xFF71657F, true);
            drawWrapped(g, selected.description, sx, sy + 46, rightW - 28, 0xFFAA9AB8);

            g.text(font, "General", sx, py + 215, 0xFFB469FF, true);
            g.horizontalLine(sx, rightX + rightW - 18, py + 229, 0x553B206C);
            g.text(font, "Module state is saved automatically.", sx, py + 242, 0xFF8D7B9E, false);
            g.text(font, "Performance options restore vanilla", sx, py + 260, 0xFF8D7B9E, false);
            g.text(font, "values when their preset is disabled.", sx, py + 274, 0xFF8D7B9E, false);
        } else {
            g.text(font, "No modules in this category yet.", sx, sy, 0xFF8D7B9E, false);
        }

        // Bottom status strip.
        g.fill(px + sidebarW + 12, py + panelH - 42, rightX - 10, py + panelH - 12, 0x99110D18);
        g.text(font, VantaClient.MODULES.all().size() + " built-in modules", px + sidebarW + 24,
                py + panelH - 32, 0xFFB469FF, true);
        g.text(font, "Purple/black • local config • Fabric 26.2", px + sidebarW + 150,
                py + panelH - 32, 0xFF8C7D99, false);
    }

    private Module selectedModule() {
        Module module = VantaClient.MODULES.get(selectedModuleName);
        if (module != null && module.category == selectedCategory) return module;
        List<Module> list = VantaClient.MODULES.in(selectedCategory);
        return list.isEmpty() ? null : list.getFirst();
    }

    private static String firstModuleName(Module.Category category) {
        List<Module> list = VantaClient.MODULES.in(category);
        return list.isEmpty() ? "" : list.getFirst().name;
    }

    private static String label(Module.Category c) {
        return switch (c) {
            case HUD -> "HUD";
            case PVP -> "PvP";
            case VISUAL -> "Visual";
            case PERFORMANCE -> "Performance";
            case UTILITY -> "Utility";
        };
    }

    private static String categoryDescription(Module.Category c) {
        return switch (c) {
            case HUD -> "Information and interface overlays";
            case PVP -> "Legit combat and input utilities";
            case VISUAL -> "Client-side visual controls";
            case PERFORMANCE -> "FPS and rendering controls";
            case UTILITY -> "Quality-of-life tools";
        };
    }

    private void drawWrapped(GuiGraphicsExtractor g, String text, int x, int y, int maxWidth, int color) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int yy = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                g.text(font, line.toString(), x, yy, color, false);
                yy += 13;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) g.text(font, line.toString(), x, yy, color, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
