package cx.ajneb97.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cx.ajneb97.Codex;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class H2Connection {

    private HikariDataSource hikari;
    private Codex plugin;

    public H2Connection(Codex plugin) {
        this.plugin = plugin;
        
        HikariConfig hikariConfig = new HikariConfig();
        
        // 获取数据库文件绝对路径，避免多余斜杠和路径问题
        File dbFile = new File(plugin.getDataFolder(), "database/codex");
        String dbFilePath = dbFile.getAbsolutePath();
        // 优化H2连接URL，添加性能参数
        String jdbcUrl = "jdbc:h2:" + dbFilePath + ";MODE=MySQL;CACHE_SIZE=65536;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE";
        plugin.getLogger().info("[Codex-H2] JDBC URL: " + jdbcUrl);
        
        // 配置H2数据库连接
        hikariConfig.setDriverClassName("org.h2.Driver");
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        
        // 连接池配置
        FileConfiguration config = plugin.getConfigsManager().getMainConfigManager().getConfig();
        String path = "h2_database.pool";
        
        if(config.contains(path+".connectionTimeout")) {
            hikariConfig.setConnectionTimeout(config.getLong(path+".connectionTimeout", 3000));
        } else {
            hikariConfig.setConnectionTimeout(3000); // 减少连接超时时间
        }
        
        if(config.contains(path+".maximumPoolSize")) {
            hikariConfig.setMaximumPoolSize(config.getInt(path+".maximumPoolSize", 5));
        } else {
            hikariConfig.setMaximumPoolSize(5); // 减少启动时的连接池大小
        }
        
        if(config.contains(path+".keepaliveTime")) {
            hikariConfig.setKeepaliveTime(config.getLong(path+".keepaliveTime", 30000));
        } else {
            hikariConfig.setKeepaliveTime(30000);
        }
        
        if(config.contains(path+".idleTimeout")) {
            hikariConfig.setIdleTimeout(config.getLong(path+".idleTimeout", 600000));
        } else {
            hikariConfig.setIdleTimeout(600000);
        }
        
        if(config.contains(path+".maxLifetime")) {
            hikariConfig.setMaxLifetime(config.getLong(path+".maxLifetime", 1800000));
        } else {
            hikariConfig.setMaxLifetime(1800000);
        }
        
        // H2特定配置 - 优化启动性能
        hikariConfig.addDataSourceProperty("DB_CLOSE_DELAY", "-1");
        hikariConfig.addDataSourceProperty("DATABASE_TO_UPPER", "false");
        // 移除AUTO_SERVER以提高启动速度
        // hikariConfig.addDataSourceProperty("AUTO_SERVER", "TRUE");
        
        // 添加性能优化配置
        hikariConfig.addDataSourceProperty("LOG", "0"); // 禁用日志以提高性能
        hikariConfig.addDataSourceProperty("CACHE_SIZE", "65536"); // 增加缓存大小
        hikariConfig.addDataSourceProperty("LOCK_TIMEOUT", "10000"); // 设置锁超时
        
        hikari = new HikariDataSource(hikariConfig);
    }

    public HikariDataSource getHikari() {
        return this.hikari;
    }

    public void disable() {
        if(hikari != null) {
            hikari.close();
        }
    }
} 