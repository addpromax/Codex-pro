package cx.ajneb97.database;

import cx.ajneb97.Codex;
import cx.ajneb97.config.CommonConfig;
import cx.ajneb97.config.PlayersConfigManager;
import cx.ajneb97.model.data.PlayerData;
import cx.ajneb97.model.data.PlayerDataCategory;
import cx.ajneb97.model.data.PlayerDataDiscovery;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 基于文件的存储管理器实现，用于兼容旧版本
 */
public class FileStorageManager extends AbstractDatabaseManager {

    private PlayersConfigManager playersConfigManager;

    public FileStorageManager(Codex plugin) {
        super(plugin);
        this.logPrefix = "[Codex-File] ";
    }

    @Override
    public boolean initialize() {
        try {
            playersConfigManager = plugin.getConfigsManager().getPlayersConfigManager();
            plugin.getLogger().info(logPrefix + "文件存储系统已初始化");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe(logPrefix + "初始化文件存储系统时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Connection getConnection() {
        // 文件存储不需要数据库连接
        return null;
    }

    @Override
    protected String getDatabaseType() {
        return "file";
    }

    @Override
    public void loadData() {
        // 这个方法在启动时会自动调用配置文件的加载
        // 所有的逻辑已经在PlayersConfigManager中处理
        plugin.getLogger().info(logPrefix + "从文件加载玩家数据");
    }

    @Override
    public void createPlayer(PlayerData player) {
        // 使用PlayerDataManager中的方法来标记玩家数据已修改
        plugin.getPlayerDataManager().setPlayerModified(player.getUuid());
        
        // 直接设置配置文件内容
        CommonConfig playerConfig = playersConfigManager.getConfigFile(player.getUuid().toString());
        playerConfig.getConfig().set("name", player.getName());
        playerConfig.saveConfig();
    }

    @Override
    public void updatePlayerName(PlayerData player) {
        // 标记玩家数据已修改
        plugin.getPlayerDataManager().setPlayerModified(player.getUuid());
        
        // 直接更新配置文件内容
        CommonConfig playerConfig = playersConfigManager.getConfigFile(player.getUuid().toString());
        playerConfig.getConfig().set("name", player.getName());
        playerConfig.saveConfig();
    }

    @Override
    public void addDiscovery(String uuid, String categoryName, String discoveryName, String discoveryDate) {
        // 获取玩家数据
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerByUUID(UUID.fromString(uuid));
        if (playerData == null) {
            return;
        }
        
        // 查找或创建玩家分类数据
        PlayerDataCategory category = playerData.getCategory(categoryName);
        if (category == null) {
            category = new PlayerDataCategory(categoryName, false, new ArrayList<>());
            playerData.getCategories().add(category);
        }
        
        // 添加发现项
        category.getDiscoveries().add(new PlayerDataDiscovery(discoveryName, discoveryDate, 0));
        
        // 标记数据已修改
        plugin.getPlayerDataManager().setPlayerModified(UUID.fromString(uuid));
    }

    @Override
    public void updateMillisActionsExecuted(String uuid, String categoryName, String discoveryName) {
        // 获取玩家数据
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerByUUID(UUID.fromString(uuid));
        if (playerData == null) {
            return;
        }
        
        // 查找玩家分类数据
        PlayerDataCategory category = playerData.getCategory(categoryName);
        if (category == null) {
            return;
        }
        
        // 查找并更新发现项
        for (PlayerDataDiscovery discovery : category.getDiscoveries()) {
            if (discovery.getDiscoveryName().equals(discoveryName)) {
                discovery.setMillisActionsExecuted(discovery.getMillisActionsExecuted() + 1);
                break;
            }
        }
        
        // 标记数据已修改
        plugin.getPlayerDataManager().setPlayerModified(UUID.fromString(uuid));
    }

    @Override
    public void updateCompletedCategories(String uuid, String categoryName) {
        // 获取玩家数据
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerByUUID(UUID.fromString(uuid));
        if (playerData == null) {
            return;
        }
        
        // 查找玩家分类数据
        PlayerDataCategory category = playerData.getCategory(categoryName);
        if (category == null) {
            category = new PlayerDataCategory(categoryName, true, new ArrayList<>());
            playerData.getCategories().add(category);
        } else {
            category.setCompleted(true);
        }
        
        // 标记数据已修改
        plugin.getPlayerDataManager().setPlayerModified(UUID.fromString(uuid));
    }

    @Override
    public void resetDataPlayer(String uuid, String categoryName, String discoveryName) {
        // 获取玩家数据
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerByUUID(UUID.fromString(uuid));
        if (playerData == null) {
            return;
        }
        
        UUID playerUuid = UUID.fromString(uuid);
        
        if (categoryName == null && discoveryName == null) {
            // 重置所有数据
            playerData.getCategories().clear();
        } else if (categoryName != null && discoveryName == null) {
            // 重置特定分类数据
            playerData.getCategories().removeIf(category -> category.getName().equals(categoryName));
        } else {
            // 重置特定发现项
            PlayerDataCategory category = playerData.getCategory(categoryName);
            if (category != null) {
                category.getDiscoveries().removeIf(discovery -> discovery.getDiscoveryName().equals(discoveryName));
            }
        }
        
        // 标记数据已修改
        plugin.getPlayerDataManager().setPlayerModified(playerUuid);
    }

    @Override
    public void updatePlayer(PlayerData playerData) {
        // 标记玩家数据已修改，让系统自动保存
        plugin.getPlayerDataManager().setPlayerModified(playerData.getUuid());
    }

    @Override
    public void getPlayer(String uuid, PlayerCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    FileConfiguration config = playersConfigManager.getConfigFile(uuid).getConfig();
                    String name = config.getString("name");

                    if(name == null){
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                callback.onDone(null);
                            }
                        }.runTask(plugin);
                        return;
                    }

                    PlayerData player = new PlayerData(UUID.fromString(uuid), name);
                    ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
                    if(categoriesSection != null){
                        for(String categoryName : categoriesSection.getKeys(false)){
                            ArrayList<PlayerDataDiscovery> discoveries = new ArrayList<>();
                            boolean completed = config.getBoolean("categories."+categoryName+".completed");

                            ConfigurationSection discoveriesSection = config.getConfigurationSection("categories."+categoryName+".discoveries");
                            if(discoveriesSection != null){
                                for(String discoveryName : discoveriesSection.getKeys(false)){
                                    String date = config.getString("categories."+categoryName+".discoveries."+discoveryName+".date");
                                    long millisActionsExecuted = config.getLong("categories."+categoryName+".discoveries."+discoveryName+".millis_actions_executed",0);
                                    discoveries.add(new PlayerDataDiscovery(discoveryName,date,millisActionsExecuted));
                                }
                            }

                            PlayerDataCategory playerDataCategory = new PlayerDataCategory(categoryName,completed,discoveries);
                            player.getCategories().add(playerDataCategory);
                        }
                    }

                    PlayerData finalPlayer = player;
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            callback.onDone(finalPlayer);
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
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

    @Override
    public void syncCategoriesAndDiscoveries() {
        // 文件存储不需要这个操作
    }

    @Override
    public void shutdown() {
        // 确保所有配置文件都被保存
        playersConfigManager.saveConfigs();
        plugin.getLogger().info(logPrefix + "玩家数据已保存到文件");
    }
} 