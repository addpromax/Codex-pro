package cx.ajneb97.database;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.MessagesManager;
import cx.ajneb97.model.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.*;

/**
 * H2数据库管理器实现
 */
public class H2DatabaseManager extends AbstractDatabaseManager {
    
    private H2Connection connection;

    public H2DatabaseManager(Codex plugin) {
        super(plugin);
        this.logPrefix = "[Codex-H2] ";
    }

    @Override
    public boolean initialize() {
        try {
            // 确保数据库目录存在
            File dbDir = new File(plugin.getDataFolder(), "database");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            connection = new H2Connection(plugin);
            connection.getHikari().getConnection();
            
            createTables();
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix + "&a成功连接到H2数据库。"));
            return true;
        } catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix + "&c连接H2数据库时出错。"));
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
        return "h2";
    }

    /**
     * 创建必要的数据库表 - 优化启动速度版本
     */
    private void createTables() {
        plugin.getLogger().info(logPrefix + "开始创建数据库表...");
        
        try(Connection connection = getConnection()) {
            // 批量执行SQL以提高性能
            Statement stmt = connection.createStatement();
            
            plugin.getLogger().info(logPrefix + "创建主表...");
            
            // 创建玩家表
            stmt.addBatch("CREATE TABLE IF NOT EXISTS codex_players" +
                    " (UUID varchar(36) NOT NULL, " +
                    " PLAYER_NAME varchar(50), " +
                    " PRIMARY KEY (UUID))");
            
            // 创建分类表 - 简化版本，移除不必要的约束
            stmt.addBatch("CREATE TABLE IF NOT EXISTS codex_categories" +
                " (ID int NOT NULL AUTO_INCREMENT, " +
                " CATEGORY_NAME varchar(100) NOT NULL, " +
                " PRIMARY KEY (ID))");
            
            // 创建发现项表 - 简化版本
            stmt.addBatch("CREATE TABLE IF NOT EXISTS codex_discoveries" +
                " (ID int NOT NULL AUTO_INCREMENT, " +
                " CATEGORY_NAME varchar(100) NOT NULL, " +
                " DISCOVERY_NAME varchar(100) NOT NULL, " +
                " PRIMARY KEY (ID))");
            
            // 创建玩家发现项表 - 移除外键约束以提高启动速度
            stmt.addBatch("CREATE TABLE IF NOT EXISTS codex_players_discoveries" +
                    " (ID int NOT NULL AUTO_INCREMENT, " +
                    " UUID varchar(36) NOT NULL, " +
                    " CATEGORY varchar(100) NOT NULL, " +
                    " DISCOVERY varchar(100) NOT NULL, " +
                    " DATE varchar(100), " +
                    " MILLIS_ACTIONS_EXECUTED bigint, " +
                    " PRIMARY KEY (ID))");
            
            // 创建玩家完成分类表 - 移除外键约束
            stmt.addBatch("CREATE TABLE IF NOT EXISTS codex_players_completed_categories" +
                    " (ID int NOT NULL AUTO_INCREMENT, " +
                    " UUID varchar(36) NOT NULL, " +
                    " CATEGORY varchar(100) NOT NULL, " +
                    " PRIMARY KEY (ID))");

            // 执行批量表创建
            stmt.executeBatch();
            plugin.getLogger().info(logPrefix + "主表创建完成");
            
            // 创建关键索引（仅创建最必要的索引）
            plugin.getLogger().info(logPrefix + "创建索引...");
            try {
                stmt.addBatch("CREATE INDEX IF NOT EXISTS idx_discoveries_uuid ON codex_players_discoveries(UUID)");
                stmt.addBatch("CREATE INDEX IF NOT EXISTS idx_completed_uuid ON codex_players_completed_categories(UUID)");
                
                stmt.executeBatch();
                plugin.getLogger().info(logPrefix + "索引创建完成");
            } catch (SQLException e) {
                // 索引创建失败不是致命错误
                plugin.getLogger().info(logPrefix + "索引创建时出现非致命错误，将继续运行: " + e.getMessage());
            }
            
            stmt.close();
            plugin.getLogger().info(logPrefix + "数据库表结构初始化完成");
            
        } catch (SQLException e) {
            plugin.getLogger().severe(logPrefix + "创建表时发生错误: " + e.getMessage());
            e.printStackTrace();
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
            plugin.getLogger().info(logPrefix + "H2数据库连接已关闭");
        }
    }
} 