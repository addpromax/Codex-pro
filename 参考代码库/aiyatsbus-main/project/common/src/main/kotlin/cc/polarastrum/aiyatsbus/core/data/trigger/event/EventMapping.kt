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
package cc.polarastrum.aiyatsbus.core.data.trigger.event

import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.core.util.enumOf
import org.bukkit.inventory.EquipmentSlot
import taboolib.common.platform.event.EventPriority
import taboolib.library.configuration.ConfigurationSection

/**
 * 事件映射类
 *
 * 定义事件与附魔系统的映射关系，包含事件类、装备槽位、引用字段等配置。
 * 用于将 Bukkit 事件映射到附魔触发系统中。
 *
 * @author mical
 * @since 2024/7/18 00:29
 */
class EventMapping @JvmOverloads constructor(
    /** 配置根节点 */
    private val root: ConfigurationSection,

    /** 事件类名，自动替换包名 */
    val clazz: String = root.getString("class")!!
        .replace("com.mcstarrysky.aiyatsbus", "cc.polarastrum.aiyatsbus"), // 自己给自己挖了一个大坑

    /** 装备槽位列表，支持单个或多个槽位 */
    val slots: List<EquipmentSlot> = if (root.isList("slots")) root.getStringList("slots")
        .mapNotNull { it.enumOf<EquipmentSlot>() } else listOfNotNull(
        root.getString("slots").enumOf<EquipmentSlot>()
    ),

    /** 玩家引用字段名，支持多种命名格式 */
    val playerReference: String? = root.getString("playerReference") ?: root.getString("player")
    ?: root.getString("player-reference"),

    /** 物品引用字段名，支持多种命名格式 */
    val itemReference: String? = root.getString("itemReference") ?: root.getString("item")
    ?: root.getString("item-reference"),

    /** 事件优先级，默认为 HIGHEST */
    val eventPriority: EventPriority = root.getString("priority").enumOf<EventPriority>() ?: EventPriority.HIGHEST,

    /** 是否忽略已取消的事件，默认为 true */
    val ignoreCancelled: Boolean = (root.getString("ignoreCancelled")
        ?: root.getString("ignore-cancelled")).coerceBoolean(true)
) {

    /** 解构函数：事件类名 */
    operator fun component1() = clazz
    /** 解构函数：装备槽位列表 */
    operator fun component2() = slots
    /** 解构函数：玩家引用字段名 */
    operator fun component3() = playerReference
    /** 解构函数：物品引用字段名 */
    operator fun component4() = itemReference
    /** 解构函数：事件优先级 */
    operator fun component5() = eventPriority
    /** 解构函数：是否忽略已取消的事件 */
    operator fun component6() = ignoreCancelled
}