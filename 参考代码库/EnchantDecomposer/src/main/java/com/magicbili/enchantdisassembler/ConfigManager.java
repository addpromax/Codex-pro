package com.magicbili.enchantdisassembler;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import com.magicbili.enchantdisassembler.IntegrationAPI;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration guiConfig;
    private FileConfiguration messages;
    private final IntegrationAPI integration;
    private Map<String, String> rarityAliases = new java.util.HashMap<>();

    public ConfigManager(JavaPlugin plugin, IntegrationAPI integration) {
        this.plugin = plugin;
        this.integration = integration;
        loadConfigs();
    }

    public void loadConfigs() {
        // 主配置
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // GUI配置
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        // 语言文件
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // 读取稀有度别名映射
        rarityAliases.clear();
        ConfigurationSection aliasSec = config.getConfigurationSection("rarity-aliases");
        if (aliasSec != null) {
            for (String key : aliasSec.getKeys(false)) {
                rarityAliases.put(key, aliasSec.getString(key));
            }
        }
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        guiConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    // 数据库配置方法
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getMysqlHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMysqlPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return config.getString("database.mysql.database", "enchant_disassembler");
    }

    public String getMysqlUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMysqlPassword() {
        return config.getString("database.mysql.password", "");
    }

    public String getMysqlTablePrefix() {
        return config.getString("database.mysql.table-prefix", "ed_");
    }

    // 获取GUI配置
    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    // 获取消息配置
    public String getMessage(String path) {
        String raw = messages.getString(path, "&cMessage not found: " + path);
        return parseMiniMessage(raw);
    }

    /**
     * 将 MiniMessage 或 & 颜色码文本转为旧版兼容字符串。
     */
    public String parseMiniMessage(String raw){
        if(raw==null) return "";
        try {
            // 尝试反射 Adventure MiniMessage
            Class<?> mmClz = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Class<?> compClz = Class.forName("net.kyori.adventure.text.Component");
            Object mm = mmClz.getMethod("miniMessage").invoke(null);
            Object comp = mmClz.getMethod("deserialize", String.class).invoke(mm, raw);
            Class<?> serializer = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            Object legacy = serializer.getMethod("legacySection").invoke(null);
            String legacyStr = (String) serializer.getMethod("serialize", compClz).invoke(legacy, comp);
            return legacyStr;
        } catch (Exception ignored){
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    // 获取稀有度配置 - 使用IntegrationAPI
    public Map<String, Map<String, Double>> getRarityItems() {
        // 兼容层现在不再使用配置文件作为框架回退。
        
        // 从配置文件获取
        Map<String, Map<String, Double>> rarityItems = new HashMap<>();
        ConfigurationSection rarities = config.getConfigurationSection("rarities");

        if (rarities != null) {
            for (String rarity : rarities.getKeys(false)) {
                ConfigurationSection raritySection = rarities.getConfigurationSection(rarity);
                if (raritySection != null) {
                    ConfigurationSection enchants = raritySection.getConfigurationSection("enchants");
                    if (enchants != null) {
                        Map<String, Double> enchantValues = new HashMap<>();
                        for (String enchant : enchants.getKeys(false)) {
                            enchantValues.put(enchant, enchants.getDouble(enchant, 1.0));
                        }
                        rarityItems.put(rarity, enchantValues);
                    }
                }
            }
        }

        return rarityItems;
    }

    // 获取稀有度乘数 - 使用IntegrationAPI
    public Map<String, Double> getRarityMultipliers() {
        return integration.getRarityMultipliers();
    }

    // 获取最大池大小 - 从配置文件获取
    public int getMaxPool(String rarity) {
        // 首先尝试从配置文件获取
        ConfigurationSection rarities = config.getConfigurationSection("rarities");
        if (rarities != null) {
            ConfigurationSection raritySection = rarities.getConfigurationSection(rarity);
            if (raritySection != null) {
                return raritySection.getInt("max-pool", 1000);
            }
        }
        
        // 如果配置文件中没有，使用默认值
        return config.getInt("pool-sizes." + rarity, 1000);
    }

    // 获取所有稀有度 - 使用IntegrationAPI
    public Set<String> getRarities() {
        return integration.getAllRarities();
    }

    // 获取主配置
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * 获取IntegrationAPI实例
     * @return IntegrationAPI实例
     */
    public IntegrationAPI getIntegrationAPI() {
        return integration;
    }

    public String translateRarity(String displayName) {
        if (displayName == null) {
            return "common";
        }

        // 直接使用配置别名（区分大小写以免意外映射）
        return rarityAliases.getOrDefault(displayName, displayName);
    }

    public Map<String, String> getRarityAliases() {
        return java.util.Collections.unmodifiableMap(rarityAliases);
    }
}
