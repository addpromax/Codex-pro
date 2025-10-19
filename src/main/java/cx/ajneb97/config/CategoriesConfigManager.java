package cx.ajneb97.config;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.CategoryManager;
import cx.ajneb97.managers.CommonItemManager;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.DiscoveredOn;
import cx.ajneb97.model.structure.Discovery;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CategoriesConfigManager extends DataFolderConfigManager {

    public CategoriesConfigManager(Codex plugin, String folderName) {
        super(plugin, folderName);
    }

    @Override
    public void loadConfigs() {
        boolean isDebug = plugin.getConfigsManager().getMainConfigManager().isDebug();
        
        if (isDebug) {
            plugin.getLogger().info("========== 开始加载分类文件 ==========");
        }
        
        ArrayList<Category> categories = new ArrayList<>();
        CategoryManager categoryManager = plugin.getCategoryManager();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();

        String path = plugin.getDataFolder() + File.separator + folderName;
        File folder = new File(path);
        
        if (!folder.exists()) {
            plugin.getLogger().warning("分类文件夹不存在: " + path);
            if (isDebug) {
                plugin.getLogger().info("创建分类文件夹...");
            }
            folder.mkdirs();
            categoryManager.setCategories(categories);
            if (isDebug) {
                plugin.getLogger().info("========== 分类加载完成 (0个分类) ==========");
            }
            return;
        }
        
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null || listOfFiles.length == 0) {
            plugin.getLogger().warning("分类文件夹为空: " + path);
            categoryManager.setCategories(categories);
            if (isDebug) {
                plugin.getLogger().info("========== 分类加载完成 (0个分类) ==========");
            }
            return;
        }
        
        if (isDebug) {
            plugin.getLogger().info("发现 " + listOfFiles.length + " 个文件，开始处理...");
        }
        
        int loadedCount = 0;
        int errorCount = 0;
        
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".yml")) {
                try {
                    if (isDebug) {
                        plugin.getLogger().info("正在加载分类文件: " + file.getName());
                    }
                    
                    CommonConfig commonConfig = new CommonConfig(file.getName(), plugin, folderName, true);
                    commonConfig.registerConfig();

                    FileConfiguration config = commonConfig.getConfig();
                    if (config == null) {
                        plugin.getLogger().severe("无法读取配置文件: " + file.getName());
                        errorCount++;
                        continue;
                    }
                    
                    String name = commonConfig.getPath().replace(".yml","");
                    if (isDebug) {
                        plugin.getLogger().info("  -> 分类名称: " + name);
                    }
                    
                    // 检查基本配置结构
                    if (!config.contains("config")) {
                        plugin.getLogger().severe("分类文件 " + file.getName() + " 缺少 'config' 节点");
                        errorCount++;
                        continue;
                    }

                    CommonItem itemCategory = commonItemManager.getCommonItemFromConfig(config,"config.inventory_items.category");
                    if (itemCategory == null && isDebug) {
                        plugin.getLogger().warning("分类 " + name + " 的分类物品配置无效");
                    }

                    CommonItem itemDiscoveryUnlocked = commonItemManager.getCommonItemFromConfig(config,"config.inventory_items.discovery_unlocked");
                    CommonItem itemDiscoveryBlocked = commonItemManager.getCommonItemFromConfig(config,"config.inventory_items.discovery_blocked");

                    List<String> rewardsPerDiscovery = config.getStringList("config.rewards.per_discovery");
                    List<String> rewardsAllDiscoveries = config.getStringList("config.rewards.all_discoveries");

                    ArrayList<Discovery> discoveries = new ArrayList<>();
                    int discoveryCount = 0;
                    if(config.contains("discoveries")){
                        if (isDebug) {
                            plugin.getLogger().info("  -> 加载发现项...");
                        }
                        for(String key : config.getConfigurationSection("discoveries").getKeys(false)){
                            try {
                                if (isDebug) {
                                    plugin.getLogger().info("    处理发现项: " + key);
                                }
                                
                                String discoveryName = config.getString("discoveries."+key+".name");
                                List<String> discoveryDescription = config.getStringList("discoveries."+key+".description");

                                DiscoveredOn discoveredOn = null;
                                if(config.contains("discoveries."+key+".discovered_on")){
                                    try {
                                        String typeStr = config.getString("discoveries."+key+".discovered_on.type");
                                        if (typeStr == null) {
                                            plugin.getLogger().warning("    发现项 " + key + " 的 discovered_on.type 为空");
                                        } else {
                                            discoveredOn = new DiscoveredOn(
                                                    DiscoveredOn.DiscoveredOnType.valueOf(typeStr)
                                            );
                                            String pathValue = "discoveries."+key+".discovered_on.value";
                                            discoveredOn.setMobName(config.getString(pathValue+".mob_name"));
                                            discoveredOn.setMobType(config.getString(pathValue+".mob_type"));
                                            discoveredOn.setRegionName(config.getString(pathValue+".region_name"));
                                            // New trigger types
                                            discoveredOn.setItemType(config.getString(pathValue+".item_type"));
                                            discoveredOn.setCommand(config.getString(pathValue+".command"));
                                            if(config.contains(pathValue+".custom_model_data")){
                                                discoveredOn.setCustomModelData(config.getInt(pathValue+".custom_model_data"));
                                            }
                                            discoveredOn.setComponents(config.getString(pathValue+".components"));
                                            discoveredOn.setCraftEngineId(config.getString(pathValue+".craft_engine_id"));
                                            discoveredOn.setCustomFishingId(config.getString(pathValue+".custom_fishing_id"));
                                            discoveredOn.setItemsAdderId(config.getString(pathValue+".itemsadder_item_id"));
                                            discoveredOn.setMmoItemsType(config.getString(pathValue+".mmoitems_type"));
                                            discoveredOn.setMmoItemsId(config.getString(pathValue+".mmoitems_id"));
                                            discoveredOn.setEnchantmentId(config.getString(pathValue+".enchantment_id"));
                                            if (isDebug) {
                                                plugin.getLogger().info("    -> discovered_on 类型: " + typeStr);
                                            }
                                        }
                                    } catch (IllegalArgumentException e) {
                                        plugin.getLogger().warning("    发现项 " + key + " 的 discovered_on 类型无效: " + e.getMessage());
                                    }
                                } else if (isDebug) {
                                    plugin.getLogger().warning("    发现项 " + key + " 缺少 discovered_on 配置");
                                }

                                List<String> rewards = config.getStringList("discoveries."+key+".rewards");
                                List<String> clickActions = config.getStringList("discoveries."+key+".click_actions");
                                int clickActionsCooldown = config.getInt("discoveries."+key+".click_actions_cooldown");

                                CommonItem customDiscoveryItemUnlocked = null;
                                CommonItem customDiscoveryItemBlocked = null;
                                if(config.contains("discoveries."+key+".discovery_item_unlocked")){
                                    customDiscoveryItemUnlocked = commonItemManager.getCommonItemFromConfig(config,"discoveries."+key+".discovery_item_unlocked");
                                }
                                if(config.contains("discoveries."+key+".discovery_item_blocked")){
                                    customDiscoveryItemBlocked = commonItemManager.getCommonItemFromConfig(config,"discoveries."+key+".discovery_item_blocked");
                                }

                                Discovery discovery = new Discovery();
                                discovery.setId(key);
                                discovery.setName(discoveryName);
                                discovery.setDescription(discoveryDescription);
                                discovery.setDiscoveredOn(discoveredOn);
                                discovery.setCustomRewards(rewards);
                                discovery.setClickActions(clickActions);
                                discovery.setClickActionsCooldown(clickActionsCooldown);
                                discovery.setCustomLevelBlockedItem(customDiscoveryItemBlocked);
                                discovery.setCustomLevelUnlockedItem(customDiscoveryItemUnlocked);
                                discoveries.add(discovery);
                                discoveryCount++;
                                if (isDebug) {
                                    plugin.getLogger().info("    -> 成功加载发现项: " + key);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().severe("    加载发现项 " + key + " 时发生错误: " + e.getMessage());
                                if (isDebug) {
                                    e.printStackTrace();
                                }
                                errorCount++;
                            }
                        }
                        if (isDebug) {
                            plugin.getLogger().info("  -> 共加载 " + discoveryCount + " 个发现项");
                        }
                    } else if (isDebug) {
                        plugin.getLogger().warning("  -> 分类 " + name + " 没有发现项配置");
                    }

                    Category category = new Category();
                    category.setName(name);
                    category.setCategoryItem(itemCategory);
                    category.setDefaultLevelUnlockedItem(itemDiscoveryUnlocked);
                    category.setDefaultLevelBlockedItem(itemDiscoveryBlocked);
                    category.setDefaultRewardsAllDiscoveries(new ArrayList<>(rewardsAllDiscoveries));
                    category.setDefaultRewardsPerDiscovery(new ArrayList<>(rewardsPerDiscovery));
                    category.setDiscoveries(discoveries);

                    categories.add(category);
                    loadedCount++;
                    if (isDebug) {
                        plugin.getLogger().info("成功加载分类: " + name + " (包含 " + discoveryCount + " 个发现项)");
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("加载分类文件 " + file.getName() + " 时发生错误: " + e.getMessage());
                    if (isDebug) {
                        e.printStackTrace();
                    }
                    errorCount++;
                }
            } else if (file.isFile() && isDebug) {
                plugin.getLogger().info("跳过非yml文件: " + file.getName());
            }
        }
        
        if (isDebug) {
            plugin.getLogger().info("========== 分类加载完成 ==========");
            plugin.getLogger().info("成功加载: " + loadedCount + " 个分类");
            plugin.getLogger().info("总共分类数量: " + categories.size());
        }
        if (errorCount > 0) {
            plugin.getLogger().warning("加载失败: " + errorCount + " 个文件");
        }

        categoryManager.setCategories(categories);
        
        // 重建缓存
        plugin.getDiscoveryManager().rebuildCache();
        if (isDebug) {
            plugin.getLogger().info("发现项缓存已重建");
        }
    }

    @Override
    public void saveConfigs() {

    }

    @Override
    public void createFiles() {
        new CommonConfig("history.yml",plugin,folderName,false).registerConfig();
        new CommonConfig("monsters.yml",plugin,folderName,false).registerConfig();
        new CommonConfig("regions.yml",plugin,folderName,false).registerConfig();
    }
}