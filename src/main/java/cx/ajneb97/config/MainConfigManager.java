package cx.ajneb97.config;

import cx.ajneb97.Codex;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainConfigManager {

    private Codex plugin;
    private CommonConfig configFile;

    private String storageType;
    private boolean isMySQL;
    private boolean isH2;
    private boolean updateNotify;
    private String discoveriesDateFormat;
    private int playerDataSave;
    private boolean autoSaveEnabled;
    private boolean showSaveNotifications;
    private String dataSaveMode;
    private int realtimeDebounceDelay;
    private boolean realtimeShowNotifications;
    private int memoryCleanupInterval;
    private boolean cleanOfflinePlayers;
    private String progressBarPlaceholderFillSymbol;
    private String progressBarPlaceholderEmptySymbol;
    private int progressBarPlaceholderAmount;
    private int configVersion;

    public MainConfigManager(Codex plugin){
        this.plugin = plugin;
        this.configFile = new CommonConfig("config.yml",plugin,null,false);
        configFile.registerConfig();
        checkUpdate();
    }

    public void configure() {
        FileConfiguration config = configFile.getConfig();
        
        // 获取存储类型，默认为file
        storageType = config.getString("storage_type", "file");
        
        // 兼容旧配置
        isMySQL = "mysql".equalsIgnoreCase(storageType) || config.getBoolean("mysql_database.enabled");
        isH2 = "h2".equalsIgnoreCase(storageType);
        
        updateNotify = config.getBoolean("update_notify");
        discoveriesDateFormat = config.getString("discoveries_date_format");
        playerDataSave = config.getInt("player_data_save");
        
        // 明确读取auto_save_enabled设置，确保使用配置文件中的值
        if (config.isSet("auto_save_enabled")) {
            autoSaveEnabled = config.getBoolean("auto_save_enabled");
        } else {
            autoSaveEnabled = true; // 默认值为true
        }
        
        showSaveNotifications = config.getBoolean("show_save_notifications", false);
        
        // 数据保存模式配置
        dataSaveMode = config.getString("data_save_mode", "batch").toLowerCase();
        if (!dataSaveMode.equals("batch") && !dataSaveMode.equals("realtime")) {
            plugin.getLogger().warning("无效的数据保存模式: " + dataSaveMode + "，使用默认值 'realtime'");
            dataSaveMode = "realtime";
        }
        
        // 实时保存配置
        realtimeDebounceDelay = config.getInt("realtime_save.debounce_delay", 3);
        realtimeShowNotifications = config.getBoolean("realtime_save.show_notifications", false);
        
        // 内存管理配置
        memoryCleanupInterval = config.getInt("memory_management.cleanup_interval", 1800);
        cleanOfflinePlayers = config.getBoolean("memory_management.clean_offline_players", true);
        
        progressBarPlaceholderFillSymbol = config.getString("progress_bar_placeholder.filled_symbol");
        progressBarPlaceholderEmptySymbol = config.getString("progress_bar_placeholder.empty_symbol");
        progressBarPlaceholderAmount = config.getInt("progress_bar_placeholder.amount");
        configVersion = config.getInt("config_version");
        
        // 如果配置文件中没有存储类型，添加默认值
        if (!config.contains("storage_type")) {
            config.set("storage_type", "file");
            
            // 添加H2数据库配置
            if (!config.contains("h2_database")) {
                config.set("h2_database.pool.connectionTimeout", 5000);
                config.set("h2_database.pool.maximumPoolSize", 10);
                config.set("h2_database.pool.keepaliveTime", 30000);
                config.set("h2_database.pool.idleTimeout", 600000);
                config.set("h2_database.pool.maxLifetime", 1800000);
                configFile.saveConfig();
            }
        }
        
        // 添加自动保存配置，如果不存在
        if (!config.contains("auto_save_enabled")) {
            config.set("auto_save_enabled", true);
            configFile.saveConfig();
        }
        
        // 添加自动保存通知配置，如果不存在
        if (!config.contains("show_save_notifications")) {
            config.set("show_save_notifications", false);
            configFile.saveConfig();
        }
    }

    public boolean reloadConfig(){
        if(!configFile.reloadConfig()){
            return false;
        }
        configure();
        return true;
    }

    public void checkUpdate(){
        try {
            // 获取配置文件的路径
            String route = configFile.getRoute();
            if(route == null) {
                plugin.getLogger().warning("无法获取配置文件路径");
                return;
            }
            Path pathConfig = Paths.get(route);
            
            // 读取配置文件内容
            String text = new String(Files.readAllBytes(pathConfig));
            boolean updated = false;
            
            FileConfiguration config = configFile.getConfig();
            if(config == null) {
                plugin.getLogger().warning("无法获取配置对象");
                return;
            }
            
            if(!text.contains("verifyServerCertificate:")){
                config.set("mysql_database.pool.connectionTimeout",5000);
                config.set("mysql_database.advanced.verifyServerCertificate",false);
                config.set("mysql_database.advanced.useSSL",true);
                config.set("mysql_database.advanced.allowPublicKeyRetrieval",true);
                updated = true;
            }
            
            if(!text.contains("storage_type:")){
                config.set("storage_type", "file");
                updated = true;
            }
            
            if(!text.contains("h2_database:")){
                config.set("h2_database.pool.connectionTimeout", 5000);
                config.set("h2_database.pool.maximumPoolSize", 10);
                config.set("h2_database.pool.keepaliveTime", 30000);
                config.set("h2_database.pool.idleTimeout", 600000);
                config.set("h2_database.pool.maxLifetime", 1800000);
                updated = true;
            }
            
            if(!text.contains("auto_save_enabled:")){
                config.set("auto_save_enabled", true);
                updated = true;
            }
            
            if(!text.contains("show_save_notifications:")){
                config.set("show_save_notifications", false);
                updated = true;
            }
            
            if(!text.contains("memory_management:")){
                config.set("memory_management.cleanup_interval", 1800);
                config.set("memory_management.clean_offline_players", true);
                updated = true;
            }
            
            if(updated){
                configFile.saveConfig();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public CommonConfig getConfigFile() {
        return configFile;
    }

    public FileConfiguration getConfig(){
        return configFile.getConfig();
    }

    public String getStorageType() {
        return storageType;
    }

    public boolean isMySQL() {
        return isMySQL;
    }

    public boolean isH2() {
        return isH2;
    }

    public boolean isUpdateNotify() {
        return updateNotify;
    }

    public String getDiscoveriesDateFormat() {
        return discoveriesDateFormat;
    }

    public int getPlayerDataSave() {
        return playerDataSave;
    }
    
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }
    
    public boolean isShowSaveNotifications() {
        return showSaveNotifications;
    }
    
    public int getMemoryCleanupInterval() {
        return memoryCleanupInterval;
    }
    
    public boolean isCleanOfflinePlayers() {
        return cleanOfflinePlayers;
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public String getProgressBarPlaceholderFillSymbol() {
        return progressBarPlaceholderFillSymbol;
    }

    public String getProgressBarPlaceholderEmptySymbol() {
        return progressBarPlaceholderEmptySymbol;
    }

    public int getProgressBarPlaceholderAmount() {
        return progressBarPlaceholderAmount;
    }

    public String getDataSaveMode() {
        return dataSaveMode;
    }

    public boolean isRealtimeSaveMode() {
        return "realtime".equals(dataSaveMode);
    }

    public boolean isBatchSaveMode() {
        return "batch".equals(dataSaveMode);
    }

    public int getRealtimeDebounceDelay() {
        return realtimeDebounceDelay;
    }

    public boolean isRealtimeShowNotifications() {
        return realtimeShowNotifications;
    }

    /**
     * 创建配置文件
     */
    public void createConfig(){
        try {
            File configFileObj = new File(plugin.getDataFolder(), "config.yml");
            // 检查文件是否存在
            if(configFileObj.exists()){
                return;
            }
            
            // 确保父目录存在
            if (!configFileObj.getParentFile().exists()) {
                configFileObj.getParentFile().mkdirs();
            }
            
            // 创建新文件
            configFileObj.createNewFile();
            
            // 使用YamlConfiguration直接操作
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFileObj);
            
            config.set("config-version", 2);
            
            // ... existing code ...
            
            config.set("enchantments_codex.enabled", true);
            config.set("enchantments_codex.auto_generate", true);
            config.set("enchantments_codex.category_name", "enchantments");
            config.set("enchantments_codex.main_menu_slot", 24);
            config.set("enchantments_codex.debug", false);
            
            // 添加钓鱼图鉴的默认配置
            config.set("fishing_codex.enabled", true);
            config.set("fishing_codex.auto_generate", true);
            config.set("fishing_codex.category_name", "fishing");
            config.set("fishing_codex.debug", false);
            
            // 添加统一的debug配置
            config.set("codex.debug", false);
            
            config.set("verify.timeout", 30);
            
            // 保存配置
            config.save(configFileObj);
            
            // 重新加载CommonConfig
            this.configFile.reloadConfig();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    
    /**
     * 获取是否启用调试模式
     * 现在使用统一的debug配置
     */
    public boolean isDebug() {
        return configFile.getConfig().getBoolean("codex.debug", false);
    }
}
