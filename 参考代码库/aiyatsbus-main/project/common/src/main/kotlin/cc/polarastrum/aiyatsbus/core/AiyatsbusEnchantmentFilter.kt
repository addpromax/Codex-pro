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
package cc.polarastrum.aiyatsbus.core

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Aiyatsbus 附魔过滤器接口
 *
 * 负责过滤和管理附魔的可见性和可用性。
 * 提供基于品质、目标、组别、字符串等条件的过滤功能。
 *
 * @author mical
 * @since 2024/2/18 12:47
 */
interface AiyatsbusEnchantmentFilter {

    /**
     * 根据过滤条件过滤附魔
     *
     * @param filters 过滤条件映射
     * @return 过滤后的附魔列表
     */
    fun filter(filters: Map<FilterType, Map<String, FilterStatement>>): List<AiyatsbusEnchantment>

    /**
     * 生成过滤器的 Lore 显示
     *
     * @param type 过滤器类型
     * @param player 玩家
     * @return Lore 列表
     */
    fun generateLore(type: FilterType, player: Player): List<String>

    /**
     * 根据规则生成过滤器的 Lore 显示
     *
     * @param type 过滤器类型
     * @param rules 过滤规则
     * @param player 玩家
     * @return Lore 列表
     */
    fun generateLore(type: FilterType, rules: Map<String, FilterStatement>, player: Player? = null): List<String>

    /**
     * 清除玩家的所有过滤器
     *
     * @param player 玩家
     */
    fun clearFilters(player: Player)

    /**
     * 清除玩家的指定类型过滤器
     *
     * @param player 玩家
     * @param type 过滤器类型
     */
    fun clearFilter(player: Player, type: FilterType)

    /**
     * 获取玩家的过滤器状态
     *
     * @param player 玩家
     * @param type 过滤器类型
     * @param value 过滤值
     * @return 过滤器状态
     */
    fun getStatement(player: Player, type: FilterType, value: String): FilterStatement?

    /**
     * 添加过滤器
     *
     * @param player 玩家
     * @param type 过滤器类型
     * @param value 过滤值
     * @param state 过滤器状态
     */
    fun addFilter(player: Player, type: FilterType, value: String, state: FilterStatement)

    /**
     * 清除指定过滤器
     *
     * @param player 玩家
     * @param type 过滤器类型
     * @param value 过滤值
     */
    fun clearFilter(player: Player, type: FilterType, value: Any)

    companion object {

        /** 所有过滤器类型列表 */
        val filterTypes = FilterType.values().toList()
    }
}

/**
 * 过滤器类型枚举
 */
enum class FilterType {
    /** 品质 */
    RARITY,
    /** 物品类别 */
    TARGET,
    /** 类型/定位 */
    GROUP,
    /** 名字/描述 */
    STRING;

    /**
     * 获取显示名称
     *
     * @param player 玩家
     * @return 显示名称
     */
    fun display(player: Player?): String {
        return (player ?: Bukkit.getConsoleSender()).asLang("filter-type-${name.lowercase()}")
    }
}

/**
 * 过滤器状态枚举
 */
enum class FilterStatement {
    /** 开启 */
    ON,
    /** 关闭 */
    OFF;

    /**
     * 获取状态符号
     *
     * @param player 玩家
     * @return 状态符号
     */
    fun symbol(player: Player?): String {
        return (player ?: Bukkit.getConsoleSender()).asLang("filter-statement-symbol-${name.lowercase()}")
    }
}