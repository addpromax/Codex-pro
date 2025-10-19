package cx.ajneb97.api;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.Collection;

/**
 * Aiyatsbus API 接口封装类
 * 
 * 提供与 Aiyatsbus 附魔系统的集成接口，包括：
 * - 检查 Aiyatsbus 是否可用
 * - 获取附魔名称、描述、稀有度等基础信息
 * - 获取附魔的适用工具、冲突附魔、依赖附魔等高级信息
 * - 获取物品上的 Aiyatsbus 附魔
 */
public class AiyatsbusAPI {
    private static final String AIYATSBUS_PLUGIN_NAME = "Aiyatsbus";
    private static boolean available = false;
    private static boolean initialized = false;
    private static Plugin plugin = null;
    private static Plugin aiyatsbusPlugin = null;
    private static boolean debug = false;
    
    // 缓存反射类和对象
    private static Class<?> aiyatsbusClass;
    private static Class<?> aiyatsbusAPIClass;
    private static Class<?> aiyatsbusEnchantmentClass;
    private static Class<?> aiyatsbusBasicDataClass;
    private static Class<?> aiyatsbusLimitationsClass;
    private static Class<?> aiyatsbusTargetClass;
    private static Object apiInstance;
    private static Object enchantmentManager;
    
    // 缓存附魔名称，避免重复获取
    private static final Map<String, String> enchantmentNameCache = new ConcurrentHashMap<>();
    
    /**
     * 初始化API
     * 
     * @param plugin 调用此API的插件实例
     * @return 是否初始化成功
     */
    public static boolean initialize(Plugin plugin) {
        if (initialized) {
            return available;
        }
        AiyatsbusAPI.plugin = plugin;
        initialized = true;
        try {
            // 只用DefaultAiyatsbusAPI方式
            Class<?> defaultApiClass = Class.forName("cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusAPI");
            apiInstance = defaultApiClass.getDeclaredConstructor().newInstance();
            plugin.getLogger().info("通过DefaultAiyatsbusAPI类创建API实例成功");
            // 获取附魔管理器
            Method getEnchantmentManagerMethod = apiInstance.getClass().getMethod("getEnchantmentManager");
            enchantmentManager = getEnchantmentManagerMethod.invoke(apiInstance);
            if (enchantmentManager != null) {
                plugin.getLogger().info("成功获取附魔管理器");
                available = true;
                return true;
            } else {
                plugin.getLogger().warning("无法获取附魔管理器");
            }
            plugin.getLogger().warning("无法获取Aiyatsbus API实例");
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "初始化Aiyatsbus API时发生错误", e);
            return false;
        }
    }
    
    /**
     * 设置调试模式
     * 
     * @param debug 是否启用调试
     */
    public static void setDebug(boolean debug) {
        AiyatsbusAPI.debug = debug;
    }
    
    /**
     * 获取当前是否为调试模式
     * 
     * @return 是否为调试模式
     */
    public static boolean isDebug() {
        return debug;
    }
    
    /**
     * 检查API是否可用
     * 
     * @return 是否可用
     */
    public static boolean isAvailable() {
        if (!initialized) {
            if (plugin != null) {
                initialize(plugin);
            } else {
                return false;
            }
        }
        
        // 额外检查API实例是否可用
        if (available && (apiInstance == null || enchantmentManager == null)) {
            available = false;
        }
        
        return available;
    }
    
    /**
     * 根据ID获取附魔实例
     * 
     * @param enchantId 附魔ID，格式为namespace:key，例如minecraft:sharpness
     * @return 附魔实例，如果不存在或API不可用则返回null
     */
    public static Object getEnchantment(String enchantId) {
        if (!isAvailable() || enchantId == null || enchantmentManager == null) {
            return null;
        }
        
        // 尝试不同格式的附魔ID
        List<String> possibleIds = new ArrayList<>();
        possibleIds.add(enchantId); // 原始ID
        
        // 添加冒号和下划线转换后的ID
        if (enchantId.contains(":")) {
            // 保持原始格式，不再转换冒号为下划线
            // 只在兼容性需要时添加替代格式
            if (enchantId.startsWith("minecraft:")) {
                // 对于原版附魔，有时候会使用无命名空间的形式
                possibleIds.add(enchantId.substring("minecraft:".length()));
            }
        } else if (enchantId.contains("_")) {
            String[] parts = enchantId.split("_", 2);
            if (parts.length == 2) {
                possibleIds.add(parts[0] + ":" + parts[1]); // 下划线转冒号，用于兼容旧数据
            }
        } else if (!enchantId.contains(":")) {
            // 没有命名空间的情况，添加minecraft:前缀
            possibleIds.add("minecraft:" + enchantId);
        }
        
        if (isDebug() && plugin != null) {
            plugin.getLogger().info("[DEBUG] 尝试获取附魔，可能的ID: " + possibleIds);
        }
        
        // 尝试每一种可能的ID
        for (String id : possibleIds) {
            try {
                // 尝试不同的方法名调用
                try {
                    Method getEnchantMethod = enchantmentManager.getClass().getMethod("getEnchant", String.class);
                    Object result = getEnchantMethod.invoke(enchantmentManager, id);
                    if (result != null) {
                        if (isDebug() && plugin != null) {
                            plugin.getLogger().info("[DEBUG] 成功获取附魔实例，使用ID: " + id);
                        }
                        return result;
                    }
                } catch (NoSuchMethodException e) {
                    // 尝试其他可能的方法名
                    for (Method method : enchantmentManager.getClass().getMethods()) {
                        if ((method.getName().startsWith("get") || method.getName().startsWith("find")) && 
                            method.getParameterCount() == 1 && 
                            method.getParameterTypes()[0].equals(String.class)) {
                            try {
                                Object result = method.invoke(enchantmentManager, id);
                                if (result != null) {
                                    if (isDebug() && plugin != null) {
                                        plugin.getLogger().info("[DEBUG] 成功获取附魔实例，使用方法: " + method.getName() + ", ID: " + id);
                                    }
                                    return result;
                                }
                            } catch (Exception ignored) {
                                // 忽略调用失败的方法
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (plugin != null && isDebug()) {
                    plugin.getLogger().log(Level.WARNING, "使用ID获取附魔时发生错误: " + id, e);
                }
            }
        }
        
        // 如果所有ID都失败，尝试遍历所有附魔
        try {
            Method getEnchantsMethod = enchantmentManager.getClass().getMethod("getEnchants");
            Object enchantsMap = getEnchantsMethod.invoke(enchantmentManager);
            
            if (enchantsMap instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) enchantsMap;
                
                if (isDebug() && plugin != null) {
                    plugin.getLogger().info("[DEBUG] 开始遍历附魔注册表，共有 " + map.size() + " 个附魔");
                    for (Object key : map.keySet()) {
                        plugin.getLogger().info("[DEBUG] 注册表中的附魔: " + key + " 类型: " + (key != null ? key.getClass().getName() : "null"));
                    }
                }
                
                // 遍历所有附魔，查找ID匹配的
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    String keyStr = key != null ? key.toString() : "";
                    
                    // 检查是否匹配任何可能的ID
                    for (String id : possibleIds) {
                        if (keyStr.equalsIgnoreCase(id) || 
                            // 保持原始格式，不进行字符替换
                            keyStr.equalsIgnoreCase(id)) {
                            
                            if (isDebug() && plugin != null) {
                                plugin.getLogger().info("[DEBUG] 通过遍历找到匹配附魔，注册表Key: " + keyStr + ", 查找ID: " + id);
                            }
                            
                            return entry.getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "遍历附魔注册表时发生错误", e);
            }
        }
        
        if (isDebug() && plugin != null) {
            plugin.getLogger().info("[DEBUG] 无法获取附魔实例: " + enchantId);
        }
        
        return null;
    }
    
    /**
     * 根据Bukkit附魔获取Aiyatsbus附魔
     * 
     * @param enchantment Bukkit附魔实例
     * @return Aiyatsbus附魔实例，如果不存在或API不可用则返回null
     */
    public static Object getEnchantment(Enchantment enchantment) {
        if (!isAvailable() || enchantment == null) {
            return null;
        }
        
        try {
            String enchantId = enchantment.getKey().toString();
            return getEnchantment(enchantId);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "获取附魔时发生错误", e);
            }
            return null;
        }
    }
    
    /**
     * 获取附魔名称
     * 
     * @param enchantId 附魔ID
     * @return 附魔名称，如果不存在或API不可用则返回null
     */
    public static String getEnchantmentName(String enchantId) {
        // 增加调试日志前缀，更容易在日志中识别
        final String DEBUG_PREFIX = "[AIYATSBUS-DEBUG] 获取附魔名称: ";
        
        // 总是输出调试信息，便于排查问题
        if (plugin != null) {
            plugin.getLogger().info(DEBUG_PREFIX + "开始获取附魔 " + enchantId + " 的名称");
        }
        
        // 检查缓存前先确认附魔是否可用
        if (!isAvailable()) {
            if (plugin != null) {
                plugin.getLogger().warning(DEBUG_PREFIX + "Aiyatsbus API 不可用，无法获取附魔名称");
            }
            return null;
        }
        
        // 检查缓存
        if (enchantmentNameCache.containsKey(enchantId)) {
            String cachedName = enchantmentNameCache.get(enchantId);
            if (plugin != null) {
                plugin.getLogger().info(DEBUG_PREFIX + "从缓存获取到名称: " + cachedName);
            }
            return cachedName;
        }
        
        Object enchant = getEnchantment(enchantId);
        if (enchant == null) {
            if (plugin != null) {
                plugin.getLogger().warning(DEBUG_PREFIX + "无法获取附魔实例: " + enchantId);
            }
            return null;
        }
        
        try {
            if (plugin != null) {
                plugin.getLogger().info(DEBUG_PREFIX + "成功获取附魔实例: " + enchantId + ", 类型: " + enchant.getClass().getName());
            }
            
            // 直接从BasicData.name获取中文名称
            try {
                Method getBasicDataMethod = enchant.getClass().getMethod("getBasicData");
                Object basicData = getBasicDataMethod.invoke(enchant);
                
                if (basicData != null) {
                    // 直接尝试访问name字段
                    try {
                        Field nameField = basicData.getClass().getDeclaredField("name");
                        nameField.setAccessible(true);
                        Object nameValue = nameField.get(basicData);
                        if (nameValue instanceof String) {
                            String nameStr = (String) nameValue;
                            if (nameStr != null && !nameStr.isEmpty()) {
                                enchantmentNameCache.put(enchantId, nameStr);
                                if (plugin != null) {
                                    plugin.getLogger().info(DEBUG_PREFIX + "直接从BasicData.name字段获取到中文名称: " + nameStr + " 可以用这个方法获取 删除其他尝试的方法");
                                }
                                return nameStr;
                            }
                        }
                    } catch (Exception e) {
                        if (plugin != null) {
                            plugin.getLogger().info(DEBUG_PREFIX + "直接获取BasicData.name字段失败: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().info(DEBUG_PREFIX + "获取BasicData失败: " + e.getMessage());
                }
            }
            
            // 如果上面的方法获取失败，则作为后备策略，从ID中提取名称
            String formattedName = formatEnchantmentName(enchantId);
            if (formattedName != null) {
                enchantmentNameCache.put(enchantId, formattedName);
                    if (plugin != null) {
                    plugin.getLogger().warning(DEBUG_PREFIX + "无法获取附魔名称: " + enchantId + ", 使用格式化ID作为名称");
                    }
                    return formattedName;
                }
            
            return null;
            
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, DEBUG_PREFIX + "获取附魔名称时发生错误: " + enchantId, e);
            }
            return null;
        }
    }
    
    // 添加一个格式化附魔名称的工具方法
    private static String formatEnchantmentName(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        // 将内部标识符格式化为可读形式
        // 例如: annihilate -> Annihilate
        String formattedName = key.toLowerCase();
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
     * 获取附魔描述
     * 
     * @param enchantId 附魔ID
     * @return 附魔描述，如果不存在或API不可用则返回null
     */
    @SuppressWarnings("unchecked")
    public static String getEnchantmentDescription(String enchantId) {
        Object enchant = getEnchantment(enchantId);
        if (enchant == null) {
            return null;
        }
        
        try {
            // 尝试直接获取描述
            try {
                Method getDescriptionMethod = enchant.getClass().getMethod("getDescription");
                Object description = getDescriptionMethod.invoke(enchant);
                
                // 处理不同类型的描述
                if (description instanceof String) {
                    return (String) description;
                } else if (description instanceof List) {
                    // 如果是列表，拼接成字符串
                    List<?> descList = (List<?>) description;
                    if (!descList.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (Object line : descList) {
                            if (line != null) {
                                if (sb.length() > 0) {
                                    sb.append("\n");
                                }
                                sb.append(String.valueOf(line));
                            }
                        }
                        return sb.toString();
                    }
                    return "";
                } else if (description != null) {
                    return description.toString();
                }
            } catch (NoSuchMethodException e) {
                // 尝试通过基础数据获取
                try {
                    Method getBasicDataMethod = enchant.getClass().getMethod("getBasicData");
                    Object basicData = getBasicDataMethod.invoke(enchant);
                    
                    if (basicData != null) {
                        Method getDescriptionMethod = basicData.getClass().getMethod("getDescription");
                        Object result = getDescriptionMethod.invoke(basicData);
                        
                        // 处理不同类型的描述
                        if (result instanceof String) {
                            return (String) result;
                        } else if (result instanceof List) {
                            // 如果是列表，拼接成字符串
                            List<?> descList = (List<?>) result;
                            if (!descList.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (Object line : descList) {
                                    if (line != null) {
                                        if (sb.length() > 0) {
                                            sb.append("\n");
                                        }
                                        sb.append(String.valueOf(line));
                                    }
                                }
                                return sb.toString();
                            }
                            return "";
                        } else if (result != null) {
                            return result.toString();
                        }
                    }
                } catch (NoSuchMethodException ex) {
                    // 尝试其他可能的方法
                    for (Method method : enchant.getClass().getMethods()) {
                        if (method.getName().equals("getDescription") || 
                            method.getName().equals("getDesc") ||
                            method.getName().equals("getInfo")) {
                            if (method.getParameterCount() == 0) {
                                Object result = method.invoke(enchant);
                                if (result instanceof String) {
                                    return (String) result;
                                } else if (result instanceof List) {
                                    // 如果是列表，拼接成字符串
                                    List<?> descList = (List<?>) result;
                                    if (!descList.isEmpty()) {
                                        StringBuilder sb = new StringBuilder();
                                        for (Object line : descList) {
                                            if (line != null) {
                                                if (sb.length() > 0) {
                                                    sb.append("\n");
                                                }
                                                sb.append(String.valueOf(line));
                                            }
                                        }
                                        return sb.toString();
                                    }
                                    return "";
                                } else if (result != null) {
                                    return result.toString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "获取附魔描述时发生错误: " + enchantId, e);
            }
        }
        
        return null;
    }
    
    /**
     * 获取附魔最大等级
     * 
     * @param enchantId 附魔ID
     * @return 附魔最大等级，如果不存在或API不可用则返回0
     */
    public static int getMaxLevel(String enchantId) {
        Object enchant = getEnchantment(enchantId);
        if (enchant == null) {
            return 0;
        }
        
        try {
            // 尝试直接获取最大等级
            try {
                Method getMaxLevelMethod = enchant.getClass().getMethod("getMaxLevel");
                Object maxLevel = getMaxLevelMethod.invoke(enchant);
                return maxLevel instanceof Number ? ((Number) maxLevel).intValue() : 0;
            } catch (NoSuchMethodException e) {
                // 尝试通过基础数据获取
                try {
                    Method getBasicDataMethod = enchant.getClass().getMethod("getBasicData");
                    Object basicData = getBasicDataMethod.invoke(enchant);
                    
                    if (basicData != null) {
                        Method getMaxLevelMethod = basicData.getClass().getMethod("getMaxLevel");
                        Object maxLevel = getMaxLevelMethod.invoke(basicData);
                        return maxLevel instanceof Number ? ((Number) maxLevel).intValue() : 0;
                    }
                } catch (NoSuchMethodException ex) {
                    // 尝试其他可能的方法
                    for (Method method : enchant.getClass().getMethods()) {
                        if (method.getName().equals("getMaxLevel") || 
                            method.getName().equals("getMaximumLevel")) {
                            if (method.getParameterCount() == 0) {
                                Object result = method.invoke(enchant);
                                if (result instanceof Number) {
                                    return ((Number) result).intValue();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "获取附魔最大等级时发生错误: " + enchantId, e);
            }
        }
        
        return 0;
    }
    
    /**
     * 获取附魔稀有度
     * 
     * @param enchantId 附魔ID
     * @return 附魔稀有度名称，如果不存在或API不可用则返回null
     */
    public static String getRarity(String enchantId) {
        Object enchant = getEnchantment(enchantId);
        if (enchant == null) {
            return null;
        }
        
        try {
            // 尝试获取稀有度
            try {
                Method getRarityMethod = enchant.getClass().getMethod("getRarity");
                Object rarity = getRarityMethod.invoke(enchant);
                
                if (rarity != null) {
                    // 尝试获取ID
                    try {
                        Method getIdMethod = rarity.getClass().getMethod("getId");
                        return (String) getIdMethod.invoke(rarity);
                    } catch (NoSuchMethodException e) {
                        // 回退到toString
                        return rarity.toString();
                    }
                }
            } catch (NoSuchMethodException e) {
                // 尝试其他方法
                for (Method method : enchant.getClass().getMethods()) {
                    if (method.getName().contains("Rarity") && method.getName().startsWith("get")) {
                        if (method.getParameterCount() == 0) {
                            Object result = method.invoke(enchant);
                            if (result != null) {
                                try {
                                    Method getIdMethod = result.getClass().getMethod("getId");
                                    return (String) getIdMethod.invoke(result);
                                } catch (Exception ex) {
                                    return result.toString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "获取附魔稀有度时发生错误: " + enchantId, e);
            }
        }
        
        return null;
    }
    
    /**
     * 获取附魔的适用物品类型（只返回配置中明确写明的类型）
     * 
     * @param enchantId 附魔ID
     * @return 附魔适用物品类型列表，如果不存在或API不可用则返回空列表
     */
    public static List<String> getTargets(String enchantId) {
        Object enchantObj = getEnchantment(enchantId);
        if (enchantObj == null) {
            return Collections.emptyList();
        }
        try {
            List<String> result = new ArrayList<>();
            
            // 从targets对象获取
            try {
                Method getTargetsMethod = enchantObj.getClass().getMethod("getTargets");
                List<?> targets = (List<?>) getTargetsMethod.invoke(enchantObj);
                
                if (plugin != null) {
                    plugin.getLogger().info("[DEBUG] 获取附魔 " + enchantId + " 的适用物品类型，原始targets数量: " + targets.size());
                }
                
                // 只处理第一个目标，通常是附魔真正的适用物品类型
                if (!targets.isEmpty()) {
                    Object firstTarget = targets.get(0);
                    try {
                        // 尝试获取物品类型列表
                        List<?> types = (List<?>) firstTarget.getClass().getMethod("getTypes").invoke(firstTarget);
                        
                        if (plugin != null) {
                            plugin.getLogger().info("[DEBUG] 目标类型数量: " + types.size());
                        }
                        
                        for (Object mat : types) {
                            try {
                                // 处理不同类型的返回值
                                String materialName;
                                if (mat instanceof org.bukkit.Material) {
                                    materialName = ((org.bukkit.Material) mat).name();
                                } else if (mat.getClass().getName().contains("Material")) {
                                    // 可能是枚举类型，尝试获取name方法
                                    try {
                                        Method nameMethod = mat.getClass().getMethod("name");
                                        Object nameObj = nameMethod.invoke(mat);
                                        materialName = nameObj.toString();
                                    } catch (Exception e) {
                                        materialName = mat.toString();
                                    }
                                } else {
                                    materialName = mat.toString();
                                }
                                
                                if (plugin != null) {
                                    plugin.getLogger().info("[DEBUG] 添加物品类型: " + materialName + " (原始类型: " + mat.getClass().getName() + ")");
                                }
                                
                                result.add(materialName);
                            } catch (Exception e) {
                                if (plugin != null && isDebug()) {
                                    plugin.getLogger().warning("处理物品类型时出错");
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (plugin != null && isDebug()) {
                            plugin.getLogger().warning("获取目标类型列表时出错");
                        }
                    }
                }
            } catch (Exception e) {
                if (plugin != null && isDebug()) {
                    plugin.getLogger().warning("获取targets字段时出错");
                }
            }
            
            // 确保列表中包含BOOK或ENCHANTED_BOOK，因为几乎所有附魔都可以应用于附魔书
            if (!result.contains("BOOK") && !result.contains("ENCHANTED_BOOK")) {
                result.add("BOOK");
            }
            
            if (plugin != null) {
                plugin.getLogger().info("[DEBUG] 最终获取到的物品类型数量: " + result.size() + ", 内容: " + result);
            }
            
            return result;
        } catch (Exception e) {
            if (plugin != null && isDebug()) {
                plugin.getLogger().log(Level.WARNING, "通过API获取附魔适用物品类型时发生错误: " + enchantId);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 获取附魔的冲突附魔ID列表（只返回配置中明确写明的冲突）
     * 
     * @param enchantId 附魔ID
     * @return 冲突附魔ID列表
     */
    public static List<String> getConflicts(String enchantId) {
        Object enchantObj = getEnchantment(enchantId);
        if (enchantObj == null) {
            return Collections.emptyList();
        }
        
        List<String> result = new ArrayList<>();
        
        // 方法1：通过getLimitations获取冲突
        try {
            Object limitations = enchantObj.getClass().getMethod("getLimitations").invoke(enchantObj);
            
            if (plugin != null) {
                plugin.getLogger().info("[DEBUG] 获取附魔 " + enchantId + " 的冲突附魔，limitations类型: " + 
                                       (limitations != null ? limitations.getClass().getName() : "null"));
            }
            
            // 获取 limitations 字段（Set<Pair<LimitType, String>>）
            java.lang.reflect.Field field = limitations.getClass().getDeclaredField("limitations");
            field.setAccessible(true);
            java.util.Set<?> limitationSet = (java.util.Set<?>) field.get(limitations);
            
            if (plugin != null) {
                plugin.getLogger().info("[DEBUG] limitationSet大小: " + limitationSet.size());
            }
            
            for (Object pair : limitationSet) {
                Object type = pair.getClass().getMethod("getFirst").invoke(pair);
                Object value = pair.getClass().getMethod("getSecond").invoke(pair);
                
                if (plugin != null) {
                    plugin.getLogger().info("[DEBUG] 限制类型: " + type + ", 值: " + value);
                }
                
                if (type.toString().equals("CONFLICT_ENCHANT")) {
                    String conflictId = value.toString();
                    result.add(conflictId);
                    
                    if (plugin != null) {
                        plugin.getLogger().info("[DEBUG] 添加冲突附魔: " + conflictId);
                    }
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("通过getLimitations获取冲突附魔时出错: " + e.getMessage());
            }
        }
        
        // 方法2：通过getConflicts方法获取
        if (result.isEmpty()) {
            try {
                Method getConflictsMethod = enchantObj.getClass().getMethod("getConflicts");
                Object conflicts = getConflictsMethod.invoke(enchantObj);
                
                if (conflicts instanceof Collection<?>) {
                    Collection<?> conflictCollection = (Collection<?>) conflicts;
                    
                    if (plugin != null) {
                        plugin.getLogger().info("[DEBUG] 通过getConflicts获取到的冲突附魔数量: " + conflictCollection.size());
                    }
                    
                    for (Object conflict : conflictCollection) {
                        String conflictId = conflict.toString();
                        result.add(conflictId);
                        
                        if (plugin != null) {
                            plugin.getLogger().info("[DEBUG] 通过getConflicts添加冲突附魔: " + conflictId);
                        }
                    }
                }
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("通过getConflicts获取冲突附魔时出错: " + e.getMessage());
                }
            }
        }
        
        // 方法3：通过配置获取
        if (result.isEmpty()) {
            try {
                Method getConfigMethod = enchantObj.getClass().getMethod("getConfig");
                Object config = getConfigMethod.invoke(enchantObj);
                
                if (config != null) {
                    try {
                        Method getConflictsMethod = config.getClass().getMethod("getConflicts");
                        Object configConflicts = getConflictsMethod.invoke(config);
                        
                        if (configConflicts instanceof Collection<?>) {
                            Collection<?> conflictCollection = (Collection<?>) configConflicts;
                            
                            if (plugin != null) {
                                plugin.getLogger().info("[DEBUG] 通过配置获取到的冲突附魔数量: " + conflictCollection.size());
                            }
                            
                            for (Object conflict : conflictCollection) {
                                String conflictId = conflict.toString();
                                result.add(conflictId);
                                
                                if (plugin != null) {
                                    plugin.getLogger().info("[DEBUG] 通过配置添加冲突附魔: " + conflictId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (plugin != null) {
                            plugin.getLogger().warning("通过配置获取冲突附魔时出错: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("获取配置对象时出错: " + e.getMessage());
                }
            }
        }
        
        if (plugin != null) {
            plugin.getLogger().info("[DEBUG] 最终获取到的冲突附魔数量: " + result.size() + ", 内容: " + result);
        }
        
        return result;
    }
    
    /**
     * 获取附魔组
     * 
     * @param enchantId 附魔ID
     * @return 附魔组ID列表，如果不存在或API不可用则返回空列表
     */
    public static List<String> getGroups(String enchantId) {
        Object enchant = getEnchantment(enchantId);
        if (enchant == null) {
            return Collections.emptyList();
        }
        
        try {
            // 获取enchant是否有getGroups方法
            List<String> result = new ArrayList<>();
            try {
                Method getGroupsMethod = enchant.getClass().getMethod("getGroups");
                Object groups = getGroupsMethod.invoke(enchant);
                
                if (groups instanceof Collection<?>) {
                    for (Object group : (Collection<?>) groups) {
                        if (group != null) {
                            result.add(group.toString());
                        }
                    }
                }
            } catch (NoSuchMethodException e) {
                // 可能没有直接的getGroups方法，尝试其他方式
                for (Method method : enchant.getClass().getMethods()) {
                    if (method.getName().contains("Group") && method.getName().startsWith("get")) {
                        Object groups = method.invoke(enchant);
                        if (groups instanceof Collection<?>) {
                            for (Object group : (Collection<?>) groups) {
                                if (group != null) {
                                    result.add(group.toString());
                                }
                            }
                            break;
                        }
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "获取附魔组时发生错误: " + enchantId, e);
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 获取所有注册的附魔ID
     * 
     * @return 所有附魔ID列表
     */
    public static List<String> getAllEnchantmentIds() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        
        try {
            Method getEnchantsMethod = enchantmentManager.getClass().getMethod("getEnchants");
            Object enchantsMap = getEnchantsMethod.invoke(enchantmentManager);
            
            if (enchantsMap instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) enchantsMap;
                List<String> result = new ArrayList<>();
                
                for (Object key : map.keySet()) {
                    if (key instanceof NamespacedKey) {
                        result.add(key.toString());
                    } else if (key != null) {
                        result.add(key.toString());
                    }
                }
                
                return result;
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "获取所有附魔ID时发生错误", e);
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 打印所有附魔ID和中文名称的映射关系
     * 此方法用于调试，可以帮助找出哪些附魔无法获取中文名称
     */
    public static void printAllEnchantmentNames() {
        if (!isAvailable() || plugin == null) {
            return;
        }
        
        plugin.getLogger().info("===== 开始打印所有附魔ID和中文名称的映射关系 =====");
        
        try {
            // 获取附魔管理器中的所有附魔
            Method getEnchantsMethod = enchantmentManager.getClass().getMethod("getEnchants");
            Object enchantsMap = getEnchantsMethod.invoke(enchantmentManager);
            
            if (!(enchantsMap instanceof Map)) {
                plugin.getLogger().info("无法获取附魔注册表");
                return;
            }
            
            Map<?, ?> map = (Map<?, ?>) enchantsMap;
            plugin.getLogger().info("共找到 " + map.size() + " 个附魔");
            
            // 打印所有附魔的key
            plugin.getLogger().info("----- 附魔注册表中的所有key -----");
            for (Object key : map.keySet()) {
                plugin.getLogger().info("Key: " + key + " 类型: " + (key != null ? key.getClass().getName() : "null"));
            }
            
            int successCount = 0;
            int failCount = 0;
            
            // 尝试获取每个附魔的中文名称
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object enchantObj = entry.getValue();
                
                String keyStr = key != null ? key.toString() : "null";
                plugin.getLogger().info("----- 处理附魔: " + keyStr + " -----");
                
                // 尝试不同格式的ID
                List<String> idFormats = new ArrayList<>();
                idFormats.add(keyStr);
                
                // 添加冒号和下划线转换后的ID
                if (keyStr.contains(":")) {
                    // 保持原始格式，不再转换冒号为下划线
                    if (keyStr.startsWith("minecraft:")) {
                        // 对于原版附魔，有时候会使用无命名空间的形式
                        idFormats.add(keyStr.substring("minecraft:".length()));
                    }
                } else if (keyStr.contains("_")) {
                    String[] parts = keyStr.split("_", 2);
                    if (parts.length == 2) {
                        idFormats.add(parts[0] + ":" + parts[1]); // 下划线转冒号，用于兼容旧数据
                    }
                } else if (!keyStr.contains(":")) {
                    // 没有命名空间的情况，添加minecraft:前缀
                    idFormats.add("minecraft:" + keyStr);
                }
                
                plugin.getLogger().info("尝试的ID格式: " + idFormats);
                
                // 直接从对象获取名称
                String name = null;
                
                // 方法1: 直接从enchantObj获取名称
                try {
                    // 尝试获取BasicData
                    Method getBasicDataMethod = enchantObj.getClass().getMethod("getBasicData");
                    Object basicData = getBasicDataMethod.invoke(enchantObj);
                    
                    if (basicData != null) {
                        // 尝试获取名称
                        Method getNameMethod = basicData.getClass().getMethod("getName");
                        Object nameObj = getNameMethod.invoke(basicData);
                        
                        if (nameObj instanceof String) {
                            name = (String) nameObj;
                            plugin.getLogger().info("通过BasicData.getName()获取到名称: " + name);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().info("通过BasicData获取名称失败: " + e.getMessage());
                }
                
                // 方法2: 通过API获取名称
                if (name == null) {
                    for (String id : idFormats) {
                        try {
                            name = getEnchantmentName(id);
                            if (name != null && !name.isEmpty()) {
                                plugin.getLogger().info("通过getEnchantmentName('" + id + "')获取到名称: " + name);
                                break;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().info("通过ID '" + id + "' 获取名称失败: " + e.getMessage());
                        }
                    }
                }
                
                // 输出结果
                if (name != null && !name.isEmpty()) {
                    plugin.getLogger().info("[成功] 附魔ID: " + keyStr + " -> 中文名称: " + name);
                    successCount++;
                } else {
                    plugin.getLogger().info("[失败] 附魔ID: " + keyStr + " -> 无法获取中文名称");
                    failCount++;
                }
            }
            
            plugin.getLogger().info("===== 附魔名称映射统计 =====");
            plugin.getLogger().info("总数: " + map.size());
            plugin.getLogger().info("成功: " + successCount);
            plugin.getLogger().info("失败: " + failCount);
            plugin.getLogger().info("=============================");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "打印附魔名称映射时出错", e);
        }
    }
    
    /**
     * 获取附魔的本地化显示名称
     * 
     * @param enchantId 附魔ID
     * @param level 附魔等级
     * @param useRoman 是否使用罗马数字显示等级
     * @return 格式化的附魔显示名称
     */
    public static String getDisplayName(String enchantId, int level, boolean useRoman) {
        Object enchant = getEnchantment(enchantId);
        if (enchant == null) {
            return enchantId;
        }
        
        try {
            // 尝试不同的方法调用
            try {
                Method displayNameMethod = enchant.getClass().getMethod("displayName", Integer.class, Boolean.TYPE);
                return (String) displayNameMethod.invoke(enchant, level, useRoman);
            } catch (NoSuchMethodException e) {
                try {
                    Method displayNameMethod = enchant.getClass().getMethod("displayName", Integer.TYPE, Boolean.TYPE);
                    return (String) displayNameMethod.invoke(enchant, level, useRoman);
                } catch (NoSuchMethodException e2) {
                    // 尝试其他可能的方法
                    for (Method method : enchant.getClass().getMethods()) {
                        if (method.getName().equals("displayName") || 
                            method.getName().equals("getDisplayName")) {
                            if (method.getParameterCount() == 0) {
                                return (String) method.invoke(enchant);
                            } else if (method.getParameterCount() == 1 && 
                                     (method.getParameterTypes()[0] == Integer.class || 
                                      method.getParameterTypes()[0] == int.class)) {
                                return (String) method.invoke(enchant, level);
                            }
                        }
                    }
                }
            }
            
            // 回退到基本名称
            String name = getEnchantmentName(enchantId);
            return name != null ? name : enchantId;
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "获取附魔显示名称时发生错误: " + enchantId, e);
            }
            
            // 回退到基本名称
            String name = getEnchantmentName(enchantId);
            return name != null ? name : enchantId;
        }
    }
    
    /**
     * 检查两个附魔是否冲突
     * 
     * @param enchantId1 第一个附魔ID
     * @param enchantId2 第二个附魔ID
     * @return 是否冲突
     */
    public static boolean conflictsWith(String enchantId1, String enchantId2) {
        Object enchant1 = getEnchantment(enchantId1);
        Object enchant2 = getEnchantment(enchantId2);
        
        if (enchant1 == null || enchant2 == null) {
            return false;
        }
        
        try {
            Method getBukkitEnchantmentMethod = enchant2.getClass().getMethod("getEnchantment");
            Object bukkitEnchant2 = getBukkitEnchantmentMethod.invoke(enchant2);
            
            if (bukkitEnchant2 instanceof Enchantment) {
                Method conflictsWithMethod = enchant1.getClass().getMethod("conflictsWith", Enchantment.class);
                Object result = conflictsWithMethod.invoke(enchant1, bukkitEnchant2);
                return result instanceof Boolean ? (Boolean) result : false;
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "检查附魔冲突时发生错误", e);
            }
        }
        
        return false;
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        enchantmentNameCache.clear();
    }
} 