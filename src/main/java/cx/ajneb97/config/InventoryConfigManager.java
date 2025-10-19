package cx.ajneb97.config;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.CommonItemManager;
import cx.ajneb97.managers.InventoryManager;
import cx.ajneb97.model.inventory.CommonInventory;
import cx.ajneb97.model.inventory.CommonInventoryItem;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.utils.OtherUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;

public class InventoryConfigManager {

    private Codex plugin;
    private CommonConfig configFile;


    public InventoryConfigManager(Codex plugin){
        this.plugin = plugin;
        this.configFile = new CommonConfig("inventory.yml",plugin,null, false);
        this.configFile.registerConfig();
        if(this.configFile.isFirstTime() && OtherUtils.isLegacy()){
            checkAndFix();
        }
    }

    public void checkAndFix(){
        FileConfiguration config = configFile.getConfig();
        config.set("inventories.main_inventory.0;8;36;44.item.id","STAINED_GLASS_PANE:14");
        config.set("inventories.main_inventory.1;7;9;17;27;35;37;43.item.id","STAINED_GLASS_PANE:15");

        config.set("inventories.category_history.39.item.id","SKULL_ITEM:3");
        config.set("inventories.category_history.0;8;36;44.item.id","STAINED_GLASS_PANE:11");
        config.set("inventories.category_history.1;7;9;17;27;35;37;43.item.id","STAINED_GLASS_PANE:15");

        config.set("inventories.category_regions.39.item.id","SKULL_ITEM:3");
        config.set("inventories.category_regions.0;8;36;44.item.id","STAINED_GLASS_PANE:11");
        config.set("inventories.category_regions.1;7;9;17;27;35;37;43.item.id","STAINED_GLASS_PANE:15");

        config.set("inventories.category_monsters.39.item.id","SKULL_ITEM:3");
        config.set("inventories.category_monsters.0;8;36;44.item.id","STAINED_GLASS_PANE:11");
        config.set("inventories.category_monsters.1;7;9;17;27;35;37;43.item.id","STAINED_GLASS_PANE:15");

        configFile.saveConfig();
    }

    public void configure(){
        FileConfiguration config = configFile.getConfig();
        InventoryManager inventoryManager = plugin.getInventoryManager();

        ArrayList<CommonInventory> inventories = new ArrayList<>();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();
        if(config.contains("inventories")) {
            for(String key : config.getConfigurationSection("inventories").getKeys(false)) {
                int slots = config.getInt("inventories."+key+".slots");
                String title = config.getString("inventories."+key+".title");

                List<CommonInventoryItem> items = new ArrayList<>();
                for(String slotString : config.getConfigurationSection("inventories."+key).getKeys(false)) {
                    if(!slotString.equals("slots") && !slotString.equals("title") && !slotString.equals("config")) {
                        String path = "inventories."+key+"."+slotString;
                        CommonItem item = null;
                        if(config.contains(path+".item")){
                            item = commonItemManager.getCommonItemFromConfig(config, path+".item");
                        }

                        String openInventory = config.contains(path+".open_inventory") ?
                                config.getString(path+".open_inventory") : null;

                        List<String> clickActions = config.contains(path+".click_actions") ?
                                config.getStringList(path+".click_actions") : null;

                        String type = config.contains(path+".type") ?
                                config.getString(path+".type") : null;

                        CommonInventoryItem inventoryItem = new CommonInventoryItem(slotString,item,openInventory,clickActions,type);
                        items.add(inventoryItem);
                    }
                }

                CommonInventory inv = new CommonInventory(key,slots,title,items);
                inventories.add(inv);
            }
        }
        inventoryManager.setInventories(inventories);
    }

    public boolean reloadConfig(){
        if(!configFile.reloadConfig()){
            return false;
        }
        configure();
        return true;
    }

    public FileConfiguration getConfig(){
        return configFile.getConfig();
    }

    /**
     * 检查指定库存中是否已有特定类型的物品
     * @param inventoryName 库存名称
     * @param key 要检查的键
     * @param value 要检查的值
     * @return 如果存在则返回true
     */
    public boolean hasInventoryItem(String inventoryName, String key, String value) {
        FileConfiguration config = configFile.getConfig();
        ConfigurationSection inventoriesSection = config.getConfigurationSection("inventories");
        if (inventoriesSection == null) {
            return false;
        }
        
        ConfigurationSection inventorySection = inventoriesSection.getConfigurationSection(inventoryName);
        if (inventorySection == null) {
            return false;
        }
        
        for (String slotKey : inventorySection.getKeys(false)) {
            if (slotKey.equals("slots") || slotKey.equals("title") || slotKey.equals("config")) {
                continue;
            }
            
            String path = "inventories." + inventoryName + "." + slotKey + "." + key;
            if (config.isSet(path) && config.getString(path).equals(value)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 添加物品到指定的库存中
     * @param inventoryName 库存名称
     * @param slot 槽位
     * @param itemConfig 物品配置
     */
    public void addInventoryItem(String inventoryName, String slot, String itemConfig) {
        FileConfiguration config = configFile.getConfig();
        String path = "inventories." + inventoryName + "." + slot;
        
        // 检查库存是否存在
        if (!config.isSet("inventories." + inventoryName)) {
            return;
        }
        
        // 将配置字符串转换为YAML格式并添加到配置中
        String[] lines = itemConfig.split("\n");
        StringBuilder yamlContent = new StringBuilder();
        
        for (String line : lines) {
            yamlContent.append(line).append("\n");
        }
        
        try {
            // 保存配置到文件
            String content = configFile.getStringContent();
            String pathToReplace = "inventories:\n  " + inventoryName + ":";
            String replacement = "inventories:\n  " + inventoryName + ":\n" + 
                                 "    " + slot + ":\n" + yamlContent.toString();
            
            if (content.contains(pathToReplace)) {
                // 检查是否已存在该槽位
                String slotPattern = "    " + slot + ":";
                if (content.contains(slotPattern)) {
                    // 如果已存在，则不做任何操作
                    return;
                }
                
                // 找到合适的位置插入新配置
                int insertPos = content.indexOf(pathToReplace) + pathToReplace.length();
                String newContent = content.substring(0, insertPos) + "\n" + 
                                    "    " + slot + ":\n" + yamlContent.toString() +
                                    content.substring(insertPos);
                
                configFile.saveStringContent(newContent);
                configFile.reloadConfig();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存库存配置: " + e.getMessage());
        }
    }
    
    /**
     * 添加新的库存配置
     * @param inventoryName 库存名称
     * @param inventoryConfig 库存配置
     */
    public void addInventoryConfig(String inventoryName, String inventoryConfig) {
        try {
            // 检查库存是否已存在
            FileConfiguration config = configFile.getConfig();
            if (config.isSet("inventories." + inventoryName)) {
                plugin.getLogger().info("库存配置 " + inventoryName + " 已存在，跳过添加");
                return; // 如果已存在，不做任何操作
            }
            
            // 记录调试信息
            plugin.getLogger().info("正在添加库存配置: " + inventoryName);
            plugin.getLogger().info("写入库存配置内容，长度: " + inventoryConfig.length() + " 字节");
            
            // 直接使用YAML API而不是文件操作
            if (!config.contains("inventories")) {
                config.createSection("inventories");
            }
            
            // 将配置添加到主配置文件
            String fullConfig = "inventories:\n  " + inventoryConfig;
            YamlConfiguration tempConfig = new YamlConfiguration();
            try {
                tempConfig.loadFromString(fullConfig);
                if (tempConfig.contains("inventories." + inventoryName)) {
                    ConfigurationSection section = tempConfig.getConfigurationSection("inventories." + inventoryName);
                    config.set("inventories." + inventoryName, section);
                    
                    // 保存并重新加载
                    configFile.saveConfig();
                    configFile.reloadConfig();
                    plugin.getLogger().info("库存配置 " + inventoryName + " 添加成功");
                } else {
                    plugin.getLogger().severe("库存配置 " + inventoryName + " 添加失败，无法解析配置");
                }
            } catch (InvalidConfigurationException e) {
                plugin.getLogger().severe("库存配置格式无效: " + e.getMessage());
                plugin.getLogger().warning("尝试使用备用方法添加配置...");
                
                // 备用方法：直接修改文件内容
                try {
                    // 读取现有文件内容
                    String content = configFile.getStringContent();
                    
                    // 确保配置以正确的格式添加
                    if (content.contains("inventories:")) {
                        // 找到适合的位置插入新配置
                        StringBuilder newContent = new StringBuilder();
                        String[] lines = content.split("\n");
                        boolean foundInv = false;
                        boolean inserted = false;
                        
                        for (String line : lines) {
                            newContent.append(line).append("\n");
                            if (!inserted && line.trim().equals("inventories:")) {
                                foundInv = true;
                            } else if (foundInv && !inserted && (!line.startsWith("  ") || line.trim().isEmpty())) {
                                // 在inventories节之后、下一个主节点之前插入
                                newContent.append("  ").append(inventoryName).append(":\n");
                                // 添加内容，确保正确缩进
                                String[] configLines = inventoryConfig.split("\n");
                                for (String configLine : configLines) {
                                    if (!configLine.trim().startsWith(inventoryName + ":")) {
                                        newContent.append("    ").append(configLine).append("\n");
                                    }
                                }
                                inserted = true;
                            }
                        }
                        
                        // 如果没有找到合适的位置，则添加到文件末尾
                        if (!inserted) {
                            newContent.append("  ").append(inventoryName).append(":\n");
                            String[] configLines = inventoryConfig.split("\n");
                            for (String configLine : configLines) {
                                if (!configLine.trim().startsWith(inventoryName + ":")) {
                                    newContent.append("    ").append(configLine).append("\n");
                                }
                            }
                        }
                        
                        // 保存修改后的内容
                        configFile.saveStringContent(newContent.toString());
                        configFile.reloadConfig();
                        
                        // 验证配置是否成功添加
                        FileConfiguration finalConfig = configFile.getConfig();
                        if (finalConfig.isSet("inventories." + inventoryName)) {
                            plugin.getLogger().info("库存配置 " + inventoryName + " 添加成功");
                        } else {
                            plugin.getLogger().severe("库存配置 " + inventoryName + " 添加失败，配置未生效");
                        }
                    } else {
                        // 如果inventories节点不存在，创建它
                        StringBuilder newContent = new StringBuilder(content);
                        newContent.append("\ninventories:\n");
                        newContent.append("  ").append(inventoryName).append(":\n");
                        String[] configLines = inventoryConfig.split("\n");
                        for (String configLine : configLines) {
                            if (!configLine.trim().startsWith(inventoryName + ":")) {
                                newContent.append("    ").append(configLine).append("\n");
                            }
                        }
                        
                        // 保存修改后的内容
                        configFile.saveStringContent(newContent.toString());
                        configFile.reloadConfig();
                    }
                } catch (IOException ex) {
                    plugin.getLogger().severe("备用添加方法失败: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("添加库存配置时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取批量添加物品界面的标题
     * @return 界面标题
     */
    public String getBatchInventoryTitle() {
        String title = configFile.getConfig().getString("batch_inventory.title");
        return title != null ? title : "&8批量添加物品";
    }
    
    /**
     * 获取批量添加物品界面的大小
     * @return 界面大小
     */
    public int getBatchInventorySize() {
        int size = configFile.getConfig().getInt("batch_inventory.size");
        return size > 0 ? size : 54; // 默认为54（6行）
    }
    
    /**
     * 获取确认按钮位置
     * @return 确认按钮槽位
     */
    public int getConfirmButtonSlot() {
        int slot = configFile.getConfig().getInt("batch_inventory.confirm_button_slot");
        return slot >= 0 ? slot : 49; // 默认为49（最后一行中间偏右）
    }
    
    /**
     * 获取取消按钮位置
     * @return 取消按钮槽位
     */
    public int getCancelButtonSlot() {
        int slot = configFile.getConfig().getInt("batch_inventory.cancel_button_slot");
        return slot >= 0 ? slot : 53; // 默认为53（最后一行最右）
    }
}
