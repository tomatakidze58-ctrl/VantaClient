package dev.vanta.client.module;

public final class Module {
    public enum Category { HUD, PVP, VISUAL, PERFORMANCE, UTILITY }

    public final String name;
    public final String description;
    public final Category category;
    public boolean enabled;

    public Module(String name, Category category, String description, boolean enabled) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
