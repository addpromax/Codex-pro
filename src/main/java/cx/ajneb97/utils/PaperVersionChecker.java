package cx.ajneb97.utils;

import org.bukkit.Bukkit;

/**
 * Paper版本检测和功能兼容性管理器
 * 负责检测不同Paper版本的功能支持情况
 */
public class PaperVersionChecker {
    
    private static Boolean isPaper = null;
    private static Boolean hasAdventureAPI = null;
    private static Boolean hasItemMetaComponent = null;
    private static Boolean hasCustomModelDataComponent = null;
    private static Boolean hasDisplayNameComponent = null;
    
    /**
     * 检测是否为Paper服务器
     */
    public static boolean isPaper() {
        if (isPaper == null) {
            isPaper = checkPaperSupport();
        }
        return isPaper;
    }
    
    /**
     * 检测Paper Adventure API支持
     */
    public static boolean hasAdventureAPI() {
        if (hasAdventureAPI == null) {
            hasAdventureAPI = isPaper() && checkAdventureAPI();
        }
        return hasAdventureAPI;
    }
    
    /**
     * 检测Paper ItemMeta Component支持
     */
    public static boolean hasItemMetaComponent() {
        if (hasItemMetaComponent == null) {
            hasItemMetaComponent = isPaper() && checkItemMetaComponent();
        }
        return hasItemMetaComponent;
    }
    
    /**
     * 检测CustomModelDataComponent支持 (Paper 1.21.4+)
     */
    public static boolean hasCustomModelDataComponent() {
        if (hasCustomModelDataComponent == null) {
            hasCustomModelDataComponent = isPaper() && checkCustomModelDataComponent();
        }
        return hasCustomModelDataComponent;
    }
    
    /**
     * 检测HasDisplayName支持 (Paper 1.20.5+)
     */
    public static boolean hasDisplayNameComponent() {
        if (hasDisplayNameComponent == null) {
            hasDisplayNameComponent = isPaper() && checkDisplayNameComponent();
        }
        return hasDisplayNameComponent;
    }
    
    /**
     * 重置所有缓存的检测结果 (用于重新加载时)
     */
    public static void resetCache() {
        isPaper = null;
        hasAdventureAPI = null;
        hasItemMetaComponent = null;
        hasCustomModelDataComponent = null;
        hasDisplayNameComponent = null;
    }
    
    /**
     * 获取完整的兼容性报告
     */
    public static String getCompatibilityReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Paper兼容性检测报告 ===\n");
        report.append("Paper服务器: ").append(isPaper() ? "✓ 支持" : "✗ 不支持").append("\n");
        report.append("Adventure API: ").append(hasAdventureAPI() ? "✓ 支持" : "✗ 不支持").append("\n");
        report.append("ItemMeta Component: ").append(hasItemMetaComponent() ? "✓ 支持" : "✗ 不支持").append("\n");
        report.append("HasDisplayName: ").append(hasDisplayNameComponent() ? "✓ 支持" : "✗ 不支持").append("\n");
        report.append("CustomModelDataComponent: ").append(hasCustomModelDataComponent() ? "✓ 支持" : "✗ 不支持").append("\n");
        report.append("==================");
        return report.toString();
    }
    
    // ========== 私有检测方法 ==========
    
    private static boolean checkPaperSupport() {
        try {
            // 检测新版Paper API
            Class.forName("io.papermc.paper.adventure.PaperAdventure");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                // 回退到旧版Paper检测
                Class.forName("com.destroystokyo.paper.ParticleBuilder");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }
    
    private static boolean checkAdventureAPI() {
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            Class.forName("net.kyori.adventure.text.Component");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static boolean checkItemMetaComponent() {
        try {
            Class.forName("io.papermc.paper.inventory.meta.HasDisplayName");
            Class.forName("io.papermc.paper.inventory.meta.HasLore");
            return true;
        } catch (ClassNotFoundException e) {
            // 在某些Paper版本中，这些类可能在不同的包中或有不同的名称
            // 尝试检测替代方法
            try {
                // 尝试检测ItemMeta的displayName方法（Paper特有）
                Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                itemMetaClass.getMethod("displayName");
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
    
    private static boolean checkCustomModelDataComponent() {
        try {
            Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static boolean checkDisplayNameComponent() {
        try {
            Class.forName("io.papermc.paper.inventory.meta.HasDisplayName");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 获取服务器信息字符串
     */
    public static String getServerInfo() {
        try {
            String version = Bukkit.getVersion();
            String serverName = Bukkit.getName();
            return String.format("%s - %s", serverName, version);
        } catch (Exception e) {
            return "Unknown Server";
        }
    }
    
    /**
     * 日志输出兼容性信息
     */
    public static void logCompatibilityInfo() {
        Bukkit.getLogger().info("[Codex] Paper版本兼容性检测:");
        Bukkit.getLogger().info("[Codex] 服务器: " + getServerInfo());
        Bukkit.getLogger().info("[Codex] Paper支持: " + (isPaper() ? "是" : "否"));
        Bukkit.getLogger().info("[Codex] Adventure API: " + (hasAdventureAPI() ? "可用" : "不可用"));
        Bukkit.getLogger().info("[Codex] ItemMeta Component: " + (hasItemMetaComponent() ? "可用" : "不可用"));
        Bukkit.getLogger().info("[Codex] CustomModelDataComponent: " + (hasCustomModelDataComponent() ? "可用" : "不可用"));
        
        // 额外调试信息
        logDebugInfo();
        
        if (!isPaper()) {
            Bukkit.getLogger().info("[Codex] 提示: 使用Paper服务器可获得更好的性能和功能支持");
        } else if (!hasItemMetaComponent()) {
            Bukkit.getLogger().info("[Codex] 提示: 当前Paper版本支持基础功能，部分现代化功能可能不可用");
        }
    }
    
    /**
     * 输出详细的调试信息
     */
    private static void logDebugInfo() {
        try {
            Bukkit.getLogger().info("[Codex] 调试信息:");
            
            // 检查 HasDisplayName 接口
            try {
                Class.forName("io.papermc.paper.inventory.meta.HasDisplayName");
                Bukkit.getLogger().info("[Codex] - HasDisplayName接口: 存在");
            } catch (ClassNotFoundException e) {
                Bukkit.getLogger().info("[Codex] - HasDisplayName接口: 不存在");
            }
            
            // 检查 ItemMeta.displayName() 方法
            try {
                Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                itemMetaClass.getMethod("displayName");
                Bukkit.getLogger().info("[Codex] - ItemMeta.displayName()方法: 存在");
            } catch (Exception e) {
                Bukkit.getLogger().info("[Codex] - ItemMeta.displayName()方法: 不存在");
            }
            
            // 检查 Component 类
            try {
                Class.forName("net.kyori.adventure.text.Component");
                Bukkit.getLogger().info("[Codex] - Adventure Component: 存在");
            } catch (ClassNotFoundException e) {
                Bukkit.getLogger().info("[Codex] - Adventure Component: 不存在");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Codex] 调试信息获取失败: " + e.getMessage());
        }
    }
}
