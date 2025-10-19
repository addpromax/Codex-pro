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

import java.math.RoundingMode

/**
 * 数学工具函数
 * 
 * 提供整数范围操作和浮点数舍入的工具函数。
 *
 * @author mical
 * @since 2024/7/18 16:55
 */

/**
 * 获取整数在指定范围内的下一个值
 * 
 * 循环获取下一个值，如果当前值超出范围上限则返回范围下限。
 *
 * @param range 整数范围
 * @return 下一个值
 * 
 * @example
 * ```kotlin
 * 5.next(1..10) // 返回 6
 * 10.next(1..10) // 返回 1
 * ```
 */
fun Int.next(range: IntRange): Int {
    var next = this + 1
    if (next > range.last) {
        next = range.first
    }
    return next
}

/**
 * 获取整数在指定范围内的上一个值
 * 
 * 循环获取上一个值，如果当前值超出范围下限则返回范围上限。
 *
 * @param range 整数范围
 * @return 上一个值
 * 
 * @example
 * ```kotlin
 * 5.last(1..10) // 返回 4
 * 1.last(1..10) // 返回 10
 * ```
 */
fun Int.last(range: IntRange): Int {
    var last = this - 1
    if (last < range.first) {
        last = range.last
    }
    return last
}

/**
 * 对浮点数进行舍入
 * 
 * 使用四舍五入的方式对浮点数进行指定小数位数的舍入。
 *
 * @param scale 保留的小数位数，默认为 2
 * @return 舍入后的浮点数
 * 
 * @example
 * ```kotlin
 * 3.14159.round(2) // 返回 3.14
 * 3.14159.round(4) // 返回 3.1416
 * ```
 */
fun Double.round(scale: Int = 2): Double = toBigDecimal().setScale(scale, RoundingMode.HALF_DOWN).toDouble()