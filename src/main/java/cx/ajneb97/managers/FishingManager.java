package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.model.structure.DiscoveredOn;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 钓鱼管理器 - 简化版（只保留分组概率计算）
 */
public class FishingManager {
    
    private Codex plugin;
    private boolean customFishingPresent;
    
    // 缓存
    private Map<String, Map<String, Object>> cachedFishData = null;
    private long lastCacheUpdate = 0;
    private final long CACHE_DURATION = 300000; // 5分钟
    
    // Loot分组概率缓存
    private Map<String, Map<String, LootGroupInfo>> cachedGroupProbabilities = new HashMap<>();
    private volatile boolean isProbabilityCalculating = false;
    
    public FishingManager(Codex plugin) {
        this.plugin = plugin;
        this.customFishingPresent = Bukkit.getPluginManager().getPlugin("CustomFishing") != null;
        
        if (customFishingPresent) {
            plugin.getLogger().info("[FISHING] CustomFishing 已检测，使用分组概率系统");
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::preCalculateAllBiomeProbabilities, 100L);
        } else {
            plugin.getLogger().warning("[FISHING] CustomFishing 未找到，钓鱼分类将被禁用");
        }
    }
    
    /**
     * 异步预计算所有鱼类的分组概率
     */
    public void preCalculateAllBiomeProbabilities() {
        if (!customFishingPresent || isProbabilityCalculating) {
            return;
        }
        
        isProbabilityCalculating = true;
        plugin.getLogger().info("[FISHING] 开始异步预计算所有鱼类的分组概率...");
        
        try {
            Map<String, Map<String, Object>> allFishData = getAllFishData();
            if (allFishData.isEmpty()) {
                plugin.getLogger().severe("[FISHING] 没有找到鱼类数据，预计算终止");
                return;
            }
            
            cachedGroupProbabilities.clear();
            int calculated = 0;
            int failed = 0;
            
            for (String fishId : allFishData.keySet()) {
                try {
                    Map<String, LootGroupInfo> groupProbabilities = calculateFishGroupProbabilities(fishId);
                    cachedGroupProbabilities.put(fishId, groupProbabilities);
                    calculated++;
                } catch (Exception e) {
                    plugin.getLogger().severe("[FISHING] 计算鱼类 " + fishId + " 的分组概率失败: " + e.getMessage());
                    failed++;
                }
            }
            
            plugin.getLogger().info("[FISHING] 分组概率预计算完成：成功 " + calculated + "，失败 " + failed + "，总计 " + allFishData.size());
            
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 预计算分组概率时出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isProbabilityCalculating = false;
        }
    }
    
    /**
     * 计算单个鱼类的Loot分组概率
     */
    private Map<String, LootGroupInfo> calculateFishGroupProbabilities(String fishId) {
        if (!customFishingPresent) {
            plugin.getLogger().severe("[FISHING] CustomFishing插件未加载，无法计算分组概率");
            throw new RuntimeException("CustomFishing插件未加载");
        }
        
        if (fishId == null || fishId.trim().isEmpty()) {
            plugin.getLogger().severe("[FISHING] fishId为空，无法计算分组概率");
            throw new IllegalArgumentException("fishId不能为空");
        }
        
        Map<String, LootGroupInfo> groupProbabilities = new LinkedHashMap<>();
        boolean isDebug = plugin.getDebugManager().isDebugEnabled();
        
        try {
            LootGroupConfigManager groupManager = plugin.getLootGroupConfigManager();
            if (groupManager == null) {
                plugin.getLogger().severe("[FISHING] LootGroupConfigManager未初始化");
                throw new RuntimeException("LootGroupConfigManager未初始化");
            }
            
            // 调用CustomFishing新的分组概率API
            Class<?> customFishingClass = Class.forName("net.momirealms.customfishing.api.BukkitCustomFishingPlugin");
            Object customFishing = customFishingClass.getMethod("getInstance").invoke(null);
            
            if (customFishing == null) {
                plugin.getLogger().severe("[FISHING] CustomFishing实例为null");
                throw new RuntimeException("CustomFishing实例未找到");
            }
            
            // 获取LootGroupProbabilityCalculator
            java.lang.reflect.Method getCalculatorMethod = customFishing.getClass().getMethod("getLootGroupProbabilityCalculator");
                Object calculator = getCalculatorMethod.invoke(customFishing);
                
            if (calculator == null) {
                plugin.getLogger().severe("[FISHING] LootGroupProbabilityCalculator为null");
                throw new RuntimeException("LootGroupProbabilityCalculator未找到");
            }
            
            // 调用calculateGroupProbabilities获取分组概率
            java.lang.reflect.Method calculateMethod = calculator.getClass().getMethod("calculateGroupProbabilities", String.class);
                    @SuppressWarnings("unchecked")
            Map<String, Object> groupProbabilitiesMap = (Map<String, Object>) calculateMethod.invoke(calculator, fishId);
            
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 鱼类 " + fishId + " 从CustomFishing获取到 " + 
                    (groupProbabilitiesMap != null ? groupProbabilitiesMap.size() : 0) + " 个分组概率");
            }
            
            if (groupProbabilitiesMap != null && !groupProbabilitiesMap.isEmpty()) {
                // 直接遍历 CustomFishing 返回的分组（已经过滤掉辅助分组）
                for (Map.Entry<String, Object> entry : groupProbabilitiesMap.entrySet()) {
                    String groupId = entry.getKey();
                    Object groupProbInfo = entry.getValue();
                    
                    double probability = 0.0;
                    try {
                        java.lang.reflect.Method getProbMethod = groupProbInfo.getClass().getMethod("getProbability");
                        probability = (Double) getProbMethod.invoke(groupProbInfo);
                    } catch (Exception e) {
                        if (isDebug) {
                            plugin.getLogger().warning("[FISHING-DEBUG] 获取分组概率失败: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    
                    // 获取显示名称
                    String displayName = groupManager.getGroupDisplayName(groupId);
                    
                    // 如果没有配置显示名称，使用一些默认规则
                    if (!groupManager.hasGroupName(groupId)) {
                        if (groupId.contains("water")) {
                            displayName = "全部水域";
                        } else if (groupId.contains("lava")) {
                            displayName = "全部岩浆";
                        } else if (groupId.startsWith("loots_in_")) {
                            displayName = "全部区域";
                        }
                    }
                    
                    LootGroupInfo groupInfo = new LootGroupInfo(groupId, displayName, probability);
                    groupProbabilities.put(displayName, groupInfo);
                    
                    if (isDebug) {
                        plugin.getLogger().info("[FISHING-DEBUG] 分组 " + groupId + "(" + displayName + ") 概率: " + probability + "%");
                    }
                }
            } else {
                plugin.getLogger().severe("[FISHING] CustomFishing返回的分组概率Map为空，鱼类: " + fishId);
                throw new RuntimeException("分组概率数据为空");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 计算分组概率时出错: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("计算分组概率失败: " + fishId, e);
        }
        
        if (groupProbabilities.isEmpty()) {
            plugin.getLogger().severe("[FISHING] 鱼类 " + fishId + " 的分组概率计算结果为空");
            throw new RuntimeException("分组概率计算结果为空");
        }
        
        return groupProbabilities;
    }
    
    /**
     * 获取所有可钓的鱼类数据
     */
    public Map<String, Map<String, Object>> getAllFishData() {
        long now = System.currentTimeMillis();
        if (cachedFishData != null && now - lastCacheUpdate < CACHE_DURATION) {
            return new HashMap<>(cachedFishData);
        }
        
        if (!customFishingPresent) {
            plugin.getLogger().severe("[FISHING] CustomFishing插件未加载，无法获取鱼类数据");
            throw new RuntimeException("CustomFishing插件未加载");
        }
        
        Map<String, Map<String, Object>> fishData = new HashMap<>();
        
        try {
            Class<?> customFishingClass = Class.forName("net.momirealms.customfishing.api.BukkitCustomFishingPlugin");
            Object customFishing = customFishingClass.getMethod("getInstance").invoke(null);
            
            if (customFishing == null) {
                plugin.getLogger().severe("[FISHING] CustomFishing实例为null");
                throw new RuntimeException("CustomFishing实例未找到");
            }
        
            Object lootManager = customFishingClass.getMethod("getLootManager").invoke(customFishing);
            if (lootManager == null) {
                plugin.getLogger().severe("[FISHING] LootManager为null");
                throw new RuntimeException("LootManager未找到");
            }
        
            Object registeredLootsResult = lootManager.getClass().getMethod("getRegisteredLoots").invoke(lootManager);
            Collection<Object> allLoots = new ArrayList<>();
            
            if (registeredLootsResult instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> lootCollection = (Collection<Object>) registeredLootsResult;
                allLoots.addAll(lootCollection);
            } else if (registeredLootsResult instanceof Object[]) {
                Object[] lootArray = (Object[]) registeredLootsResult;
                allLoots.addAll(Arrays.asList(lootArray));
            } else if (registeredLootsResult != null) {
                plugin.getLogger().severe("[FISHING] 获取战利品时返回了意外的类型: " + registeredLootsResult.getClass().getSimpleName());
                throw new RuntimeException("战利品数据类型错误");
            } else {
                plugin.getLogger().severe("[FISHING] 获取战利品返回null");
                throw new RuntimeException("无法获取战利品数据");
            }
            
            Class<?> lootTypeClass = Class.forName("net.momirealms.customfishing.api.mechanic.loot.LootType");
            Object itemLootType = lootTypeClass.getField("ITEM").get(null);
            
            for (Object loot : allLoots) {
                Object lootType = loot.getClass().getMethod("type").invoke(loot);
                boolean disableStats = (Boolean) loot.getClass().getMethod("disableStats").invoke(loot);
                
                if (lootType.equals(itemLootType) && !disableStats) {
                    Map<String, Object> lootInfo = new HashMap<>();
                    String lootId = (String) loot.getClass().getMethod("id").invoke(loot);
                    String lootTypeName = lootType.toString();
                    
                    Object lootGroupResult = loot.getClass().getMethod("lootGroup").invoke(loot);
                    Collection<String> lootGroups = new ArrayList<>();
                    
                    if (lootGroupResult instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<String> groupCollection = (Collection<String>) lootGroupResult;
                        lootGroups.addAll(groupCollection);
                    } else if (lootGroupResult instanceof String[]) {
                        String[] groupArray = (String[]) lootGroupResult;
                        lootGroups.addAll(Arrays.asList(groupArray));
                    } else if (lootGroupResult instanceof String) {
                        lootGroups.add((String) lootGroupResult);
                    } else if (lootGroupResult != null) {
                        lootGroups.add(lootGroupResult.toString());
                    }
                    
                    lootInfo.put("id", lootId);
                    lootInfo.put("type", lootTypeName);
                    lootInfo.put("groups", lootGroups);
                    lootInfo.put("disableStats", disableStats);
                    
                    // 调试：输出鱼类的分组信息
                    if (plugin.getDebugManager().isDebugEnabled()) {
                        plugin.getLogger().info("[FISHING-DEBUG] 鱼类: " + lootId + ", 分组: " + lootGroups);
                    }
                    
                    fishData.put(lootId, lootInfo);
                }
                    }
        
            cachedFishData = fishData;
            lastCacheUpdate = System.currentTimeMillis();
        
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 获取鱼类数据时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取鱼类数据失败", e);
        }
        
        return fishData;
    }
    
    /**
     * CustomFishing重载后调用
     */
    public void onCustomFishingReload() {
        plugin.getLogger().info("[FISHING] 检测到CustomFishing重载，清除缓存并重新计算概率...");
        
        cachedFishData = null;
        cachedGroupProbabilities.clear();
        lastCacheUpdate = 0;
        
        if (customFishingPresent && !isProbabilityCalculating) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::preCalculateAllBiomeProbabilities, 20L);
        }
    }
    
    /**
     * 清除鱼类数据缓存
     */
    public void clearFishingCache() {
        this.cachedFishData = null;
        this.lastCacheUpdate = 0;
    }
    
    /**
     * 获取鱼类详细信息（使用分组概率）
     */
    public Map<String, String> getFishDetailInfo(String fishId, org.bukkit.Location biomeLocation) {
        return getFishDetailInfo(fishId, biomeLocation, true);
    }
    
    /**
     * 获取鱼类详细信息（使用分组概率）
     */
    public Map<String, String> getFishDetailInfo(String fishId, org.bukkit.Location biomeLocation, boolean showProbability) {
        Map<String, String> detailInfo = new HashMap<>();
        
        try {
            Map<String, Map<String, Object>> allFishData = getAllFishData();
            if (!allFishData.containsKey(fishId)) {
                plugin.getLogger().warning("[FISHING] 鱼类 " + fishId + " 不存在于数据中");
                detailInfo.put("biomes", "无法获取");
                detailInfo.put("rarity", "无法获取");
                detailInfo.put("environment", "无法获取");
                return detailInfo;
            }
            
            // 获取分组概率数据
            Map<String, LootGroupInfo> groupProbabilities = cachedGroupProbabilities.get(fishId);
            
            if (groupProbabilities == null || groupProbabilities.isEmpty()) {
                if (isProbabilityCalculating) {
                    detailInfo.put("biomes", "概率计算中...");
                } else {
                    try {
                        groupProbabilities = calculateFishGroupProbabilities(fishId);
                        cachedGroupProbabilities.put(fishId, groupProbabilities);
                    } catch (Exception e) {
                        plugin.getLogger().severe("[FISHING] 计算鱼类 " + fishId + " 分组概率失败: " + e.getMessage());
                        detailInfo.put("biomes", "无法获取");
                    }
                }
            }
            
            // 生成分组显示文本
            if (groupProbabilities != null && !groupProbabilities.isEmpty()) {
                StringBuilder groupDisplay = new StringBuilder();
                String firstProbability = null;
                
                for (Map.Entry<String, LootGroupInfo> entry : groupProbabilities.entrySet()) {
                    if (groupDisplay.length() > 0) {
                        groupDisplay.append("\n");
                    }
                    
                    String displayName = entry.getKey();
                    LootGroupInfo groupInfo = entry.getValue();
                    
                    groupDisplay.append(displayName);
                    
                    if (showProbability) {
                        groupDisplay.append("（").append(groupInfo.getFormattedProbability()).append("）");
                    }
                    
                    if (firstProbability == null) {
                        firstProbability = groupInfo.getFormattedProbability();
                    }
                }
                
                if (groupDisplay.length() > 0) {
                    detailInfo.put("biomes", groupDisplay.toString());
                    if (showProbability && firstProbability != null) {
                        detailInfo.put("probability", firstProbability);
                    }
                }
            }
            
            // 获取稀有度和环境信息
            Map<String, Object> fishInfo = allFishData.get(fishId);
            if (fishInfo != null) {
                String rarity = determineRarityFromGroups(fishInfo);
                if (rarity != null && !rarity.equals("普通")) {
                    detailInfo.put("rarity", rarity);
                }
                
                String environment = determineEnvironmentFromGroups(fishInfo);
                if (environment != null) {
                    detailInfo.put("environment", environment);
                }
            }
            
            return detailInfo;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 获取鱼类 " + fishId + " 详细信息失败: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put("biomes", "无法获取");
            errorInfo.put("rarity", "无法获取");
            errorInfo.put("environment", "无法获取");
            return errorInfo;
        }
    }
    
    private String determineRarityFromGroups(Map<String, Object> fishInfo) {
        try {
            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) fishInfo.get("groups");
            if (groups == null || groups.isEmpty()) {
                plugin.getLogger().warning("[FISHING] 鱼类分组信息为空，无法确定稀有度");
                return "无法获取";
            }
            
            // 从配置读取稀有度映射
            Map<String, String> rarityMapping = getRarityMappingFromConfig();
            
            // 遍历映射配置（按优先级）
            for (Map.Entry<String, String> entry : rarityMapping.entrySet()) {
                String configKey = entry.getKey();
                String rarityName = entry.getValue();
                
                // 规范化配置键（移除下划线和连字符）
                String normalizedKey = configKey.toLowerCase().replace("_", "").replace("-", "");
                
                // 检查鱼的分组中是否包含此关键词
                for (String group : groups) {
                    String normalizedGroup = group.toLowerCase().replace("_", "").replace("-", "");
                    if (normalizedGroup.contains(normalizedKey) || normalizedGroup.equals(normalizedKey)) {
                        return rarityName;
                    }
                }
            }
            
            plugin.getLogger().warning("[FISHING] 无法从分组中匹配稀有度: " + groups);
            return "无法获取";
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 确定稀有度时发生错误: " + e.getMessage());
            e.printStackTrace();
            return "无法获取";
        }
    }
    
    /**
     * 从配置文件读取稀有度映射
     */
    private Map<String, String> getRarityMappingFromConfig() {
        Map<String, String> mapping = new java.util.LinkedHashMap<>();
        try {
            org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigsManager().getFishingConfigManager().getConfig();
            org.bukkit.configuration.ConfigurationSection raritySection = config.getConfigurationSection("rarity_mapping");
            
            if (raritySection != null) {
                // 保持顺序，遍历所有配置的映射
                for (String key : raritySection.getKeys(false)) {
                    String value = raritySection.getString(key);
                    if (value != null) {
                        mapping.put(key, value);
                    }
                }
            }
            
            // 如果配置为空，使用默认映射
            if (mapping.isEmpty()) {
                mapping.put("iridium_star", "传奇");
                mapping.put("iridium", "传奇");
                mapping.put("golden_star", "史诗");
                mapping.put("golden", "史诗");
                mapping.put("silver_star", "稀有");
                mapping.put("silver", "稀有");
                mapping.put("no_star", "普通");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[FISHING] 读取稀有度映射配置失败: " + e.getMessage());
        }
        return mapping;
    }
    
    private String determineEnvironmentFromGroups(Map<String, Object> fishInfo) {
        try {
            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) fishInfo.get("groups");
            if (groups == null || groups.isEmpty()) {
                plugin.getLogger().warning("[FISHING] 鱼类分组信息为空，无法确定环境");
                return "无法获取";
            }
            
            // 从配置读取环境映射
            Map<String, String> environmentMapping = getEnvironmentMappingFromConfig();
            
            // 遍历映射配置
            for (Map.Entry<String, String> entry : environmentMapping.entrySet()) {
                String configKey = entry.getKey();
                String environmentName = entry.getValue();
                
                // 规范化配置键
                String normalizedKey = configKey.toLowerCase().replace("_", "").replace("-", "");
                
                // 检查鱼的分组中是否包含此关键词
                for (String group : groups) {
                    String normalizedGroup = group.toLowerCase().replace("_", "").replace("-", "");
                    if (normalizedGroup.contains(normalizedKey) || normalizedGroup.equals(normalizedKey)) {
                        return environmentName;
                    }
                }
            }
            
            plugin.getLogger().warning("[FISHING] 无法从分组中匹配环境: " + groups);
            return "无法获取";
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 确定环境时发生错误: " + e.getMessage());
            e.printStackTrace();
            return "无法获取";
        }
    }
    
    /**
     * 从配置文件读取环境映射
     */
    private Map<String, String> getEnvironmentMappingFromConfig() {
        Map<String, String> mapping = new java.util.LinkedHashMap<>();
        try {
            org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigsManager().getFishingConfigManager().getConfig();
            org.bukkit.configuration.ConfigurationSection envSection = config.getConfigurationSection("environment_mapping");
            
            if (envSection != null) {
                for (String key : envSection.getKeys(false)) {
                    String value = envSection.getString(key);
                    if (value != null) {
                        mapping.put(key, value);
                    }
                }
            }
            
            // 如果配置为空，使用默认映射
            if (mapping.isEmpty()) {
                mapping.put("lava", "岩浆中");
                mapping.put("lava_fish", "岩浆中");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[FISHING] 读取环境映射配置失败: " + e.getMessage());
        }
        return mapping;
    }
    
    /**
     * 生成钓鱼分类
     */
    public Category generateFishingCategory() {
        plugin.getLogger().info("[FISHING] 开始生成钓鱼分类...");
        
        try {
            Map<String, Map<String, Object>> allFishData = getAllFishData();
            
            if (allFishData.isEmpty()) {
                plugin.getLogger().severe("[FISHING] 无法获取鱼类数据，生成分类失败");
                throw new RuntimeException("无法获取鱼类数据");
            }
        
        // 从钓鱼配置文件读取钓鱼分类配置
        FileConfiguration config = plugin.getConfigsManager().getFishingConfigManager().getConfig();
        plugin.getLogger().info("[FISHING] 从钓鱼配置文件(fishing.yml)加载钓鱼分类设置");
        
        // 创建钓鱼分类
        Category fishingCategory = new Category();
        fishingCategory.setName("fishing");
        
        // 从配置文件读取分类物品
        CommonItem categoryItem;
        if (config.contains("display.category_item")) {
            categoryItem = plugin.getCommonItemManager().getCommonItemFromConfig(config, "display.category_item");
            plugin.getLogger().info("[FISHING] 从配置加载分类物品");
        } else {
            // 默认分类物品
            categoryItem = new CommonItem("FISHING_ROD");
            categoryItem.setName("&b钓鱼图鉴");
            List<String> categoryLore = new ArrayList<>();
            categoryLore.add("&7记录所有可钓到的鱼类");
            categoryLore.add("&7发现进度: &e%progress_bar%");
            categoryLore.add("&7已解锁: %unlocked%");
            categoryItem.setLore(categoryLore);
            plugin.getLogger().info("[FISHING] 使用默认分类物品");
        }
        fishingCategory.setCategoryItem(categoryItem);
        
        // 从配置文件读取默认解锁物品
        CommonItem unlockedItem;
        if (config.contains("display.discovery_unlocked")) {
            unlockedItem = plugin.getCommonItemManager().getCommonItemFromConfig(config, "display.discovery_unlocked");
            plugin.getLogger().info("[FISHING] 从配置加载已解锁物品样式");
        } else {
            // 默认解锁物品
            unlockedItem = new CommonItem("COD");
            unlockedItem.setName("%name%");
            List<String> unlockedLore = new ArrayList<>();
            unlockedLore.add("%description%");
            unlockedLore.add("");
            unlockedLore.add("#7289da发现于: #ffffff%date%");
            unlockedLore.add("#7289da捕获数量: #ffffff%amount%条");
            unlockedLore.add("#7289da最大尺寸: #ffffff%max_size%厘米");
            unlockedLore.add("");
            unlockedLore.add("#7289da可钓生物群系:");
            unlockedLore.add("#ffffff%biomes%");
            unlockedLore.add("#7289da稀有度: #ffffff%rarity%");
            unlockedItem.setLore(unlockedLore);
            plugin.getLogger().info("[FISHING] 使用默认已解锁物品样式");
        }
        fishingCategory.setDefaultLevelUnlockedItem(unlockedItem);
        
        // 从配置文件读取默认未解锁物品
        CommonItem blockedItem;
        if (config.contains("display.discovery_blocked")) {
            blockedItem = plugin.getCommonItemManager().getCommonItemFromConfig(config, "display.discovery_blocked");
            plugin.getLogger().info("[FISHING] 从配置加载未解锁物品样式");
        } else {
            // 默认未解锁物品
            blockedItem = new CommonItem("BARRIER");
            blockedItem.setName("#8c8c8c未知鱼类");
            List<String> blockedLore = new ArrayList<>();
            blockedLore.add("#8c8c8c继续钓鱼以发现这种鱼!");
            blockedLore.add("");
            blockedLore.add("#7289da可钓生物群系:");
            blockedLore.add("#ffffff%biomes%");
            blockedLore.add("#7289da稀有度: #ffffff%rarity%");
            blockedLore.add("#7289da钓鱼环境: #ffffff%environment%");
            blockedItem.setLore(blockedLore);
            plugin.getLogger().info("[FISHING] 使用默认未解锁物品样式");
        }
        fishingCategory.setDefaultLevelBlockedItem(blockedItem);
        
        ArrayList<Discovery> discoveries = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : allFishData.entrySet()) {
            String fishId = entry.getKey();
            
            // 创建发现项
            Discovery discovery = new Discovery();
            discovery.setId(fishId);
            discovery.setCategoryName("fishing");
            
            // 从CustomFishing获取物品信息
            Map<String, Object> itemInfo = getCustomFishingItemInfo(fishId);
            
            String displayName = (String) itemInfo.get("displayName");
            if (displayName != null && !displayName.trim().isEmpty()) {
                discovery.setName(displayName);
            } else {
                discovery.setName(fishId);
            }
            
            // 设置描述
            @SuppressWarnings("unchecked")
            List<String> customLore = (List<String>) itemInfo.get("lore");
            if (customLore != null && !customLore.isEmpty()) {
                List<String> cleanDescription = customLore.stream()
                    .map(line -> line.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "")
                                    .replaceAll("<[^>]*>", "")
                                    .replaceAll("#[0-9a-fA-F]{6}", "")
                                    .trim())
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
                
                if (!cleanDescription.isEmpty()) {
                    discovery.setDescription(cleanDescription);
            } else {
                    discovery.setDescription(Arrays.asList("CustomFishing物品: " + fishId));
                }
                } else {
                discovery.setDescription(Arrays.asList("从CustomFishing获取的鱼类"));
            }
            
            // 设置发现条件
            DiscoveredOn discoveredOn = new DiscoveredOn(DiscoveredOn.DiscoveredOnType.FISHING);
            discoveredOn.setCustomFishingId(fishId);
            discovery.setDiscoveredOn(discoveredOn);
            
            discoveries.add(discovery);
        }
        
        fishingCategory.setDiscoveries(discoveries);
        
        // 从配置文件读取奖励
        if (config.contains("rewards.per_discovery")) {
            List<String> rewardsPerDiscovery = config.getStringList("rewards.per_discovery");
            fishingCategory.setDefaultRewardsPerDiscovery(new ArrayList<>(rewardsPerDiscovery));
            plugin.getLogger().info("[FISHING] 从配置加载发现奖励");
        }
        
        if (config.contains("rewards.all_discoveries")) {
            List<String> rewardsAllDiscoveries = config.getStringList("rewards.all_discoveries");
            fishingCategory.setDefaultRewardsAllDiscoveries(new ArrayList<>(rewardsAllDiscoveries));
            plugin.getLogger().info("[FISHING] 从配置加载全部发现奖励");
        }
        
            plugin.getLogger().info("[FISHING] 成功生成钓鱼分类，包含 " + discoveries.size() + " 个发现项");
            return fishingCategory;
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 生成钓鱼分类时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("生成钓鱼分类失败", e);
        }
    }
    
    /**
     * 获取CustomFishing物品的显示名称
     */
    public String getCustomFishingDisplayName(String fishId, org.bukkit.entity.Player player) {
        try {
            Map<String, Object> itemInfo = getCustomFishingItemInfo(fishId);
            String displayName = (String) itemInfo.get("displayName");
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName;
            }
            plugin.getLogger().warning("[FISHING] 鱼类 " + fishId + " 没有配置显示名称");
            return "无法获取";
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 获取鱼类 " + fishId + " 显示名称失败: " + e.getMessage());
            return "无法获取";
        }
    }
    
    /**
     * 获取CustomFishing物品的Lore
     */
    public List<String> getCustomFishingLore(String fishId, org.bukkit.entity.Player player) {
        try {
            Map<String, Object> itemInfo = getCustomFishingItemInfo(fishId);
            @SuppressWarnings("unchecked")
            List<String> lore = (List<String>) itemInfo.get("lore");
            if (lore != null && !lore.isEmpty()) {
                return new ArrayList<>(lore);
            }
            plugin.getLogger().warning("[FISHING] 鱼类 " + fishId + " 没有配置Lore");
            List<String> errorLore = new ArrayList<>();
            errorLore.add("§c无法获取物品描述");
            return errorLore;
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 获取鱼类 " + fishId + " Lore失败: " + e.getMessage());
            List<String> errorLore = new ArrayList<>();
            errorLore.add("§c无法获取物品描述");
            return errorLore;
        }
    }
    
    /**
     * 获取玩家钓到某种鱼的数量
     */
    public int getFishAmount(org.bukkit.entity.Player player, String fishId) {
        boolean isDebug = plugin.getDebugManager().isDebugEnabled();
        try {
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 获取鱼类数量 - 玩家: " + player.getName() + ", fishId: " + fishId);
            }
            
            // 使用新版CustomFishing API
            Class<?> customFishingClass = Class.forName("net.momirealms.customfishing.api.BukkitCustomFishingPlugin");
            Object customFishing = customFishingClass.getMethod("getInstance").invoke(null);
            if (customFishing == null) {
                plugin.getLogger().severe("[FISHING] CustomFishing实例为null，无法获取鱼类数量");
                throw new RuntimeException("CustomFishing实例未找到");
            }
            
            // 获取StorageManager
            Object storageManager = customFishing.getClass().getMethod("getStorageManager").invoke(customFishing);
            if (storageManager == null) {
                plugin.getLogger().severe("[FISHING] StorageManager为null，无法获取鱼类数量");
                throw new RuntimeException("StorageManager未找到");
            }
            
            // 通过UUID获取在线玩家数据 (返回Optional<UserData>)
            java.util.Optional<?> userDataOptional = (java.util.Optional<?>) storageManager.getClass()
                .getMethod("getOnlineUser", java.util.UUID.class)
                .invoke(storageManager, player.getUniqueId());
            
            if (userDataOptional == null || !userDataOptional.isPresent()) {
                plugin.getLogger().severe("[FISHING] 玩家 " + player.getName() + " 的UserData不存在，无法获取鱼类数量");
                throw new RuntimeException("玩家数据未找到");
            }
            
            // 获取UserData对象
            Object userData = userDataOptional.get();
            
            // 获取statistics对象
            Object statistics = userData.getClass().getMethod("statistics").invoke(userData);
            if (statistics == null) {
                plugin.getLogger().severe("[FISHING] Statistics对象为null，无法获取鱼类数量");
                throw new RuntimeException("Statistics对象未找到");
            }
            
            // 获取数量
            Integer amount = (Integer) statistics.getClass().getMethod("getAmount", String.class).invoke(statistics, fishId);
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 获取到的数量: " + (amount != null ? amount : 0));
            }
            return amount != null ? amount : 0;
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 获取鱼类 " + fishId + " 数量失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取鱼类数量失败", e);
        }
    }
    
    /**
     * 获取玩家钓到某种鱼的最大尺寸
     */
    public double getFishMaxSize(org.bukkit.entity.Player player, String fishId) {
        boolean isDebug = plugin.getDebugManager().isDebugEnabled();
        try {
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 获取鱼类最大尺寸 - 玩家: " + player.getName() + ", fishId: " + fishId);
            }
            
            // 使用新版CustomFishing API
            Class<?> customFishingClass = Class.forName("net.momirealms.customfishing.api.BukkitCustomFishingPlugin");
            Object customFishing = customFishingClass.getMethod("getInstance").invoke(null);
            if (customFishing == null) {
                plugin.getLogger().severe("[FISHING] CustomFishing实例为null，无法获取鱼类最大尺寸");
                throw new RuntimeException("CustomFishing实例未找到");
            }
            
            // 获取StorageManager
            Object storageManager = customFishing.getClass().getMethod("getStorageManager").invoke(customFishing);
            if (storageManager == null) {
                plugin.getLogger().severe("[FISHING] StorageManager为null，无法获取鱼类最大尺寸");
                throw new RuntimeException("StorageManager未找到");
            }
            
            // 通过UUID获取在线玩家数据 (返回Optional<UserData>)
            java.util.Optional<?> userDataOptional = (java.util.Optional<?>) storageManager.getClass()
                .getMethod("getOnlineUser", java.util.UUID.class)
                .invoke(storageManager, player.getUniqueId());
            
            if (userDataOptional == null || !userDataOptional.isPresent()) {
                plugin.getLogger().severe("[FISHING] 玩家 " + player.getName() + " 的UserData不存在，无法获取鱼类最大尺寸");
                throw new RuntimeException("玩家数据未找到");
            }
            
            // 获取UserData对象
            Object userData = userDataOptional.get();
            
            // 获取statistics对象
            Object statistics = userData.getClass().getMethod("statistics").invoke(userData);
            if (statistics == null) {
                plugin.getLogger().severe("[FISHING] Statistics对象为null，无法获取鱼类最大尺寸");
                throw new RuntimeException("Statistics对象未找到");
            }
            
            // 获取最大尺寸
            Float maxSize = (Float) statistics.getClass().getMethod("getMaxSize", String.class).invoke(statistics, fishId);
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 获取到的最大尺寸: " + (maxSize != null ? maxSize : 0.0));
            }
            return maxSize != null ? maxSize : 0.0;
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 获取鱼类 " + fishId + " 最大尺寸失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取鱼类最大尺寸失败", e);
        }
    }
    
    /**
     * 处理物品Lore（添加概率等信息）
     */
    public List<String> processItemLore(List<String> originalLore, String fishId, org.bukkit.entity.Player player, boolean showProbability) {
        if (originalLore == null) {
            originalLore = new ArrayList<>();
        }
        
        List<String> processedLore = new ArrayList<>();
        
        // 获取鱼类详细信息用于占位符替换（使用玩家当前位置，如果玩家为null则使用null）
        org.bukkit.Location location = player != null ? player.getLocation() : null;
        Map<String, String> detailInfo = getFishDetailInfo(fishId, location, showProbability); // 根据showProbability参数决定是否显示概率
        
        // 处理每一行lore，替换占位符
        for (String line : originalLore) {
            String processedLine = line;
            
            // 替换 %biomes% 占位符
            if (processedLine.contains("%biomes%")) {
                String biomes = detailInfo.getOrDefault("biomes", "无法获取");
                processedLine = processedLine.replace("%biomes%", biomes);
            }
            
            // 替换 %rarity% 占位符
            if (processedLine.contains("%rarity%")) {
                String rarity = detailInfo.getOrDefault("rarity", "无法获取");
                processedLine = processedLine.replace("%rarity%", rarity);
            }
            
            // 替换 %environment% 占位符
            if (processedLine.contains("%environment%")) {
                String environment = detailInfo.getOrDefault("environment", "无法获取");
                processedLine = processedLine.replace("%environment%", environment);
            }
            
            processedLore.add(processedLine);
        }
        
        if (showProbability) {
            // 添加分组概率信息
            Map<String, LootGroupInfo> groupProbabilities = calculateFishGroupProbabilities(fishId);
            if (groupProbabilities != null && !groupProbabilities.isEmpty()) {
                processedLore.add("");
                processedLore.add("#7289da可钓分组:");
                
                for (Map.Entry<String, LootGroupInfo> entry : groupProbabilities.entrySet()) {
                    String displayName = entry.getKey();
                    LootGroupInfo groupInfo = entry.getValue();
                    processedLore.add("#ffffff  " + displayName + ": #a8e063" + groupInfo.getFormattedProbability());
                }
            }
        }
        
        return processedLore;
    }
    
    /**
     * 获取CustomFishing物品的基本信息
     * 通过API获取物品的显示名称和Lore
     */
    public Map<String, Object> getCustomFishingItemInfo(String fishId) {
        Map<String, Object> itemInfo = new HashMap<>();
        
        if (!customFishingPresent) {
            plugin.getLogger().severe("[FISHING] CustomFishing插件未加载，无法获取物品信息");
            throw new RuntimeException("CustomFishing插件未加载");
        }
        
        if (fishId == null || fishId.trim().isEmpty()) {
            plugin.getLogger().severe("[FISHING] fishId为空，无法获取物品信息");
            throw new IllegalArgumentException("fishId不能为空");
        }
        
        try {
            Class<?> customFishingClass = Class.forName("net.momirealms.customfishing.api.BukkitCustomFishingPlugin");
            Object customFishing = customFishingClass.getMethod("getInstance").invoke(null);
            
            if (customFishing == null) {
                plugin.getLogger().severe("[FISHING] CustomFishing实例为null");
                throw new RuntimeException("CustomFishing实例未找到");
            }
            
            Object lootManager = customFishing.getClass().getMethod("getLootManager").invoke(customFishing);
            if (lootManager == null) {
                plugin.getLogger().severe("[FISHING] LootManager为null");
                throw new RuntimeException("LootManager未找到");
            }
            
            // 获取Loot对象
            Object lootOptional = lootManager.getClass().getMethod("getLoot", String.class).invoke(lootManager, fishId);
            
            if (lootOptional != null) {
                Boolean isPresent = (Boolean) lootOptional.getClass().getMethod("isPresent").invoke(lootOptional);
                if (isPresent) {
                    Object loot = lootOptional.getClass().getMethod("get").invoke(lootOptional);
                    
                    // 获取显示名称
                    String displayName = (String) loot.getClass().getMethod("nick").invoke(loot);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        itemInfo.put("displayName", displayName);
                    }
                    
                    // 获取Lore
                    @SuppressWarnings("unchecked")
                    List<String> lore = (List<String>) loot.getClass().getMethod("lore").invoke(loot);
                    if (lore != null && !lore.isEmpty()) {
                        itemInfo.put("lore", lore);
                    }
                    
                    if (itemInfo.isEmpty()) {
                        plugin.getLogger().warning("[FISHING] 鱼类 " + fishId + " 没有配置displayName或lore");
                    }
                    
                    return itemInfo;
                } else {
                    plugin.getLogger().severe("[FISHING] 无法找到鱼类 " + fishId);
                    throw new RuntimeException("鱼类不存在: " + fishId);
                }
            } else {
                plugin.getLogger().severe("[FISHING] getLoot返回null，鱼类: " + fishId);
                throw new RuntimeException("无法获取鱼类信息: " + fishId);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            plugin.getLogger().severe("[FISHING] 通过API获取鱼类 " + fishId + " 信息时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("API获取失败: " + fishId, e);
        }
    }
    
    /**
     * Loot分组信息类
     */
    private static class LootGroupInfo {
        @SuppressWarnings("unused")
        private final String groupId;
        @SuppressWarnings("unused")
        private final String displayName;
        private double probability;
        
        public LootGroupInfo(String groupId, String displayName, double probability) {
            this.groupId = groupId;
            this.displayName = displayName;
            this.probability = probability;
        }
        
        public String getFormattedProbability() {
            if (probability >= 10) {
                return String.format("%.1f%%", probability);
            } else if (probability >= 1) {
                return String.format("%.2f%%", probability);
            } else {
                return String.format("%.3f%%", probability);
            }
        }
    }
}

