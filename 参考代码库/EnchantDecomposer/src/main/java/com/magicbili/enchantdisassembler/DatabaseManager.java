package com.magicbili.enchantdisassembler;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final EnchantDisassembler plugin;
    private Connection connection;

    public DatabaseManager(EnchantDisassembler plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String dbType = plugin.getConfigManager().getDatabaseType();

        try {
            if ("mysql".equalsIgnoreCase(dbType)) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }
            createTable();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "数据库初始化失败", e);
        }
    }

    private void initializeSQLite() throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = plugin.getDataFolder().getAbsolutePath() + "/playerdata.db";
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        plugin.getLogger().info("SQLite 数据库已连接: " + dbPath);
    }

    private void initializeMySQL() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String host = plugin.getConfigManager().getMysqlHost();
        int port = plugin.getConfigManager().getMysqlPort();
        String database = plugin.getConfigManager().getMysqlDatabase();
        String username = plugin.getConfigManager().getMysqlUsername();
        String password = plugin.getConfigManager().getMysqlPassword();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        connection = DriverManager.getConnection(url, username, password);
        plugin.getLogger().info("MySQL 数据库已连接: " + url);
    }

    private void createTable() throws SQLException {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接未初始化，无法创建表");
            return;
        }

        String tableName = getTableName("player_data");
        String sql;

        if ("mysql".equalsIgnoreCase(plugin.getConfigManager().getDatabaseType())) {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "data TEXT)";
        } else {
            // SQLite 语法
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "data TEXT)";
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            plugin.getLogger().info("数据表创建成功: " + tableName);
        }

        // 创建玩家点数表 (跨服共享，防刷安全)
        String poolsTable = getTableName("player_pools");
        String poolsSql;
        if("mysql".equalsIgnoreCase(plugin.getConfigManager().getDatabaseType())){
            poolsSql = "CREATE TABLE IF NOT EXISTS " + poolsTable + " ("+
                    "uuid VARCHAR(36) NOT NULL, "+
                    "rarity VARCHAR(32) NOT NULL, "+
                    "points INT NOT NULL DEFAULT 0, "+
                    "PRIMARY KEY (uuid, rarity)"+
                    ")";
        } else {
            poolsSql = "CREATE TABLE IF NOT EXISTS " + poolsTable + " ("+
                    "uuid TEXT NOT NULL, "+
                    "rarity TEXT NOT NULL, "+
                    "points INTEGER NOT NULL DEFAULT 0, "+
                    "PRIMARY KEY (uuid, rarity)"+
                    ")";
        }
        try(Statement stmt = connection.createStatement()){
            stmt.execute(poolsSql);
            plugin.getLogger().info("点数表创建成功: " + poolsTable);
        }
    }

    private String getTableName(String baseName) {
        if ("mysql".equalsIgnoreCase(plugin.getConfigManager().getDatabaseType())) {
            return plugin.getConfigManager().getMysqlTablePrefix() + baseName;
        }
        return baseName; // SQLite 不需要前缀
    }

    public void savePlayerData(UUID uuid, String data) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接未初始化，无法保存数据");
            return;
        }

        String tableName = getTableName("player_data");
        String sql = "INSERT INTO " + tableName + " (uuid, data) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE data = ?"; // MySQL 语法

        if ("sqlite".equalsIgnoreCase(plugin.getConfigManager().getDatabaseType())) {
            sql = "INSERT OR REPLACE INTO " + tableName + " (uuid, data) VALUES (?, ?)";
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, data);

            // MySQL 需要设置第三个参数
            if (sql.contains("ON DUPLICATE KEY UPDATE")) {
                pstmt.setString(3, data);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "保存玩家数据失败: UUID=" + uuid, e);
        }
    }

    public String loadPlayerData(UUID uuid) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接未初始化，无法加载数据");
            return null;
        }

        String tableName = getTableName("player_data");
        String sql = "SELECT data FROM " + tableName + " WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("data");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "加载玩家数据失败: UUID=" + uuid, e);
        }
        return null;
    }

    public void closeConnection() {
        if (connection == null) return;

        try {
            if (!connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("数据库连接已关闭");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "关闭数据库连接失败", e);
        } finally {
            connection = null;
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ================= 点数相关 API ====================

    public void addPoints(UUID uuid, String rarity, int amount){
        if(connection==null) return;
        String table = getTableName("player_pools");
        String sql;
        if("mysql".equalsIgnoreCase(plugin.getConfigManager().getDatabaseType())){
            sql = "INSERT INTO " + table + " (uuid, rarity, points) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE points = points + VALUES(points)";
        } else {
            sql = "INSERT INTO " + table + " (uuid, rarity, points) VALUES (?,?,?) " +
                    "ON CONFLICT(uuid, rarity) DO UPDATE SET points = points + excluded.points";
        }
        try(PreparedStatement pst = connection.prepareStatement(sql)){
            pst.setString(1, uuid.toString());
            pst.setString(2, rarity);
            pst.setInt(3, amount);
            pst.executeUpdate();
        } catch (SQLException e){
            plugin.getLogger().log(Level.WARNING, "增加点数失败", e);
        }
    }

    /**
     * 尝试扣除点数（原子操作）。若成功则返回 true。
     */
    public boolean deductPoints(UUID uuid, String rarity, int amount){
        if(connection==null) return false;
        String table = getTableName("player_pools");
        String sql = "UPDATE " + table + " SET points = points - ? WHERE uuid = ? AND rarity = ? AND points >= ?";
        try(PreparedStatement pst = connection.prepareStatement(sql)){
            pst.setInt(1, amount);
            pst.setString(2, uuid.toString());
            pst.setString(3, rarity);
            pst.setInt(4, amount);
            int affected = pst.executeUpdate();
            return affected > 0;
        } catch (SQLException e){
            plugin.getLogger().log(Level.WARNING, "扣除点数失败", e);
            return false;
        }
    }

    /**
     * 将指定稀有度点数清零（安全领取）。返回操作是否成功（原点数>0）。
     */
    public boolean resetPoints(UUID uuid, String rarity){
        if(connection==null) return false;
        String table = getTableName("player_pools");
        String sql = "UPDATE " + table + " SET points = 0 WHERE uuid = ? AND rarity = ? AND points > 0";
        try(PreparedStatement pst = connection.prepareStatement(sql)){
            pst.setString(1, uuid.toString());
            pst.setString(2, rarity);
            return pst.executeUpdate() > 0;
        } catch (SQLException e){
            plugin.getLogger().log(Level.WARNING, "重置点数失败", e);
            return false;
        }
    }

    public int getPoints(UUID uuid, String rarity){
        if(connection==null) return 0;
        String table = getTableName("player_pools");
        String sql = "SELECT points FROM " + table + " WHERE uuid=? AND rarity=?";
        try(PreparedStatement pst = connection.prepareStatement(sql)){
            pst.setString(1, uuid.toString());
            pst.setString(2, rarity);
            try(ResultSet rs = pst.executeQuery()){
                if(rs.next()) return rs.getInt("points");
            }
        } catch (SQLException e){
            plugin.getLogger().log(Level.WARNING, "查询点数失败", e);
        }
        return 0;
    }

    public java.util.Map<String,Integer> getAllPoints(UUID uuid){
        java.util.Map<String,Integer> map = new java.util.HashMap<>();
        if(connection==null) return map;
        String table = getTableName("player_pools");
        String sql = "SELECT rarity, points FROM " + table + " WHERE uuid=?";
        try(PreparedStatement pst = connection.prepareStatement(sql)){
            pst.setString(1, uuid.toString());
            try(ResultSet rs = pst.executeQuery()){
                while(rs.next()){
                    map.put(rs.getString("rarity"), rs.getInt("points"));
                }
            }
        } catch (SQLException e){
            plugin.getLogger().log(Level.WARNING, "查询全部点数失败", e);
        }
        return map;
    }
}
