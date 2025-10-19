package cx.ajneb97.database;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.MessagesManager;
import cx.ajneb97.model.data.PlayerData;
import cx.ajneb97.model.data.PlayerDataCategory;
import cx.ajneb97.model.data.PlayerDataDiscovery;
import cx.ajneb97.model.structure.Discovery;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 抽象数据库管理器，实现共有的数据库操作逻辑
 */
public abstract class AbstractDatabaseManager implements DatabaseManager {

    protected Codex plugin;
    protected String logPrefix;

    public AbstractDatabaseManager(Codex plugin) {
        this.plugin = plugin;
        this.logPrefix = "[Codex-DB] ";
    }

    /**
     * 加载所有玩家数据
     */
    @Override
    public void loadData() {
        Map<UUID, PlayerData> playerMap = new HashMap<>();
        
        try (Connection connection = getConnection()) {
            String sql = "SELECT codex_players.UUID, codex_players.PLAYER_NAME, " +
                    "(SELECT GROUP_CONCAT(cc.CATEGORY) FROM codex_players_completed_categories cc WHERE cc.UUID = codex_players.UUID) AS COMPLETED_CATEGORIES, " +
                    "codex_players_discoveries.CATEGORY AS DISCOVERY_CATEGORY, " +
                    "codex_players_discoveries.DISCOVERY, " +
                    "codex_players_discoveries.DATE, " +
                    "codex_players_discoveries.MILLIS_ACTIONS_EXECUTED " +
                    "FROM codex_players " +
                    "LEFT JOIN codex_players_discoveries ON codex_players.UUID = codex_players_discoveries.UUID";
            
            PreparedStatement statement = null;
            ResultSet result = null;
            
            try {
                statement = connection.prepareStatement(sql);
                result = statement.executeQuery();
            } catch (SQLException e) {
                // 兼容首次启动或表结构变化，降级为只查主表
                plugin.getLogger().warning(logPrefix + "loadData SQL降级: " + e.getMessage());
                sql = "SELECT UUID, PLAYER_NAME FROM codex_players";
                statement = connection.prepareStatement(sql);
                result = statement.executeQuery();
            }
            
            while (result.next()) {
                UUID uuid = UUID.fromString(result.getString("UUID"));
                String playerName = result.getString("PLAYER_NAME");
                
                String completedCategories = null;
                String discoveryCategoryName = null;
                String discoveryName = null;
                String discoveryDate = null;
                long discoveryMillisActionsExecuted = 0;
                
                try { completedCategories = result.getString("COMPLETED_CATEGORIES"); } catch(Exception ignore) {}
                try { discoveryCategoryName = result.getString("DISCOVERY_CATEGORY"); } catch(Exception ignore) {}
                try { discoveryName = result.getString("DISCOVERY"); } catch(Exception ignore) {}
                try { discoveryDate = result.getString("DATE"); } catch(Exception ignore) {}
                try { discoveryMillisActionsExecuted = result.getLong("MILLIS_ACTIONS_EXECUTED"); } catch(Exception ignore) {}

                PlayerData player = playerMap.get(uuid);
                if (player == null) {
                    // 创建并添加玩家数据
                    player = new PlayerData(uuid, playerName);
                    playerMap.put(uuid, player);
                }

                if (discoveryCategoryName != null) {
                    boolean hasCompletedCategory = completedCategories != null && completedCategories.contains(discoveryCategoryName);
                    PlayerDataCategory playerDataCategory = player.getCategory(discoveryCategoryName);
                    if (playerDataCategory == null) {
                        playerDataCategory = new PlayerDataCategory(discoveryCategoryName, hasCompletedCategory, new ArrayList<>());
                        player.getCategories().add(playerDataCategory);
                    }

                    playerDataCategory.getDiscoveries().add(new PlayerDataDiscovery(discoveryName, discoveryDate, discoveryMillisActionsExecuted));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(logPrefix + "加载数据时出错: " + e.getMessage());
            e.printStackTrace();
        }

        plugin.getPlayerDataManager().setPlayers(playerMap);
        plugin.getLogger().info(logPrefix + "成功加载了 " + playerMap.size() + " 名玩家的数据");
    }

    /**
     * 根据UUID异步获取玩家数据
     */
    @Override
    public void getPlayer(String uuid, PlayerCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerData player = null;
                try (Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT codex_players.UUID, codex_players.PLAYER_NAME, " +
                                    "(SELECT GROUP_CONCAT(cc.CATEGORY) FROM codex_players_completed_categories cc WHERE cc.UUID = codex_players.UUID) AS COMPLETED_CATEGORIES, " +
                                    "codex_players_discoveries.CATEGORY AS DISCOVERY_CATEGORY, " +
                                    "codex_players_discoveries.DISCOVERY, " +
                                    "codex_players_discoveries.DATE, " +
                                    "codex_players_discoveries.MILLIS_ACTIONS_EXECUTED " +
                                    "FROM codex_players " +
                                    "LEFT JOIN codex_players_discoveries ON codex_players.UUID = codex_players_discoveries.UUID " +
                                    "WHERE codex_players.UUID = ?");

                    statement.setString(1, uuid);
                    ResultSet result = statement.executeQuery();

                    while (result.next()) {
                        UUID playerUuid = UUID.fromString(result.getString("UUID"));
                        String playerName = result.getString("PLAYER_NAME");
                        String completedCategories = result.getString("COMPLETED_CATEGORIES");
                        String discoveryCategoryName = result.getString("DISCOVERY_CATEGORY");
                        String discoveryName = result.getString("DISCOVERY");
                        String discoveryDate = result.getString("DATE");
                        long discoveryMillisActionsExecuted = result.getLong("MILLIS_ACTIONS_EXECUTED");

                        if (player == null) {
                            player = new PlayerData(playerUuid, playerName);
                        }

                        if (discoveryCategoryName != null) {
                            boolean hasCompletedCategory = completedCategories != null && completedCategories.contains(discoveryCategoryName);
                            PlayerDataCategory playerDataCategory = player.getCategory(discoveryCategoryName);
                            if (playerDataCategory == null) {
                                playerDataCategory = new PlayerDataCategory(discoveryCategoryName, hasCompletedCategory, new ArrayList<>());
                                player.getCategories().add(playerDataCategory);
                            }

                            playerDataCategory.getDiscoveries().add(new PlayerDataDiscovery(discoveryName, discoveryDate, discoveryMillisActionsExecuted));
                        }
                    }

                    PlayerData finalPlayer = player;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.onDone(finalPlayer);
                        }
                    }.runTask(plugin);
                } catch (SQLException e) {
                    plugin.getLogger().severe(logPrefix + "获取玩家数据时出错: " + e.getMessage());
                    e.printStackTrace();
                    
                    // 确保回调被调用，即使发生错误
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.onDone(null);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * 执行SQL更新操作的通用方法
     * @param sql SQL语句
     * @param params SQL参数
     * @return 是否成功执行
     */
    protected boolean executeUpdate(String sql, Object... params) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe(logPrefix + "执行SQL更新时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 同步分类和发现项到数据库
     */
    @Override
    public void syncCategoriesAndDiscoveries() {
        try {
            plugin.getLogger().info(logPrefix + "开始同步分类和发现项到数据库...");
            int totalCategories = 0;
            int totalDiscoveries = 0;
            
            // 获取所有分类
            for (cx.ajneb97.model.structure.Category category : plugin.getCategoryManager().getCategories()) {
                String categoryName = category.getName();
                totalCategories++;
                
                // 插入分类
                try (Connection connection = getConnection()) {
                    try {
                        String sql = getDatabaseType().equals("mysql") ?
                                "INSERT IGNORE INTO codex_categories (CATEGORY_NAME) VALUES (?)" :
                                "MERGE INTO codex_categories (CATEGORY_NAME) KEY(CATEGORY_NAME) VALUES (?)";
                        
                        PreparedStatement insertCategoryStmt = connection.prepareStatement(sql);
                        insertCategoryStmt.setString(1, categoryName);
                        insertCategoryStmt.executeUpdate();
                        
                        // 插入所有发现项
                        for (Discovery discovery : category.getDiscoveries()) {
                            totalDiscoveries++;
                            String discoveryId = discovery.getId();
                            
                            String sql2 = getDatabaseType().equals("mysql") ?
                                    "INSERT IGNORE INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)" :
                                    "MERGE INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) KEY(CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)";
                            
                            PreparedStatement insertDiscoveryStmt = connection.prepareStatement(sql2);
                            insertDiscoveryStmt.setString(1, categoryName);
                            insertDiscoveryStmt.setString(2, discoveryId);
                            insertDiscoveryStmt.executeUpdate();
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().warning(logPrefix + "同步分类/发现项时出错: " + e.getMessage());
                    }
                }
            }
            plugin.getLogger().info(logPrefix + "已同步 " + totalCategories + " 个分类和 " + totalDiscoveries + " 个发现项到数据库");
        } catch (Exception e) {
            plugin.getLogger().severe(logPrefix + "同步分类和发现项时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取数据库类型，用于特定SQL语法的选择
     * @return 数据库类型 "mysql" 或 "h2"
     */
    protected abstract String getDatabaseType();
} 