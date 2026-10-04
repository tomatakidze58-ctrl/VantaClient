package dev.vanta.client.util;

import dev.vanta.client.module.Module;
import dev.vanta.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vantaclient.properties");
    private Config() {}

    public static void load(ModuleManager modules) {
        if (!Files.exists(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            for (Module m : modules.all()) {
                String v = p.getProperty("module." + m.name);
                if (v != null) m.enabled = Boolean.parseBoolean(v);
            }
        } catch (Exception ignored) {}
    }

    public static void save(ModuleManager modules) {
        Properties p = new Properties();
        for (Module m : modules.all()) p.setProperty("module." + m.name, Boolean.toString(m.enabled));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Vanta Client"); }
        } catch (Exception ignored) {}
    }
}
