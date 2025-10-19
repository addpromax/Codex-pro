package net.momirealms.craftengine.bukkit.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitCraftEnginePlugin extends JavaPlugin {
    private final BukkitCraftEngine plugin;

    public BukkitCraftEnginePlugin() {
        this.plugin = new BukkitCraftEngine(this);
        this.plugin.applyDependencies();
        this.plugin.setUpConfig();
    }

    @Override
    public void onLoad() {
        this.plugin.onPluginLoad();
    }

    @Override
    public void onEnable() {
        this.plugin.onPluginEnable();
    }

    @Override
    public void onDisable() {
        this.plugin.onPluginDisable();
    }
}
