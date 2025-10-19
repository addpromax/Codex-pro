package cx.ajneb97.managers.dependencies;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.dependencies.worldguard.WorldGuardManager;
import cx.ajneb97.utils.PaperVersionChecker;
import org.bukkit.Bukkit;

public class DependencyManager {

    private Codex plugin;

    private boolean isPlaceholderAPI;
    private boolean isPaper;
    private boolean isMythicMobs;
    private boolean isEliteMobs;
    private WorldGuardManager worldGuardManager;

    public DependencyManager(Codex plugin){
        this.plugin = plugin;

        if(Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null){
            isPlaceholderAPI = true;
        }
        if(Bukkit.getServer().getPluginManager().getPlugin("WorldGuard") != null){
            worldGuardManager = new WorldGuardManager(plugin);
        }
        if(Bukkit.getServer().getPluginManager().getPlugin("MythicMobs") != null){
            isMythicMobs = true;
        }
        if(Bukkit.getServer().getPluginManager().getPlugin("EliteMobs") != null){
            isEliteMobs = true;
        }
        // 使用统一的Paper版本检测器
        isPaper = PaperVersionChecker.isPaper();
    }

    public boolean isPlaceholderAPI() {
        return isPlaceholderAPI;
    }

    public boolean isPaper() {
        return isPaper;
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    public boolean isMythicMobs() {
        return isMythicMobs;
    }

    public boolean isEliteMobs() {
        return isEliteMobs;
    }
}
