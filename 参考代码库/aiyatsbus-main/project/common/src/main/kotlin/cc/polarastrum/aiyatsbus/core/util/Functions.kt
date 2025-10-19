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

/**
 * 函数式接口工具类
 * 
 * 提供各种函数式接口定义，用于支持函数式编程。
 * 包含单参数、双参数、三参数等不同数量的函数接口。
 * 这些接口与 Java 兼容，可以在 Java 代码中使用。
 *
 * @author mical
 * @since 2024/7/18 16:55
 */

/**
 * 单参数函数接口
 * 
 * 接受一个参数但不返回值的函数接口。
 * 适用于需要执行副作用操作的场景。
 * 
 * @param T 输入参数类型
 * 
 * @example
 * ```kotlin
 * val printer: Function1<String> = Function1 { println("Hello $it") }
 * printer.apply("World") // 输出: Hello World
 * ```
 */
fun interface Function1<in T> {

    /**
     * 执行函数
     * 
     * @param t 输入参数
     */
    fun apply(t: T)
}

/**
 * 双参数函数接口
 * 
 * 接受一个参数并返回一个值的函数接口。
 * 适用于需要转换或计算数据的场景。
 * 
 * @param T 输入参数类型
 * @param R 返回值类型
 * 
 * @example
 * ```kotlin
 * val converter: Function2<String, Int> = Function2 { it.length }
 * val length = converter.apply("Hello") // 返回 5
 * ```
 */
fun interface Function2<in T, R> {

    /**
     * 执行函数
     * 
     * @param t 输入参数
     * @return 函数执行结果
     */
    fun apply(t: T): R
}

/**
 * 三参数转双返回值函数接口
 * 
 * 接受三个参数并返回两个值的函数接口。
 * 适用于需要同时返回多个结果的复杂计算场景。
 * 
 * @param T 第一个输入参数类型
 * @param R 第二个输入参数类型
 * @param C 第三个输入参数类型
 * @param B 第一个返回值类型
 * @param K 第二个返回值类型
 * 
 * @example
 * ```kotlin
 * val calculator: Function3To2<Int, Int, Int, Int, Double> = Function3To2 { a, b, c ->
 *     Pair1(a + b, c.toDouble() / 2)
 * }
 * val result = calculator.apply(1, 2, 10) // 返回 Pair1(3, 5.0)
 * ```
 */
fun interface Function3To2<in T, in R, in C, B, K> {

    /**
     * 执行函数
     * 
     * @param t 第一个输入参数
     * @param r 第二个输入参数
     * @param c 第三个输入参数
     * @return 包含两个值的键值对
     */
    fun apply(t: T, r: R, c: C): Pair1<B, K>
}

/**
 * 双参数转双返回值函数接口
 * 
 * 接受两个参数并返回两个值的函数接口。
 * 适用于需要同时返回多个结果的场景。
 * 
 * @param T 第一个输入参数类型
 * @param R 第二个输入参数类型
 * @param B 第一个返回值类型
 * @param K 第二个返回值类型
 * 
 * @example
 * ```kotlin
 * val splitter: Function2To2<String, String, String, Int> = Function2To2 { text, delimiter ->
 *     val parts = text.split(delimiter)
 *     Pair1(parts.firstOrNull() ?: "", parts.size)
 * }
 * val result = splitter.apply("a,b,c", ",") // 返回 Pair1("a", 3)
 * ```
 */
fun interface Function2To2<in T, in R, B, K> {

    /**
     * 执行函数
     * 
     * @param t 第一个输入参数
     * @param r 第二个输入参数
     * @return 包含两个值的键值对
     */
    fun apply(t: T, r: R): Pair1<B, K>
}