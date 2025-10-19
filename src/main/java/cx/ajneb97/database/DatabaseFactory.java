package cx.ajneb97.database;

import cx.ajneb97.Codex;
import cx.ajneb97.config.MainConfigManager;

/**
 * 数据库工厂类，负责根据配置创建适当的数据库管理器实例
 */
public class DatabaseFactory {
    
    /**
     * 根据配置创建数据库管理器
     * @param plugin 插件实例
     * @return 数据库管理器实例
     */
    public static DatabaseManager createDatabaseManager(Codex plugin) {
        MainConfigManager configManager = plugin.getConfigsManager().getMainConfigManager();
        String storageType = configManager.getStorageType();
        
        // 记录选择的存储类型
        plugin.getLogger().info("数据存储类型: " + storageType);
        
        // 根据配置创建对应的数据库管理器
        DatabaseManager databaseManager;
        
        if ("mysql".equalsIgnoreCase(storageType) || configManager.isMySQL()) {
            // MySQL数据库
            databaseManager = new MySQLDatabaseManager(plugin);
            plugin.getLogger().info("使用MySQL数据库存储");
        } else if ("h2".equalsIgnoreCase(storageType) || configManager.isH2()) {
            // H2数据库
            databaseManager = new H2DatabaseManager(plugin);
            plugin.getLogger().info("使用H2数据库存储 (高性能)");
        } else {
            // 文件存储
            databaseManager = new FileStorageManager(plugin);
            plugin.getLogger().info("使用文件存储 (兼容模式)");
        }
        
        // 初始化数据库
        if (!databaseManager.initialize()) {
            plugin.getLogger().severe("数据库初始化失败，将回退到文件存储模式");
            // 如果初始化失败，使用文件存储作为备选
            databaseManager = new FileStorageManager(plugin);
            databaseManager.initialize();
        }
        
        return databaseManager;
    }
} 