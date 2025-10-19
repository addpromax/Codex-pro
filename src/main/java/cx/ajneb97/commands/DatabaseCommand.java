package cx.ajneb97.commands;

import cx.ajneb97.Codex;
import cx.ajneb97.database.*;
import cx.ajneb97.managers.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 处理数据库相关命令
 */
public class DatabaseCommand {
    private Codex plugin;
    
    public DatabaseCommand(Codex plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 处理数据库命令
     * @param sender 命令发送者
     * @param args 命令参数
     * @return 是否处理成功
     */
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "info":
                return handleInfo(sender);
            case "migrate":
                return handleMigrate(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }
    
    /**
     * 显示数据库信息
     */
    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== 数据库信息 ===");
        sender.sendMessage(ChatColor.GREEN + "当前存储类型: " + plugin.getConfigsManager().getMainConfigManager().getStorageType());
        
        // 显示玩家数量
        int playerCount = plugin.getPlayerDataManager().getPlayers().size();
        sender.sendMessage(ChatColor.GREEN + "已加载玩家数据: " + playerCount + " 名玩家");
        
        return true;
    }
    
    /**
     * 处理数据迁移命令
     */
    private boolean handleMigrate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /codex db migrate <目标类型>");
            sender.sendMessage(ChatColor.RED + "可用类型: file, h2, mysql");
            return true;
        }
        
        String currentType = plugin.getConfigsManager().getMainConfigManager().getStorageType();
        String targetType = args[2].toLowerCase();
        
        // 检查目标类型是否有效
        if (!targetType.equals("file") && !targetType.equals("h2") && !targetType.equals("mysql")) {
            sender.sendMessage(ChatColor.RED + "无效的存储类型! 可用类型: file, h2, mysql");
            return true;
        }
        
        // 检查是否与当前类型相同
        if (targetType.equals(currentType)) {
            sender.sendMessage(ChatColor.RED + "目标存储类型与当前类型相同，无需迁移!");
            return true;
        }
        
        // 如果需要迁移到MySQL，确保配置正确
        if (targetType.equals("mysql") && !plugin.getConfigsManager().getMainConfigManager().getConfig().getBoolean("mysql_database.enabled")) {
            sender.sendMessage(ChatColor.RED + "MySQL未配置或未启用! 请先在config.yml中配置MySQL数据库。");
            return true;
        }
        
        // 创建目标数据库管理器
        DatabaseManager targetDb;
        switch (targetType) {
            case "file":
                targetDb = new FileStorageManager(plugin);
                break;
            case "h2":
                targetDb = new H2DatabaseManager(plugin);
                break;
            case "mysql":
                targetDb = new MySQLDatabaseManager(plugin);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "无法创建目标数据库管理器!");
                return true;
        }
        
        // 初始化目标数据库
        if (!targetDb.initialize()) {
            sender.sendMessage(ChatColor.RED + "无法初始化目标数据库，迁移失败!");
            return true;
        }
        
        // 执行数据迁移
        sender.sendMessage(ChatColor.YELLOW + "正在开始数据迁移，请不要关闭服务器...");
        
        // 创建迁移管理器并执行迁移
        DatabaseMigrationManager migrationManager = new DatabaseMigrationManager(plugin);
        migrationManager.migrateData(plugin.getDatabaseManager(), targetDb, (migratedCount) -> {
            // 迁移完成后更新配置
            plugin.getConfigsManager().getMainConfigManager().getConfig().set("storage_type", targetType);
            if (targetType.equals("mysql")) {
                plugin.getConfigsManager().getMainConfigManager().getConfig().set("mysql_database.enabled", true);
            }
            plugin.getConfigsManager().getMainConfigManager().getConfigFile().saveConfig();
            
            // 通知完成
            sender.sendMessage(ChatColor.GREEN + "数据迁移完成! 成功迁移 " + migratedCount + " 个玩家数据。");
            sender.sendMessage(ChatColor.GREEN + "请重启服务器以应用新的存储类型。");
            
            // 关闭目标数据库连接
            targetDb.shutdown();
        });
        
        return true;
    }
    
    /**
     * 发送命令使用说明
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== 数据库命令 ===");
        sender.sendMessage(ChatColor.GOLD + "/codex db info " + ChatColor.WHITE + "- 显示当前数据库信息");
        sender.sendMessage(ChatColor.GOLD + "/codex db migrate <类型> " + ChatColor.WHITE + "- 迁移数据到指定类型的数据库");
        sender.sendMessage(ChatColor.GRAY + "可用类型: file, h2, mysql");
    }
} 