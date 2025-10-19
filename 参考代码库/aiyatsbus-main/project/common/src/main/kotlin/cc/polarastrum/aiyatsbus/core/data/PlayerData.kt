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

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentFilter
import cc.polarastrum.aiyatsbus.core.FilterStatement
import cc.polarastrum.aiyatsbus.core.FilterType
import cc.polarastrum.aiyatsbus.core.aiyatsbusEt
import taboolib.common5.clong

/**
 * Aiyatsbus 玩家数据类
 *
 * 存储和管理玩家的附魔相关数据，包括菜单模式、收藏夹、过滤器和冷却时间。
 * 支持数据的序列化和反序列化，用于持久化存储。
 *
 * @author mical
 * @since 2024/2/18 12:59
 */
data class PlayerData(private val serializedData: String?) {
    /** 菜单模式 */
    var menuMode: MenuMode = MenuMode.NORMAL
    /** 收藏的附魔列表 */
    var favorites: MutableList<String> = mutableListOf()
    /** 过滤器设置 */
    var filters: Map<FilterType, MutableMap<String, FilterStatement>> =
        FilterType.values().associateWith { mutableMapOf() }
    /** 冷却时间映射 */
    var cooldown: MutableMap<String, Long> = mutableMapOf()

    init {
        serializedData?.let {
            serializedData.split("||") // 通过 || 分割数据
                .map { pair -> pair.split("==")[0] to pair.split("==")[1] } // 等号拼接键和值
                .forEach { (key, value) ->
                    when (key) {
                        "menu_mode" -> menuMode = MenuMode.valueOf(value)
                        "favorites" -> favorites.addAll(value.split(";").mapNotNull { id -> aiyatsbusEt(id)?.basicData?.id })

                        "filters" -> {
                            var tot = 0
                            // $ 分割 FilterType
                            value.split("$").forEach { content ->
                                filters[AiyatsbusEnchantmentFilter.filterTypes[tot++]]!!.putAll(content.split(";")
                                    .filter { filter -> filter.isNotBlank() }
                                    .associate { filter ->
                                        filter.split("=")[0] to
                                                FilterStatement.valueOf(filter.split("=")[1])
                                    })
                            }
                        }

                        "cooldown" -> {
                            cooldown.putAll(value
                                .split(";")
                                .filter { pair -> pair.isNotBlank() }
                                .associate { pair -> pair.split("=")[0] to pair.split("=")[1].clong })
                        }

                        else -> {}
                    }
                }
        }
    }

    /**
     * 序列化玩家数据
     *
     * @return 序列化后的字符串
     */
    fun serialize() = "menu_mode==$menuMode||" +
            "favorites==${favorites.joinToString(";")}||" +
            "filters==${
                AiyatsbusEnchantmentFilter.filterTypes.map {
                    filters[it]!!.map { (value, state) -> "$value=$state" }.joinToString(";")
                }.joinToString("$")
            }||" +
            "cooldown==${cooldown.map { (id, stamp) -> "$id=$stamp" }.joinToString(";")}"
}

/**
 * 菜单模式枚举
 */
enum class MenuMode {
    /** 普通模式 */
    NORMAL,
    /** 作弊模式 */
    CHEAT
}
