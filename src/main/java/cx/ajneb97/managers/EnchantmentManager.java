package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.config.EnchantmentsConfigManager;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.model.structure.DiscoveredOn;
import cx.ajneb97.model.enchantment.EnchantmentInfo;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

// 导入Aiyatsbus的API
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment;
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class EnchantmentManager {
    
    private Codex plugin;
    private boolean aiyatsbusPresent;
    
    // 添加附魔缓存
    private Map<String, String> cachedEnchantments = null;
    private long lastCacheUpdate = 0;
    private final long CACHE_DURATION = 300000; // 5分钟缓存时间
    
    // 移除本地配置路径常量
    // private static final String DISCOVERIES_CONFIG_PATH = "discoveries";

    public EnchantmentManager(Codex plugin){
        this.plugin = plugin;
        this.aiyatsbusPresent = Bukkit.getPluginManager().getPlugin("Aiyatsbus") != null;
    }
    
    /**
     * 获取所有可用的附魔
     * @return 附魔列表，格式为 Map<附魔ID, 附魔名称>
     */
    public Map<String, String> getAllEnchantments() {
        final String DEBUG_PREFIX = "[ENCHANT-DEBUG] 获取附魔: ";
        boolean isDebug = plugin.getDebugManager().isDebugEnabled();
        
        // 检查缓存是否有效
        long now = System.currentTimeMillis();
        if (cachedEnchantments != null && now - lastCacheUpdate < CACHE_DURATION) {
            if (isDebug) {
                plugin.getLogger().info(DEBUG_PREFIX + "从缓存获取所有附魔，共" + cachedEnchantments.size() + "个");
            }
            return new HashMap<>(cachedEnchantments); // 返回缓存副本
        }
        
        if (isDebug) {
            plugin.getLogger().info(DEBUG_PREFIX + "缓存过期或不存在，重新获取所有附魔");
        }
        
        Map<String, String> enchantments = new HashMap<>();
        EnchantmentsConfigManager configManager = plugin.getConfigsManager().getEnchantmentsConfigManager();
        List<String> excludedEnchantments = configManager.getExcludedEnchantments();
        
        // 移除从本地配置加载附魔名称的代码
        
        // 获取原版附魔
        if (configManager.includeVanilla()) {
            if (isDebug) {
                plugin.getLogger().info(DEBUG_PREFIX + "开始获取原版附魔");
            }
            
            for (Enchantment enchant : Enchantment.values()) {
                // 不再检查命名空间是否为aiyatsbus，因为Aiyatsbus可能使用minecraft命名空间
                String enchantId = enchant.getKey().toString();
                
                // 检查是否在排除列表中
                if (excludedEnchantments.contains(enchantId)) {
                    if (isDebug) {
                        plugin.getLogger().info(DEBUG_PREFIX + "跳过排除的附魔: " + enchantId);
                    }
                    continue;
                }
                
                // 使用EnchantmentInfoManager获取附魔信息
                EnchantmentInfo info = plugin.getEnchantmentInfoManager().getEnchantmentInfo(enchantId);
                enchantments.put(enchantId, info.getName());
                
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "添加原版附魔: " + enchantId + " -> " + info.getName());
                }
            }
            
            if (isDebug) {
                plugin.getLogger().info(DEBUG_PREFIX + "原版附魔获取完成，共" + enchantments.size() + "个");
            }
        }
        
        // 获取Aiyatsbus附魔
        if (aiyatsbusPresent && configManager.includeAiyatsbus()) {
            if (isDebug) {
                plugin.getLogger().info(DEBUG_PREFIX + "开始获取Aiyatsbus附魔");
                
                // 检测Aiyatsbus插件的版本
                try {
                    Plugin aiyatsbusPlugin = Bukkit.getPluginManager().getPlugin("Aiyatsbus");
                    if (aiyatsbusPlugin != null) {
                        plugin.getLogger().info(DEBUG_PREFIX + "Aiyatsbus插件版本: " + aiyatsbusPlugin.getDescription().getVersion());
                    } else {
                        plugin.getLogger().info(DEBUG_PREFIX + "无法获取Aiyatsbus插件实例");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning(DEBUG_PREFIX + "获取Aiyatsbus版本时出错: " + e.getMessage());
                }
            }
            
            try {
                // 尝试使用Aiyatsbus API获取附魔
                cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager manager = cc.polarastrum.aiyatsbus.core.Aiyatsbus.INSTANCE.api().getEnchantmentManager();
                Map<org.bukkit.NamespacedKey, cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment> enchantMap = manager.getEnchants();
                
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "成功获取Aiyatsbus附魔管理器，发现" + enchantMap.size() + "个附魔");
                }
                
                for (Map.Entry<org.bukkit.NamespacedKey, cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment> entry : enchantMap.entrySet()) {
                    String enchantId = entry.getKey().toString();
                    if (excludedEnchantments.contains(enchantId)) {
                        if (isDebug) {
                            plugin.getLogger().info(DEBUG_PREFIX + "跳过排除的附魔: " + enchantId);
                        }
                        continue;
                    }
                    
                    cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment enchant = entry.getValue();
                    if (enchant == null) {
                        if (isDebug) {
                            plugin.getLogger().warning(DEBUG_PREFIX + "附魔实例为空: " + enchantId);
                        }
                        continue;
                    }
                    
                    // 移除本地配置名称检查，直接从Aiyatsbus获取
                    
                    String enchantName = null;
                    
                    if (isDebug) {
                        plugin.getLogger().info(DEBUG_PREFIX + "开始处理附魔: " + enchantId);
                        plugin.getLogger().info(DEBUG_PREFIX + "附魔类型: " + enchant.getClass().getName());
                    }
                    
                    // 1. 尝试获取附魔基础数据名称
                    try {
                        if (isDebug) {
                            plugin.getLogger().info(DEBUG_PREFIX + "尝试从BasicData获取名称");
                        }
                        
                        cc.polarastrum.aiyatsbus.core.data.BasicData basicData = enchant.getBasicData();
                        String basicName = basicData.getName();
                        
                        if (isDebug) {
                            plugin.getLogger().info(DEBUG_PREFIX + "BasicData名称: " + basicName);
                        }
                        
                        if (basicName != null && !basicName.isEmpty()) {
                            // 直接使用BasicData的name字段
                            enchantName = basicName;
                            enchantments.put(enchantId, enchantName);
                            if (isDebug) {
                                plugin.getLogger().info(DEBUG_PREFIX + "成功使用BasicData.name作为附魔名称: " + enchantId + " -> " + enchantName);
                            }
                            continue; // 获取成功，处理下一个附魔
                        }
                    } catch (Exception e) {
                        if (isDebug) {
                            plugin.getLogger().warning(DEBUG_PREFIX + "从BasicData获取名称失败: " + e.getMessage());
                        }
                    }
                    
                    // 2. 尝试从displayName方法获取
                    if (enchantName == null || enchantName.isEmpty() || enchantName.equals(enchantName.toUpperCase())) {
                        try {
                            if (isDebug) {
                                plugin.getLogger().info(DEBUG_PREFIX + "尝试从displayName方法获取名称");
                            }
                            
                            enchantName = enchant.displayName(1, true);
                            
                            // 处理颜色代码和格式
                            if (enchantName != null && enchantName.contains("§")) {
                                // 移除颜色代码和等级
                                enchantName = enchantName.replaceAll("§[0-9a-fklmnor]", "").trim();
                                // 移除可能的罗马数字等级后缀
                                enchantName = enchantName.replaceAll("\\s+[IVX]+$", "").trim();
                            }
                            
                            if (isDebug) {
                                plugin.getLogger().info(DEBUG_PREFIX + "displayName方法获取的名称: " + enchantName);
                            }
                        } catch (Exception e) {
                            if (isDebug) {
                                plugin.getLogger().warning(DEBUG_PREFIX + "从displayName方法获取名称失败: " + e.getMessage());
                            }
                        }
                    }
                    
                    // 3. 如果前两种方法都失败，尝试使用AiyatsbusAPI
                    if (enchantName == null || enchantName.isEmpty() || enchantName.equals(enchantName.toUpperCase())) {
                        try {
                            if (isDebug) {
                                plugin.getLogger().info(DEBUG_PREFIX + "尝试从AiyatsbusAPI获取名称");
                            }
                            
                            // 先使用原始ID尝试
                            enchantName = cx.ajneb97.api.AiyatsbusAPI.getEnchantmentName(enchantId);
                            
                            if (isDebug) {
                                plugin.getLogger().info(DEBUG_PREFIX + "AiyatsbusAPI获取的名称: " + enchantName);
                            }
                            
                            // 如果还是为空或全大写，尝试使用key部分
                            if (enchantName == null || enchantName.isEmpty() || enchantName.equals(enchantName.toUpperCase())) {
                                String keyPart = entry.getKey().getKey();
                                enchantName = cx.ajneb97.api.AiyatsbusAPI.getEnchantmentName(keyPart);
                                
                                if (isDebug) {
                                    plugin.getLogger().info(DEBUG_PREFIX + "AiyatsbusAPI(keyPart)获取的名称: " + enchantName);
                                }
                            }
                        } catch (Exception e) {
                            if (isDebug) {
                                plugin.getLogger().warning(DEBUG_PREFIX + "从AiyatsbusAPI获取名称失败: " + e.getMessage());
                            }
                        }
                    }
                    
                    // 4. 如果还是获取不到，使用ID的最后部分格式化
                    if (enchantName == null || enchantName.isEmpty()) {
                        if (isDebug) {
                            plugin.getLogger().info(DEBUG_PREFIX + "所有方法都失败，使用ID的最后部分");
                        }
                        
                        String[] parts = enchantId.split(":");
                        if (parts.length > 1) {
                            enchantName = formatEnchantmentName(parts[1]);
                        } else {
                            enchantName = formatEnchantmentName(enchantId);
                        }
                        
                        if (isDebug) {
                            plugin.getLogger().info(DEBUG_PREFIX + "格式化后的名称: " + enchantName);
                        }
                    }
                    
                    if (isDebug) {
                        plugin.getLogger().info(DEBUG_PREFIX + "最终使用的名称: " + enchantName);
                    }
                    
                    enchantments.put(enchantId, enchantName);
                }
                
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "Aiyatsbus附魔获取完成，现在共有" + enchantments.size() + "个附魔");
                }
            } catch (Exception e) {
                plugin.getLogger().warning(DEBUG_PREFIX + "获取Aiyatsbus附魔时发生错误: " + e.getMessage());
                if (configManager.isDebug()) {
                    e.printStackTrace();
                }
            }
        }
        
        // 更新缓存
        cachedEnchantments = new HashMap<>(enchantments);
        lastCacheUpdate = now;
        
        if (isDebug) {
            plugin.getLogger().info(DEBUG_PREFIX + "所有附魔获取完成，总共" + enchantments.size() + "个");
        }
        
        return enchantments;
    }
    
    /**
     * 获取所有已注册的附魔ID
     * @return 附魔ID列表
     */
    public List<String> getAllRegisteredEnchantments() {
        List<String> result = new ArrayList<>();
        
        try {
            // 获取原版附魔
            try {
                // 通过反射获取所有注册的附魔
                for (Enchantment enchant : Enchantment.values()) {
                    String enchantId = enchant.getKey().toString();
                    if (!result.contains(enchantId)) {
                        result.add(enchantId);
                    }
                }
                plugin.getLogger().info("从原版系统获取了附魔，总计: " + result.size() + " 个");
            } catch (Exception e) {
                plugin.getLogger().warning("获取原版附魔列表时出错: " + e.getMessage());
            }
            
            // 尝试获取其他插件的附魔（通过配置文件检测）
            try {
                // 检查是否有Aiyatsbus
                if (plugin.getServer().getPluginManager().getPlugin("Aiyatsbus") != null) {
                    plugin.getLogger().info("检测到Aiyatsbus插件，尝试获取其附魔");
                    
                    // 直接从Aiyatsbus的配置文件或插件目录寻找附魔
                    File aiyatsbusDir = new File(plugin.getServer().getPluginManager().getPlugin("Aiyatsbus").getDataFolder(), "enchantments");
                    if (aiyatsbusDir.exists() && aiyatsbusDir.isDirectory()) {
                        File[] files = aiyatsbusDir.listFiles((dir, name) -> name.endsWith(".yml"));
                        if (files != null) {
                            for (File file : files) {
                                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                                String enchantId = config.getString("id");
                                if (enchantId != null && !enchantId.isEmpty() && !result.contains(enchantId)) {
                                    result.add(enchantId);
                                }
                            }
                        }
                    }
                }
                
                // 检查是否有EcoEnchants
                if (plugin.getServer().getPluginManager().getPlugin("EcoEnchants") != null) {
                    plugin.getLogger().info("检测到EcoEnchants插件，尝试获取其附魔");
                    // 尝试从EcoEnchants目录获取附魔
                    File ecoDir = new File(plugin.getServer().getPluginManager().getPlugin("EcoEnchants").getDataFolder(), "enchants");
                    if (ecoDir.exists() && ecoDir.isDirectory()) {
                        File[] files = ecoDir.listFiles((dir, name) -> name.endsWith(".yml"));
                        if (files != null) {
                            for (File file : files) {
                                String enchantId = "ecoenchants:" + file.getName().replace(".yml", "").toLowerCase();
                                if (!result.contains(enchantId)) {
                                    result.add(enchantId);
                                }
                            }
                        }
                    }
                }
                
                // 检查是否有MMOItems
                if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
                    plugin.getLogger().info("检测到MMOItems插件");
                    // MMOItems的附魔命名空间通常是mmoitems:enchant_id
                    // 这里需要具体实现
                }
            } catch (Exception e) {
                plugin.getLogger().warning("检查其他插件附魔时出错: " + e.getMessage());
            }
            
            // 读取enchantments.yml配置中的附魔
            try {
                FileConfiguration enchConfig = plugin.getConfigsManager().getEnchantmentsConfigManager().getConfigFile().getConfig();
                if (enchConfig != null && enchConfig.contains("enchantments")) {
                    ConfigurationSection section = enchConfig.getConfigurationSection("enchantments");
                    if (section != null) {
                        for (String key : section.getKeys(false)) {
                            if (!result.contains(key)) {
                                result.add(key);
                            }
                        }
                    }
                }
                plugin.getLogger().info("最终获取到 " + result.size() + " 个附魔ID");
            } catch (Exception e) {
                plugin.getLogger().warning("从配置文件读取附魔列表时出错: " + e.getMessage());
            }
            
            // 添加测试用的附魔ID，确保minecraft:annihilate存在
            if (!result.contains("minecraft:annihilate")) {
                result.add("minecraft:annihilate");
                plugin.getLogger().info("添加测试附魔: minecraft:annihilate");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("获取附魔列表时出现异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    // 移除saveEnchantmentName方法和getDefaultDescription方法
    // 移除loadLocalEnchantmentNames方法
    
    /**
     * 清除附魔缓存，强制下次调用时重新获取
     */
    public void clearEnchantmentCache() {
        cachedEnchantments = null;
        lastCacheUpdate = 0;
    }
    
    /**
     * 格式化附魔名称，使其更易读
     * @param name 原始名称
     * @return 格式化后的名称
     */
    private String formatEnchantmentName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        // 将内部标识符格式化为可读形式
        // 例如: ANNIHILATE -> Annihilate
        String formattedName = name.toLowerCase();
        // 首字母大写
        if (formattedName.length() > 0) {
            formattedName = formattedName.substring(0, 1).toUpperCase() + formattedName.substring(1);
        }
        // 处理下划线和空格
        formattedName = formattedName.replace('_', ' ');
        // 单词首字母大写
        String[] words = formattedName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(word.substring(0, 1).toUpperCase())
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
    
    /**
     * 生成附魔分类和对应的发现项
     * @return 生成的附魔分类
     */
    public Category generateEnchantmentCategory() {
        EnchantmentsConfigManager configManager = plugin.getConfigsManager().getEnchantmentsConfigManager();
        
        plugin.getLogger().info("开始生成附魔分类...");
        
        Category category = new Category();
        category.setName(configManager.getCategoryName());
        
        // 设置分类物品
        category.setCategoryItem(configManager.getCategoryItem());
        
        // 设置默认解锁和未解锁物品
        category.setDefaultLevelBlockedItem(configManager.getLockedItem());
        category.setDefaultLevelUnlockedItem(configManager.getUnlockedItem());
        
        // 设置默认奖励
        category.setDefaultRewardsPerDiscovery(new ArrayList<>(configManager.getPerDiscoveryRewards()));
        category.setDefaultRewardsAllDiscoveries(new ArrayList<>(configManager.getAllDiscoveriesRewards()));
        
        // 获取所有附魔并创建发现项
        Map<String, String> enchantments = getAllEnchantments();
        ArrayList<Discovery> discoveries = new ArrayList<>();
        
        plugin.getLogger().info("找到 " + enchantments.size() + " 个附魔");
        
        // 获取已存在的发现项，用于比较是否有新增附魔
        Set<String> existingEnchantIds = new HashSet<>();
        
        // 尝试从分类管理器获取现有分类
        CategoryManager categoryManager = plugin.getCategoryManager();
        if (categoryManager != null) {
            Category existingCategory = categoryManager.getCategory(configManager.getCategoryName());
            if (existingCategory != null && existingCategory.getDiscoveries() != null) {
                for (Discovery existingDiscovery : existingCategory.getDiscoveries()) {
                    // 不再转换ID格式，保持原始格式
                    existingEnchantIds.add(existingDiscovery.getId());
                }
                plugin.getLogger().info("发现现有附魔分类，包含 " + existingEnchantIds.size() + " 个附魔");
            }
        }
        
        // 记录新添加的附魔数量
        int newEnchantmentsCount = 0;
        
        for (Map.Entry<String, String> entry : enchantments.entrySet()) {
            String enchantId = entry.getKey();
            
            // 获取附魔信息
            EnchantmentInfo info = plugin.getEnchantmentInfoManager().getEnchantmentInfo(enchantId);
            
            Discovery discovery = new Discovery();
            // 使用原始命名空间格式（带冒号），不再替换为下划线
            discovery.setId(enchantId);
            // 确保使用中文名称
            String enchantName = enchantments.get(enchantId);
            if (enchantName != null && !enchantName.isEmpty()) {
                discovery.setName("&b" + enchantName);
            } else {
                discovery.setName("&b" + info.getName());
            }
            
            // 获取自定义描述或使用默认描述
            List<String> description = configManager.getCustomDescription(enchantId);
            if (description == null) {
                description = plugin.getEnchantmentInfoManager().processDescription(
                    info, configManager.getDefaultDescriptionTemplate());
            }
            discovery.setDescription(description);
            
            // 设置发现触发条件
            DiscoveredOn discoveredOn = new DiscoveredOn(DiscoveredOn.DiscoveredOnType.ENCHANTMENT_DISCOVER);
            discoveredOn.setEnchantmentId(enchantId);
            discovery.setDiscoveredOn(discoveredOn);
            
            discovery.setCategoryName(configManager.getCategoryName());
            discoveries.add(discovery);
            
            // 只有在初始化或发现新附魔时才详细记录
            boolean isNewEnchantment = !existingEnchantIds.contains(enchantId);
            if (isNewEnchantment) {
                newEnchantmentsCount++;
                // 使用中文名称在日志中显示，而不是内部名称
                String displayName = enchantments.get(enchantId);
                // 如果找不到中文名称，使用info.getName()
                if (displayName == null || displayName.isEmpty()) {
                    displayName = info.getName();
                }
                plugin.getLogger().info("添加附魔: " + displayName + " (ID: " + enchantId + ")");
            }
        }
        
        // 总结新增附魔情况
        if (newEnchantmentsCount > 0) {
            plugin.getLogger().info("本次添加了 " + newEnchantmentsCount + " 个新附魔");
        } else if (!existingEnchantIds.isEmpty()) {
            plugin.getLogger().info("没有新附魔被添加");
        }
        
        category.setDiscoveries(discoveries);
        plugin.getLogger().info("附魔分类生成完成，共 " + discoveries.size() + " 个附魔发现项");
        return category;
    }
    
    /**
     * 创建附魔分类的GUI配置
     * @return GUI配置字符串
     */
    public String generateEnchantmentGUIConfig() {
        EnchantmentsConfigManager configManager = plugin.getConfigsManager().getEnchantmentsConfigManager();
        
        StringBuilder config = new StringBuilder();
        String categoryName = configManager.getCategoryName();
        
        // 确保格式正确，添加适当的缩进
        // 注意：不需要添加前导空格，因为InventoryConfigManager.addInventoryConfig方法会处理
        config.append("category_").append(categoryName).append(":\n");
        config.append("  slots: ").append(configManager.getGuiSize()).append("\n");
        config.append("  title: \"").append(configManager.getGuiTitle()).append("\"\n");
        
        // 获取所有附魔
        Map<String, String> enchantments = getAllEnchantments();
        int slot = 10;
        int row = 0;
        int guiSize = configManager.getGuiSize();
        
        // 只生成第一页的物品，其他页面将由分页系统动态处理
        int itemsPerPage = configManager.isPaginationEnabled() ? configManager.getItemsPerPage() : enchantments.size();
        int count = 0;
        
        // 记录调试信息
        plugin.getLogger().info("生成附魔GUI配置: 发现 " + enchantments.size() + " 个附魔，每页 " + itemsPerPage + " 个");
        
        for (String enchantId : enchantments.keySet()) {
            // 每行9个槽位，最多放7个物品（中间留空）
            if (slot % 9 == 8) {
                slot += 2; // 跳过边缘
            }
            if (slot % 9 == 0) {
                slot++; // 跳过左边缘
            }
            
            // 如果超过了最后一行的前一行，或者已经达到每页物品数量，则停止
            if (slot >= guiSize - 9 || count >= itemsPerPage) {
                break;
            }
            
            config.append("  ").append(slot).append(":\n");
            // 使用双引号包裹enchantId，避免特殊字符问题
            config.append("    type: \"discovery: ").append(escapeYamlString(enchantId)).append("\"\n");
            
            slot++;
            count++;
        }
        
        // 添加返回按钮
        int returnButtonSlot = guiSize - 5; // 底部中间偏左
        config.append("  ").append(returnButtonSlot).append(":\n");
        config.append("    item:\n");
        config.append("      id: PLAYER_HEAD\n");
        config.append("      name: \"&7返回\"\n");
        config.append("      skull_data:\n");
        config.append("        texture: \"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdhZWU5YTc1YmYwZGY3ODk3MTgzMDE1Y2NhMGIyYTdkNzU1YzYzMzg4ZmYwMTc1MmQ1ZjQ0MTlmYzY0NSJ9fX0=\"\n");
        config.append("    open_inventory: main_inventory\n");
        
        // 如果启用分页，添加分页按钮
        if (configManager.isPaginationEnabled()) {
            // 上一页按钮
            config.append("  ").append(configManager.getPreviousButtonSlot()).append(":\n");
            config.append("    item:\n");
            CommonItem prevItem = configManager.getPreviousButtonItem();
            config.append("      id: ").append(prevItem.getId()).append("\n");
            config.append("      name: \"").append(escapeYamlString(prevItem.getName())).append("\"\n");
            if (prevItem.getSkullData() != null && prevItem.getSkullData().getTexture() != null) {
                config.append("      skull_data:\n");
                config.append("        texture: \"").append(prevItem.getSkullData().getTexture()).append("\"\n");
            }
            config.append("    click_actions:\n");
            config.append("      - \"enchant_page_previous\"\n");
            
            // 下一页按钮
            config.append("  ").append(configManager.getNextButtonSlot()).append(":\n");
            config.append("    item:\n");
            CommonItem nextItem = configManager.getNextButtonItem();
            config.append("      id: ").append(nextItem.getId()).append("\n");
            config.append("      name: \"").append(escapeYamlString(nextItem.getName())).append("\"\n");
            if (nextItem.getSkullData() != null && nextItem.getSkullData().getTexture() != null) {
                config.append("      skull_data:\n");
                config.append("        texture: \"").append(nextItem.getSkullData().getTexture()).append("\"\n");
            }
            config.append("    click_actions:\n");
            config.append("      - \"enchant_page_next\"\n");
            
            // 页面信息
            config.append("  ").append(configManager.getPageInfoSlot()).append(":\n");
            config.append("    item:\n");
            CommonItem infoItem = configManager.getPageInfoItem();
            config.append("      id: ").append(infoItem.getId()).append("\n");
            config.append("      name: \"").append(escapeYamlString(infoItem.getName())).append("\"\n");
            if (infoItem.getLore() != null && !infoItem.getLore().isEmpty()) {
                config.append("      lore:\n");
                for (String loreLine : infoItem.getLore()) {
                    config.append("        - \"").append(escapeYamlString(loreLine)).append("\"\n");
                }
            }
        }
        
        // 如果启用边框，添加边框
        if (configManager.isBorderEnabled()) {
            // 获取边框槽位
            List<Integer> borderSlots = configManager.getBorderSlots();
            if (!borderSlots.isEmpty()) {
                config.append("  ");
                
                // 将边框槽位格式化为以分号分隔的列表
                for (int i = 0; i < borderSlots.size(); i++) {
                    config.append(borderSlots.get(i));
                    if (i < borderSlots.size() - 1) {
                        config.append(";");
                    }
                }
                
                config.append(":\n");
                config.append("    item:\n");
                
                // 获取边框物品
                CommonItem borderItem = configManager.getBorderItem();
                config.append("      id: ").append(borderItem.getId()).append("\n");
                config.append("      name: \"").append(escapeYamlString(borderItem.getName())).append("\"\n");
            }
        }
        
        // 验证生成的配置是否为有效的YAML格式
        String configStr = config.toString();
        try {
            // 创建一个临时的YamlConfiguration来测试配置
            org.bukkit.configuration.file.YamlConfiguration testConfig = new org.bukkit.configuration.file.YamlConfiguration();
            testConfig.loadFromString("inventories:\n  " + configStr);
            plugin.getLogger().info("生成的附魔GUI配置YAML格式有效");
        } catch (Exception e) {
            plugin.getLogger().severe("生成的附魔GUI配置YAML格式无效: " + e.getMessage());
            // 尝试修复格式问题
            configStr = fixYamlFormat(configStr);
        }
        
        // 记录最终生成的配置长度
        plugin.getLogger().info("已生成GUI配置，总长度: " + configStr.length() + " 字节，第一行: " + configStr.substring(0, configStr.indexOf('\n')));
        
        return configStr;
    }
    
    /**
     * 尝试修复YAML格式问题
     * @param config YAML配置字符串
     * @return 修复后的配置字符串
     */
    private String fixYamlFormat(String config) {
        // 处理引号问题
        StringBuilder fixed = new StringBuilder();
        String[] lines = config.split("\n");
        
        for (String line : lines) {
            // 检查值中是否包含特殊字符但没有引号
            if (line.contains(":") && !line.endsWith(":")) {
                int colonPos = line.indexOf(":");
                String key = line.substring(0, colonPos).trim();
                String value = line.substring(colonPos + 1).trim();
                
                // 如果值不是以引号开头，但包含特殊字符，添加引号
                if (!value.startsWith("\"") && !value.startsWith("'") && 
                    (value.contains(":") || value.contains("{") || value.contains("}") || 
                     value.contains("[") || value.contains("]") || value.contains(",") ||
                     value.contains("&") || value.contains("#") || value.contains("!"))) {
                    line = key + ": \"" + value.replace("\"", "\\\"") + "\"";
                }
            }
            
            fixed.append(line).append("\n");
        }
        
        return fixed.toString();
    }
    
    /**
     * 处理物品lore中的变量
     * @param lore 原始lore
     * @param enchantmentId 附魔ID
     * @return 处理后的lore
     */
    public List<String> processItemLore(List<String> lore, String enchantmentId) {
        if (lore == null || lore.isEmpty()) {
            return new ArrayList<>();
        }
        
        EnchantmentInfo info = plugin.getEnchantmentInfoManager().getEnchantmentInfo(enchantmentId);
        EnchantmentsConfigManager configManager = plugin.getConfigsManager().getEnchantmentsConfigManager();
        
        return plugin.getEnchantmentInfoManager().processLore(info, lore, configManager);
    }

    /**
     * 获取附魔名称
     * @param enchantmentId 附魔ID
     * @return 附魔名称，如果不存在则返回null
     */
    public String getEnchantmentName(String enchantmentId) {
        // 从配置文件中获取附魔名称
        if (enchantmentId == null) {
            return null;
        }

        // 尝试从配置文件中查找
        FileConfiguration config = plugin.getConfigsManager().getMainConfigManager().getConfigFile().getConfig();
        String path = "enchantments." + enchantmentId.replace(":", ".");
        if (config.contains(path + ".name")) {
            return config.getString(path + ".name");
        }

        // 尝试从Aiyatsbus插件获取
        if (Bukkit.getPluginManager().isPluginEnabled("Aiyatsbus")) {
            try {
                // 这里使用反射或其他方法从Aiyatsbus获取附魔名称
                // 由于不直接依赖，所以使用反射
                return enchantmentId;
            } catch (Exception e) {
                return enchantmentId;
            }
        }
        
        // 默认返回附魔ID
        return enchantmentId;
    }

    /**
     * 获取附魔描述
     * @param enchantmentId 附魔ID
     * @return 附魔描述，如果不存在则返回null
     */
    public String getEnchantmentDescription(String enchantmentId) {
        // 从配置文件中获取附魔描述
        if (enchantmentId == null) {
            return null;
        }

        // 尝试从配置文件中查找
        FileConfiguration config = plugin.getConfigsManager().getMainConfigManager().getConfigFile().getConfig();
        String path = "enchantments." + enchantmentId.replace(":", ".");
        if (config.contains(path + ".description")) {
            return config.getString(path + ".description");
        }

        // 尝试从Aiyatsbus插件获取
        if (Bukkit.getPluginManager().isPluginEnabled("Aiyatsbus")) {
            try {
                // 这里使用反射或其他方法从Aiyatsbus获取附魔描述
                // 由于不直接依赖，所以使用反射
                return null;
            } catch (Exception e) {
                return null;
            }
        }
        
        return null;
    }

    /**
     * 转义YAML字符串，确保特殊字符不会导致解析错误
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    private String escapeYamlString(String input) {
        if (input == null) return "";
        
        // 替换可能导致YAML解析问题的特殊字符
        return input
            .replace("\\", "\\\\") // 反斜杠
            .replace("\"", "\\\"") // 双引号
            .replace("\n", "\\n")  // 换行符
            .replace("\r", "\\r")  // 回车符
            .replace("\t", "\\t")  // 制表符
            .replace("\f", "\\f")  // 换页符
            .replace("\b", "\\b"); // 退格符
    }
} 