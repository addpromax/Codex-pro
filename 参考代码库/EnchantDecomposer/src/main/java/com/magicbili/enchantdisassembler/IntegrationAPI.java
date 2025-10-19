package com.magicbili.enchantdisassembler;

import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 统一的附魔框架兼容层，内部持有 EcoEnchants 与 Aiyatsbus 两个实现。
 * 调用时优先 EcoEnchants，其次 Aiyatsbus，均不可用时返回默认值。 
 */
public class IntegrationAPI {
    private final EcoEnchantsIntegration eco;
    private final AiyatsbusIntegration aiya;

    public IntegrationAPI(EnchantDisassembler plugin) {
        this.eco = new EcoEnchantsIntegration(plugin);
        this.aiya = new AiyatsbusIntegration(plugin);
    }

    /** 是否检测到任意框架 */
    public boolean hasAnyFramework() {
        return eco.isAvailable() || aiya.isAiyatsbusAvailable();
    }

    public String getEnchantRarity(Enchantment enchant) {
        if (eco.isAvailable()) {
            return eco.getEnchantRarity(enchant);
        }
        if (aiya.isAiyatsbusAvailable()) {
            return aiya.getEnchantRarity(enchant);
        }
        return "common";
    }

    public Map<String, Double> getRarityMultipliers() {
        if (eco.isAvailable()) {
            return eco.getRarityMultipliers();
        }
        if (aiya.isAiyatsbusAvailable()) {
            return aiya.getRarityMultipliers();
        }
        return new HashMap<>();
    }

    public Set<String> getAllRarities() {
        if (eco.isAvailable()) {
            return eco.getAllRarities();
        }
        if (aiya.isAiyatsbusAvailable()) {
            return aiya.getAllRarities();
        }
        return new HashSet<>();
    }

    public AiyatsbusIntegration getAiyatsbus() {
        return aiya;
    }

    public EcoEnchantsIntegration getEco() {
        return eco;
    }
} 