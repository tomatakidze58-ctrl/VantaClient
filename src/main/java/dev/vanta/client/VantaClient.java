package dev.vanta.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanta.client.gui.VantaScreen;
import dev.vanta.client.hud.VantaHud;
import dev.vanta.client.module.ModuleManager;
import dev.vanta.client.util.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;

public final class VantaClient implements ClientModInitializer {
    public static final String MOD_ID = "vantaclient";
    public static final ModuleManager MODULES = new ModuleManager();
    private static final Deque<Long> CLICKS = new ArrayDeque<>();

    private KeyMapping menuKey;
    private KeyMapping zoomKey;

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    @Override
    public void onInitializeClient() {
        Config.load(MODULES);

        KeyMapping.Category category = KeyMapping.Category.register(id("main"));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.vantaclient.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));
        zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.vantaclient.zoom", InputConstants.Type.KEYSYM, InputConstants.KEY_C, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) client.gui.setScreen(new VantaScreen());
            MODULES.tick(client);
            VantaHud.setZoomHeld(zoomKey.isDown());
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("hud"), VantaHud::render);
    }

    public static void registerClick() {
        long now = System.currentTimeMillis();
        CLICKS.addLast(now);
        while (!CLICKS.isEmpty() && CLICKS.peekFirst() < now - 1000L) CLICKS.removeFirst();
    }

    public static int cps() {
        long now = System.currentTimeMillis();
        while (!CLICKS.isEmpty() && CLICKS.peekFirst() < now - 1000L) CLICKS.removeFirst();
        return CLICKS.size();
    }
}
