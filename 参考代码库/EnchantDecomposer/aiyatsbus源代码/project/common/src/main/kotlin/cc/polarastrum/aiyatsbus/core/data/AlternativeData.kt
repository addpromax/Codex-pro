/*
 *  Copyright (C) 2022-2024 PolarAstrumLab
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cc.polarastrum.aiyatsbus.core.data

import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.core.util.coerceInt
import taboolib.library.configuration.ConfigurationSection

/**
 * 附魔额外数据类
 *
 * 包含附魔的可选配置信息，这些数据一般不需要填写，使用默认值即可。
 * 定义了附魔的各种行为属性，如是否可移除、权重、是否宝藏附魔等。
 *
 * @author mical
 * @since 2024/2/17 15:11
 */
data class AlternativeData(
    /** 配置根节点，可为空 */
    private val root: ConfigurationSection?,

    /** 附魔权重，影响获取概率，默认为 100 */
    val weight: Int = root?.getInt("weight", 100).coerceInt(100),

    /** 是否为原版附魔，用于检测原版附魔，默认为 false */
    /** 3.0 的检测原版附魔的方法有点弱智，把检测原版放到这里其实是更好的选择 */
    val isVanilla: Boolean = (root?.getBoolean("is-vanilla", false) ?: root?.getBoolean("is_vanilla", false)).coerceBoolean(false),

    /** 是否可通过砂轮移除，默认为 true */
    val grindstoneable: Boolean = root?.getBoolean("grindstoneable", true).coerceBoolean(true),
    /** 是否为宝藏附魔，默认为 false */
    val isTreasure: Boolean = (root?.getBoolean("is-treasure", false) ?: root?.getBoolean("is_treasure", false)).coerceBoolean(false),
    /** 是否为诅咒附魔，默认为 false */
    val isCursed: Boolean = (root?.getBoolean("is-cursed", false) ?: root?.getBoolean("is_cursed", false)).coerceBoolean(false),
    /** 是否可通过交易获得，默认为 true */
    val isTradeable: Boolean = (root?.getBoolean("is-tradeable", true) ?: root?.getBoolean("is_tradeable", true)).coerceBoolean(true),
    /** 是否可通过附魔台发现，默认为 true */
    val isDiscoverable: Boolean = (root?.getBoolean("is-discoverable", true) ?: root?.getBoolean("is_discoverable", true)).coerceBoolean(true),

    /** 交易最大等级限制，-1 表示无限制，默认为 -1 */
    val tradeMaxLevel: Int = (root?.getInt("trade-max-level", -1) ?: root?.getInt("trade_max_level", -1)).coerceInt(-1),
    /** 附魔台最大等级限制，-1 表示无限制，默认为 -1 */
    val enchantMaxLevel: Int = (root?.getInt("enchant-max-level", -1) ?: root?.getInt("enchant-max-level", -1)).coerceInt(-1),
    /** 战利品最大等级限制，-1 表示无限制，默认为 -1 */
    val lootMaxLevel: Int = (root?.getInt("loot-max-level", -1) ?: root?.getInt("loot_max_level", -1)).coerceInt(-1),

    /** 是否不可获得，为 true 时玩家无法获得该附魔，默认为 false */
    val inaccessible: Boolean = root?.getBoolean("inaccessible", false).coerceBoolean(false),
) {

    /**
     * 获取交易等级限制
     *
     * 优先使用附魔自身的交易等级限制，其次使用全局限制，最后使用最大等级。
     * 返回的值不会超过附魔的最大等级。
     *
     * @param maxLevel 附魔的最大等级
     * @param globalLimit 全局等级限制
     * @return 实际可用的交易等级限制
     *
     * @example
     * ```kotlin
     * val limit = alternativeData.getTradeLevelLimit(5, 3) // 返回 3
     * ```
     */
    fun getTradeLevelLimit(maxLevel: Int, globalLimit: Int): Int {
        return (if (tradeMaxLevel != -1) tradeMaxLevel else if (globalLimit != -1) globalLimit else maxLevel).coerceAtMost(maxLevel)
    }

    /**
     * 获取附魔台等级限制
     *
     * 优先使用附魔自身的附魔台等级限制，其次使用全局限制，最后使用最大等级。
     * 返回的值不会超过附魔的最大等级。
     *
     * @param maxLevel 附魔的最大等级
     * @param globalLimit 全局等级限制
     * @return 实际可用的附魔台等级限制
     *
     * @example
     * ```kotlin
     * val limit = alternativeData.getEnchantMaxLevelLimit(5, 3) // 返回 3
     * ```
     */
    fun getEnchantMaxLevelLimit(maxLevel: Int, globalLimit: Int): Int {
        return (if (enchantMaxLevel != -1) enchantMaxLevel else if (globalLimit != -1) globalLimit else maxLevel).coerceAtMost(maxLevel)
    }

    /**
     * 获取战利品等级限制
     *
     * 优先使用附魔自身的战利品等级限制，其次使用全局限制，最后使用最大等级。
     * 返回的值不会超过附魔的最大等级。
     *
     * @param maxLevel 附魔的最大等级
     * @param globalLimit 全局等级限制
     * @return 实际可用的战利品等级限制
     *
     * @example
     * ```kotlin
     * val limit = alternativeData.getLootMaxLevelLimit(5, 3) // 返回 3
     * ```
     */
    fun getLootMaxLevelLimit(maxLevel: Int, globalLimit: Int): Int {
        return (if (lootMaxLevel != -1) lootMaxLevel else if (globalLimit != -1) globalLimit else maxLevel).coerceAtMost(maxLevel)
    }
}