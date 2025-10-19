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

import taboolib.common5.FileWatcher
import java.io.File

/**
 * 文件工具类
 * 
 * 提供文件操作相关的工具函数，包括递归读取、文件监听等。
 * 支持深度遍历文件夹和实时文件变化监听。
 *
 * @author mical
 * @since 2024/8/27 17:21
 */

/**
 * 递归读取文件夹内的所有指定后缀名文件
 * 
 * 深度遍历文件夹及其子文件夹，收集所有指定扩展名的文件。
 * 使用递归算法确保遍历所有子目录。
 *
 * @param extension 文件扩展名（不包含点号）
 * @return 匹配的文件列表
 * 
 * @example
 * ```kotlin
 * // 获取所有 YAML 配置文件
 * val ymlFiles = File("config").deepRead("yml")
 * 
 * // 获取所有 JSON 文件
 * val jsonFiles = File("data").deepRead("json")
 * 
 * // 获取所有文本文件
 * val txtFiles = File("logs").deepRead("txt")
 * ```
 */
fun File.deepRead(extension: String): List<File> {
    val files = mutableListOf<File>()
    listFiles()?.forEach {
        if (it.isDirectory) {
            files.addAll(it.deepRead(extension))
        } else if (it.extension == extension) {
            files.add(it)
        }
    }
    return files
}

/**
 * 文件监听器对象
 * 
 * 提供文件变化监听功能，支持添加和移除监听器。
 * 使用 TabooLib 的 FileWatcher 实现实时监听。
 * 防止重复添加监听器和处理中的文件冲突。
 */
object FileWatcher {

    private val fileWatcher = FileWatcher.INSTANCE
    private val fileListeners = LinkedHashSet<File>()
    private val watching = LinkedHashSet<File>()

    /**
     * 监听文件改动
     * 
     * 为指定文件添加变化监听器。
     * 当文件发生变化时会自动调用回调函数。
     * 防止重复添加监听器。
     *
     * @param callback 文件变化时的回调函数，参数为发生变化的文件
     * 
     * @example
     * ```kotlin
     * // 监听配置文件变化
     * configFile.watch { changedFile ->
     *     println("配置文件 ${changedFile.name} 发生了变化")
     *     reloadConfig()
     * }
     * 
     * // 监听日志文件变化
     * logFile.watch { changedFile ->
     *     println("日志文件 ${changedFile.name} 已更新")
     *     updateLogDisplay()
     * }
     * ```
     */
    fun File.watch(callback: (File) -> Unit) {
        if (!hasListener) {
            fileWatcher.addSimpleListener(this) {
                callback(this)
            }
            hasListener = true
        }
    }

    /**
     * 取消监听
     * 
     * 移除指定文件的监听器。
     * 释放相关资源，停止监听文件变化。
     * 
     * @example
     * ```kotlin
     * // 停止监听配置文件
     * configFile.unwatch()
     * 
     * // 停止监听日志文件
     * logFile.unwatch()
     * ```
     */
    fun File.unwatch() {
        if (hasListener) {
            fileWatcher.removeListener(this)
            hasListener = false
        }
    }

    /**
     * 检测文件是否正在被监听器处理
     * 
     * 检查文件是否正在被监听器处理中。
     * 用于防止在处理文件变化时重复触发监听器。
     */
    var File.isProcessingByWatcher: Boolean
        get() = this in watching && hasListener
        set(value) {
            if (value) watching += this else watching -= this
        }

    /**
     * 文件是否已添加监听器
     * 
     * 内部属性，用于跟踪文件是否已添加监听器。
     * 防止重复添加监听器导致的内存泄漏。
     */
    private var File.hasListener: Boolean
        get() = this in fileListeners
        set(value) {
            if (value) fileListeners += this else fileListeners -= this
        }
}