package dev.vanta.client.module;

import dev.vanta.client.VantaClient;
import dev.vanta.client.util.Config;
import dev.vanta.client.util.Reflect;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vanta's module registry. The performance modules deliberately use vanilla
 * options instead of renderer hacks so they stay compatible with 26.2's
 * OpenGL/Vulkan rendering abstraction.
 */
public final class ModuleManager {
    private final List<Module> all = new ArrayList<>();
    private boolean lastAttackDown;

    private boolean fpsPresetApplied;
    private boolean bobApplied;
    private boolean shadowsApplied;
    private boolean fullbrightApplied;
    private boolean dynamicFpsApplied;

    private Object oldRenderDistance;
    private Object oldSimulationDistance;
    private Object oldEntityDistance;
    private Object oldParticles;
    private Object oldBobbing;
    private Object oldEntityShadows;
    private Object oldGamma;
    private Object oldMaxFps;

    public ModuleManager() {
        // HUD — every item in this section is rendered by VantaHud.
        add("FPS Counter", Module.Category.HUD, "Current frames per second", true);
        add("Ping Display", Module.Category.HUD, "Server latency", true);
        add("Coordinates", Module.Category.HUD, "XYZ position", true);
        add("Direction", Module.Category.HUD, "Facing direction", false);
        add("Armor HUD", Module.Category.HUD, "Equipped armor overview", true);
        add("Hunger + Saturation", Module.Category.HUD, "Food and saturation values", true);
        add("Totem Counter", Module.Category.HUD, "Totems in inventory", false);
        add("Potion Effects", Module.Category.HUD, "Active effect count", false);
        add("Keystrokes", Module.Category.HUD, "WASD key state", true);
        add("CPS Counter", Module.Category.HUD, "Recent attack clicks", true);
        add("Memory Usage", Module.Category.HUD, "JVM memory use", false);
        add("Clock", Module.Category.HUD, "Local time", false);
        add("Server Address", Module.Category.HUD, "Current server", false);
        add("Player Count", Module.Category.HUD, "Players on server", false);

        // Legit PvP / convenience.
        add("Auto Sprint", Module.Category.PVP, "Automatically sprint while moving forward", true);
        add("Crosshair", Module.Category.PVP, "Purple Vanta crosshair overlay", false);
        add("Zoom", Module.Category.PVP, "Hold C to zoom", true);

        // Visual controls that are wired now.
        add("Fullbright", Module.Category.VISUAL, "Uniform maximum lightmap brightness", false);
        add("View Bobbing Off", Module.Category.VISUAL, "Disable view bobbing", false);
        add("Entity Shadows Off", Module.Category.VISUAL, "Disable entity shadows", false);
        add("Particles Minimal", Module.Category.VISUAL, "Use minimal particles", false);

        // Performance. FPS Boost is a reversible vanilla-settings preset.
        add("FPS Boost", Module.Category.PERFORMANCE, "Balanced reversible performance preset", true);
        add("Dynamic FPS", Module.Category.PERFORMANCE, "Lower FPS while the window is unfocused", true);
        add("Render Distance Cap", Module.Category.PERFORMANCE, "Cap render distance at 10 chunks", false);
        add("Fast Particles", Module.Category.PERFORMANCE, "Use minimal particle rendering", true);
        add("Entity Distance", Module.Category.PERFORMANCE, "Lower distant entity rendering", false);
        add("Chunk Performance", Module.Category.PERFORMANCE, "Cap simulation distance for stable FPS", false);
    }

    private void add(String n, Module.Category c, String d, boolean on) {
        all.add(new Module(n, c, d, on));
    }

    public List<Module> all() { return Collections.unmodifiableList(all); }
    public List<Module> in(Module.Category c) { return all.stream().filter(m -> m.category == c).toList(); }
    public Module get(String name) { return all.stream().filter(m -> m.name.equals(name)).findFirst().orElse(null); }
    public boolean on(String name) { Module m = get(name); return m != null && m.enabled; }

    public void toggle(Module m) {
        m.toggle();
        Config.save(this);
    }

    public void tick(Minecraft mc) {
        if (mc == null) return;

        // Auto Sprint: only when the player's normal forward key is actually held.
        if (mc.player != null && on("Auto Sprint") && Reflect.keyDown(mc.options, "keyUp")) {
            try { mc.player.setSprinting(true); } catch (Throwable ignored) {}
        }

        // CPS sampling from attack-key rising edges.
        boolean attack = Reflect.keyDown(mc.options, "keyAttack");
        if (attack && !lastAttackDown) VantaClient.registerClick();
        lastAttackDown = attack;

        applyFullbrightOption(mc);
        applyStandaloneVisualOptions(mc);
        applyFpsPreset(mc);
        applyIndividualPerformance(mc);
        applyDynamicFps(mc);
    }

    private void applyFullbrightOption(Minecraft mc) {
        boolean wanted = on("Fullbright");
        if (wanted && !fullbrightApplied) {
            oldGamma = Reflect.getOption(mc.options, "gamma");
            fullbrightApplied = true;
        }
        if (wanted) {
            // The lightmap mixin supplies uniform brightness. Gamma is also raised
            // as a fallback in case a renderer path does not consult the helper.
            Reflect.setOption(mc.options, 1.0D, "gamma");
        } else if (fullbrightApplied) {
            restore(mc.options, oldGamma, "gamma");
            oldGamma = null;
            fullbrightApplied = false;
        }
    }

    private void applyStandaloneVisualOptions(Minecraft mc) {
        boolean bobWanted = on("View Bobbing Off") || on("FPS Boost");
        if (bobWanted && !bobApplied) {
            oldBobbing = Reflect.getOption(mc.options, "bobView", "viewBobbing");
            bobApplied = true;
        }
        if (bobWanted) {
            Reflect.setOption(mc.options, false, "bobView", "viewBobbing");
        } else if (bobApplied) {
            restore(mc.options, oldBobbing, "bobView", "viewBobbing");
            oldBobbing = null;
            bobApplied = false;
        }

        boolean shadowsWanted = on("Entity Shadows Off") || on("FPS Boost");
        if (shadowsWanted && !shadowsApplied) {
            oldEntityShadows = Reflect.getOption(mc.options, "entityShadows");
            shadowsApplied = true;
        }
        if (shadowsWanted) {
            Reflect.setOption(mc.options, false, "entityShadows");
        } else if (shadowsApplied) {
            restore(mc.options, oldEntityShadows, "entityShadows");
            oldEntityShadows = null;
            shadowsApplied = false;
        }
    }

    private void applyFpsPreset(Minecraft mc) {
        boolean wanted = on("FPS Boost");
        if (wanted && !fpsPresetApplied) {
            oldRenderDistance = Reflect.getOption(mc.options, "renderDistance");
            oldSimulationDistance = Reflect.getOption(mc.options, "simulationDistance");
            oldEntityDistance = Reflect.getOption(mc.options, "entityDistanceScaling", "entityDistance");
            oldParticles = particleValue(mc);
            fpsPresetApplied = true;
        }

        if (wanted) {
            capIntOption(mc.options, 10, "renderDistance");
            capIntOption(mc.options, 6, "simulationDistance");
            capDoubleOption(mc.options, 0.75D, "entityDistanceScaling", "entityDistance");
            setMinimalParticles(mc);
        } else if (fpsPresetApplied) {
            restore(mc.options, oldRenderDistance, "renderDistance");
            restore(mc.options, oldSimulationDistance, "simulationDistance");
            restore(mc.options, oldEntityDistance, "entityDistanceScaling", "entityDistance");
            restoreParticles(mc, oldParticles);
            oldRenderDistance = oldSimulationDistance = oldEntityDistance = oldParticles = null;
            fpsPresetApplied = false;
        }
    }

    private void applyIndividualPerformance(Minecraft mc) {
        if (on("Render Distance Cap") && !on("FPS Boost")) capIntOption(mc.options, 10, "renderDistance");
        if (on("Chunk Performance") && !on("FPS Boost")) capIntOption(mc.options, 6, "simulationDistance");
        if (on("Entity Distance") && !on("FPS Boost")) capDoubleOption(mc.options, 0.75D, "entityDistanceScaling", "entityDistance");
        if ((on("Fast Particles") || on("Particles Minimal")) && !on("FPS Boost")) setMinimalParticles(mc);
    }

    private void applyDynamicFps(Minecraft mc) {
        boolean wanted = on("Dynamic FPS");
        boolean active = windowActive(mc);
        if (wanted && !active) {
            if (!dynamicFpsApplied) {
                oldMaxFps = Reflect.getOption(mc.options, "framerateLimit", "maxFps");
                dynamicFpsApplied = true;
            }
            capIntOption(mc.options, 30, "framerateLimit", "maxFps");
        } else if (dynamicFpsApplied) {
            restore(mc.options, oldMaxFps, "framerateLimit", "maxFps");
            oldMaxFps = null;
            dynamicFpsApplied = false;
        }
    }

    private static boolean windowActive(Minecraft mc) {
        Object v = Reflect.call(mc, "isWindowActive");
        if (!(v instanceof Boolean)) v = Reflect.call(mc, "isWindowFocused");
        return !(v instanceof Boolean b) || b;
    }

    private static void capIntOption(Object options, int max, String... names) {
        Object current = Reflect.getOption(options, names);
        if (current instanceof Number n && n.intValue() > max) Reflect.setOption(options, max, names);
    }

    private static void capDoubleOption(Object options, double max, String... names) {
        Object current = Reflect.getOption(options, names);
        if (current instanceof Number n && n.doubleValue() > max) Reflect.setOption(options, max, names);
    }

    private static Object particleOption(Minecraft mc) {
        Object option = Reflect.call(mc.options, "particles");
        if (option == null) option = Reflect.field(mc.options, "particles");
        return option;
    }

    private static Object particleValue(Minecraft mc) {
        Object option = particleOption(mc);
        return option == null ? null : Reflect.call(option, "get");
    }

    private static void restoreParticles(Minecraft mc, Object value) {
        if (value == null) return;
        Object option = particleOption(mc);
        if (option != null) Reflect.call(option, "set", value);
    }

    private static void setMinimalParticles(Minecraft mc) {
        Object option = particleOption(mc);
        if (option == null) return;
        Object current = Reflect.call(option, "get");
        if (current == null || !current.getClass().isEnum()) return;
        for (Object e : current.getClass().getEnumConstants()) {
            if (((Enum<?>) e).name().equalsIgnoreCase("MINIMAL")) {
                Reflect.call(option, "set", e);
                return;
            }
        }
    }

    private static void restore(Object options, Object value, String... names) {
        if (value != null) Reflect.setOption(options, value, names);
    }
}
