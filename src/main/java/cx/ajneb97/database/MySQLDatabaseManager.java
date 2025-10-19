package cx.ajneb97.database;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.MessagesManager;
import cx.ajneb97.model.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MySQL数据库管理器实现
 */
public class MySQLDatabaseManager extends AbstractDatabaseManager {

    private HikariConnection connection;

    public MySQLDatabaseManager(Codex plugin) {
        super(plugin);
        this.logPrefix = "[Codex-MySQL] ";
    }

    @Override
    public boolean initialize() {
        try {
            connection = new HikariConnection(plugin.getConfigsManager().getMainConfigManager().getConfig());
            connection.getHikari().getConnection();
            createTables();
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix + "&a成功连接到MySQL数据库。"));
            return true;
        } catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix + "&c连接MySQL数据库时出错。"));
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Connection getConnection() {
        try {
            return connection.getHikari().getConnection();
        } catch (Exception e) {
            plugin.getLogger().severe(logPrefix + "获取数据库连接失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected String getDatabaseType() {
        return "mysql";
    }

    /**
     * 创建必要的数据库表
     */
    private void createTables() {
        try(Connection connection = getConnection()) {
            // 创建玩家表
            PreparedStatement statement1 = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS codex_players" +
                    " (UUID varchar(36) NOT NULL, " +
                    " PLAYER_NAME varchar(50), " +
                    " PRIMARY KEY (UUID))" +
                    " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            statement1.executeUpdate();
            
            // 创建分类表
            PreparedStatement categoryStatement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS codex_categories" +
                " (ID int NOT NULL AUTO_INCREMENT, " +
                " CATEGORY_NAME varchar(100) NOT NULL, " +
                " PRIMARY KEY (ID), " +
                " UNIQUE KEY (CATEGORY_NAME))" +
                " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            categoryStatement.executeUpdate();
            
            // 创建发现项表
            PreparedStatement discoveryStatement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS codex_discoveries" +
                " (ID int NOT NULL AUTO_INCREMENT, " +
                " CATEGORY_NAME varchar(100) NOT NULL, " +
                " DISCOVERY_NAME varchar(100) NOT NULL, " +
                " PRIMARY KEY (ID), " +
                " UNIQUE KEY (CATEGORY_NAME, DISCOVERY_NAME))" +
                " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            discoveryStatement.executeUpdate();
            
            // 创建玩家发现项表
            PreparedStatement statement2 = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS codex_players_discoveries" +
                    " (ID int NOT NULL AUTO_INCREMENT, " +
                    " UUID varchar(36) NOT NULL, " +
                    " CATEGORY varchar(100) NOT NULL, " +
                    " DISCOVERY varchar(100) NOT NULL, " +
                    " DATE varchar(100), " +
                    " MILLIS_ACTIONS_EXECUTED bigint, " +
                    " PRIMARY KEY (ID), " +
                    " FOREIGN KEY (UUID) REFERENCES codex_players(UUID) ON DELETE CASCADE)" +
                    " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            statement2.executeUpdate();
            
            // 创建玩家完成分类表
            PreparedStatement statement3 = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS codex_players_completed_categories" +
                    " (ID int NOT NULL AUTO_INCREMENT, " +
                    " UUID varchar(36) NOT NULL, " +
                    " CATEGORY varchar(100) NOT NULL, " +
                    " PRIMARY KEY (ID), " +
                    " FOREIGN KEY (UUID) REFERENCES codex_players(UUID) ON DELETE CASCADE)" +
                    " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            statement3.executeUpdate();

            // 创建索引以提高查询性能
            try {
                PreparedStatement indexStatement1 = connection.prepareStatement(
                        "CREATE INDEX IF NOT EXISTS idx_discoveries_uuid ON codex_players_discoveries(UUID)");
                indexStatement1.executeUpdate();
            } catch (SQLException e) {
                // MySQL可能不支持IF NOT EXISTS创建索引
                try {
                    PreparedStatement indexStatement = connection.prepareStatement(
                            "CREATE INDEX idx_discoveries_uuid ON codex_players_discoveries(UUID)");
                    indexStatement.executeUpdate();
                } catch (SQLException e2) {
                    // 索引可能已经存在
                    if (!e2.getMessage().contains("Duplicate key name") && !e2.getMessage().contains("already exists")) {
                        plugin.getLogger().warning(logPrefix + "创建索引idx_discoveries_uuid时出错: " + e2.getMessage());
                    }
                }
            }

            // 其他索引
            createMySQLIndexIfNotExists(connection, "idx_discoveries_category_discovery", "codex_players_discoveries(CATEGORY, DISCOVERY)");
            createMySQLIndexIfNotExists(connection, "idx_completed_uuid", "codex_players_completed_categories(UUID)");
            createMySQLIndexIfNotExists(connection, "idx_completed_category", "codex_players_completed_categories(CATEGORY)");
            
        } catch (SQLException e) {
            plugin.getLogger().severe(logPrefix + "创建表时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * MySQL专用，创建索引如果不存在（MySQL不支持原生的IF NOT EXISTS创建索引）
     */
    private void createMySQLIndexIfNotExists(Connection connection, String indexName, String indexDef) {
        try {
            PreparedStatement indexStatement = connection.prepareStatement(
                    "CREATE INDEX " + indexName + " ON " + indexDef);
            indexStatement.executeUpdate();
        } catch (SQLException e) {
            // 索引可能已经存在
            if (!e.getMessage().contains("Duplicate key name") && !e.getMessage().contains("already exists")) {
                plugin.getLogger().warning(logPrefix + "创建索引" + indexName + "时出错: " + e.getMessage());
            }
        }
    }

    @Override
    public void createPlayer(PlayerData player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO codex_players (UUID, PLAYER_NAME) VALUES (?,?)");
                    statement.setString(1, player.getUuid().toString());
                    statement.setString(2, player.getName());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe(logPrefix + "创建玩家数据时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public void updatePlayerName(PlayerData player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE codex_players SET PLAYER_NAME = ? WHERE UUID = ?");
                    statement.setString(1, player.getName());
                    statement.setString(2, player.getUuid().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe(logPrefix + "更新玩家名称时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public void addDiscovery(String uuid, String categoryName, String discoveryName, String discoveryDate) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO codex_players_discoveries (UUID, CATEGORY, DISCOVERY, DATE) VALUES (?,?,?,?)");
                    statement.setString(1, uuid);
                    statement.setString(2, categoryName);
                    statement.setString(3, discoveryName);
                    statement.setString(4, discoveryDate);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe(logPrefix + "添加发现项时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public void updateMillisActionsExecuted(String uuid, String categoryName, String discoveryName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE codex_players_discoveries SET MILLIS_ACTIONS_EXECUTED = MILLIS_ACTIONS_EXECUTED + 1 " +
                                    "WHERE UUID = ? AND CATEGORY = ? AND DISCOVERY = ?");
                    statement.setString(1, uuid);
                    statement.setString(2, categoryName);
                    statement.setString(3, discoveryName);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe(logPrefix + "更新动作执行计数时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public void updateCompletedCategories(String uuid, String categoryName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO codex_players_completed_categories (UUID, CATEGORY) VALUES (?, ?)");
                    statement.setString(1, uuid);
                    statement.setString(2, categoryName);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe(logPrefix + "更新完成分类时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public void resetDataPlayer(String uuid, String categoryName, String discoveryName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    if(categoryName == null && discoveryName == null) {
                        // 删除所有玩家数据
                        PreparedStatement statement1 = connection.prepareStatement(
                                "DELETE FROM codex_players_discoveries WHERE UUID = ?");
                        statement1.setString(1, uuid);
                        statement1.executeUpdate();
                        
                        PreparedStatement statement2 = connection.prepareStatement(
                                "DELETE FROM codex_players_completed_categories WHERE UUID = ?");
                        statement2.setString(1, uuid);
                        statement2.executeUpdate();
                    } else if(categoryName != null && discoveryName == null) {
                        // 删除某个分类的所有数据
                        PreparedStatement statement1 = connection.prepareStatement(
                                "DELETE FROM codex_players_discoveries WHERE UUID = ? AND CATEGORY = ?");
                        statement1.setString(1, uuid);
                        statement1.setString(2, categoryName);
                        statement1.executeUpdate();
                        
                        PreparedStatement statement2 = connection.prepareStatement(
                                "DELETE FROM codex_players_completed_categories WHERE UUID = ? AND CATEGORY = ?");
                        statement2.setString(1, uuid);
                        statement2.setString(2, categoryName);
                        statement2.executeUpdate();
                    } else {
                        // 删除某个特定发现项
                        PreparedStatement statement = connection.prepareStatement(
                                "DELETE FROM codex_players_discoveries WHERE UUID = ? AND CATEGORY = ? AND DISCOVERY = ?");
                        statement.setString(1, uuid);
                        statement.setString(2, categoryName);
                        statement.setString(3, discoveryName);
                        statement.executeUpdate();
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe(logPrefix + "重置玩家数据时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public void updatePlayer(PlayerData playerData) {
        // 如果是新玩家，先创建玩家记录
        try(Connection connection = getConnection()) {
            PreparedStatement checkStatement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM codex_players WHERE UUID = ?");
            checkStatement.setString(1, playerData.getUuid().toString());
            ResultSet result = checkStatement.executeQuery();
            
            if(result.next() && result.getInt(1) == 0) {
                PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO codex_players (UUID, PLAYER_NAME) VALUES (?, ?)");
                insertStatement.setString(1, playerData.getUuid().toString());
                insertStatement.setString(2, playerData.getName());
                insertStatement.executeUpdate();
            } else {
                // 更新玩家名称
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE codex_players SET PLAYER_NAME = ? WHERE UUID = ?");
                updateStatement.setString(1, playerData.getName());
                updateStatement.setString(2, playerData.getUuid().toString());
                updateStatement.executeUpdate();
            }
            
            // 清除旧数据
            PreparedStatement clearDiscoveries = connection.prepareStatement(
                    "DELETE FROM codex_players_discoveries WHERE UUID = ?");
            clearDiscoveries.setString(1, playerData.getUuid().toString());
            clearDiscoveries.executeUpdate();
            
            PreparedStatement clearCategories = connection.prepareStatement(
                    "DELETE FROM codex_players_completed_categories WHERE UUID = ?");
            clearCategories.setString(1, playerData.getUuid().toString());
            clearCategories.executeUpdate();
            
            // 重新插入完成的分类
            for(cx.ajneb97.model.data.PlayerDataCategory category : playerData.getCategories()) {
                if(category.isCompleted()) {
                    PreparedStatement insertCategoryStatement = connection.prepareStatement(
                            "INSERT INTO codex_players_completed_categories (UUID, CATEGORY) VALUES (?, ?)");
                    insertCategoryStatement.setString(1, playerData.getUuid().toString());
                    insertCategoryStatement.setString(2, category.getName());
                    insertCategoryStatement.executeUpdate();
                }
                
                // 重新插入发现项
                for(cx.ajneb97.model.data.PlayerDataDiscovery discovery : category.getDiscoveries()) {
                    PreparedStatement insertDiscoveryStatement = connection.prepareStatement(
                            "INSERT INTO codex_players_discoveries (UUID, CATEGORY, DISCOVERY, DATE, MILLIS_ACTIONS_EXECUTED) VALUES (?, ?, ?, ?, ?)");
                    insertDiscoveryStatement.setString(1, playerData.getUuid().toString());
                    insertDiscoveryStatement.setString(2, category.getName());
                    insertDiscoveryStatement.setString(3, discovery.getDiscoveryName());
                    insertDiscoveryStatement.setString(4, discovery.getDiscoverDate());
                    insertDiscoveryStatement.setLong(5, discovery.getMillisActionsExecuted());
                    insertDiscoveryStatement.executeUpdate();
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe(logPrefix + "更新玩家数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if(connection != null) {
            connection.disable();
            plugin.getLogger().info(logPrefix + "MySQL数据库连接已关闭");
        }
    }
} 