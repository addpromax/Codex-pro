package com.magicbili.enchantdisassembler;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Aiyatsbus集成类，用于处理稀有度获取
 * 当Aiyatsbus可用时使用其API，否则回退到配置文件
 */
public class AiyatsbusIntegration {
    private final EnchantDisassembler plugin;
    private boolean aiyatsbusAvailable = false;
    
    // 缓存的反射类和方法
    private Class<?> aiyatsbusUtilsClass;
    private Class<?> rarityClass;
    // EcoEnchants 相关
    private boolean ecoEnchantsAvailable = false;
    private Class<?> ecoUtilKtClass;
    private Class<?> ecoEnchantmentRarityClass;
    private boolean loggedApiSuccess = false; // Aiyatsbus

    public AiyatsbusIntegration(EnchantDisassembler plugin) {
        this.plugin = plugin;
        checkAiyatsbusAvailability();
        checkEcoEnchantsAvailability();
    }

    /**
     * 检查Aiyatsbus是否可用
     */
    private void checkAiyatsbusAvailability() {
        try {
            // 尝试加载Aiyatsbus工具类
            aiyatsbusUtilsClass = Class.forName("cc.polarastrum.aiyatsbus.core.AiyatsbusUtilsKt");
            rarityClass = Class.forName("cc.polarastrum.aiyatsbus.core.data.registry.Rarity");
            
            plugin.getLogger().info("§aAiyatsbus类已加载，将在首次使用时检测API可用性");
            aiyatsbusAvailable = true; // 先标记为可用，在首次使用时再检测
            
        } catch (ClassNotFoundException e) {
            aiyatsbusAvailable = false;
            plugin.getLogger().info("§eAiyatsbus未检测到，将使用配置文件获取稀有度");
        } catch (Exception e) {
            aiyatsbusAvailable = false;
            plugin.getLogger().warning("§cAiyatsbus检测失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查 EcoEnchants 是否可用
     */
    private void checkEcoEnchantsAvailability() {
        try {
            // 尝试加载 EcoEnchants 的工具类（顶级扩展函数所在的 UtilKt）
            ecoUtilKtClass = Class.forName("com.willfp.ecoenchants.enchant.UtilKt");
            ecoEnchantmentRarityClass = Class.forName("com.willfp.ecoenchants.rarity.EnchantmentRarity");

            plugin.getLogger().info("§a检测到 EcoEnchants，将在首次使用时尝试其 API");
            ecoEnchantsAvailable = true;
        } catch (ClassNotFoundException e) {
            ecoEnchantsAvailable = false;
            plugin.getLogger().info("§eEcoEnchants 未检测到");
        } catch (Exception e) {
            ecoEnchantsAvailable = false;
            plugin.getLogger().warning("§cEcoEnchants 检测失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 延迟检测Aiyatsbus API是否真正可用
     */
    private boolean checkAiyatsbusAPIAvailability() {
        if (!aiyatsbusAvailable) {
            return false;
        }

        try {
            // 简单调用 util 方法，若可正常返回对象则视为可用
            Method testMethod = aiyatsbusUtilsClass.getMethod("aiyatsbusEt", String.class);
            Object testEnchant = testMethod.invoke(null, "sharpness");
            if (testEnchant == null) {
                plugin.getLogger().warning("§eAiyatsbus 返回 null(测试)，API 可能仍在加载中");
                return false;
            }
 
            if (!loggedApiSuccess) {
                plugin.getLogger().info("§aAiyatsbus API 检测成功，将使用其API获取稀有度");
                loggedApiSuccess = true;
            }
            return true;
 
        } catch (Exception e) {
            plugin.getLogger().warning("§cAiyatsbus API 检测失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * 延迟检测 EcoEnchants API 是否真正可用
     */
    private boolean checkEcoAPIAvailability(Enchantment sampleEnchant) {
        if (!ecoEnchantsAvailable) {
            return false;
        }

        try {
            // 调用 UtilKt.wrap(enchant) 方法
            java.lang.reflect.Method wrapMethod = ecoUtilKtClass.getMethod("wrap", Enchantment.class);
            Object ecoLike = wrapMethod.invoke(null, sampleEnchant);

            if (ecoLike == null) {
                return false;
            }

            // 尝试获取稀有度
            java.lang.reflect.Method getRarityMethod = ecoLike.getClass().getMethod("getEnchantmentRarity");
            Object rarityObj = getRarityMethod.invoke(ecoLike);

            if (rarityObj != null) {
                if (!loggedApiSuccess) {
                    plugin.getLogger().info("§aEcoEnchants API 检测成功，将使用其 API 获取稀有度");
                    loggedApiSuccess = true;
                }
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("§cEcoEnchants API 检测失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 获取附魔的稀有度
     * @param enchant 附魔
     * @return 稀有度ID
     */
    public String getEnchantRarity(Enchantment enchant) {
        // 1. 尝试 EcoEnchants
        if (ecoEnchantsAvailable && checkEcoAPIAvailability(enchant)) {
            try {
                Method wrapMethod = ecoUtilKtClass.getMethod("wrap", Enchantment.class);
                Object ecoLike = wrapMethod.invoke(null, enchant);

                if (ecoLike != null) {
                    Object rarityObj = ecoLike.getClass().getMethod("getEnchantmentRarity").invoke(ecoLike);
                    if (rarityObj != null) {
                        // Kotlin val id -> getId()
                        return (String) rarityObj.getClass().getMethod("getId").invoke(rarityObj);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("EcoEnchants API 调用失败: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            }
        }

        // 2. 尝试 Aiyatsbus
        if (aiyatsbusAvailable && checkAiyatsbusAPIAvailability()) {
            try {
                String keySimple = enchant.getKey().getKey(); // e.g. "sharpness"
                String keyFull = enchant.getKey().toString();  // e.g. "minecraft:sharpness" or custom

                Method etMethod = aiyatsbusUtilsClass.getMethod("aiyatsbusEt", String.class);
                Object etObj = etMethod.invoke(null, keySimple);
                if (etObj == null) {
                    etObj = etMethod.invoke(null, keyFull);
                }
                if (etObj != null) {
                    Object rarityObj = etObj.getClass().getMethod("getRarity").invoke(etObj);
                    if (rarityObj != null) {
                        return getRarityName(rarityObj);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Aiyatsbus API 调用失败: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            }
        }

        // 3. 未能获取时返回默认值
        return "common";
    }

    /**
     * 处理附魔分解
     * @param enchant 附魔
     * @param level 等级
     * @return 分解点数
     */
    public double processEnchantment(Enchantment enchant, int level) {
        try {
            // 使用 getEnchantRarity 已经包含 Eco→Aiyatsbus→Config 的优先级
            String rarity = getEnchantRarity(enchant);
            double baseValue = getBaseValueFromConfig(enchant);
            double levelIncr = plugin.getConfig().getDouble("level_increment", 0.5);
            double multiplier = getRarityMultipliers().getOrDefault(rarity, 1.0);
            double levelFactor = 1 + (level - 1) * levelIncr;
            return baseValue * levelFactor * multiplier;
        } catch (Exception e) {
            plugin.getLogger().warning("附魔分解计算失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }

        return processEnchantmentFromConfig(enchant, level);
    }

    /**
     * 获取所有稀有度
     * @return 稀有度集合
     */
    public Set<String> getAllRarities() {
        if (!aiyatsbusAvailable || !checkAiyatsbusAPIAvailability()) {
            return new java.util.HashSet<>();
        }

        try {
            // 使用Aiyatsbus API - 根据api.txt中的接口
            Object rarities = rarityClass.getField("Companion").get(null);
            if (rarities != null) {
                return getRaritiesKeySet(rarities);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Aiyatsbus API调用失败，回退到配置文件: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }

        return new java.util.HashSet<>();
    }

    /**
     * 获取稀有度乘数
     * @return 稀有度乘数映射
     */
    public Map<String, Double> getRarityMultipliers() {
        if (!aiyatsbusAvailable || !checkAiyatsbusAPIAvailability()) {
            return new java.util.HashMap<>();
        }

        try {
            // 使用Aiyatsbus API - 根据api.txt中的接口
            Object rarities = rarityClass.getField("Companion").get(null);
            if (rarities != null) {
                return getRaritiesMultipliers(rarities);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Aiyatsbus API调用失败，回退到配置文件: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }

        return new java.util.HashMap<>();
    }

    // 配置文件回退方法
    private String getEnchantRarityFromConfig(Enchantment enchant) {
        String enchantKey = enchant.getKey().getKey();
        Map<String, Map<String, Double>> rarityItems = plugin.getConfigManager().getRarityItems();
        Map<String, Double> multipliers = plugin.getConfigManager().getRarityMultipliers();

        String highestRarity = "common";
        double highestMultiplier = 1.0;

        for (Map.Entry<String, Map<String, Double>> entry : rarityItems.entrySet()) {
            if (entry.getValue().containsKey(enchantKey)) {
                String rarity = entry.getKey();
                double multiplier = multipliers.getOrDefault(rarity, 1.0);
                if (multiplier > highestMultiplier) {
                    highestMultiplier = multiplier;
                    highestRarity = rarity;
                }
            }
        }

        return highestRarity;
    }

    private double processEnchantmentFromConfig(Enchantment enchant, int level) {
        String enchantKey = enchant.getKey().getKey();
        Map<String, Map<String, Double>> rarityItems = plugin.getConfigManager().getRarityItems();
        Map<String, Double> multipliers = plugin.getConfigManager().getRarityMultipliers();

        String rarity = "common";
        double baseValue = 1.0;

        for (Map.Entry<String, Map<String, Double>> entry : rarityItems.entrySet()) {
            if (entry.getValue().containsKey(enchantKey)) {
                rarity = entry.getKey();
                baseValue = entry.getValue().get(enchantKey);
                break;
            }
        }

        double multiplier = multipliers.getOrDefault(rarity, 1.0);
        double levelFactor = 1 + (level - 1) * 0.5;
        return baseValue * levelFactor * multiplier;
    }

    private double getBaseValueFromConfig(Enchantment enchant) {
        String enchantKey = enchant.getKey().getKey();
        Map<String, Map<String, Double>> rarityItems = plugin.getConfigManager().getRarityItems();
        
        for (Map.Entry<String, Map<String, Double>> entry : rarityItems.entrySet()) {
            if (entry.getValue().containsKey(enchantKey)) {
                return entry.getValue().get(enchantKey);
            }
        }
        
        return 1.0; // 默认值
    }

    private String getRarityName(Object rarity) throws Exception {
        if (rarity == null) {
            throw new RuntimeException("稀有度对象为 null");
        }

        Class<?> clazz = rarity.getClass();

        // 1. 尝试常见的 getter 方法
        try {
            Method getIdMethod = clazz.getMethod("getId");
            Object id = getIdMethod.invoke(rarity);
            if (id != null) {
                return id.toString();
            }
        } catch (NoSuchMethodException ignored) {}

        try {
            Method getNameMethod = clazz.getMethod("getName");
            Object name = getNameMethod.invoke(rarity);
            if (name != null) {
                return name.toString();
            }
        } catch (NoSuchMethodException ignored) {}

        // 2. 如果是枚举, 直接使用枚举名称
        if (rarity instanceof Enum<?>) {
            return ((Enum<?>) rarity).name().toLowerCase();
        }

        // 3. 尝试读取字段
        try {
            java.lang.reflect.Field nameField = clazz.getDeclaredField("name");
            nameField.setAccessible(true);
            Object name = nameField.get(rarity);
            if (name != null) {
                return name.toString();
            }
        } catch (NoSuchFieldException ignored) {}

        // 4. 最后回退 toString()
        return rarity.toString();
    }

    private int getRarityWeight(Object rarity) throws Exception {
        if (rarity == null) {
            throw new RuntimeException("稀有度对象为 null");
        }

        Class<?> clazz = rarity.getClass();

        try {
            Method getWeightMethod = clazz.getMethod("getWeight");
            Object weightObj = getWeightMethod.invoke(rarity);
            if (weightObj instanceof Number) {
                return ((Number) weightObj).intValue();
            }
        } catch (NoSuchMethodException ignored) {}

        try {
            java.lang.reflect.Field weightField = clazz.getDeclaredField("weight");
            weightField.setAccessible(true);
            Object weightObj = weightField.get(rarity);
            if (weightObj instanceof Number) {
                return ((Number) weightObj).intValue();
            }
        } catch (NoSuchFieldException ignored) {}

        // 若无法获取, 返回默认权重 100
        return 100;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getRaritiesKeySet(Object rarities) throws Exception {
        Map<?, ?> rarityMap = (Map<?, ?>) rarities;
        Set<String> rarityNames = new java.util.HashSet<>();
        
        for (Object rarity : rarityMap.values()) {
            if (rarity != null) {
                String rarityName = getRarityName(rarity);
                rarityNames.add(rarityName);
            }
        }
        
        return rarityNames;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> getRaritiesMultipliers(Object rarities) throws Exception {
        Map<?, ?> rarityMap = (Map<?, ?>) rarities;
        Map<String, Double> multipliers = new HashMap<>();
        
        for (Object rarity : rarityMap.values()) {
            if (rarity != null) {
                String rarityName = getRarityName(rarity);
                int weight = getRarityWeight(rarity);
                double multiplier = weight / 100.0; // 将权重转换为乘数
                multipliers.put(rarityName, multiplier);
            }
        }
        
        return multipliers;
    }

    /**
     * 检查Aiyatsbus是否可用
     * @return 是否可用
     */
    public boolean isAiyatsbusAvailable() {
        return aiyatsbusAvailable;
    }

    /**
     * EcoEnchants 是否可用
     */
    public boolean isEcoEnchantsAvailable() {
        return ecoEnchantsAvailable;
    }
} 