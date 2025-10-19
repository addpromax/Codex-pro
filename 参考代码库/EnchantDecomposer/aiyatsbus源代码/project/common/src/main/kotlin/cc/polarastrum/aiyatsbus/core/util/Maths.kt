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

import redempt.crunch.CompiledExpression
import redempt.crunch.Crunch
import redempt.crunch.functional.EvaluationEnvironment
import redempt.crunch.functional.Function
import taboolib.common.platform.function.warning
import taboolib.common.util.random
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 数学工具类
 * 
 * 提供数学计算相关的工具函数，包括罗马数字转换、表达式计算、权重选择等。
 * 使用 Crunch 库进行表达式解析和计算，支持变量和自定义函数。
 *
 * @author HamsterYDS
 * @since 2024/2/17 23:15
 */

// 罗马数字转换相关常量
private val romanUnits = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
private val romanSymbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")

/**
 * 将数字转换为罗马数字
 * 
 * 支持 1-3999 范围内的数字转换。
 * 使用标准的罗马数字表示法，支持简化模式。
 *
 * @param simplified 是否简化模式（1 不显示），默认为 false
 * @param blank 是否在结果前添加空格，默认为 false
 * @return 罗马数字字符串
 * 
 * @example
 * ```kotlin
 * // 基本转换
 * 1234.roman() // 返回 "MCCXXXIV"
 * 5.roman() // 返回 "V"
 * 
 * // 简化模式（1 不显示）
 * 1.roman(simplified = true) // 返回 ""
 * 5.roman(simplified = true) // 返回 "V"
 * 
 * // 添加空格前缀
 * 5.roman(blank = true) // 返回 " V"
 * 
 * // 超出范围
 * 4000.roman() // 返回 ""
 * ```
 */
fun Int.roman(simplified: Boolean = false, blank: Boolean = false): String {
    if ((this == 1 && simplified) || this !in 1..3999) return ""
    var number = this
    val roman = StringBuilder()
    for (i in romanUnits.indices)
        while (number >= romanUnits[i]) {
            roman.append(romanSymbols[i])
            number -= romanUnits[i]
        }
    return if (blank) " $roman" else "$roman"
}

/**
 * 判断浮点数是否为整数
 * 
 * 检查浮点数是否等于其整数部分，例如 6.0 被认为是整数。
 * 用于判断数值是否为整数值。
 *
 * @return 如果是整数则返回 true
 * 
 * @example
 * ```kotlin
 * // 整数值
 * 6.0.isInteger() // 返回 true
 * 0.0.isInteger() // 返回 true
 * -5.0.isInteger() // 返回 true
 * 
 * // 非整数值
 * 6.5.isInteger() // 返回 false
 * 3.14.isInteger() // 返回 false
 * ```
 */
fun Double.isInteger(): Boolean {
    return this == toInt().toDouble()
}

/**
 * 数学工具对象
 * 
 * 提供表达式计算和权重选择功能。
 * 使用 Crunch 库进行高性能的数学表达式计算。
 */
object MathUtils {

    // 内置数学函数
    private val min = Function("min", 2) {
        min(it[0], it[1])
    }

    private val max = Function("max", 2) {
        max(it[0], it[1])
    }

    private val rand = Function("random", 2) {
        random(it[0], it[1])
    }

    // 表达式缓存
    private val cache = mutableMapOf<String, CompiledExpression?>()

    /**
     * 预热表达式
     * 
     * 预编译表达式以提高后续计算性能。
     * 将表达式编译为字节码，减少运行时解析开销。
     *
     * @return 原始表达式字符串
     * 
     * @example
     * ```kotlin
     * // 预热常用表达式
     * "{x} + {y} * 2".preheatExpression()
     * "{damage} * {multiplier}".preheatExpression()
     * ```
     */
    fun String.preheatExpression(): String {
        val expression = replaceVariable()
        val variables = extractVariableNames()

        val env = EvaluationEnvironment()
        env.setVariableNames(*variables.toTypedArray())
        env.addFunctions(rand, min, max)
        val compiled = runCatching { Crunch.compileExpression(expression, env) }.getOrElse {
            error("compiling", this, variables, doubleArrayOf(), it)
            null
        }

        if (compiled != null) {
            cache[expression] = compiled
        }

        return this
    }

    /**
     * 计算表达式
     * 
     * 使用 Crunch 库计算包含变量的数学表达式。
     * 支持基本运算、函数调用和变量替换。
     * 使用缓存机制提高重复计算性能。
     *
     * @param holders 变量名和值的键值对列表
     * @return 计算结果
     * 
     * @example
     * ```kotlin
     * // 基本运算
     * "{x} + {y} * 2".calculate(listOf("x" to 5, "y" to 3)) // 返回 11.0
     * 
     * // 使用内置函数
     * "min({x}, {y}) + max({a}, {b})".calculate(
     *     listOf("x" to 10, "y" to 5, "a" to 3, "b" to 8)
     * ) // 返回 13.0
     * 
     * // 随机数
     * "random({min}, {max})".calculate(listOf("min" to 1, "max" to 10)) // 返回 1-10 之间的随机数
     * ```
     */
    fun String.calculate(holders: List<Pair<String, Any>>): Double {
        val expression = replaceVariable()
        val variables = extractVariableNames()

        val pairMap = holders.toMap()
        val values = variables.map { pairMap[it] }
            .map { it.toString().toDoubleOrNull() ?: 0.0 }
            .toDoubleArray()

        val compiled = cache.getOrPut(expression) {
            val env = EvaluationEnvironment()
            env.setVariableNames(*variables.toTypedArray())
            env.addFunctions(rand, min, max)
            runCatching { Crunch.compileExpression(expression, env) }.getOrElse {
                error("compiling", this, variables, values, it)
                null
            }
        }

        return runCatching { compiled?.evaluate(*values) }.getOrElse {
            error("evaluating", this, variables, values, it)
            0.0
        } ?: 0.0
    }

    /**
     * 根据权重选择元素
     * 
     * 使用权重随机选择映射表中的元素。
     * 权重越高的元素被选中的概率越大。
     *
     * @param T 元素类型
     * @param random 随机数生成器
     * @return 选中的元素，如果没有元素则返回 null
     * 
     * @example
     * ```kotlin
     * // 物品掉落概率
     * val items = mapOf(
     *     "diamond" to 1,    // 1% 概率
     *     "iron" to 10,      // 10% 概率
     *     "coal" to 89       // 89% 概率
     * )
     * val dropped = items.selectByWeight(Random.Default)
     * 
     * // 技能触发概率
     * val skills = mapOf(
     *     "critical" to 20,
     *     "normal" to 80
     * )
     * val triggered = skills.selectByWeight(Random.Default)
     * ```
     */
    fun <T> Map<T, Int>.selectByWeight(random: Random): T? {
        val total = values.sum()
        if (total <= 0) return null

        var accumulator = 0
        val target = random.nextInt(total)
        return entries.firstOrNull { entry ->
            accumulator += entry.value
            accumulator > target
        }?.key
    }

    /**
     * 输出错误信息
     * 
     * 格式化输出表达式计算过程中的错误信息。
     * 包含表达式、变量、值和具体错误信息。
     */
    private fun error(action: String, expression: String, variables: List<String>, values: DoubleArray, error: Throwable) {
        warning("Error occurred while $action expression!")
        warning("|- Expression: $expression")
        warning("|- Variables: $variables")
        warning("|- Values: ${values.toList()}")
        warning("|- Error: $error")
    }

    // 变量提取正则表达式
    private val variableRegex = "\\{([^}]+)}".toRegex()

    /**
     * 提取表达式中的变量名
     * 
     * 从形如 {variable} 的字符串中提取变量名。
     * 使用正则表达式匹配所有变量占位符。
     *
     * @return 变量名列表
     */
    private fun String.extractVariableNames(): List<String> {
        return (variableRegex.findAll(this).map { it.groupValues[1] }.toList())
    }

    /**
     * 替换变量占位符
     * 
     * 将 {variable} 格式的占位符替换为 $variable 格式。
     * 用于适配 Crunch 库的变量格式。
     *
     * @return 替换后的表达式
     */
    private fun String.replaceVariable(): String = replace(variableRegex, "\$1")
}