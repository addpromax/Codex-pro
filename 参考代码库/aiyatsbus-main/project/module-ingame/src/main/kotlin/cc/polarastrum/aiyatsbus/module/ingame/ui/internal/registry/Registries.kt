@file:Suppress("MemberVisibilityCanBePrivate", "unused", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")

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
package cc.polarastrum.aiyatsbus.module.ingame.ui.internal.registry

/**
 * 注册表抽象基类
 * 
 * 提供通用的注册表功能，支持键值对的注册、注销和查询。
 * 实现了 Map 接口，可以直接作为 Map 使用。
 *
 * @param K 键的类型
 * @param V 值的类型
 * @param registered 内部存储的映射表
 *
 * @author mical
 * @since 2024/7/18 16:55
 */
abstract class Registry<K, V>(val registered: MutableMap<K, V>) : Map<K, V> by registered {
    
    /**
     * 转换键值
     * 
     * 在注册前对键进行转换，例如大小写转换等。
     * 子类可以重写此方法来自定义键的转换逻辑。
     *
     * @param key 原始键值
     * @return 转换后的键值
     */
    open fun transformKey(key: K): K = key

    /**
     * 注册键值对
     * 
     * 将键值对注册到注册表中。
     *
     * @param key 键
     * @param value 值
     * @param force 是否强制覆盖已存在的键值对
     * @throws IllegalArgumentException 当键已存在且 force 为 false 时
     */
    open fun register(key: K, value: V, force: Boolean = false) {
        // requireNotNull(value) { "尝试向 ${this::class.java.canonicalName} 注册空值" }
        val transformed = transformKey(key)
        require(force || transformed !in registered) { "尝试向 ${this::class.java.canonicalName} 重复注册 $key" }
        registered[transformed] = value
    }

    /**
     * 延迟注册键值对
     * 
     * 通过函数延迟创建值并注册。
     *
     * @param key 键
     * @param force 是否强制覆盖已存在的键值对
     * @param value 创建值的函数
     * @return 注册结果
     */
    open fun register(key: K, force: Boolean = false, value: () -> V): Result<V> {
        return runCatching {
            value()
        }.onSuccess {
            register(key, it, force)
        }
    }

    /**
     * 注销键值对
     * 
     * 从注册表中移除指定的键值对。
     *
     * @param key 要注销的键
     * @return 被移除的值，如果键不存在则返回 null
     */
    open fun unregister(key: K): V? = registered.remove(transformKey(key))

    /**
     * 条件注销
     * 
     * 根据条件批量注销键值对。
     *
     * @param predicate 判断是否注销的条件函数
     * @return 是否有键值对被注销
     */
    open fun unregisterIf(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
        val before = size
        registered.entries.filter(predicate).forEach { (key, _) ->
            registered.remove(key)
        }
        return size < before
    }

    /**
     * 获取值
     * 
     * 重写 Map 的 get 方法，使用转换后的键进行查找。
     *
     * @param key 键
     * @return 对应的值，如果不存在则返回 null
     */
    override fun get(key: K): V? = registered[transformKey(key)]

    /**
     * 获取值的别名方法
     *
     * @param key 键
     * @return 对应的值，如果不存在则返回 null
     */
    fun of(key: K): V? = get(key)

    /**
     * 安全获取值
     * 
     * 处理空键的情况。
     *
     * @param key 可能为 null 的键
     * @return 对应的值，如果键为 null 或不存在则返回 null
     */
    fun ofNullable(key: K?): V? = if (key != null) of(key) else null

    /**
     * 检查键是否存在
     * 
     * 重写 Map 的 containsKey 方法，使用转换后的键进行检查。
     *
     * @param key 键
     * @return 如果键存在则返回 true
     */
    override fun containsKey(key: K): Boolean = registered.containsKey(transformKey(key))

    /**
     * 清空注册表
     * 
     * 移除所有已注册的键值对。
     */
    fun clearRegistry() = registered.clear()
}

/**
 * 简单注册表抽象类
 * 
 * 继承自 Registry，提供基于值的自动键生成功能。
 * 适用于值对象本身包含键信息的场景。
 *
 * @param K 键的类型
 * @param V 值的类型
 * @param source 内部存储的映射表
 *
 * @author mical
 * @since 2024/7/18 16:55
 */
abstract class SimpleRegistry<K, V>(source: MutableMap<K, V>) : Registry<K, V>(source) {
    
    /**
     * 从值中提取键
     * 
     * 子类必须实现此方法来定义如何从值中提取键。
     *
     * @param value 值对象
     * @return 从值中提取的键
     */
    abstract fun getKey(value: V): K

    /**
     * 注册值
     * 
     * 自动从值中提取键并注册。
     *
     * @param value 要注册的值
     * @param force 是否强制覆盖已存在的键值对
     */
    fun register(value: V, force: Boolean = false) = register(getKey(value), value, force)

    /**
     * 延迟注册值
     * 
     * 通过函数延迟创建值并自动注册。
     *
     * @param force 是否强制覆盖已存在的键值对
     * @param value 创建值的函数
     * @return 注册结果
     */
    fun register(force: Boolean = false, value: () -> V): Result<V> {
        return runCatching {
            value()
        }.onSuccess {
            register(it, force)
        }
    }
}