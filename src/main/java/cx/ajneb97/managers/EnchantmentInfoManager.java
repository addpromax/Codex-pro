package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.config.EnchantmentsConfigManager;
import cx.ajneb97.model.enchantment.EnchantmentInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

// 添加Aiyatsbus的导入
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * 管理附魔信息，包括从Aiyatsbus获取信息和处理变量替换
 */
public class EnchantmentInfoManager {
    
    private Codex plugin;
    private boolean aiyatsbusPresent;
    private Map<String, EnchantmentInfo> enchantmentInfoCache;
    
    // 物品类型映射表，用于将Material转换为更友好的名称
    private static final Map<String, String> ITEM_TYPE_NAMES = new HashMap<>();
    
    static {
        // 初始化物品类型映射表
        ITEM_TYPE_NAMES.put("DIAMOND_SWORD", "钻石剑");
        ITEM_TYPE_NAMES.put("IRON_SWORD", "铁剑");
        ITEM_TYPE_NAMES.put("GOLDEN_SWORD", "金剑");
        ITEM_TYPE_NAMES.put("STONE_SWORD", "石剑");
        ITEM_TYPE_NAMES.put("WOODEN_SWORD", "木剑");
        ITEM_TYPE_NAMES.put("NETHERITE_SWORD", "下界合金剑");
        
        ITEM_TYPE_NAMES.put("DIAMOND_AXE", "钻石斧");
        ITEM_TYPE_NAMES.put("IRON_AXE", "铁斧");
        ITEM_TYPE_NAMES.put("GOLDEN_AXE", "金斧");
        ITEM_TYPE_NAMES.put("STONE_AXE", "石斧");
        ITEM_TYPE_NAMES.put("WOODEN_AXE", "木斧");
        ITEM_TYPE_NAMES.put("NETHERITE_AXE", "下界合金斧");
        
        ITEM_TYPE_NAMES.put("DIAMOND_PICKAXE", "钻石镐");
        ITEM_TYPE_NAMES.put("IRON_PICKAXE", "铁镐");
        ITEM_TYPE_NAMES.put("GOLDEN_PICKAXE", "金镐");
        ITEM_TYPE_NAMES.put("STONE_PICKAXE", "石镐");
        ITEM_TYPE_NAMES.put("WOODEN_PICKAXE", "木镐");
        ITEM_TYPE_NAMES.put("NETHERITE_PICKAXE", "下界合金镐");
        
        ITEM_TYPE_NAMES.put("DIAMOND_SHOVEL", "钻石锹");
        ITEM_TYPE_NAMES.put("IRON_SHOVEL", "铁锹");
        ITEM_TYPE_NAMES.put("GOLDEN_SHOVEL", "金锹");
        ITEM_TYPE_NAMES.put("STONE_SHOVEL", "石锹");
        ITEM_TYPE_NAMES.put("WOODEN_SHOVEL", "木锹");
        ITEM_TYPE_NAMES.put("NETHERITE_SHOVEL", "下界合金锹");
        
        ITEM_TYPE_NAMES.put("DIAMOND_HOE", "钻石锄");
        ITEM_TYPE_NAMES.put("IRON_HOE", "铁锄");
        ITEM_TYPE_NAMES.put("GOLDEN_HOE", "金锄");
        ITEM_TYPE_NAMES.put("STONE_HOE", "石锄");
        ITEM_TYPE_NAMES.put("WOODEN_HOE", "木锄");
        ITEM_TYPE_NAMES.put("NETHERITE_HOE", "下界合金锄");
        
        ITEM_TYPE_NAMES.put("LEATHER_HELMET", "皮革头盔");
        ITEM_TYPE_NAMES.put("CHAINMAIL_HELMET", "锁链头盔");
        ITEM_TYPE_NAMES.put("IRON_HELMET", "铁头盔");
        ITEM_TYPE_NAMES.put("GOLDEN_HELMET", "金头盔");
        ITEM_TYPE_NAMES.put("DIAMOND_HELMET", "钻石头盔");
        ITEM_TYPE_NAMES.put("NETHERITE_HELMET", "下界合金头盔");
        ITEM_TYPE_NAMES.put("TURTLE_HELMET", "海龟壳");
        
        ITEM_TYPE_NAMES.put("LEATHER_CHESTPLATE", "皮革胸甲");
        ITEM_TYPE_NAMES.put("CHAINMAIL_CHESTPLATE", "锁链胸甲");
        ITEM_TYPE_NAMES.put("IRON_CHESTPLATE", "铁胸甲");
        ITEM_TYPE_NAMES.put("GOLDEN_CHESTPLATE", "金胸甲");
        ITEM_TYPE_NAMES.put("DIAMOND_CHESTPLATE", "钻石胸甲");
        ITEM_TYPE_NAMES.put("NETHERITE_CHESTPLATE", "下界合金胸甲");
        
        ITEM_TYPE_NAMES.put("LEATHER_LEGGINGS", "皮革护腿");
        ITEM_TYPE_NAMES.put("CHAINMAIL_LEGGINGS", "锁链护腿");
        ITEM_TYPE_NAMES.put("IRON_LEGGINGS", "铁护腿");
        ITEM_TYPE_NAMES.put("GOLDEN_LEGGINGS", "金护腿");
        ITEM_TYPE_NAMES.put("DIAMOND_LEGGINGS", "钻石护腿");
        ITEM_TYPE_NAMES.put("NETHERITE_LEGGINGS", "下界合金护腿");
        
        ITEM_TYPE_NAMES.put("LEATHER_BOOTS", "皮革靴子");
        ITEM_TYPE_NAMES.put("CHAINMAIL_BOOTS", "锁链靴子");
        ITEM_TYPE_NAMES.put("IRON_BOOTS", "铁靴子");
        ITEM_TYPE_NAMES.put("GOLDEN_BOOTS", "金靴子");
        ITEM_TYPE_NAMES.put("DIAMOND_BOOTS", "钻石靴子");
        ITEM_TYPE_NAMES.put("NETHERITE_BOOTS", "下界合金靴子");
        
        ITEM_TYPE_NAMES.put("BOW", "弓");
        ITEM_TYPE_NAMES.put("CROSSBOW", "弩");
        ITEM_TYPE_NAMES.put("TRIDENT", "三叉戟");
        ITEM_TYPE_NAMES.put("FISHING_ROD", "钓鱼竿");
        ITEM_TYPE_NAMES.put("ELYTRA", "鞘翅");
        ITEM_TYPE_NAMES.put("SHIELD", "盾牌");
        ITEM_TYPE_NAMES.put("SHEARS", "剪刀");
        ITEM_TYPE_NAMES.put("FLINT_AND_STEEL", "打火石");
        ITEM_TYPE_NAMES.put("CARROT_ON_A_STICK", "胡萝卜钓竿");
        ITEM_TYPE_NAMES.put("WARPED_FUNGUS_ON_A_STICK", "诡异菌钓竿");
        ITEM_TYPE_NAMES.put("BOOK", "附魔书");
    }
    
    public EnchantmentInfoManager(Codex plugin) {
        this.plugin = plugin;
        this.aiyatsbusPresent = Bukkit.getPluginManager().getPlugin("Aiyatsbus") != null;
        this.enchantmentInfoCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取附魔信息
     * @param enchantmentId 附魔ID
     * @return 附魔信息
     */
    public EnchantmentInfo getEnchantmentInfo(String enchantmentId) {
        // 调试前缀，方便日志筛选
        final String DEBUG_PREFIX = "[CODEX-DEBUG] 获取附魔信息: ";
        
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info(DEBUG_PREFIX + "开始获取附魔信息: " + enchantmentId);
        }
        
        // 检查缓存
        if (enchantmentInfoCache.containsKey(enchantmentId)) {
            if (plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info(DEBUG_PREFIX + "从缓存获取到附魔信息: " + enchantmentId);
            }
            return enchantmentInfoCache.get(enchantmentId);
        }
        
        // 尝试不同格式的ID
        List<String> possibleIds = new ArrayList<>();
        possibleIds.add(enchantmentId); // 原始ID
        
        // 添加冒号和下划线转换后的ID，保持原始格式不变
        if (enchantmentId.contains(":")) {
            // 对于原版附魔，有时候会使用无命名空间的形式
            if (enchantmentId.startsWith("minecraft:")) {
                possibleIds.add(enchantmentId.substring("minecraft:".length()));
            }
        } else if (enchantmentId.contains("_")) {
            String[] parts = enchantmentId.split("_", 2);
            if (parts.length == 2) {
                possibleIds.add(parts[0] + ":" + parts[1]); // 下划线转冒号，用于兼容旧数据
            }
        } else if (!enchantmentId.contains(":")) {
            // 没有命名空间的情况，添加minecraft:前缀
            possibleIds.add("minecraft:" + enchantmentId);
        }
        
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info(DEBUG_PREFIX + "尝试的ID格式: " + possibleIds);
        }
        
        // 尝试从AiyatsbusAPI获取中文名称
        if (aiyatsbusPresent && plugin.getConfigsManager().getEnchantmentsConfigManager().fetchAiyatsbusInfo()) {
            EnchantmentInfo infoWithChineseName = tryGetChineseNameFromAiyatsbus(enchantmentId, possibleIds, DEBUG_PREFIX);
            if (infoWithChineseName != null) {
                return infoWithChineseName;
            }
        }
        
        EnchantmentInfo info = createEnchantmentInfo(enchantmentId);
        enchantmentInfoCache.put(enchantmentId, info);
        return info;
    }
    
    /**
     * 尝试从Aiyatsbus获取中文名称
     * @param originalEnchantId 原始附魔ID
     * @param possibleIds 可能的ID格式列表
     * @param debugPrefix 调试日志前缀
     * @return 如果成功获取中文名称，返回附魔信息；否则返回null
     */
    private EnchantmentInfo tryGetChineseNameFromAiyatsbus(String originalEnchantId, List<String> possibleIds, String debugPrefix) {
        // 尝试强制启用调试模式
        boolean oldDebugSetting = cx.ajneb97.api.AiyatsbusAPI.isDebug();
        cx.ajneb97.api.AiyatsbusAPI.setDebug(true);
        
        for (String id : possibleIds) {
            try {
                if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info(debugPrefix + "尝试从AiyatsbusAPI获取附魔中文名称: " + id);
                }
                
                String chineseName = cx.ajneb97.api.AiyatsbusAPI.getEnchantmentName(id);
                if (chineseName != null && !chineseName.isEmpty()) {
                    if (plugin.getDebugManager().isDebugEnabled()) {
                        plugin.getLogger().info(debugPrefix + "成功获取附魔中文名称: " + id + " -> " + chineseName);
                    }
                    
                    // 创建附魔信息并设置中文名称
                    EnchantmentInfo info = createEnchantmentInfo(originalEnchantId);
                    info.setName(chineseName);
                    
                    // 移除保存到配置文件的逻辑
                    
                    // 更新缓存
                    enchantmentInfoCache.put(originalEnchantId, info);
                    
                    return info;
                } else if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info(debugPrefix + "AiyatsbusAPI返回的名称为空: " + id);
                }
            } catch (Exception e) {
                if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().warning(debugPrefix + "从AiyatsbusAPI获取附魔中文名称时出错: " + id + " -> " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // 尝试直接通过反射获取Aiyatsbus附魔实例
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info(debugPrefix + "常规方法未获取到中文名称，尝试直接从Aiyatsbus获取附魔实例");
        }
        
        try {
            // 获取Aiyatsbus API实例
            Class<?> aiyatsbusClass = Class.forName("cc.polarastrum.aiyatsbus.core.Aiyatsbus");
            Object apiInstance = aiyatsbusClass.getMethod("api").invoke(null);
            
            // 获取附魔管理器
            Object enchantManager = apiInstance.getClass().getMethod("getEnchantmentManager").invoke(apiInstance);
            
            // 打印所有附魔以便调试
            if (plugin.getDebugManager().isDebugEnabled()) {
                try {
                    Method getEnchantsMethod = enchantManager.getClass().getMethod("getEnchants");
                    Object enchantsMap = getEnchantsMethod.invoke(enchantManager);
                    if (enchantsMap instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) enchantsMap;
                        plugin.getLogger().info(debugPrefix + "Aiyatsbus注册表中共有 " + map.size() + " 个附魔");
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            Object key = entry.getKey();
                            Object enchant = entry.getValue();
                            plugin.getLogger().info(debugPrefix + "附魔: " + key + " -> " + 
                                                   (enchant != null ? enchant.getClass().getName() : "null"));
                            
                            if (enchant != null) {
                                try {
                                    // 获取附魔基础数据
                                    Method getBasicDataMethod = enchant.getClass().getMethod("getBasicData");
                                    Object basicData = getBasicDataMethod.invoke(enchant);
                                    
                                    // 获取附魔名称
                                    Method getNameMethod = basicData.getClass().getMethod("getName");
                                    String name = (String) getNameMethod.invoke(basicData);
                                    
                                    plugin.getLogger().info(debugPrefix + "附魔名称: " + name);
                                } catch (Exception e) {
                                    plugin.getLogger().info(debugPrefix + "获取附魔详情失败: " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning(debugPrefix + "获取附魔列表失败: " + e.getMessage());
                }
            }
            
            // 尝试查找对应的附魔
            for (String id : possibleIds) {
                try {
                    // 尝试用getEnchant(String)方法获取
                    Method getEnchantMethod = enchantManager.getClass().getMethod("getEnchant", String.class);
                    Object enchant = getEnchantMethod.invoke(enchantManager, id);
                    
                    if (enchant != null) {
                        plugin.getLogger().info(debugPrefix + "成功找到附魔: " + id);
                        
                        // 获取基础数据
                        Method getBasicDataMethod = enchant.getClass().getMethod("getBasicData");
                        Object basicData = getBasicDataMethod.invoke(enchant);
                        
                        // 获取附魔名称
                        Method getNameMethod = basicData.getClass().getMethod("getName");
                        String name = (String) getNameMethod.invoke(basicData);
                        
                        if (name != null && !name.isEmpty()) {
                            plugin.getLogger().info(debugPrefix + "成功获取附魔中文名称: " + id + " -> " + name);
                            
                            // 创建附魔信息并设置中文名称
                            EnchantmentInfo info = createEnchantmentInfo(originalEnchantId);
                            info.setName(name);
                            
                            // 更新缓存
                            enchantmentInfoCache.put(originalEnchantId, info);
                            
                            return info;
                        }
                    }
                } catch (Exception e) {
                    if (plugin.getDebugManager().isDebugEnabled()) {
                        plugin.getLogger().warning(debugPrefix + "直接获取附魔实例失败: " + id + " -> " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().warning(debugPrefix + "反射调用Aiyatsbus API失败: " + e.getMessage());
            }
        } finally {
            // 恢复原来的调试设置
            cx.ajneb97.api.AiyatsbusAPI.setDebug(oldDebugSetting);
        }
        
        return null;
    }
    
    /**
     * 创建附魔信息
     * @param enchantmentId 附魔ID
     * @return 附魔信息
     */
    private EnchantmentInfo createEnchantmentInfo(String enchantmentId) {
        EnchantmentInfo info = new EnchantmentInfo(enchantmentId);
        
        // 尝试获取附魔对象
        Enchantment enchantment = getEnchantment(enchantmentId);
        if (enchantment == null) {
            // 如果找不到附魔，设置默认值
            info.setName(formatEnchantmentName(info.getKey()));
            info.setMaxLevel(1);
            info.setRarity("unknown");
            info.addDescription("&7未知附魔");
            return info;
        }
        
        // 设置基本信息
        info.setName(formatEnchantmentName(info.getKey()));
        info.setMaxLevel(enchantment.getMaxLevel());
        
        // 设置稀有度
        try {
            // 尝试通过反射获取稀有度，因为不同版本的Bukkit API可能不同
            try {
                java.lang.reflect.Method getRarityMethod = enchantment.getClass().getMethod("getRarity");
                Object rarityEnum = getRarityMethod.invoke(enchantment);
                if (rarityEnum != null) {
                    java.lang.reflect.Method nameMethod = rarityEnum.getClass().getMethod("name");
                    String rarityName = (String) nameMethod.invoke(rarityEnum);
                    info.setRarity(rarityName.toLowerCase());
                } else {
                    info.setRarity("common");
                }
            } catch (Exception e) {
                // 如果反射失败，使用默认值
                info.setRarity("common");
            }
        } catch (Exception e) {
            info.setRarity("unknown");
        }
        
        // 获取适用物品
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) {
                try {
                    if (enchantment.canEnchantItem(new org.bukkit.inventory.ItemStack(material))) {
                        String itemName = getItemTypeName(material.name());
                        info.addApplicableItem(itemName);
                    }
                } catch (Exception ignored) {
                    // 忽略可能的错误
                }
            }
        }
        
        // 获取冲突附魔
        for (Enchantment other : Enchantment.values()) {
            if (!enchantment.equals(other) && enchantment.conflictsWith(other)) {
                String otherName = formatEnchantmentName(other.getKey().getKey());
                info.addConflict(otherName);
            }
        }
        
        // 如果是Aiyatsbus附魔，尝试获取更多信息
        if (aiyatsbusPresent && plugin.getConfigsManager().getEnchantmentsConfigManager().fetchAiyatsbusInfo()) {
            try {
                fetchAiyatsbusEnchantmentInfo(info, enchantment);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "无法获取Aiyatsbus附魔信息: " + e.getMessage(), e);
            }
        }
        
        // 如果没有描述，添加默认描述
        if (info.getDescription().isEmpty()) {
            info.addDescription("&7" + info.getName() + " 附魔");
            info.addDescription("&7最大等级: &e" + info.getMaxLevel());
        }
        
        return info;
    }
    
    /**
     * 从Aiyatsbus获取附魔信息
     * @param info 附魔信息对象
     * @param enchantment 附魔对象
     */
    // 用于跟踪已尝试但失败的附魔ID，避免重复输出警告
    private static final Set<String> failedEnchantmentIds = new HashSet<>();
    
    // 添加方法缓存，避免重复反射获取相同的方法
    private Method apiMethod;
    private Method getEnchantmentManagerMethod;
    private Method getEnchantStringMethod;
    private Method getEnchantKeyMethod;
    private Method getBasicDataMethod;
    private Method getNameMethod;
    private Method getMaxLevelMethod;
    private Method getDisplayerMethod;
    private Method getGeneralDescriptionMethod;
    private Method getByNameMethod; // 新增：用于通过名称查找附魔

    /**
     * 从Aiyatsbus获取附魔信息
     */
    private void fetchAiyatsbusEnchantmentInfo(EnchantmentInfo info, Enchantment enchantment) {
        try {
            String enchantId = enchantment.getKey().toString();
            String keyOnly = enchantment.getKey().getKey();
            String upperName = enchantment.getKey().getKey().toUpperCase();
            String lowerName = enchantment.getKey().getKey().toLowerCase();
            String nsKey = enchantment.getKey().getNamespace() + ":" + keyOnly;
            
            // 尝试从Aiyatsbus插件获取中文名称
            Object aiyatsbusPlugin = Bukkit.getPluginManager().getPlugin("Aiyatsbus");
            if (aiyatsbusPlugin == null) {
                useDefaultName(info, enchantment);
                return;
            }

            try {
                initReflectionMethods();
                Object apiInstance = apiMethod.invoke(null);
                if (apiInstance == null) {
                    useDefaultName(info, enchantment);
                    return;
                }
                Object enchantManager = getEnchantmentManagerMethod.invoke(apiInstance);
                if (enchantManager == null) {
                    useDefaultName(info, enchantment);
                    return;
                }
                
                // 调试：打印所有Aiyatsbus注册key
                if (plugin.getDebugManager().isDebugEnabled()) {
                    try {
                        Method getEnchantsMethod = enchantManager.getClass().getMethod("getEnchants");
                        Object enchantsMap = getEnchantsMethod.invoke(enchantManager);
                        if (enchantsMap instanceof Map) {
                            Map<?, ?> enchantMap = (Map<?, ?>) enchantsMap;
                            for (Object key : enchantMap.keySet()) {
                                plugin.getLogger().info("[Aiyatsbus调试] 注册表Key: " + key + " 类型: " + key.getClass().getName());
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("[Aiyatsbus调试] 打印注册表key失败: " + e.getMessage());
                    }
                }

                Object aiyatsbusEnchant = null;
                // 1. NamespacedKey查找
                try {
                    aiyatsbusEnchant = getEnchantKeyMethod.invoke(enchantManager, enchantment.getKey());
                } catch (Exception ignored) {}
                // 2. 完整ID字符串查找
                if (aiyatsbusEnchant == null) {
                    try { aiyatsbusEnchant = getEnchantStringMethod.invoke(enchantManager, enchantId); } catch (Exception ignored) {}
                }
                // 3. 只用key部分查找
                if (aiyatsbusEnchant == null) {
                    try { aiyatsbusEnchant = getEnchantStringMethod.invoke(enchantManager, keyOnly); } catch (Exception ignored) {}
                }
                // 4. 使用原始格式，不再转换冒号为下划线
                if (aiyatsbusEnchant == null) {
                    try { aiyatsbusEnchant = getEnchantStringMethod.invoke(enchantManager, nsKey); } catch (Exception ignored) {}
                }
                // 5. 用大写/小写名称查找
                        if (aiyatsbusEnchant == null) {
                    try { aiyatsbusEnchant = getByNameMethod.invoke(enchantManager, upperName); } catch (Exception ignored) {}
                        }
                if (aiyatsbusEnchant == null) {
                    try { aiyatsbusEnchant = getByNameMethod.invoke(enchantManager, lowerName); } catch (Exception ignored) {}
                        }
                // 6. 兼容nsKey查找
                if (aiyatsbusEnchant == null) {
                    try { aiyatsbusEnchant = getEnchantStringMethod.invoke(enchantManager, nsKey); } catch (Exception ignored) {}
                }
                // 7. 兜底：遍历所有附魔
                if (aiyatsbusEnchant == null) {
                    try {
                        Method getEnchantsMethod = enchantManager.getClass().getMethod("getEnchants");
                        Object enchantsMap = getEnchantsMethod.invoke(enchantManager);
                        if (enchantsMap instanceof Map) {
                            Map<?, ?> enchantMap = (Map<?, ?>) enchantsMap;
                            for (Map.Entry<?, ?> entry : enchantMap.entrySet()) {
                                if (entry.getKey() != null && entry.getValue() != null) {
                                    String kstr = entry.getKey().toString();
                                    if (kstr.equalsIgnoreCase(enchantId) || kstr.equalsIgnoreCase(keyOnly) || kstr.equalsIgnoreCase(nsKey)) {
                                        aiyatsbusEnchant = entry.getValue();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("[Aiyatsbus调试] 遍历注册表查找失败: " + e.getMessage());
                    }
                }
                // 8. 还是找不到就兜底
                if (aiyatsbusEnchant == null) {
                    useDefaultName(info, enchantment);
                    return;
                }
                // 后续逻辑不变...
                Object basicData = null;
                try { basicData = getBasicDataMethod.invoke(aiyatsbusEnchant); } catch (Exception e) {}
                if (basicData == null) { useDefaultName(info, enchantment); return; }
                String chineseName = null;
                try { Object nameObj = getNameMethod.invoke(basicData); if (nameObj != null) chineseName = nameObj.toString(); } catch (Exception e) {}
                if (chineseName != null && !chineseName.isEmpty()) {
                    info.setName(chineseName);
                    try {
                        Object displayer = null;
                        try { displayer = getDisplayerMethod.invoke(aiyatsbusEnchant); } catch (Exception e) {}
                        if (displayer != null) {
                            Class<?> displayerClass = displayer.getClass();
                            java.lang.reflect.Field generalDescriptionField = displayerClass.getDeclaredField("generalDescription");
                            generalDescriptionField.setAccessible(true);
                            Object descriptionObj = generalDescriptionField.get(displayer);
                            if (descriptionObj != null) {
                                String description = descriptionObj.toString();
                                if (!description.isEmpty() && !description.equals("&7")) {
                                    info.setDescription(Collections.singletonList("&7" + description));
                                } else {
                                    info.setDescription(Collections.singletonList("&7" + chineseName + " 附魔"));
                                }
                            } else {
                                info.setDescription(Collections.singletonList("&7" + chineseName + " 附魔"));
                            }
                        } else {
                            info.setDescription(Collections.singletonList("&7" + chineseName + " 附魔"));
                        }
                    } catch (Exception e) {
                        info.setDescription(Collections.singletonList("&7" + chineseName + " 附魔"));
                    }
                    try {
                    Object maxLevelObj = getMaxLevelMethod.invoke(basicData);
                    if (maxLevelObj != null && maxLevelObj instanceof Integer) {
                        info.setMaxLevel((Integer) maxLevelObj);
                        } else {
                            info.setMaxLevel(enchantment.getMaxLevel());
                        }
                    } catch (Exception e) {
                        info.setMaxLevel(enchantment.getMaxLevel());
                    }
                } else {
                    useDefaultName(info, enchantment);
                }
            } catch (Exception e) {
                useDefaultName(info, enchantment);
            }
        } catch (Exception e) {
            useDefaultName(info, enchantment);
        }
    }
    
    /**
     * 初始化反射方法
     */
    private void initReflectionMethods() throws ClassNotFoundException, NoSuchMethodException {
        if (apiMethod == null) {
            // 尝试直接访问Aiyatsbus的静态API方法
            Class<?> aiyatsbusClass = Class.forName("cc.polarastrum.aiyatsbus.core.Aiyatsbus");
            apiMethod = aiyatsbusClass.getMethod("api");
        }
        
        if (getEnchantmentManagerMethod == null) {
            Class<?> apiClass = Class.forName("cc.polarastrum.aiyatsbus.core.AiyatsbusAPI");
            getEnchantmentManagerMethod = apiClass.getMethod("getEnchantmentManager");
        }
        
        if (getEnchantStringMethod == null) {
            Class<?> managerClass = Class.forName("cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager");
            getEnchantStringMethod = managerClass.getMethod("getEnchant", String.class);
        }
        
        if (getEnchantKeyMethod == null) {
            Class<?> managerClass = Class.forName("cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager");
            getEnchantKeyMethod = managerClass.getMethod("getEnchant", NamespacedKey.class);
        }
        
        if (getBasicDataMethod == null) {
            Class<?> enchantClass = Class.forName("cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment");
            getBasicDataMethod = enchantClass.getMethod("getBasicData");
        }
        
        if (getNameMethod == null) {
            Class<?> basicDataClass = Class.forName("cc.polarastrum.aiyatsbus.core.data.BasicData");
            getNameMethod = basicDataClass.getMethod("getName");
        }
        
        if (getMaxLevelMethod == null) {
            Class<?> basicDataClass = Class.forName("cc.polarastrum.aiyatsbus.core.data.BasicData");
            getMaxLevelMethod = basicDataClass.getMethod("getMaxLevel");
        }
        
        if (getDisplayerMethod == null) {
            Class<?> enchantClass = Class.forName("cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment");
            getDisplayerMethod = enchantClass.getMethod("getDisplayer");
        }

        if (getByNameMethod == null) {
            Class<?> managerClass = Class.forName("cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentManager");
            getByNameMethod = managerClass.getMethod("getEnchant", String.class); // 假设通过名称查找也是getEnchant方法
        }
    }
    
    /**
     * 使用默认名称
     */
    private void useDefaultName(EnchantmentInfo info, Enchantment enchantment) {
        String name = enchantment.getName();
        info.setName(name);
        info.setMaxLevel(enchantment.getMaxLevel());
        info.setDescription(Collections.singletonList("&7" + name + " 附魔"));
    }
    
    /**
     * 记录失败的附魔ID和原因，避免重复输出警告
     */
    private void recordFailure(String enchantId, String reason) {
        if (!failedEnchantmentIds.contains(enchantId)) {
            // 只在配置启用时显示警告
            if (plugin.getConfigsManager().getEnchantmentsConfigManager().showApiErrorWarnings()) {
                plugin.getLogger().warning("无法获取附魔 " + enchantId + " 的中文名称: " + reason);
            }
            failedEnchantmentIds.add(enchantId);
        }
    }
    
    /**
     * 获取附魔对象
     * @param enchantmentId 附魔ID
     * @return 附魔对象
     */
    private Enchantment getEnchantment(String enchantmentId) {
        try {
            if (enchantmentId.contains(":")) {
                String[] parts = enchantmentId.split(":", 2);
                return Enchantment.getByKey(new NamespacedKey(parts[0], parts[1]));
            } else {
                return Enchantment.getByKey(NamespacedKey.minecraft(enchantmentId));
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 将附魔ID格式化为更友好的名称
     * @param key 附魔ID
     * @return 格式化后的名称
     */
    private String formatEnchantmentName(String key) {
        String[] parts = key.split("_");
        StringBuilder name = new StringBuilder();
        
        for (String part : parts) {
            if (part.length() > 0) {
                name.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase())
                    .append(" ");
            }
        }
        
        return name.toString().trim();
    }
    
    /**
     * 获取物品类型的友好名称
     * @param materialName 物品类型名称
     * @return 友好名称
     */
    private String getItemTypeName(String materialName) {
        if (materialName.equalsIgnoreCase("BOOK") || materialName.equalsIgnoreCase("ENCHANTED_BOOK")) {
            return "附魔书";
        }
        return ITEM_TYPE_NAMES.getOrDefault(materialName, materialName);
    }
    
    /**
     * 处理附魔描述中的变量
     * @param info 附魔信息
     * @param template 描述模板
     * @return 处理后的描述
     */
    public List<String> processDescription(EnchantmentInfo info, List<String> template) {
        List<String> result = new ArrayList<>();
        String aiyatsbusDesc = null;
        try {
            cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment aiyatsbusEnchant = cc.polarastrum.aiyatsbus.core.Aiyatsbus.INSTANCE.api().getEnchantmentManager().getEnchant(info.getId());
            if (aiyatsbusEnchant != null) {
                // 获取displayer的specificDescription并做变量替换
                String descRaw = aiyatsbusEnchant.getDisplayer().getSpecificDescription();
                Map<String, String> holders = aiyatsbusEnchant.getDisplayer().holders(aiyatsbusEnchant.getBasicData().getMaxLevel(), null, null);
                aiyatsbusDesc = descRaw;
                for (Map.Entry<String, String> entry : holders.entrySet()) {
                    aiyatsbusDesc = aiyatsbusDesc.replace("%" + entry.getKey() + "%", entry.getValue());
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        for (String line : template) {
            String processed = line;
            processed = processed.replace("%enchant_name%", info.getName());
            processed = processed.replace("%max_level%", String.valueOf(info.getMaxLevel()));
            processed = processed.replace("%namespace%", info.getNamespace());
            processed = processed.replace("%key%", info.getKey());
            // 优先用Aiyatsbus的description
            if (aiyatsbusDesc != null && !aiyatsbusDesc.isEmpty()) {
                processed = processed.replace("%aiyatsbus_description%", aiyatsbusDesc);
            } else if (!info.getDescription().isEmpty()) {
                processed = processed.replace("%aiyatsbus_description%", String.join("\n", info.getDescription()));
            } else {
                processed = processed.replace("%aiyatsbus_description%", "");
            }
            result.add(processed);
        }
        return result;
    }
    
    /**
     * 格式化适用物品列表
     * @param info 附魔信息
     * @param configManager 配置管理器
     * @return 格式化后的适用物品列表
     */
    public List<String> formatApplicableItems(EnchantmentInfo info, EnchantmentsConfigManager configManager) {
        List<String> result = new ArrayList<>();
        List<String> items = new ArrayList<>();
        
        // 使用AiyatsbusAPI获取适用物品类型
        if (aiyatsbusPresent && plugin.getConfigsManager().getEnchantmentsConfigManager().fetchAiyatsbusInfo()) {
            try {
                List<String> apiTargets = cx.ajneb97.api.AiyatsbusAPI.getTargets(info.getId());
                if (!apiTargets.isEmpty()) {
                    // 将API返回的物品类型转换为友好名称
                    for (String target : apiTargets) {
                        String itemName = getItemTypeName(target);
                        items.add(itemName);
                    }
                } else {
                    // 如果API返回为空，使用原有方法获取
                    items = info.getApplicableItems();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("通过API获取附魔适用物品类型时出错: " + e.getMessage());
                items = info.getApplicableItems();
            }
        } else {
            // 如果不使用API，使用原有方法获取
            items = info.getApplicableItems();
        }
        
        if (items.isEmpty()) {
            result.add(configManager.getNoItemsText());
            return result;
        }
        
        // 统一物品类型显示
        Map<String, Integer> itemTypeCount = new HashMap<>();
        Map<String, String> itemTypeNames = new HashMap<>();
        
        // 分类统计不同类型的物品
        for (String item : items) {
            String itemType = getItemType(item);
            itemTypeCount.put(itemType, itemTypeCount.getOrDefault(itemType, 0) + 1);
            
            // 保存每种类型的通用名称
            if (!itemTypeNames.containsKey(itemType)) {
                itemTypeNames.put(itemType, getGenericTypeName(itemType));
            }
        }
        
        // 将统计后的物品类型转换为显示列表
        List<String> unifiedItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : itemTypeCount.entrySet()) {
            unifiedItems.add(itemTypeNames.get(entry.getKey()));
        }
        
        // 排序物品类型，保证显示顺序一致
        Collections.sort(unifiedItems);
        
        int maxItems = configManager.getMaxItems();
        int itemsPerLine = configManager.getItemsPerLine();
        String format = configManager.getItemFormat();
        
        // 如果物品类型太多，只显示一部分
        if (unifiedItems.size() > maxItems) {
            List<String> displayItems = unifiedItems.subList(0, maxItems);
            
            // 分行显示物品
            for (int i = 0; i < displayItems.size(); i += itemsPerLine) {
                StringBuilder line = new StringBuilder();
                
                for (int j = 0; j < itemsPerLine && i + j < displayItems.size(); j++) {
                    if (j > 0) {
                        line.append(", ");
                    }
                    line.append(displayItems.get(i + j));
                }
                
                result.add(format.replace("%item%", line.toString()));
            }
            
            // 添加"以及更多..."文本
            result.add(configManager.getTooManyItemsText());
        } else {
            // 分行显示所有物品类型
            for (int i = 0; i < unifiedItems.size(); i += itemsPerLine) {
                StringBuilder line = new StringBuilder();
                
                for (int j = 0; j < itemsPerLine && i + j < unifiedItems.size(); j++) {
                    if (j > 0) {
                        line.append(", ");
                    }
                    line.append(unifiedItems.get(i + j));
                }
                
                result.add(format.replace("%item%", line.toString()));
            }
        }
        
        return result;
    }
    
    /**
     * 获取物品的通用类型
     * @param itemName 物品名称
     * @return 物品通用类型
     */
    private String getItemType(String itemName) {
        if (itemName.contains("剑")) {
            return "SWORD";
        } else if (itemName.contains("斧")) {
            return "AXE";
        } else if (itemName.contains("镐")) {
            return "PICKAXE";
        } else if (itemName.contains("锹")) {
            return "SHOVEL";
        } else if (itemName.contains("锄")) {
            return "HOE";
        } else if (itemName.contains("头盔")) {
            return "HELMET";
        } else if (itemName.contains("胸甲")) {
            return "CHESTPLATE";
        } else if (itemName.contains("护腿")) {
            return "LEGGINGS";
        } else if (itemName.contains("靴子")) {
            return "BOOTS";
        } else {
            // 对于其他物品，直接返回原始名称
            return itemName;
        }
    }
    
    /**
     * 获取物品类型的通用名称
     * @param itemType 物品类型
     * @return 通用名称
     */
    private String getGenericTypeName(String itemType) {
        switch (itemType) {
            case "SWORD":
                return "剑";
            case "AXE":
                return "斧";
            case "PICKAXE":
                return "镐";
            case "SHOVEL":
                return "锹";
            case "HOE":
                return "锄";
            case "HELMET":
                return "头盔";
            case "CHESTPLATE":
                return "胸甲";
            case "LEGGINGS":
                return "护腿";
            case "BOOTS":
                return "靴子";
            default:
                return itemType;
        }
    }
    
    /**
     * 格式化冲突附魔列表
     * @param info 附魔信息
     * @param configManager 配置管理器
     * @return 格式化后的冲突附魔列表
     */
    public List<String> formatConflicts(EnchantmentInfo info, EnchantmentsConfigManager configManager) {
        List<String> result = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        
        // 使用AiyatsbusAPI获取冲突附魔
        if (aiyatsbusPresent && plugin.getConfigsManager().getEnchantmentsConfigManager().fetchAiyatsbusInfo()) {
            try {
                List<String> apiConflicts = cx.ajneb97.api.AiyatsbusAPI.getConflicts(info.getId());
                if (configManager.isDebug()) {
                    plugin.getLogger().info("[DEBUG] 获取到的冲突附魔ID列表: " + apiConflicts);
                }
                
                if (!apiConflicts.isEmpty()) {
                    // 将API返回的冲突附魔ID转换为友好名称
                    for (String conflictId : apiConflicts) {
                        try {
                            // 直接从API获取中文名称
                            String chineseName = cx.ajneb97.api.AiyatsbusAPI.getEnchantmentName(conflictId);
                            if (chineseName != null && !chineseName.isEmpty()) {
                                conflicts.add(chineseName);
                                if (configManager.isDebug()) {
                                    plugin.getLogger().info("[DEBUG] 直接从API获取冲突附魔中文名称: " + conflictId + " -> " + chineseName);
                                }
                            } else {
                                // 尝试获取Aiyatsbus附魔实例并直接访问basicData.name
                                try {
                                    Object enchantObj = cx.ajneb97.api.AiyatsbusAPI.getEnchantment(conflictId);
                                    if (enchantObj != null) {
                                        Method getBasicDataMethod = enchantObj.getClass().getMethod("getBasicData");
                                        Object basicData = getBasicDataMethod.invoke(enchantObj);
                                        if (basicData != null) {
                                            Method getNameMethod = basicData.getClass().getMethod("getName");
                                            String nameStr = (String) getNameMethod.invoke(basicData);
                                            if (nameStr != null && !nameStr.isEmpty()) {
                                                conflicts.add(nameStr);
                                                if (configManager.isDebug()) {
                                                    plugin.getLogger().info("[DEBUG] 通过BasicData获取冲突附魔名称: " + conflictId + " -> " + nameStr);
                                                }
                                                continue;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    if (configManager.isDebug()) {
                                        plugin.getLogger().info("[DEBUG] 通过BasicData获取冲突附魔名称失败: " + e.getMessage());
                                    }
                                }
                                
                                // 尝试通过EnchantmentInfo获取
                                EnchantmentInfo conflictInfo = getEnchantmentInfo(conflictId);
                                if (conflictInfo != null) {
                                    conflicts.add(conflictInfo.getName());
                                    if (configManager.isDebug()) {
                                        plugin.getLogger().info("[DEBUG] 通过EnchantmentInfo获取冲突附魔名称: " + conflictId + " -> " + conflictInfo.getName());
                                    }
                                } else {
                                    // 如果无法获取附魔信息，使用ID作为名称
                                    conflicts.add(formatEnchantmentName(conflictId));
                                    if (configManager.isDebug()) {
                                        plugin.getLogger().info("[DEBUG] 无法获取附魔信息，使用格式化ID: " + conflictId + " -> " + formatEnchantmentName(conflictId));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 如果处理特定附魔出错，跳过
                            plugin.getLogger().warning("处理冲突附魔 " + conflictId + " 时出错: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } else {
                    // 如果API返回为空，使用原有方法获取
                    conflicts = info.getConflicts();
                    if (configManager.isDebug()) {
                        plugin.getLogger().info("[DEBUG] API返回冲突列表为空，使用原始列表: " + conflicts);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("通过API获取附魔冲突时出错: " + e.getMessage());
                e.printStackTrace();
                conflicts = info.getConflicts();
            }
        } else {
            // 如果不使用API，使用原有方法获取
            conflicts = info.getConflicts();
        }
        
        if (configManager.isDebug()) {
            plugin.getLogger().info("[DEBUG] 最终的冲突附魔列表: " + conflicts);
        }
        
        // 确保所有冲突附魔都是中文名称
        List<String> translatedConflicts = new ArrayList<>();
        for (String conflict : conflicts) {
            // 如果看起来像是英文ID（包含下划线或冒号），尝试转换为中文名称
            if (conflict.contains("_") || conflict.contains(":")) {
                // 使用原始格式，不进行替换
                String enchantId = conflict;
                
                try {
                    // 尝试直接从API获取中文名称
                    String chineseName = cx.ajneb97.api.AiyatsbusAPI.getEnchantmentName(enchantId);
                    if (chineseName != null && !chineseName.isEmpty()) {
                        translatedConflicts.add(chineseName);
                        continue;
                    }
                    
                    // 尝试通过EnchantmentInfo获取
                    EnchantmentInfo conflictInfo = getEnchantmentInfo(enchantId);
                    if (conflictInfo != null && !conflictInfo.getName().equals(formatEnchantmentName(enchantId))) {
                        translatedConflicts.add(conflictInfo.getName());
                        continue;
                    }
                } catch (Exception ignored) {}
                
                // 如果无法获取中文名称，使用格式化的ID
                translatedConflicts.add(formatEnchantmentName(enchantId));
            } else {
                // 已经是中文名称，直接添加
                translatedConflicts.add(conflict);
            }
        }
        conflicts = translatedConflicts;
        
        if (conflicts.isEmpty()) {
            result.add(configManager.getNoConflictsText());
            return result;
        }
        
        int maxConflicts = configManager.getMaxConflicts();
        int enchantsPerLine = configManager.getEnchantsPerLine();
        String format = configManager.getEnchantFormat();
        
        // 如果冲突附魔太多，只显示一部分
        if (conflicts.size() > maxConflicts) {
            List<String> displayConflicts = conflicts.subList(0, maxConflicts);
            
            // 分行显示冲突附魔
            for (int i = 0; i < displayConflicts.size(); i += enchantsPerLine) {
                StringBuilder line = new StringBuilder();
                
                for (int j = 0; j < enchantsPerLine && i + j < displayConflicts.size(); j++) {
                    if (j > 0) {
                        line.append(", ");
                    }
                    line.append(displayConflicts.get(i + j));
                }
                
                result.add(format.replace("%enchant%", line.toString()));
            }
            
            // 添加"以及更多..."文本
            result.add(configManager.getTooManyConflictsText());
        } else {
            // 分行显示所有冲突附魔
            for (int i = 0; i < conflicts.size(); i += enchantsPerLine) {
                StringBuilder line = new StringBuilder();
                
                for (int j = 0; j < enchantsPerLine && i + j < conflicts.size(); j++) {
                    if (j > 0) {
                        line.append(", ");
                    }
                    line.append(conflicts.get(i + j));
                }
                
                result.add(format.replace("%enchant%", line.toString()));
            }
        }
        
        return result;
    }
    
    /**
     * 处理物品lore中的变量
     * @param info 附魔信息
     * @param lore 物品lore
     * @param configManager 配置管理器
     * @return 处理后的lore
     */
    public List<String> processLore(EnchantmentInfo info, List<String> lore, EnchantmentsConfigManager configManager) {
        final String DEBUG_PREFIX = "[LORE-DEBUG] 处理物品lore: ";
        boolean isDebug = configManager.isDebug();
        
        if (isDebug) {
            plugin.getLogger().info(DEBUG_PREFIX + "开始处理附魔 " + info.getId() + " 的lore");
            plugin.getLogger().info(DEBUG_PREFIX + "附魔名称: " + info.getName());
            plugin.getLogger().info(DEBUG_PREFIX + "原始lore: " + lore);
        }
        
        List<String> result = new ArrayList<>();
        
        for (String line : lore) {
            // 如果行包含 %description%，替换为自定义描述或默认描述
            if (line.contains("%description%")) {
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "处理描述变量");
                }
                
                List<String> description;
                List<String> customDescription = configManager.getCustomDescription(info.getId());
                
                if (customDescription != null && !customDescription.isEmpty()) {
                    // 使用自定义描述
                    if (isDebug) {
                        plugin.getLogger().info(DEBUG_PREFIX + "使用自定义描述: " + customDescription);
                    }
                    description = processDescription(info, customDescription);
                } else {
                    // 使用默认描述模板
                    if (isDebug) {
                        plugin.getLogger().info(DEBUG_PREFIX + "使用默认描述模板");
                    }
                    description = processDescription(info, configManager.getDefaultDescriptionTemplate());
                }
                
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "处理后的描述: " + description);
                }
                
                result.addAll(description);
                continue;
            }
            
            // 如果行包含 %applicable_items%，替换为适用物品列表
            if (line.contains("%applicable_items%")) {
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "处理适用物品变量");
                }
                
                List<String> items = formatApplicableItems(info, configManager);
                
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "适用物品列表: " + items);
                }
                
                result.addAll(items);
                continue;
            }
            
            // 如果行包含 %conflicts%，替换为冲突附魔列表
            if (line.contains("%conflicts%")) {
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "处理冲突附魔变量");
                }
                
                List<String> conflicts = formatConflicts(info, configManager);
                
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "冲突附魔列表: " + conflicts);
                }
                
                result.addAll(conflicts);
                continue;
            }
            
            // 替换其他变量
            String processed = line;
            
            // 优先处理enchant_name变量，确保使用正确的中文名称
            if (processed.contains("%enchant_name%") || processed.contains("%name%")) {
                String enchantName = info.getName();
                
                // 如果名称为空或者看起来不是中文（可能是ID），尝试重新获取
                if (enchantName == null || enchantName.isEmpty() || (enchantName.contains(":") || enchantName.contains("_") || enchantName.equals(enchantName.toUpperCase()))) {
                    if (isDebug) {
                        plugin.getLogger().warning(DEBUG_PREFIX + "附魔名称可能不正确: " + enchantName + "，尝试重新获取");
                    }
                    
                    // 清除该附魔的缓存
                    enchantmentInfoCache.remove(info.getId());
                    
                    // 从AiyatsbusAPI重新获取名称
                    if (aiyatsbusPresent) {
                        String newName = cx.ajneb97.api.AiyatsbusAPI.getEnchantmentName(info.getId());
                        if (newName != null && !newName.isEmpty() && !newName.equals(newName.toUpperCase()) && !newName.equals(enchantName)) {
                            if (isDebug) {
                                plugin.getLogger().info(DEBUG_PREFIX + "成功重新获取附魔名称: " + newName);
                            }
                            enchantName = newName;
                            info.setName(newName); // 更新信息对象的名称
                        }
                    }
                }
                
                // 如果名称仍然无效，使用格式化的ID作为后备
                if (enchantName == null || enchantName.isEmpty() || (enchantName.contains(":") || enchantName.contains("_") || enchantName.equals(enchantName.toUpperCase()))) {
                    String formattedName = formatEnchantmentName(info.getKey());
                    if (isDebug) {
                        plugin.getLogger().info(DEBUG_PREFIX + "使用格式化的ID作为名称: " + formattedName);
                    }
                    enchantName = formattedName;
                    info.setName(formattedName); // 更新信息对象的名称
                }
                
                if (isDebug) {
                    plugin.getLogger().info(DEBUG_PREFIX + "最终使用的附魔名称: " + enchantName);
                }
                
                processed = processed.replace("%enchant_name%", enchantName);
                processed = processed.replace("%name%", enchantName);
            }
            
            processed = processed.replace("%max_level%", String.valueOf(info.getMaxLevel()));
            processed = processed.replace("%namespace%", info.getNamespace());
            processed = processed.replace("%key%", info.getKey());
            processed = processed.replace("%rarity%", configManager.getRarityText(info.getRarity()));
            
            if (isDebug) {
                plugin.getLogger().info(DEBUG_PREFIX + "处理后的行: " + processed);
            }
            
            result.add(processed);
        }
        
        if (isDebug) {
            plugin.getLogger().info(DEBUG_PREFIX + "处理后的完整lore: " + result);
        }
        
        return result;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[清除缓存] 正在清除附魔信息缓存，缓存大小: " + enchantmentInfoCache.size());
        }
        
        // 清除本地缓存
        enchantmentInfoCache.clear();
        
        // 清除AiyatsbusAPI中的缓存
        cx.ajneb97.api.AiyatsbusAPI.clearCache();
        
        // 清除EnchantmentManager中的缓存
        plugin.getEnchantmentManager().clearEnchantmentCache();
        
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[清除缓存] 附魔信息缓存已清除");
        }
    }
} 