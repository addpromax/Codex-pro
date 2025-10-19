package cx.ajneb97.utils;

import cx.ajneb97.Codex;
import cx.ajneb97.api.AiyatsbusAPI;
import cx.ajneb97.model.enchantment.EnchantmentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Aiyatsbus集成工具类
 * 
 * 提供与Aiyatsbus附魔系统交互的工具方法。
 */
public class AiyatsbusUtils {
    
    private static boolean loggedApiWarning = false;
    
    /**
     * 从Aiyatsbus获取附魔名称
     *
     * @param plugin Codex插件实例
     * @param enchantmentId 附魔ID (格式: namespace:key，例如 minecraft:sharpness)
     * @param fallbackName 如果无法从Aiyatsbus获取则使用的回退名称
     * @return 附魔的名称，优先使用Aiyatsbus中的名称
     */
    public static String getEnchantmentName(Codex plugin, String enchantmentId, String fallbackName) {
        // 初始化API
        if (!AiyatsbusAPI.isAvailable()) {
            plugin.getLogger().info("Aiyatsbus API 不可用，尝试初始化...");
            boolean initialized = AiyatsbusAPI.initialize(plugin);
            plugin.getLogger().info("Aiyatsbus API 初始化结果: " + (initialized ? "成功" : "失败"));
            
            if (!initialized && !loggedApiWarning) {
                plugin.getLogger().warning("无法初始化Aiyatsbus API，将使用原始附魔名称");
                loggedApiWarning = true;
            }
        }
        
        // 尝试从Aiyatsbus获取名称
        if (AiyatsbusAPI.isAvailable()) {
            plugin.getLogger().info("正在从Aiyatsbus获取附魔名称: " + enchantmentId);
            String name = AiyatsbusAPI.getEnchantmentName(enchantmentId);
            
            if (name != null && !name.isEmpty()) {
                plugin.getLogger().info("成功获取附魔名称: " + name);
                return name;
            } else {
                plugin.getLogger().info("未能从Aiyatsbus获取附魔名称，使用回退名称: " + fallbackName);
            }
        } else {
            plugin.getLogger().info("Aiyatsbus API 不可用，使用回退名称: " + fallbackName);
        }
        
        // 如果无法从Aiyatsbus获取，返回回退名称
        return fallbackName;
    }
    
    /**
     * 从Aiyatsbus获取附魔名称
     *
     * @param plugin Codex插件实例
     * @param enchantmentId 附魔ID (格式: namespace:key，例如 minecraft:sharpness)
     * @return 附魔的名称，优先使用Aiyatsbus中的名称，如果不存在则使用ID
     */
    public static String getEnchantmentName(Codex plugin, String enchantmentId) {
        // 先尝试获取原始信息作为回退
        EnchantmentInfo info = plugin.getEnchantmentInfoManager().getEnchantmentInfo(enchantmentId);
        String fallbackName = (info != null) ? info.getName() : enchantmentId;
        
        return getEnchantmentName(plugin, enchantmentId, fallbackName);
    }
    
    /**
     * 获取附魔的本地化显示名称
     *
     * @param plugin Codex插件实例
     * @param enchantmentId 附魔ID
     * @param level 附魔等级
     * @return 格式化后的显示名称
     */
    public static String getDisplayName(Codex plugin, String enchantmentId, int level) {
        // 初始化API
        if (!AiyatsbusAPI.isAvailable()) {
            if (!AiyatsbusAPI.initialize(plugin) && !loggedApiWarning) {
                plugin.getLogger().info("无法初始化Aiyatsbus API，将使用默认附魔名称");
                loggedApiWarning = true;
            }
        }
        
        try {
            // 使用API获取附魔显示名称
            String name = AiyatsbusAPI.getDisplayName(enchantmentId, level, true);
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "获取Aiyatsbus附魔显示名称失败: " + e.getMessage(), e);
        }
        
        // 回退到原始获取方法
        EnchantmentInfo info = plugin.getEnchantmentInfoManager().getEnchantmentInfo(enchantmentId);
        return (info != null) ? info.getName() : enchantmentId;
    }
    
    /**
     * 从Aiyatsbus获取附魔描述
     *
     * @param plugin Codex插件实例
     * @param enchantmentId 附魔ID (格式: namespace:key，例如 minecraft:sharpness)
     * @param fallbackDescription 如果无法从Aiyatsbus获取则使用的回退描述
     * @return 附魔的描述，优先使用Aiyatsbus中的描述
     */
    public static String getEnchantmentDescription(Codex plugin, String enchantmentId, String fallbackDescription) {
        // 初始化API
        if (!AiyatsbusAPI.isAvailable()) {
            plugin.getLogger().info("Aiyatsbus API 不可用，尝试初始化...");
            boolean initialized = AiyatsbusAPI.initialize(plugin);
            plugin.getLogger().info("Aiyatsbus API 初始化结果: " + (initialized ? "成功" : "失败"));
            
            if (!initialized && !loggedApiWarning) {
                plugin.getLogger().warning("无法初始化Aiyatsbus API，将使用原始附魔描述");
                loggedApiWarning = true;
            }
        }
        
        // 尝试从Aiyatsbus获取描述
        if (AiyatsbusAPI.isAvailable()) {
            plugin.getLogger().info("正在从Aiyatsbus获取附魔描述: " + enchantmentId);
            String description = AiyatsbusAPI.getEnchantmentDescription(enchantmentId);
            
            if (description != null && !description.isEmpty()) {
                plugin.getLogger().info("成功获取附魔描述，长度: " + description.length());
                return description;
            } else {
                plugin.getLogger().info("未能从Aiyatsbus获取附魔描述，使用回退描述");
            }
        } else {
            plugin.getLogger().info("Aiyatsbus API 不可用，使用回退描述");
        }
        
        // 如果无法从Aiyatsbus获取，返回回退描述
        return fallbackDescription;
    }
    
    /**
     * 获取附魔最大等级
     *
     * @param plugin Codex插件实例
     * @param enchantmentId 附魔ID
     * @return 最大等级
     */
    public static int getMaxLevel(Codex plugin, String enchantmentId) {
        // 初始化API
        if (!AiyatsbusAPI.isAvailable()) {
            if (!AiyatsbusAPI.initialize(plugin) && !loggedApiWarning) {
                plugin.getLogger().info("无法初始化Aiyatsbus API，将使用默认附魔最大等级");
                loggedApiWarning = true;
            }
        }
        
        try {
            // 使用API获取最大等级
            int maxLevel = AiyatsbusAPI.getMaxLevel(enchantmentId);
            if (maxLevel > 0) {
                return maxLevel;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "获取Aiyatsbus附魔最大等级失败: " + e.getMessage());
        }
        
        // 回退到默认值
        EnchantmentInfo info = plugin.getEnchantmentInfoManager().getEnchantmentInfo(enchantmentId);
        return (info != null) ? info.getMaxLevel() : 1;
    }
    
    /**
     * 获取附魔稀有度
     *
     * @param plugin Codex插件实例
     * @param enchantmentId 附魔ID
     * @return 稀有度名称
     */
    public static String getRarity(Codex plugin, String enchantmentId) {
        // 初始化API
        if (!AiyatsbusAPI.isAvailable()) {
            if (!AiyatsbusAPI.initialize(plugin) && !loggedApiWarning) {
                plugin.getLogger().info("无法初始化Aiyatsbus API，将使用默认附魔稀有度");
                loggedApiWarning = true;
            }
        }
        
        try {
            // 使用API获取稀有度
            String rarity = AiyatsbusAPI.getRarity(enchantmentId);
            if (rarity != null && !rarity.trim().isEmpty()) {
                return rarity;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "获取Aiyatsbus附魔稀有度失败: " + e.getMessage());
        }
        
        // 回退到默认值
        return "common";
    }
    
    /**
     * 从Aiyatsbus获取附魔信息
     *
     * @param plugin Codex插件实例
     * @param enchantmentId 附魔ID (格式: namespace:key，例如 minecraft:sharpness)
     * @return 附魔信息对象，如果无法获取则返回null
     */
    public static EnchantmentInfo getEnchantmentInfo(Codex plugin, String enchantmentId) {
        // 初始化API
        if (!AiyatsbusAPI.isAvailable()) {
            plugin.getLogger().info("Aiyatsbus API 不可用，尝试初始化...");
            boolean initialized = AiyatsbusAPI.initialize(plugin);
            plugin.getLogger().info("Aiyatsbus API 初始化结果: " + (initialized ? "成功" : "失败"));
            
            if (!initialized && !loggedApiWarning) {
                plugin.getLogger().warning("无法初始化Aiyatsbus API，将使用原始附魔信息");
                loggedApiWarning = true;
            }
        }
        
        // 尝试从Aiyatsbus获取信息
        if (AiyatsbusAPI.isAvailable()) {
            plugin.getLogger().info("正在从Aiyatsbus获取附魔信息: " + enchantmentId);
            try {
                String name = AiyatsbusAPI.getEnchantmentName(enchantmentId);
                String descriptionText = AiyatsbusAPI.getEnchantmentDescription(enchantmentId);
                int maxLevel = AiyatsbusAPI.getMaxLevel(enchantmentId);
                String rarity = AiyatsbusAPI.getRarity(enchantmentId);
                
                plugin.getLogger().info("附魔信息获取结果 - 名称: " + name + ", 最大等级: " + maxLevel + ", 稀有度: " + rarity);
                
                if (name != null) {
                    EnchantmentInfo info = new EnchantmentInfo(enchantmentId);
                    info.setName(name);
                    
                    // 处理描述文本，转换为List<String>
                    if (descriptionText != null && !descriptionText.isEmpty()) {
                        String[] lines = descriptionText.split("\n");
                        List<String> descriptionList = new ArrayList<>();
                        for (String line : lines) {
                            descriptionList.add(line);
                        }
                        info.setDescription(descriptionList);
                    }
                    
                    info.setMaxLevel(maxLevel);
                    info.setRarity(rarity);
                    return info;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "获取附魔信息时出错: " + e.getMessage(), e);
            }
        } else {
            plugin.getLogger().info("Aiyatsbus API 不可用，无法获取附魔信息");
        }
        
        return null;
    }
    
    /**
     * 检查Aiyatsbus是否可用
     *
     * @param plugin Codex插件实例
     * @return 如果Aiyatsbus可用则返回true，否则返回false
     */
    public static boolean isAiyatsbusAvailable(Codex plugin) {
        if (!AiyatsbusAPI.isAvailable()) {
            plugin.getLogger().info("Aiyatsbus API 不可用，尝试初始化...");
            boolean result = AiyatsbusAPI.initialize(plugin);
            plugin.getLogger().info("Aiyatsbus API 初始化结果: " + (result ? "成功" : "失败"));
            return result;
        }
        return true;
    }
    
    /**
     * 重置API初始化状态
     * 
     * 用于在插件重载时重新初始化API
     */
    public static void resetApiState() {
        loggedApiWarning = false;
    }
} 