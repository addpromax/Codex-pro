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

import cc.polarastrum.aiyatsbus.core.util.MathUtils.calculate
import org.bukkit.entity.Player
import taboolib.platform.compat.replacePlaceholder
import kotlin.math.roundToInt

/**
 * 字符串工具类
 * 
 * 提供字符串处理相关的工具函数，包括占位符替换、变量计算等。
 * 支持玩家占位符 API 和自定义变量替换。
 * 包含数学表达式计算功能。
 *
 * @author HamsterYDS
 * @since 2024/2/17 22:53
 */

/**
 * 替换字符串中的占位符
 * 
 * 使用玩家的占位符 API 替换字符串中的占位符。
 * 支持 PlaceholderAPI 等占位符插件的变量。
 *
 * @param player 玩家对象，如果为 null 则返回原字符串
 * @return 替换后的字符串
 * 
 * @example
 * ```kotlin
 * // 基本用法
 * "Hello {player}!".replacePlaceholder(player) // 返回 "Hello PlayerName!"
 * 
 * // 使用占位符 API
 * "Your balance: {vault_eco_balance}".replacePlaceholder(player)
 * 
 * // 玩家为 null 的情况
 * "Welcome {player}!".replacePlaceholder(null) // 返回原字符串
 * ```
 */
fun String.replacePlaceholder(player: Player?): String {
    return if (player != null) replacePlaceholder(player) else this
}

/**
 * 替换字符串中的变量占位符
 * 
 * 使用键值对列表替换字符串中的变量占位符。
 * 支持标签格式（{variable}）和普通格式。
 *
 * @param holders 变量名和值的键值对列表
 * @param tagged 是否使用标签格式（{variable}），默认为 true
 * @return 替换后的字符串
 * 
 * @example
 * ```kotlin
 * // 使用标签格式
 * "Hello {name}!".replace(listOf("name" to "World")) // 返回 "Hello World!"
 * 
 * // 不使用标签格式
 * "Hello {name}!".replace(listOf("name" to "World"), false) // 返回原字符串
 * 
 * // 多个变量
 * "Hello {name}, you are {age} years old!".replace(
 *     listOf("name" to "Alice", "age" to "25")
 * ) // 返回 "Hello Alice, you are 25 years old!"
 * ```
 */
fun String.replace(holders: List<Pair<String, Any>>, tagged: Boolean = true): String {
    var tmp = this
    holders.forEach { (holder, value) -> tmp = tmp.replace(if (tagged) "{$holder}" else holder, "$value") }
    return tmp
}

/**
 * 替换字符串中的变量占位符（Map 版本）
 * 
 * 使用映射表替换字符串中的变量占位符。
 * 提供更便捷的 Map 接口。
 *
 * @param holders 变量名和值的映射表
 * @param tagged 是否使用标签格式（{variable}），默认为 true
 * @return 替换后的字符串
 * 
 * @example
 * ```kotlin
 * // 使用 Map
 * val variables = mapOf("name" to "World", "age" to "25")
 * "Hello {name}, you are {age} years old!".replace(variables)
 * // 返回 "Hello World, you are 25 years old!"
 * ```
 */
fun String.replace(holders: Map<String, Any>, tagged: Boolean = true): String = replace(holders.toList(), tagged)

/**
 * 替换字符串中的变量占位符（可变参数版本）
 * 
 * 使用可变参数替换字符串中的变量占位符。
 * 提供最便捷的语法。
 *
 * @param holders 变量名和值的键值对
 * @param tagged 是否使用标签格式（{variable}），默认为 true
 * @return 替换后的字符串
 * 
 * @example
 * ```kotlin
 * // 使用可变参数
 * "Hello {name}!".replace("name" to "World") // 返回 "Hello World!"
 * 
 * // 多个变量
 * "Hello {name}, you are {age} years old!".replace(
 *     "name" to "Alice",
 *     "age" to "25"
 * ) // 返回 "Hello Alice, you are 25 years old!"
 * ```
 */
fun String.replace(vararg holders: Pair<String, Any>, tagged: Boolean = true): String = replace(holders.toList(), tagged)

/**
 * 计算字符串中的数学表达式
 * 
 * 将字符串中的变量替换为数值后计算数学表达式。
 * 支持基本的数学运算：加、减、乘、除、幂等。
 *
 * @param holders 变量名和值的键值对
 * @return 计算结果字符串
 * 
 * @example
 * ```kotlin
 * // 基本运算
 * "{x} + {y}".calculate("x" to 5, "y" to 3) // 返回 "8"
 * 
 * // 复杂运算
 * "{x} * {y} + {z}".calculate("x" to 2, "y" to 3, "z" to 1) // 返回 "7"
 * 
 * // 除法运算
 * "{x} / {y}".calculate("x" to 10, "y" to 3) // 返回 "3.33"
 * 
 * // 幂运算
 * "{x} ^ {y}".calculate("x" to 2, "y" to 3) // 返回 "8"
 * ```
 */
fun String.calculate(vararg holders: Pair<String, Any>): String {
    val result = calcToDouble(*holders)
    return if (result.isInteger()) result.toInt().toString() else result.toString()
}

/**
 * 计算字符串中的数学表达式（返回双精度浮点数）
 * 
 * 将字符串中的变量替换为数值后计算数学表达式。
 * 返回精确的双精度浮点数结果。
 *
 * @param holders 变量名和值的键值对
 * @return 计算结果
 * 
 * @example
 * ```kotlin
 * // 基本运算
 * "{x} + {y}".calcToDouble("x" to 5, "y" to 3) // 返回 8.0
 * 
 * // 除法运算
 * "{x} / {y}".calcToDouble("x" to 10, "y" to 3) // 返回 3.333...
 * 
 * // 用于条件判断
 * val damage = "{base} * {multiplier}".calcToDouble("base" to 10, "multiplier" to 1.5)
 * if (damage > 15.0) {
 *     // 伤害足够高
 * }
 * ```
 */
fun String.calcToDouble(vararg holders: Pair<String, Any>): Double = calculate(holders.toList())

/**
 * 计算字符串中的数学表达式（返回整数）
 * 
 * 将字符串中的变量替换为数值后计算数学表达式，结果四舍五入为整数。
 * 适用于需要整数结果的场景。
 *
 * @param holders 变量名和值的键值对
 * @return 计算结果（整数）
 * 
 * @example
 * ```kotlin
 * // 基本运算
 * "{x} + {y}".calcToInt("x" to 5, "y" to 3) // 返回 8
 * 
 * // 除法运算（四舍五入）
 * "{x} / {y}".calcToInt("x" to 10, "y" to 3) // 返回 3
 * 
 * // 用于物品数量计算
 * val itemCount = "{base} * {bonus}".calcToInt("base" to 5, "bonus" to 1.5) // 返回 8
 * ```
 */
fun String.calcToInt(vararg holders: Pair<String, Any>): Int = calcToDouble(*holders).roundToInt()