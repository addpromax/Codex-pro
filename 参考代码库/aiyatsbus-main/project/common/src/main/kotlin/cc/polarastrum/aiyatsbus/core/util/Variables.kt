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
@file:Suppress("unused")
package cc.polarastrum.aiyatsbus.core.util

import taboolib.common.util.VariableReader
import java.util.*

/**
 * 变量工具类
 * 
 * 提供变量读取器、变量替换函数等工具，支持多种变量格式。
 * 包含大括号、双大括号、百分号等不同格式的变量读取器。
 *
 * @author mical
 * @since 2024/2/17 17:07
 */

/**
 * 变量读取器对象
 *
 * 提供不同格式的变量读取器，支持大括号、双大括号和百分号格式。
 */
object VariableReaders {
    /** 大括号格式变量读取器 {variable} */
    val BRACES by lazy { VariableReader("{", "}") }
    /** 双大括号格式变量读取器 {{variable}} */
    val DOUBLE_BRACES by lazy { VariableReader("{{", "}}") }
    /** 百分号格式变量读取器 %variable% */
    val PERCENT by lazy { VariableReader("%", "%") }

    /** 区域开始标记正则表达式 */
    internal val AREA_START by lazy { "^#area (?<area>.+)$".toRegex() }
    /** 区域结束标记正则表达式 */
    internal val AREA_END by lazy { "^#end(?: (?<area>.+))?$".toRegex() }
}

/**
 * 变量函数接口
 *
 * 定义变量替换的函数接口，支持返回多个替换值。
 */
fun interface VariableFunction {
    /**
     * 转换变量
     *
     * @param name 变量名
     * @return 替换值集合，如果变量不存在则返回 null
     */
    fun transfer(name: String): Collection<String>?
}

/**
 * 单变量函数接口
 *
 * 定义单变量替换的函数接口，继承自 VariableFunction。
 */
fun interface SingleVariableFunction : VariableFunction {
    /**
     * 替换变量
     *
     * @param name 变量名
     * @return 替换值，如果变量不存在则返回 null
     */
    fun replace(name: String): String?

    override fun transfer(name: String): Collection<String>? = replace(name)?.let(::listOf)
}

/**
 * 批量变量替换
 *
 * 对字符串集合进行批量变量替换。
 *
 * @param reader 变量读取器，默认为大括号格式
 * @param func 变量替换函数
 * @return 替换后的字符串列表
 * 
 * @example
 * ```kotlin
 * listOf("Hello {name}!").variables { if (it == "name") listOf("World") else null }
 * // 返回 ["Hello World!"]
 * ```
 */
fun Collection<String>.variables(reader: VariableReader = VariableReaders.BRACES, func: VariableFunction): List<String> {
    return flatMap { context ->
        val result = ArrayList<String>()
        val queued = HashMap<String, Queue<String>>()
        reader.replaceNested(context) scan@{
            queued[this] = LinkedList(func.transfer(this) ?: return@scan this)
            this
        }
        if (queued.isEmpty()) {
            return@flatMap listOf(context)
        }

        while (queued.any { (_, queue) -> queue.isNotEmpty() }) {
            result += reader.replaceNested(context) {
                if (this in queued) {
                    queued[this]!!.poll() ?: ""
                } else {
                    this
                }
            }
        }
        result
    }
}

/**
 * 单键变量替换
 *
 * 使用指定的键值对进行变量替换。
 *
 * @param key 变量键名
 * @param value 替换值集合
 * @param reader 变量读取器，默认为大括号格式
 * @return 替换后的字符串列表
 * 
 * @example
 * ```kotlin
 * listOf("Hello {name}!").variable("name", listOf("World"))
 * // 返回 ["Hello World!"]
 * ```
 */
fun Collection<String>.variable(key: String, value: Collection<String>, reader: VariableReader = VariableReaders.BRACES): List<String> {
    return variables(reader) { if (it == key) value else null }
}

/**
 * 单例变量替换（字符串版本）
 *
 * 对单个字符串进行变量替换。
 *
 * @param reader 变量读取器，默认为大括号格式
 * @param func 变量替换函数
 * @return 替换后的字符串
 * 
 * @example
 * ```kotlin
 * "Hello {name}!".singletons { name, _ -> if (name == "name") "World" else null }
 * // 返回 "Hello World!"
 * ```
 */
fun String.singletons(reader: VariableReader = VariableReaders.BRACES, func: Function2<String, String>): String {
    return reader.replaceNested(this) { func.apply(this) }
}

/**
 * 单例变量替换（集合版本）
 *
 * 对字符串集合进行单例变量替换。
 *
 * @param reader 变量读取器，默认为大括号格式
 * @param func 单变量替换函数
 * @return 替换后的字符串列表
 * 
 * @example
 * ```kotlin
 * listOf("Hello {name}!").singletons { if (it == "name") "World" else null }
 * // 返回 ["Hello World!"]
 * ```
 */
fun Collection<String>.singletons(reader: VariableReader = VariableReaders.BRACES, func: SingleVariableFunction): List<String> {
    return variables(reader, func)
}

/**
 * 单键单例变量替换
 *
 * 使用指定的键值对进行单例变量替换。
 *
 * @param key 变量键名
 * @param value 替换值
 * @param reader 变量读取器，默认为大括号格式
 * @return 替换后的字符串列表
 * 
 * @example
 * ```kotlin
 * listOf("Hello {name}!").singleton("name", "World")
 * // 返回 ["Hello World!"]
 * ```
 */
fun Collection<String>.singleton(key: String, value: String, reader: VariableReader = VariableReaders.BRACES): List<String> {
    return singletons(reader) { if (it == key) value else null }
}