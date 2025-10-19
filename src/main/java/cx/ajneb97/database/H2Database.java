package cx.ajneb97.database;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.MessagesManager;
import cx.ajneb97.model.data.PlayerData;
import cx.ajneb97.model.data.PlayerDataCategory;
import cx.ajneb97.model.data.PlayerDataDiscovery;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class H2Database {

    private Codex plugin;
    private H2Connection connection;

    public H2Database(Codex plugin) {
        this.plugin = plugin;
    }

    public void setupH2() {
        try {
            connection = new H2Connection(plugin);
            connection.getHikari().getConnection();
            createTables();
            loadData();
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix+"&a成功连接到H2数据库。"));
        } catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix+"&c连接H2数据库时出错。"));
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            return connection.getHikari().getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void loadData() {
        Map<UUID, PlayerData> playerMap = new HashMap<>();
        try(Connection connection = getConnection()) {
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
                plugin.getLogger().warning("[Codex-H2] loadData SQL降级: " + e.getMessage());
                sql = "SELECT UUID, PLAYER_NAME FROM codex_players";
                statement = connection.prepareStatement(sql);
                result = statement.executeQuery();
            }
            while(result.next()) {
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
                if(player == null) {
                    //Create and add it
                    player = new PlayerData(uuid, playerName);
                    playerMap.put(uuid, player);
                }

                if(discoveryCategoryName != null) {
                    boolean hasCompletedCategory = completedCategories != null && completedCategories.contains(discoveryCategoryName);
                    PlayerDataCategory playerDataCategory = player.getCategory(discoveryCategoryName);
                    if(playerDataCategory == null) {
                        playerDataCategory = new PlayerDataCategory(discoveryCategoryName, hasCompletedCategory, new ArrayList<>());
                        player.getCategories().add(playerDataCategory);
                    }

                    playerDataCategory.getDiscoveries().add(new PlayerDataDiscovery(discoveryName, discoveryDate, discoveryMillisActionsExecuted));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        plugin.getPlayerDataManager().setPlayers(playerMap);
    }

    public void createTables() {
        try(Connection connection = getConnection()) {
            // 创建玩家表
            PreparedStatement statement1 = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS codex_players" +
                    " (UUID varchar(36) NOT NULL, " +
                    " PLAYER_NAME varchar(50), " +
                    " PRIMARY KEY ( UUID ))"
            );
            statement1.executeUpdate();
            
            // 创建分类表 - 使用CASCADE以便分类被删除时自动清理相关记录
            PreparedStatement categoryStatement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS codex_categories" +
                " (ID int NOT NULL AUTO_INCREMENT, " +
                " CATEGORY_NAME varchar(100) NOT NULL, " +
                " PRIMARY KEY (ID), " +
                " UNIQUE KEY (CATEGORY_NAME))"
            );
            categoryStatement.executeUpdate();
            
            // 创建发现项表 - 使用CASCADE以便分类被删除时自动清理相关记录
            PreparedStatement discoveryStatement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS codex_discoveries" +
                " (ID int NOT NULL AUTO_INCREMENT, " +
                " CATEGORY_NAME varchar(100) NOT NULL, " +
                " DISCOVERY_NAME varchar(100) NOT NULL, " +
                " PRIMARY KEY (ID), " +
                " UNIQUE KEY (CATEGORY_NAME, DISCOVERY_NAME), " +
                " FOREIGN KEY (CATEGORY_NAME) REFERENCES codex_categories(CATEGORY_NAME) ON DELETE CASCADE)"
            );
            discoveryStatement.executeUpdate();
            
            // 创建玩家发现项表 - 不再对(CATEGORY, DISCOVERY)添加外键约束
            // 这样可以避免在添加玩家发现项时必须先添加到codex_discoveries表
            PreparedStatement statement2 = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS codex_players_discoveries" +
                    " (ID int NOT NULL AUTO_INCREMENT, " +
                    " UUID varchar(36) NOT NULL, " +
                    " CATEGORY varchar(100) NOT NULL, " +
                    " DISCOVERY varchar(100) NOT NULL, " +
                    " DATE varchar(100), " +
                    " MILLIS_ACTIONS_EXECUTED bigint, " +
                    " PRIMARY KEY ( ID ), " +
                    " FOREIGN KEY (UUID) REFERENCES codex_players(UUID) ON DELETE CASCADE)"
            );
            statement2.executeUpdate();
            
            // 创建玩家完成分类表
            PreparedStatement statement3 = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS codex_players_completed_categories" +
                            " (ID int NOT NULL AUTO_INCREMENT, " +
                            " UUID varchar(36) NOT NULL, " +
                            " CATEGORY varchar(100) NOT NULL, " +
                            " PRIMARY KEY ( ID ), " +
                            " FOREIGN KEY (UUID) REFERENCES codex_players(UUID) ON DELETE CASCADE)"
            );
            statement3.executeUpdate();

            // 创建索引以提高查询性能
            try {
                PreparedStatement indexStatement1 = connection.prepareStatement(
                        "CREATE INDEX IF NOT EXISTS idx_discoveries_uuid ON codex_players_discoveries(UUID)");
                indexStatement1.executeUpdate();
                
                PreparedStatement indexStatement2 = connection.prepareStatement(
                        "CREATE INDEX IF NOT EXISTS idx_discoveries_category_discovery ON codex_players_discoveries(CATEGORY, DISCOVERY)");
                indexStatement2.executeUpdate();
                
                PreparedStatement indexStatement3 = connection.prepareStatement(
                        "CREATE INDEX IF NOT EXISTS idx_completed_uuid ON codex_players_completed_categories(UUID)");
                indexStatement3.executeUpdate();
                
                PreparedStatement indexStatement4 = connection.prepareStatement(
                        "CREATE INDEX IF NOT EXISTS idx_completed_category ON codex_players_completed_categories(CATEGORY)");
                indexStatement4.executeUpdate();
            } catch (SQLException e) {
                // 索引可能已存在，忽略错误
                plugin.getLogger().info("创建索引时出现非致命错误：" + e.getMessage());
            }
            
            // 尝试删除可能存在的错误约束（如果存在）
            try {
                // 检查并移除任何隐式约束
                Statement checkStmt = connection.createStatement();
                ResultSet constraints = checkStmt.executeQuery(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS " +
                    "WHERE TABLE_NAME='CODEX_PLAYERS_DISCOVERIES' AND CONSTRAINT_TYPE='REFERENTIAL'");
                
                while(constraints.next()) {
                    String constraintName = constraints.getString("CONSTRAINT_NAME");
                    // 只删除非主动创建的外键约束
                    if (!constraintName.equalsIgnoreCase("CONSTRAINT_7")) { // UUID外键约束
                        try {
                            Statement dropConstraint = connection.createStatement();
                            dropConstraint.execute("ALTER TABLE codex_players_discoveries DROP CONSTRAINT " + constraintName);
                            plugin.getLogger().info("已删除可能导致问题的约束: " + constraintName);
                        } catch (SQLException e) {
                            plugin.getLogger().warning("删除约束失败: " + e.getMessage());
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("检查约束时出错: " + e.getMessage());
            }
            
            // 同步所有现有分类和发现项到数据库
            syncCategoriesAndDiscoveries();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("创建表时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 将所有现有分类和发现项同步到数据库中
     */
    public void syncCategoriesAndDiscoveries() {
        try {
            plugin.getLogger().info("开始同步分类和发现项到数据库...");
            int totalCategories = 0;
            int totalDiscoveries = 0;
            
            // 获取所有分类
            for (cx.ajneb97.model.structure.Category category : plugin.getCategoryManager().getCategories()) {
                String categoryName = category.getName();
                totalCategories++;
                
                // 插入分类
                try (Connection connection = getConnection()) {
                    try {
                        PreparedStatement insertCategoryStmt = connection.prepareStatement(
                            "MERGE INTO codex_categories (CATEGORY_NAME) KEY(CATEGORY_NAME) VALUES (?)");
                        insertCategoryStmt.setString(1, categoryName);
                        insertCategoryStmt.executeUpdate();
                        
                        plugin.getLogger().info("已将分类 " + categoryName + " 同步到数据库");
                    } catch (SQLException e) {
                        plugin.getLogger().warning("同步分类 " + categoryName + " 时出错: " + e.getMessage());
                        // 尝试使用标准INSERT语句
                        try {
                            PreparedStatement insertStmt = connection.prepareStatement(
                                "INSERT INTO codex_categories (CATEGORY_NAME) VALUES (?)");
                            insertStmt.setString(1, categoryName);
                            insertStmt.executeUpdate();
                            plugin.getLogger().info("已将分类 " + categoryName + " 同步到数据库 (备用方法)");
                        } catch (SQLException e2) {
                            if (!e2.getMessage().contains("unique constraint") && 
                                !e2.getMessage().contains("Duplicate entry")) {
                                plugin.getLogger().warning("同步分类失败 (备用方法): " + e2.getMessage());
                            }
                        }
                    }
                    
                    // 插入该分类的所有发现项
                    for (cx.ajneb97.model.structure.Discovery discovery : category.getDiscoveries()) {
                        String discoveryId = discovery.getId();
                        totalDiscoveries++;
                        
                        plugin.getLogger().info("正在同步发现项: " + discoveryId + " (分类: " + categoryName + ")");
                        
                        try {
                            // 使用MERGE语句替代INSERT以避免唯一性冲突
                            PreparedStatement insertDiscoveryStmt = connection.prepareStatement(
                                "MERGE INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) KEY(CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)");
                            insertDiscoveryStmt.setString(1, categoryName);
                            insertDiscoveryStmt.setString(2, discoveryId);
                            insertDiscoveryStmt.executeUpdate();
                            
                            plugin.getLogger().info("成功添加发现项: " + discoveryId + " (分类: " + categoryName + ")");
                        } catch (SQLException e) {
                            // MERGE语句失败时，尝试使用标准INSERT语句
                            try {
                                PreparedStatement insertStmt = connection.prepareStatement(
                                    "INSERT INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)");
                                insertStmt.setString(1, categoryName);
                                insertStmt.setString(2, discoveryId);
                                insertStmt.executeUpdate();
                                plugin.getLogger().info("成功添加发现项: " + discoveryId + " (分类: " + categoryName + ") (备用方法)");
                            } catch (SQLException e2) {
                                // 忽略唯一约束冲突，说明发现项已存在
                                if (!e2.getMessage().contains("unique constraint") && 
                                    !e2.getMessage().contains("Duplicate entry")) {
                                    plugin.getLogger().warning("同步发现项 " + discoveryId + " (分类: " + categoryName + ") 时出错: " + e2.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            
            // 同步完成后，检查并添加附魔发现项
            syncEnchantmentDiscoveries();
            
            plugin.getLogger().info("分类和发现项数据库同步完成 - 共处理 " + totalCategories + " 个分类和 " + totalDiscoveries + " 个发现项");
        } catch (Exception e) {
            plugin.getLogger().severe("同步分类和发现项时出现异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 专门同步附魔发现项到数据库
     * 这是为了确保即使enchantments分类中添加了新附魔，也能正确同步
     */
    private void syncEnchantmentDiscoveries() {
        try {
            if (plugin.getEnchantmentManager() == null || 
                plugin.getConfigsManager().getEnchantmentsConfigManager() == null ||
                !plugin.getConfigsManager().getEnchantmentsConfigManager().isEnabled()) {
                return;
            }
            
            String categoryName = plugin.getConfigsManager().getEnchantmentsConfigManager().getCategoryName();
            plugin.getLogger().info("开始同步附魔发现项到数据库，分类: " + categoryName);
            
            // 确保分类存在
            try (Connection connection = getConnection()) {
                PreparedStatement insertCategoryStmt = connection.prepareStatement(
                    "MERGE INTO codex_categories (CATEGORY_NAME) KEY(CATEGORY_NAME) VALUES (?)");
                insertCategoryStmt.setString(1, categoryName);
                insertCategoryStmt.executeUpdate();
            } catch (SQLException e) {
                // 尝试使用INSERT
                try (Connection connection = getConnection()) {
                    PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO codex_categories (CATEGORY_NAME) VALUES (?)");
                    insertStmt.setString(1, categoryName);
                    insertStmt.executeUpdate();
                } catch (SQLException e2) {
                    // 忽略唯一性冲突
                }
            }
            
            // 获取服务器上所有已注册的附魔
            List<String> enchantments = plugin.getEnchantmentManager().getAllRegisteredEnchantments();
            int count = 0;
            
            for (String enchantId : enchantments) {
                try (Connection connection = getConnection()) {
                    PreparedStatement stmt = connection.prepareStatement(
                        "MERGE INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) KEY(CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)");
                    stmt.setString(1, categoryName);
                    stmt.setString(2, enchantId);
                    stmt.executeUpdate();
                    count++;
                } catch (SQLException e) {
                    try (Connection connection = getConnection()) {
                        PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)");
                        stmt.setString(1, categoryName);
                        stmt.setString(2, enchantId);
                        stmt.executeUpdate();
                        count++;
                    } catch (SQLException e2) {
                        // 忽略唯一性冲突
                        if (!e2.getMessage().contains("unique constraint") && 
                            !e2.getMessage().contains("Duplicate entry")) {
                            plugin.getLogger().warning("添加附魔 " + enchantId + " 到数据库时出错: " + e2.getMessage());
                        }
                    }
                }
            }
            
            plugin.getLogger().info("附魔同步完成，共处理 " + count + " 个附魔");
            
        } catch (Exception e) {
            plugin.getLogger().severe("同步附魔时出现异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void getPlayer(String uuid, PlayerCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerData player = null;
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT codex_players.UUID, codex_players.PLAYER_NAME, " +
                                    "(SELECT GROUP_CONCAT(cc.Category) FROM codex_players_completed_categories cc WHERE cc.UUID = codex_players.UUID) AS COMPLETED_CATEGORIES, " +
                                    "codex_players_discoveries.CATEGORY AS DISCOVERY_CATEGORY, " +
                                    "codex_players_discoveries.DISCOVERY, " +
                                    "codex_players_discoveries.DATE, " +
                                    "codex_players_discoveries.MILLIS_ACTIONS_EXECUTED " +
                                    "FROM codex_players " +
                                    "LEFT JOIN codex_players_discoveries ON codex_players.UUID = codex_players_discoveries.UUID " +
                                    "WHERE codex_players.UUID = ?");

                    statement.setString(1, uuid);
                    ResultSet result = statement.executeQuery();

                    while(result.next()) {
                        UUID playerUuid = UUID.fromString(result.getString("UUID"));
                        String playerName = result.getString("PLAYER_NAME");
                        String completedCategories = result.getString("COMPLETED_CATEGORIES");
                        String discoveryCategoryName = result.getString("DISCOVERY_CATEGORY");
                        String discoveryName = result.getString("DISCOVERY");
                        String discoveryDate = result.getString("DATE");
                        long discoveryMillisActionsExecuted = result.getLong("MILLIS_ACTIONS_EXECUTED");

                        if(player == null) {
                            player = new PlayerData(playerUuid, playerName);
                        }

                        if(discoveryCategoryName != null) {
                            boolean hasCompletedCategory = completedCategories != null && completedCategories.contains(discoveryCategoryName);
                            PlayerDataCategory playerDataCategory = player.getCategory(discoveryCategoryName);
                            if(playerDataCategory == null) {
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
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void createPlayer(PlayerData player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO codex_players " +
                                    "(UUID, PLAYER_NAME) VALUE (?,?)");

                    statement.setString(1, player.getUuid().toString());
                    statement.setString(2, player.getName());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void updatePlayerName(PlayerData player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE codex_players SET " +
                                    "PLAYER_NAME=? WHERE UUID=?");

                    statement.setString(1, player.getName());
                    statement.setString(2, player.getUuid().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void addDiscovery(String uuid, String categoryName, String discoveryName, String discoveryDate) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    // 首先确保分类存在
                    try {
                        PreparedStatement catStmt = connection.prepareStatement(
                            "MERGE INTO codex_categories (CATEGORY_NAME) KEY(CATEGORY_NAME) VALUES (?)");
                        catStmt.setString(1, categoryName);
                        catStmt.executeUpdate();
                    } catch (SQLException e) {
                        // 如果MERGE失败，尝试使用INSERT
                        try {
                            PreparedStatement insertStmt = connection.prepareStatement(
                                "INSERT INTO codex_categories (CATEGORY_NAME) VALUES (?)");
                            insertStmt.setString(1, categoryName);
                            insertStmt.executeUpdate();
                        } catch (SQLException e2) {
                            // 忽略唯一性错误
                            if (!e2.getMessage().contains("unique constraint") && 
                                !e2.getMessage().contains("Duplicate entry")) {
                                plugin.getLogger().warning("添加分类 " + categoryName + " 时出错: " + e2.getMessage());
                            }
                        }
                    }
                    
                    // 然后确保发现项存在
                    try {
                        PreparedStatement discStmt = connection.prepareStatement(
                            "MERGE INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) KEY(CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)");
                        discStmt.setString(1, categoryName);
                        discStmt.setString(2, discoveryName);
                        discStmt.executeUpdate();
                    } catch (SQLException e) {
                        // 如果MERGE失败，尝试使用INSERT
                        try {
                            PreparedStatement insertStmt = connection.prepareStatement(
                                "INSERT INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)");
                            insertStmt.setString(1, categoryName);
                            insertStmt.setString(2, discoveryName);
                            insertStmt.executeUpdate();
                        } catch (SQLException e2) {
                            // 忽略唯一性错误
                            if (!e2.getMessage().contains("unique constraint") && 
                                !e2.getMessage().contains("Duplicate entry")) {
                                plugin.getLogger().warning("添加发现项 " + discoveryName + " (分类: " + categoryName + ") 时出错: " + e2.getMessage());
                            }
                        }
                    }
                    
                    // 最后添加玩家发现记录
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO codex_players_discoveries " +
                                    "(UUID, CATEGORY, DISCOVERY, DATE, MILLIS_ACTIONS_EXECUTED) VALUE (?,?,?,?,?)");

                    statement.setString(1, uuid);
                    statement.setString(2, categoryName);
                    statement.setString(3, discoveryName);
                    statement.setString(4, discoveryDate);
                    statement.setLong(5, 0);
                    statement.executeUpdate();
                    
                    plugin.getLogger().info("成功添加玩家发现项: " + discoveryName + " (分类: " + categoryName + ")");
                } catch (SQLException e) {
                    plugin.getLogger().warning("添加玩家发现项时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void updateMillisActionsExecuted(String uuid, String categoryName, String discoveryName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE codex_players_discoveries SET " +
                                    "MILLIS_ACTIONS_EXECUTED=? WHERE UUID=? AND CATEGORY=? AND DISCOVERY=?");

                    statement.setLong(1, System.currentTimeMillis());
                    statement.setString(2, uuid);
                    statement.setString(3, categoryName);
                    statement.setString(4, discoveryName);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void updateCompletedCategories(String uuid, String categoryName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO codex_players_completed_categories " +
                                    "(UUID, CATEGORY) VALUE (?,?)");

                    statement.setString(1, uuid);
                    statement.setString(2, categoryName);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void resetDataPlayer(String uuid, String categoryName, String discoveryName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try(Connection connection = getConnection()) {
                    PreparedStatement statement;
                    if(categoryName != null && discoveryName != null) {
                        statement = connection.prepareStatement(
                                "DELETE FROM codex_players_discoveries WHERE UUID=? AND CATEGORY=? AND DISCOVERY=?");
                        statement.setString(1, uuid);
                        statement.setString(2, categoryName);
                        statement.setString(3, discoveryName);
                    } else if(categoryName != null) {
                        statement = connection.prepareStatement(
                                "DELETE FROM codex_players_discoveries WHERE UUID=? AND CATEGORY=?");
                        statement.setString(1, uuid);
                        statement.setString(2, categoryName);
                        
                        PreparedStatement statement2 = connection.prepareStatement(
                                "DELETE FROM codex_players_completed_categories WHERE UUID=? AND CATEGORY=?");
                        statement2.setString(1, uuid);
                        statement2.setString(2, categoryName);
                        statement2.executeUpdate();
                    } else {
                        statement = connection.prepareStatement(
                                "DELETE FROM codex_players_discoveries WHERE UUID=?");
                        statement.setString(1, uuid);
                        
                        PreparedStatement statement2 = connection.prepareStatement(
                                "DELETE FROM codex_players_completed_categories WHERE UUID=?");
                        statement2.setString(1, uuid);
                        statement2.executeUpdate();
                    }
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    public void disable() {
        plugin.getLogger().info("正在关闭H2数据库连接...");
        try {
            // 在关闭连接前执行CHECKPOINT SYNC强制写入数据
            try (Connection conn = getConnection()) {
                if (conn != null) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CHECKPOINT SYNC");
                        plugin.getLogger().info("已强制同步H2数据库数据到磁盘");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("执行H2数据库同步时出错: " + e.getMessage());
            }
            
            // 关闭连接池
            if (connection != null) {
                connection.disable();
                plugin.getLogger().info("H2数据库连接已安全关闭");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("关闭H2数据库时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 更新玩家数据
     * @param playerData 玩家数据
     */
    public void updatePlayer(PlayerData playerData) {
        Connection connection = null;
        try {
            // 获取连接并关闭自动提交
            connection = getConnection();
            if (connection == null) {
                plugin.getLogger().severe("无法获取H2数据库连接，数据保存失败");
                return;
            }
            
            // 关闭自动提交，开始事务
            connection.setAutoCommit(false);
            
            // 更新玩家名称
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE codex_players SET PLAYER_NAME = ? WHERE UUID = ?");
            statement.setString(1, playerData.getName());
            statement.setString(2, playerData.getUuid().toString());
            statement.executeUpdate();
            
            // 清除旧的发现记录
            PreparedStatement deleteDiscoveries = connection.prepareStatement(
                    "DELETE FROM codex_players_discoveries WHERE UUID = ?");
            deleteDiscoveries.setString(1, playerData.getUuid().toString());
            deleteDiscoveries.executeUpdate();
            
            // 清除旧的已完成分类记录
            PreparedStatement deleteCompletedCategories = connection.prepareStatement(
                    "DELETE FROM codex_players_completed_categories WHERE UUID = ?");
            deleteCompletedCategories.setString(1, playerData.getUuid().toString());
            deleteCompletedCategories.executeUpdate();
            
            // 提前准备好添加分类和发现项的语句，以便重复使用
            PreparedStatement addCategoryStmt = connection.prepareStatement(
                "MERGE INTO codex_categories (CATEGORY_NAME) KEY(CATEGORY_NAME) VALUES (?)");
            
            PreparedStatement addDiscoveryStmt = connection.prepareStatement(
                "MERGE INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) KEY(CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)");
            
            // 添加新的发现记录
            int skippedCategories = 0;
            int skippedDiscoveries = 0;
            
            for (PlayerDataCategory category : playerData.getCategories()) {
                String categoryName = category.getName();
                
                // 确保分类存在
                try {
                    addCategoryStmt.setString(1, categoryName);
                    addCategoryStmt.executeUpdate();
                } catch (SQLException e) {
                    // 忽略错误，继续处理
                    plugin.getLogger().warning("添加分类 " + categoryName + " 时出错: " + e.getMessage());
                }
                
                // 添加已完成分类
                if (category.isCompleted()) {
                    PreparedStatement addCompletedCategory = connection.prepareStatement(
                            "INSERT INTO codex_players_completed_categories (UUID, CATEGORY) VALUES (?, ?)");
                    addCompletedCategory.setString(1, playerData.getUuid().toString());
                    addCompletedCategory.setString(2, categoryName);
                    addCompletedCategory.executeUpdate();
                }
                
                // 添加发现记录
                for (PlayerDataDiscovery discovery : category.getDiscoveries()) {
                    String discoveryName = discovery.getDiscoveryName();
                    
                    // 确保发现项在codex_discoveries表中存在
                    try {
                        addDiscoveryStmt.setString(1, categoryName);
                        addDiscoveryStmt.setString(2, discoveryName);
                        addDiscoveryStmt.executeUpdate();
                    } catch (SQLException e) {
                        // 如果MERGE失败，尝试INSERT
                        try {
                            PreparedStatement insertStmt = connection.prepareStatement(
                                "INSERT INTO codex_discoveries (CATEGORY_NAME, DISCOVERY_NAME) VALUES (?, ?)");
                            insertStmt.setString(1, categoryName);
                            insertStmt.setString(2, discoveryName);
                            insertStmt.executeUpdate();
                        } catch (SQLException e2) {
                            // 忽略唯一性错误
                            if (!e2.getMessage().contains("unique constraint") && 
                                !e2.getMessage().contains("Duplicate entry")) {
                                plugin.getLogger().warning("确保发现项存在时出错: " + e2.getMessage());
                            }
                        }
                    }
                    
                    // 添加玩家发现记录
                    try {
                        PreparedStatement addDiscovery = connection.prepareStatement(
                                "INSERT INTO codex_players_discoveries (UUID, CATEGORY, DISCOVERY, DATE, MILLIS_ACTIONS_EXECUTED) VALUES (?, ?, ?, ?, ?)");
                        addDiscovery.setString(1, playerData.getUuid().toString());
                        addDiscovery.setString(2, categoryName);
                        addDiscovery.setString(3, discoveryName);
                        addDiscovery.setString(4, discovery.getDiscoverDate());
                        addDiscovery.setLong(5, discovery.getMillisActionsExecuted());
                        addDiscovery.executeUpdate();
                    } catch (SQLException e) {
                        // 单个发现项插入失败，记录但继续处理其他发现项
                        plugin.getLogger().warning("插入发现项失败: " + discoveryName + " (分类: " + categoryName + ", 玩家: " + playerData.getName() + ") - " + e.getMessage());
                        skippedDiscoveries++;
                    }
                }
            }
            
            // 如果跳过了任何数据，记录警告
            if (skippedCategories > 0 || skippedDiscoveries > 0) {
                plugin.getLogger().warning("保存玩家数据时跳过了 " + skippedCategories + " 个无效分类和 " + skippedDiscoveries + " 个无效发现项 (玩家: " + playerData.getName() + ")");
            }
            
            // 提交事务
            connection.commit();
            
            // 标记数据已保存
            playerData.setModified(false);
            
        } catch (SQLException e) {
            // 发生错误，回滚事务
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().severe("回滚事务时出错: " + rollbackEx.getMessage());
                }
            }
            plugin.getLogger().warning("更新H2数据库中的玩家数据时出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 恢复自动提交并关闭连接
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    plugin.getLogger().warning("关闭数据库连接时出错: " + closeEx.getMessage());
                }
            }
        }
    }

    public interface PlayerCallback {
        void onDone(PlayerData player);
    }
} 