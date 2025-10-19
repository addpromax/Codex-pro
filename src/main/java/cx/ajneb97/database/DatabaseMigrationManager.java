package cx.ajneb97.database;

import cx.ajneb97.Codex;
import cx.ajneb97.model.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据库迁移管理器，处理不同数据库类型之间的数据迁移
 */
public class DatabaseMigrationManager {
    
    private Codex plugin;
    
    public DatabaseMigrationManager(Codex plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 执行数据迁移
     * @param source 源数据库管理器
     * @param target 目标数据库管理器
     * @param callback 完成回调，参数为成功迁移的玩家数据数量
     */
    public void migrateData(DatabaseManager source, DatabaseManager target, MigrationCallback callback) {
        plugin.getLogger().info("开始数据迁移...");
        Bukkit.getConsoleSender().sendMessage(plugin.prefix + "正在迁移数据，请勿关闭服务器...");
        
        // 获取所有玩家数据
        Map<UUID, PlayerData> players = plugin.getPlayerDataManager().getPlayers();
        final int totalPlayers = players.size();
        
        if (totalPlayers == 0) {
            plugin.getLogger().info("没有玩家数据需要迁移");
            if (callback != null) {
                callback.onComplete(0);
            }
            return;
        }
        
        plugin.getLogger().info("开始迁移 " + totalPlayers + " 个玩家数据");
        
        // 使用并发集合来跟踪进度
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 为每个玩家创建迁移任务
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            PlayerData playerData = entry.getValue();
            
            // 使用异步任务执行迁移，避免阻塞主线程
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // 更新玩家数据到目标数据库
                        target.updatePlayer(playerData);
                        successCount.incrementAndGet();
                        
                        // 记录日志
                        if (playerData.getName() != null) {
                            plugin.getLogger().info("成功迁移玩家数据: " + playerData.getName());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("迁移玩家数据时出错: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        // 更新完成计数
                        int completed = completedCount.incrementAndGet();
                        
                        // 如果所有任务都完成，调用回调
                        if (completed == totalPlayers) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    int total = successCount.get();
                                    plugin.getLogger().info("数据迁移完成，成功迁移 " + total + "/" + totalPlayers + " 个玩家数据");
                                    Bukkit.getConsoleSender().sendMessage(plugin.prefix + "数据迁移完成，成功迁移 " + total + "/" + totalPlayers + " 个玩家数据");
                                    
                                    if (callback != null) {
                                        callback.onComplete(total);
                                    }
                                }
                            }.runTask(plugin);
                        }
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }
    
    /**
     * 迁移回调接口
     */
    public interface MigrationCallback {
        void onComplete(int migratedCount);
    }
} 