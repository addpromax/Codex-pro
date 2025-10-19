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

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.function.warning
import taboolib.library.reflex.ClassField
import taboolib.library.reflex.ClassMethod
import taboolib.library.reflex.ReflexClass
import taboolib.module.configuration.Configuration
import java.util.function.Consumer

/**
 * 可重载注解
 *
 * 标记方法或字段为可重载的，支持热重载功能。
 * 被标记的方法或字段会在重载时自动执行。
 * 适用于需要动态更新的配置和方法。
 * 
 * @example
 * ```kotlin
 * @Reloadable
 * fun updateConfig() {
 *     // 重载时自动执行
 * }
 * 
 * @Reloadable
 * lateinit var config: Configuration
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reloadable

/**
 * 重载管理器
 *
 * 管理所有标记为可重载的方法和字段，提供统一的重载执行功能。
 * 使用类访问器自动发现和注册可重载的组件。
 * 支持配置文件的自动重载和方法的动态调用。
 *
 * @author mical
 * @since 2024/2/17 17:07
 */
@Awake
@Suppress("unused")
object Reloadables : ClassVisitor() {

    /** 已注册的可重载组件映射表 */
    val registered: Multimap<Any, Consumer<Any>> = HashMultimap.create()

    override fun getLifeCycle(): LifeCycle = LifeCycle.LOAD

    /**
     * 访问方法
     *
     * 检查方法是否标记为可重载，如果是则注册到映射表中。
     * 支持无参数方法的自动注册和调用。
     *
     * @param method 类方法
     * @param owner 所属类
     */
    override fun visit(method: ClassMethod, owner: ReflexClass) {
        if (method.isAnnotationPresent(Reloadable::class.java)) {
            registered.put(findInstance(owner) ?: return) { method.invoke(it) }
        }
    }

    /**
     * 访问字段
     *
     * 检查字段是否标记为可重载，如果是则注册到映射表中。
     * 目前支持 Configuration 类型的字段重载。
     * 其他类型的字段会输出警告信息。
     *
     * @param field 类字段
     * @param owner 所属类
     */
    override fun visit(field: ClassField, owner: ReflexClass) {
        if (field.isAnnotationPresent(Reloadable::class.java)) {
            val ins = findInstance(owner) ?: return
            val type = field.fieldType
            when {
                Configuration::class.java.isAssignableFrom(type) ->
                    registered.put(ins) { (field.get(it) as? Configuration)?.reload() }
                else -> warning("Unknown reloadable field type: ${type.canonicalName}")
            }
        }
    }

    /**
     * 执行重载
     *
     * 遍历所有已注册的可重载组件并执行重载操作。
     * 会依次执行所有注册的方法和字段重载。
     * 
     * @example
     * ```kotlin
     * // 手动触发重载
     * Reloadables.execute()
     * 
     * // 在插件重载时自动调用
     * override fun onReload() {
     *     Reloadables.execute()
     * }
     * ```
     */
    fun execute() {
        registered.asMap().forEach { (instance, methods) ->
            methods.forEach {
                it.accept(instance)
            }
        }
    }
}

/**
 * 注册可重载函数
 *
 * 将函数注册为可重载的，支持热重载功能。
 * 注册的函数会在重载时自动执行。
 *
 * @param func 要注册的函数
 * 
 * @example
 * ```kotlin
 * // 注册一个简单的重载函数
 * reloadable { 
 *     println("配置已重载") 
 * }
 * 
 * // 注册一个复杂的重载函数
 * reloadable { 
 *     config.reload()
 *     updateCache()
 *     notifyPlayers("配置已更新")
 * }
 * ```
 */
fun reloadable(func: Consumer<Any>) {
    func.accept(0)
    // toString 随便 put 进一个
    Reloadables.registered.put(System.currentTimeMillis(), func)
}