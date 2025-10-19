package com.magicbili.enchantdisassembler;

import org.bukkit.enchantments.Enchantment;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * EcoEnchants 集成类，仅负责从 EcoEnchants API 获取附魔稀有度信息。
 * 若 EcoEnchants 不可用，所有方法均返回默认值或空集合。
 */
public class EcoEnchantsIntegration {
    private final EnchantDisassembler plugin;

    private boolean ecoAvailable = false;
    private Class<?> utilKtClass;                // com.willfp.ecoenchants.enchant.UtilKt
    private boolean logged = false;

    public EcoEnchantsIntegration(EnchantDisassembler plugin) {
        this.plugin = plugin;
        detectEco();
    }

    private void detectEco() {
        try {
            utilKtClass = Class.forName("com.willfp.ecoenchants.enchant.UtilKt");
            ecoAvailable = true;
            plugin.getLogger().info("§a检测到 EcoEnchants，附魔稀有度将由其提供");
        } catch (ClassNotFoundException e) {
            ecoAvailable = false;
            plugin.getLogger().info("§eEcoEnchants 未检测到");
        } catch (Exception ex) {
            ecoAvailable = false;
            plugin.getLogger().warning("§cEcoEnchants 检测失败: " + ex.getMessage());
            if (plugin.isDebug()) {
                ex.printStackTrace();
            }
        }
    }

    public boolean isAvailable() {
        return ecoAvailable;
    }

    /**
     * 获取稀有度 ID，如 "common"、"rare" 等。
     */
    public String getEnchantRarity(Enchantment enchant) {
        if (!ecoAvailable) {
            return "common";
        }
        try {
            Method wrap = utilKtClass.getMethod("wrap", Enchantment.class);
            Object ecoLike = wrap.invoke(null, enchant);
            if (ecoLike != null) {
                Object rarityObj = ecoLike.getClass().getMethod("getEnchantmentRarity").invoke(ecoLike);
                if (rarityObj != null) {
                    return (String) rarityObj.getClass().getMethod("getId").invoke(rarityObj);
                }
            }
        } catch (Exception e) {
            if (!logged) {
                plugin.getLogger().warning("§cEcoEnchants API 调用失败，将返回默认稀有度。错误: " + e.getMessage());
                logged = true;
            }
            if (plugin.isDebug()) {
                e.printStackTrace();
            }
        }
        return "common";
    }

    /**
     * EcoEnchants 暂未暴露权重 -> 乘数 API，默认返回空 Map。
     */
    public Map<String, Double> getRarityMultipliers() {
        return new HashMap<>();
    }

    public Set<String> getAllRarities() {
        return Collections.emptySet();
    }
} 