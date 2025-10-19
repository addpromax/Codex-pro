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
package cc.polarastrum.aiyatsbus.core.util

import taboolib.common5.Coerce

/**
 * 类型转换工具类
 * 
 * 提供安全的类型转换函数，基于 TabooLib 的 Coerce 工具。
 * 支持基本类型和集合类型的转换，包含默认值处理。
 * 避免类型转换异常，提供优雅的错误处理。
 *
 * @author Lanscarlos
 * @since 2022-02-27 16:45
 */

/**
 * 转换为布尔值
 * 
 * 将任意值安全转换为布尔值。
 * 支持字符串、数字、布尔值等多种输入类型。
 *
 * @param def 默认值，当值为 null 时使用
 * @return 转换后的布尔值
 * 
 * @example
 * ```kotlin
 * // 字符串转换
 * "true".coerceBoolean() // 返回 true
 * "false".coerceBoolean() // 返回 false
 * "yes".coerceBoolean() // 返回 true
 * "no".coerceBoolean() // 返回 false
 * 
 * // 数字转换
 * 1.coerceBoolean() // 返回 true
 * 0.coerceBoolean() // 返回 false
 * 
 * // null 值处理
 * null.coerceBoolean(false) // 返回 false
 * null.coerceBoolean(true) // 返回 true
 * 
 * // 在配置文件中使用
 * val enabled = config.getString("feature.enabled").coerceBoolean(false)
 * ```
 */
fun Any?.coerceBoolean(def: Boolean? = null): Boolean {
    if (this == null && def != null) return def
    return Coerce.toBoolean(this)
}

/**
 * 转换为短整型
 * 
 * 将任意值安全转换为短整型。
 * 支持字符串、数字等输入类型，超出范围时会被截断。
 *
 * @param def 默认值，当值为 null 时使用
 * @return 转换后的短整型
 * 
 * @example
 * ```kotlin
 * // 字符串转换
 * "123".coerceShort() // 返回 123
 * "32767".coerceShort() // 返回 32767 (Short.MAX_VALUE)
 * 
 * // 数字转换
 * 100.coerceShort() // 返回 100
 * 
 * // null 值处理
 * null.coerceShort(0) // 返回 0
 * 
 * // 在配置中使用
 * val port = config.getString("server.port").coerceShort(25565)
 * ```
 */
fun Any?.coerceShort(def: Short? = null): Short {
    if (this == null && def != null) return def
    return Coerce.toShort(this)
}

/**
 * 转换为整型
 * 
 * 将任意值安全转换为整型。
 * 支持字符串、数字、浮点数等输入类型。
 *
 * @param def 默认值，当值为 null 时使用
 * @return 转换后的整型
 * 
 * @example
 * ```kotlin
 * // 字符串转换
 * "123".coerceInt() // 返回 123
 * "3.14".coerceInt() // 返回 3
 * 
 * // 数字转换
 * 100.coerceInt() // 返回 100
 * 3.14.coerceInt() // 返回 3
 * 
 * // null 值处理
 * null.coerceInt(0) // 返回 0
 * 
 * // 在配置中使用
 * val maxPlayers = config.getString("server.max-players").coerceInt(20)
 * 
 * // 在计算中使用
 * val damage = "{base} * {multiplier}".calculate(
 *     "base" to config.getString("damage.base").coerceInt(10),
 *     "multiplier" to config.getString("damage.multiplier").coerceDouble(1.0)
 * )
 * ```
 */
fun Any?.coerceInt(def: Int? = null): Int {
    if (this == null && def != null) return def
    return Coerce.toInteger(this)
}

/**
 * 转换为长整型
 * 
 * 将任意值安全转换为长整型。
 * 支持大数值的转换，适用于时间戳等场景。
 *
 * @param def 默认值，当值为 null 时使用
 * @return 转换后的长整型
 * 
 * @example
 * ```kotlin
 * // 字符串转换
 * "123456789".coerceLong() // 返回 123456789L
 * 
 * // 时间戳转换
 * "1640995200000".coerceLong() // 返回时间戳
 * 
 * // null 值处理
 * null.coerceLong(0L) // 返回 0L
 * 
 * // 在时间处理中使用
 * val lastLogin = config.getString("player.last-login").coerceLong(0L)
 * val currentTime = System.currentTimeMillis()
 * val timeDiff = currentTime - lastLogin
 * ```
 */
fun Any?.coerceLong(def: Long? = null): Long {
    if (this == null && def != null) return def
    return Coerce.toLong(this)
}

/**
 * 转换为浮点型
 * 
 * 将任意值安全转换为浮点型。
 * 支持字符串、数字等输入类型。
 *
 * @param def 默认值，当值为 null 时使用
 * @return 转换后的浮点型
 * 
 * @example
 * ```kotlin
 * // 字符串转换
 * "3.14".coerceFloat() // 返回 3.14f
 * "100".coerceFloat() // 返回 100.0f
 * 
 * // 数字转换
 * 3.14.coerceFloat() // 返回 3.14f
 * 100.coerceFloat() // 返回 100.0f
 * 
 * // null 值处理
 * null.coerceFloat(0.0f) // 返回 0.0f
 * 
 * // 在配置中使用
 * val speed = config.getString("player.speed").coerceFloat(1.0f)
 * ```
 */
fun Any?.coerceFloat(def: Float? = null): Float {
    if (this == null && def != null) return def
    return Coerce.toFloat(this)
}

/**
 * 转换为双精度浮点型
 * 
 * 将任意值安全转换为双精度浮点型。
 * 提供最高精度的数值转换。
 *
 * @param def 默认值，当值为 null 时使用
 * @return 转换后的双精度浮点型
 * 
 * @example
 * ```kotlin
 * // 字符串转换
 * "3.14159".coerceDouble() // 返回 3.14159
 * "100".coerceDouble() // 返回 100.0
 * 
 * // 数字转换
 * 3.14159.coerceDouble() // 返回 3.14159
 * 100.coerceDouble() // 返回 100.0
 * 
 * // null 值处理
 * null.coerceDouble(0.0) // 返回 0.0
 * 
 * // 在数学计算中使用
 * val pi = config.getString("math.pi").coerceDouble(3.14159)
 * val radius = config.getString("circle.radius").coerceDouble(5.0)
 * val area = pi * radius * radius
 * 
 * // 在概率计算中使用
 * val chance = config.getString("drop.chance").coerceDouble(0.1)
 * if (Math.random() < chance) {
 *     // 触发掉落
 * }
 * ```
 */
fun Any?.coerceDouble(def: Double? = null): Double {
    if (this == null && def != null) return def
    return Coerce.toDouble(this)
}

/**
 * 转换为列表
 * 
 * 将任意值安全转换为指定类型的列表。
 * 支持数组、集合等多种输入类型。
 *
 * @param T 列表元素类型
 * @param def 默认列表，当转换失败时使用
 * @param transfer 元素转换函数
 * @return 转换后的列表
 * 
 * @example
 * ```kotlin
 * // 字符串列表转换
 * listOf("1", "2", "3").coerceList { it.toString().toInt() } // 返回 [1, 2, 3]
 * 
 * // 数组转换
 * arrayOf("a", "b", "c").coerceList { it.toString().uppercase() } // 返回 ["A", "B", "C"]
 * 
 * // 混合类型转换
 * listOf("10", "20", "30").coerceList { it.toString().toDouble() } // 返回 [10.0, 20.0, 30.0]
 * 
 * // 在配置中使用
 * val allowedWorlds = config.getStringList("allowed-worlds").coerceList { it.toString() }
 * 
 * // 处理转换失败
 * val invalidList = listOf("1", "invalid", "3")
 * val numbers = invalidList.coerceList { it.toString().toIntOrNull() ?: 0 } // 返回 [1, 0, 3]
 * ```
 */
inline fun <reified T> Any?.coerceList(def: List<T> = emptyList(), transfer: (Any?) -> T): List<T> {
    return when (this) {
        is T -> listOf(this)
        is Array<*> -> this.map(transfer)
        is Collection<*> -> this.map(transfer)
        else -> def
    }
}

/**
 * 转换为非空列表
 * 
 * 将任意值安全转换为指定类型的非空列表，过滤掉 null 值。
 * 适用于需要过滤无效数据的场景。
 *
 * @param T 列表元素类型
 * @param def 默认列表，当转换失败时使用
 * @param transfer 元素转换函数，返回 null 的元素将被过滤
 * @return 转换后的非空列表
 * 
 * @example
 * ```kotlin
 * // 过滤无效数字
 * listOf("1", "invalid", "3").coerceListNotNull { it.toString().toIntOrNull() } // 返回 [1, 3]
 * 
 * // 过滤空字符串
 * listOf("hello", "", "world", null).coerceListNotNull { it.toString().takeIf { str -> str.isNotEmpty() } } // 返回 ["hello", "world"]
 * 
 * // 在配置中使用
 * val validPlayers = config.getStringList("players").coerceListNotNull { 
 *     it.toString().takeIf { name -> name.isNotEmpty() } 
 * }
 * 
 * // 处理复杂转换
 * val items = listOf("diamond", "invalid_item", "iron")
 * val validItems = items.coerceListNotNull { 
 *     Material.getMaterial(it.toString().uppercase()) 
 * } // 返回 [DIAMOND, IRON]
 * ```
 */
inline fun <reified T: Any> Any?.coerceListNotNull(def: List<T> = emptyList(), transfer: (Any?) -> T?): List<T> {
    return when (this) {
        is T -> listOf(this)
        is Array<*> -> this.mapNotNull(transfer)
        is Collection<*> -> this.mapNotNull(transfer)
        else -> def
    }
}