package cx.ajneb97.config;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.DebugManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class FishingConfigManager {
    
    private Codex plugin;
    private CommonConfig configFile;
    private FileConfiguration config;
    private boolean enabled;
    private boolean autoGenerate;
    private String categoryName;
    
    public FishingConfigManager(Codex plugin) {
        this.plugin = plugin;
        this.configFile = new CommonConfig("fishing.yml", plugin, "", true);
        this.configFile.registerConfig();
        loadConfig();
    }
    
    /**
     * 加载钓鱼图鉴配置
     */
    public void loadConfig() {
        this.config = configFile.getConfig();
        
        if (config == null) {
            plugin.getLogger().warning("[FISHING] 配置文件不存在，使用默认设置");
            useDefaultSettings();
            return;
        }
        
        // 加载配置项，如果不存在则使用默认值
        this.enabled = config.getBoolean("settings.enabled", true);
        this.autoGenerate = config.getBoolean("settings.auto_generate", true);
        this.categoryName = config.getString("settings.category_name", "fishing");
    }
    
    /**
     * 重新加载配置
     */
    public boolean reload() {
        try {
            configFile.reloadConfig();
            loadConfig();
            plugin.getLogger().info("[FISHING] 钓鱼配置重新加载成功");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 钓鱼配置重新加载失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用默认设置
     */
    private void useDefaultSettings() {
        this.enabled = true;
        this.autoGenerate = true;
        this.categoryName = "fishing";
    }
    
    /**
     * 获取配置文件
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 获取CommonConfig对象
     */
    public CommonConfig getConfigFile() {
        return configFile;
    }
    
    /**
     * 获取钓鱼图鉴是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取是否自动生成钓鱼图鉴
     */
    public boolean isAutoGenerate() {
        return autoGenerate;
    }
    
    /**
     * 获取钓鱼图鉴的分类名称
     */
    public String getCategoryName() {
        return categoryName;
    }
    
    /**
     * 获取是否启用调试模式
     * 现在使用统一的DebugManager
     */
    public boolean isDebug() {
        DebugManager debugManager = DebugManager.getInstance();
        return debugManager != null ? debugManager.isDebugEnabled() : false;
    }
    
    /**
     * 获取排除的钓鱼物品列表
     */
    public List<String> getExcludedFish() {
        if (config == null) {
            return new ArrayList<>();
        }
        return config.getStringList("settings.excluded_fish");
    }
} 