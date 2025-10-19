@file:Suppress("unused")

/*
 * This file is part of ParrotX, licensed under the MIT License.
 *
 *  Copyright (c) 2020 Legoshi
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package cc.polarastrum.aiyatsbus.module.ingame.ui.internal.function

import com.google.common.base.Enums

/**
 * 枚举工具函数
 * 
 * 提供枚举类型的便捷操作函数，包括字符串转枚举、获取下一个/上一个枚举值等。
 *
 * @author mical
 * @since 2024/7/18 16:55
 */

/**
 * 将字符串转换为枚举值
 * 
 * 使用 Guava 的 Enums 工具类进行安全的枚举转换。
 * 支持自定义字符串转换函数，默认转换为大写。
 *
 * @param T 枚举类型
 * @param transfer 字符串转换函数，默认为转换为大写
 * @return 对应的枚举值，如果转换失败则返回 null
 * 
 * @example
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * "red".enumOf<Color>() // 返回 Color.RED
 * "unknown".enumOf<Color>() // 返回 null
 * ```
 */
inline fun <reified T : Enum<T>> String?.enumOf(transfer: (String) -> String = { it.uppercase() }): T? {
    return if (this == null) null else Enums.getIfPresent(T::class.java, transfer(this)).orNull()
}

/**
 * 获取枚举的下一个值
 * 
 * 循环获取枚举的下一个值，如果当前是最后一个则返回第一个。
 *
 * @param T 枚举类型
 * @return 下一个枚举值
 * 
 * @example
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * Color.RED.next() // 返回 Color.GREEN
 * Color.BLUE.next() // 返回 Color.RED
 * ```
 */
inline fun <reified T : Enum<T>> T.next(): T = enumValues<T>().let { it[ordinal.next(it.indices)] }

/**
 * 获取枚举的上一个值
 * 
 * 循环获取枚举的上一个值，如果当前是第一个则返回最后一个。
 *
 * @param T 枚举类型
 * @return 上一个枚举值
 * 
 * @example
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * Color.GREEN.last() // 返回 Color.RED
 * Color.RED.last() // 返回 Color.BLUE
 * ```
 */
inline fun <reified T : Enum<T>> T.last(): T = enumValues<T>().let { it[ordinal.last(it.indices)] }