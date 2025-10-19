package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 统一的调试模式管理器
 * 用于集中管理所有模块的debug状态
 */
public class DebugManager {
    
    private static DebugManager instance;
    private final Codex plugin;
    private boolean debugEnabled;
    
    private DebugManager(Codex plugin) {
        this.plugin = plugin;
        loadDebugState();
    }
    
    /**
     * 获取DebugManager实例
     * @param plugin 插件实例
     * @return DebugManager实例
     */
    public static DebugManager getInstance(Codex plugin) {
        if (instance == null) {
            instance = new DebugManager(plugin);
        }
        return instance;
    }
    
    /**
     * 获取DebugManager实例（无参数版本）
     * @return DebugManager实例，如果尚未初始化则返回null
     */
    public static DebugManager getInstance() {
        return instance;
    }
    
    /**
     * 从配置文件加载debug状态
     */
    private void loadDebugState() {
        FileConfiguration config = plugin.getConfigsManager().getMainConfigManager().getConfigFile().getConfig();
        this.debugEnabled = config.getBoolean("codex.debug", false);
        
        if (debugEnabled) {
            plugin.getLogger().info("[DEBUG] Debug模式已启用");
        }
    }
    
    /**
     * 检查是否启用了debug模式
     * @return 是否启用debug模式
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * 设置debug模式
     * @param enabled 是否启用debug模式
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        
        // 更新配置文件
        FileConfiguration config = plugin.getConfigsManager().getMainConfigManager().getConfigFile().getConfig();
        config.set("codex.debug", enabled);
        plugin.getConfigsManager().getMainConfigManager().getConfigFile().saveConfig();
        
        // 同时更新AiyatsbusAPI的debug状态
        cx.ajneb97.api.AiyatsbusAPI.setDebug(enabled);
        
        if (enabled) {
            plugin.getLogger().info("[DEBUG] Debug模式已启用");
        } else {
            plugin.getLogger().info("[DEBUG] Debug模式已关闭");
        }
    }
    
    /**
     * 输出debug信息到控制台
     * @param message 调试消息
     */
    public void debug(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * 输出附魔相关的debug信息
     * @param message 调试消息
     */
    public void debugEnchantment(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[ENCHANT-DEBUG] " + message);
        }
    }
    
    /**
     * 输出钓鱼相关的debug信息
     * @param message 调试消息
     */
    public void debugFishing(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[FISHING-DEBUG] " + message);
        }
    }
    
    /**
     * 输出API相关的debug信息
     * @param message 调试消息
     */
    public void debugAPI(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[API-DEBUG] " + message);
        }
    }
    
    /**
     * 输出配置相关的debug信息
     * @param message 调试消息
     */
    public void debugConfig(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[CONFIG-DEBUG] " + message);
        }
    }
    
    /**
     * 重新加载debug配置
     */
    public void reload() {
        loadDebugState();
    }
}
