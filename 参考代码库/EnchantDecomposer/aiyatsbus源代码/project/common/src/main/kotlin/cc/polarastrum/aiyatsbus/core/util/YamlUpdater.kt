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

import org.bukkit.configuration.file.YamlConfiguration
import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration

/**
 * YAML 配置文件更新器
 *
 * 提供配置文件自动更新功能，支持增量更新和节点更新。
 * 可以自动检测配置文件版本差异并更新到最新版本。
 * 使用 Bukkit Configuration 加载配置，防止注释丢失。
 *
 * @author 米擦亮
 * @since 2023/9/6 20:50
 */
object YamlUpdater {

    /**
     * 从文件加载配置
     *
     * 加载配置文件，支持自动更新功能。
     * 如果配置文件不存在，会自动从资源文件中释放。
     * 支持指定需要更新的节点，只更新指定的配置项。
     *
     * @param path 配置文件路径
     * @param autoUpdate 是否启用自动更新，默认为 true
     * @param updateNodes 需要更新的节点列表，默认为空列表
     * @param cache 缓存配置，默认为 null
     * @return 加载的配置对象
     * 
     * @example
     * ```kotlin
     * // 基本用法
     * val config = YamlUpdater.loadFromFile("config.yml")
     * 
     * // 指定更新节点
     * val config = YamlUpdater.loadFromFile(
     *     "config.yml", 
     *     true, 
     *     listOf("new-feature", "updated-setting")
     * )
     * 
     * // 禁用自动更新
     * val config = YamlUpdater.loadFromFile("config.yml", false)
     * ```
     */
    fun loadFromFile(path: String, autoUpdate: Boolean = true, updateNodes: List<String> = emptyList(), cache: Configuration? = null): Configuration {
        // 如果配置不存在，直接释放即可，并不需要任何检查操作
        if (!newFile(getDataFolder(), path, create = false).exists()) {
            return Configuration.loadFromFile(releaseResourceFile(path))
        }
        val configFile = newFile(getDataFolder(), path)

        // 如果没开启自动更新则直接加载
        if (!autoUpdate) {
            return Configuration.loadFromFile(configFile)
        }

        // 由 Bukkit Configuration 加载配置，防止注释丢失
        val config = YamlConfiguration.loadConfiguration(configFile)

        // 读取 Jar 包内的对应配置文件
        val cachedFile = cache ?: Configuration.loadFromInputStream(javaClass.classLoader.getResourceAsStream(path.replace('\\', '/')) ?: return Configuration.loadFromOther(config))

        val updated = mutableListOf<String>()
        read(cachedFile, config, updateNodes, updated)
        if (updated.isNotEmpty()) {
            config.save(configFile)
        }

        return Configuration.loadFromOther(config)
    }

    /**
     * 读取并更新配置节点
     *
     * 递归读取缓存配置并更新目标配置。
     * 支持根节点和具体节点的更新。
     * 会记录所有更新的节点信息。
     *
     * @param cache 缓存配置（Jar 包内的原始配置）
     * @param to 目标配置（用户当前的配置文件）
     * @param updateNodes 需要更新的节点列表
     * @param updated 已更新的节点列表，用于记录更新日志
     */
    private fun read(cache: ConfigurationSection, to: org.bukkit.configuration.ConfigurationSection, updateNodes: List<String>, updated: MutableList<String>) {
        for (key in cache.getKeys(true)) {
            val root = key.split(".").first()
            if (root !in updateNodes && key !in updateNodes) {
                continue
            }
            // 旧版没有，添加
            if (!to.contains(key)) {
                updated += "$key (+)"
                to[key] = cache[key]
                continue
            }

            if (cache[key] == null) {
                updated += "$key (${to[key]} -> null)"
                to[key] = null
                continue
            }

            val read = cache[key]
            if (to[key] == read) continue
            to[key] = read
            updated += "$key (${to[key]} -> $read)"
        }
    }
}