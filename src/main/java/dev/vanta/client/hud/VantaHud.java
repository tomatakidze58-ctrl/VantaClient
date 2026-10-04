package dev.vanta.client.hud;

import dev.vanta.client.VantaClient;
import dev.vanta.client.util.Reflect;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class VantaHud {
    private static boolean zoomHeld;
    private static Double oldFov;

    private VantaHud() {}
    public static void setZoomHeld(boolean held) { zoomHeld = held; }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        applyZoom(mc);

        int x = 6, y = 6;
        final int purple = 0xFFB469FF;
        final int white = 0xFFFFFFFF;
        final int panel = 0xB30A0810;

        if (VantaClient.MODULES.on("FPS Counter")) y = row(g, mc, x, y, "FPS", Integer.toString(fps(mc)), panel, purple, white);
        if (VantaClient.MODULES.on("Ping Display")) y = row(g, mc, x, y, "Ping", ping(mc) + " ms", panel, purple, white);
        if (VantaClient.MODULES.on("Coordinates")) y = row(g, mc, x, y, "XYZ", String.format("%.1f  %.1f  %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()), panel, purple, white);
        if (VantaClient.MODULES.on("Direction")) y = row(g, mc, x, y, "Facing", direction(mc.player.getYRot()), panel, purple, white);
        if (VantaClient.MODULES.on("Hunger + Saturation")) y = row(g, mc, x, y, "Food", mc.player.getFoodData().getFoodLevel() + "  Sat " + String.format("%.1f", mc.player.getFoodData().getSaturationLevel()), panel, purple, white);
        if (VantaClient.MODULES.on("Armor HUD")) y = row(g, mc, x, y, "Armor", armorText(mc), panel, purple, white);
        if (VantaClient.MODULES.on("Totem Counter")) y = row(g, mc, x, y, "Totems", Integer.toString(countTotems(mc)), panel, purple, white);
        if (VantaClient.MODULES.on("Potion Effects")) y = row(g, mc, x, y, "Effects", Integer.toString(mc.player.getActiveEffects().size()), panel, purple, white);
        if (VantaClient.MODULES.on("CPS Counter")) y = row(g, mc, x, y, "CPS", Integer.toString(VantaClient.cps()), panel, purple, white);
        if (VantaClient.MODULES.on("Memory Usage")) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) / 1024 / 1024;
            long max = r.maxMemory() / 1024 / 1024;
            y = row(g, mc, x, y, "RAM", used + "/" + max + " MB", panel, purple, white);
        }
        if (VantaClient.MODULES.on("Clock")) y = row(g, mc, x, y, "Time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), panel, purple, white);
        if (VantaClient.MODULES.on("Server Address")) y = row(g, mc, x, y, "Server", server(mc), panel, purple, white);
        if (VantaClient.MODULES.on("Player Count")) y = row(g, mc, x, y, "Players", playerCount(mc), panel, purple, white);
        if (VantaClient.MODULES.on("Keystrokes")) drawKeys(g, mc, 6, y + 4, panel, purple, white);
        if (VantaClient.MODULES.on("Crosshair")) drawCrosshair(g, mc, purple);
    }

    private static int row(GuiGraphicsExtractor g, Minecraft mc, int x, int y, String k, String v, int panel, int accent, int white) {
        int w = Math.max(95, mc.font.width(k + "  " + v) + 12);
        g.fill(x, y, x + w, y + 14, panel);
        g.text(mc.font, k, x + 4, y + 3, accent, true);
        g.text(mc.font, v, x + 6 + mc.font.width(k), y + 3, white, false);
        return y + 16;
    }

    private static int fps(Minecraft mc) { return Reflect.intCall(mc, "getFps", 0); }

    private static int ping(Minecraft mc) {
        try {
            Object conn = Reflect.call(mc, "getConnection");
            Object info = conn == null ? null : Reflect.call(conn, "getPlayerInfo", mc.player.getUUID());
            int p = Reflect.intCall(info, "getLatency", -1);
            if (p < 0) p = Reflect.intCall(info, "getPing", -1);
            return Math.max(0, p);
        } catch (Throwable ignored) { return 0; }
    }

    private static String server(Minecraft mc) {
        Object s = Reflect.call(mc, "getCurrentServer");
        Object ip = s == null ? null : Reflect.field(s, "ip");
        return ip == null ? "Singleplayer" : String.valueOf(ip);
    }

    private static String playerCount(Minecraft mc) {
        Object conn = Reflect.call(mc, "getConnection");
        Object list = conn == null ? null : Reflect.call(conn, "getOnlinePlayers");
        if (list instanceof java.util.Collection<?> c) return Integer.toString(c.size());
        return "1";
    }

    private static String armorText(Minecraft mc) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = mc.player.getItemBySlot(slot);
            if (!s.isEmpty()) pieces++;
        }
        return pieces + "/4";
    }

    private static int countTotems(Minecraft mc) {
        int count = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == Items.TOTEM_OF_UNDYING) count += s.getCount();
        }
        return count;
    }

    private static String direction(float yaw) {
        int d = Math.floorMod(Math.round(yaw / 90f), 4);
        return switch (d) { case 0 -> "South"; case 1 -> "West"; case 2 -> "North"; default -> "East"; };
    }

    private static void drawKeys(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int bg, int accent, int white) {
        boolean w = Reflect.keyDown(mc.options, "keyUp");
        boolean a = Reflect.keyDown(mc.options, "keyLeft");
        boolean s = Reflect.keyDown(mc.options, "keyDown");
        boolean d = Reflect.keyDown(mc.options, "keyRight");
        key(g, mc, x + 18, y, "W", w, bg, accent, white);
        key(g, mc, x, y + 18, "A", a, bg, accent, white);
        key(g, mc, x + 18, y + 18, "S", s, bg, accent, white);
        key(g, mc, x + 36, y + 18, "D", d, bg, accent, white);
    }

    private static void key(GuiGraphicsExtractor g, Minecraft mc, int x, int y, String text, boolean down, int bg, int accent, int white) {
        g.fill(x, y, x + 16, y + 16, down ? 0xCC8B3DFF : bg);
        g.outline(x, y, 16, 16, down ? accent : 0x553B2E6F);
        g.text(mc.font, text, x + 5, y + 4, down ? white : accent, true);
    }

    private static void drawCrosshair(GuiGraphicsExtractor g, Minecraft mc, int color) {
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;
        g.fill(cx - 6, cy, cx - 2, cy + 1, color);
        g.fill(cx + 3, cy, cx + 7, cy + 1, color);
        g.fill(cx, cy - 6, cx + 1, cy - 2, color);
        g.fill(cx, cy + 3, cx + 1, cy + 7, color);
    }

    private static void applyZoom(Minecraft mc) {
        // Always restore the user's FOV if Zoom is switched off while held.
        if (!VantaClient.MODULES.on("Zoom")) {
            if (oldFov != null) {
                Reflect.setOption(mc.options, oldFov.intValue(), "fov");
                oldFov = null;
            }
            return;
        }

        Object current = Reflect.getOption(mc.options, "fov");
        if (!(current instanceof Number n)) return;
        if (zoomHeld) {
            if (oldFov == null) oldFov = n.doubleValue();
            Reflect.setOption(mc.options, 30, "fov");
        } else if (oldFov != null) {
            Reflect.setOption(mc.options, oldFov.intValue(), "fov");
            oldFov = null;
        }
    }
}
