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
package cc.polarastrum.aiyatsbus.core.data

import org.bukkit.Bukkit
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.nms.MinecraftVersion

/**
 * 依赖接口
 *
 * 标记需要依赖检查的组件。
 * 实现此接口的类必须提供依赖项配置。
 *
 * @author mical
 * @since 2024/5/1 18:30
 */
interface Dependency {

    /** 依赖项配置 */
    val dependencies: Dependencies
}

/**
 * 依赖项管理类
 *
 * 管理组件的依赖条件，包括 Minecraft 版本支持、数据包依赖和插件依赖。
 * 提供依赖检查功能，确保组件在满足所有条件时才能正常使用。
 *
 * @param root 配置根节点，可为空
 * @param supportsRangeStr 版本支持范围字符串，格式为 "最低版本" 或 "最低版本-最高版本"
 * @param supportsLowest 最低支持的 Minecraft 版本 ID
 * @param supportsHighest 最高支持的 Minecraft 版本 ID
 * @param datapacks 必需的数据包列表
 * @param plugins 必需的插件列表
 *
 * @author mical
 * @since 2024/5/1 18:30
 */
class Dependencies(
    /** 配置根节点，可为空 */
    private val root: ConfigurationSection?,
    /** 版本支持范围字符串，默认为 "11600" */
    supportsRangeStr: String = root?.getString("supports", "11600") ?: "11600",
    /** 最低支持的 Minecraft 版本 ID，从支持范围字符串解析 */
    supportsLowest: Int = if ('-' in supportsRangeStr) supportsRangeStr.split('-')[0].toInt() else supportsRangeStr.toInt(),
    /** 最高支持的 Minecraft 版本 ID，从支持范围字符串解析，默认为 Int.MAX_VALUE */
    supportsHighest: Int = if ('-' in supportsRangeStr) supportsRangeStr.split('-')[1].toInt() else Int.MAX_VALUE,
    /** 必需的数据包列表，默认为空列表 */
    private val datapacks: List<String> = root?.getStringList("datapacks") ?: emptyList(),
    /** 必需的插件列表，默认为空列表 */
    private val plugins: List<String> = root?.getStringList("plugins") ?: emptyList()
) {

    /**
     * 版本支持范围
     *
     * 使用 Kotlin 范围语法表示支持的版本区间。
     */
    private val supportsRange = supportsLowest..supportsHighest

    /**
     * 检查依赖是否可用
     *
     * 检查当前 Minecraft 版本是否在支持范围内，
     * 检查所有必需的数据包是否已启用，
     * 检查所有必需的插件是否已加载。
     *
     * @return true 表示所有依赖条件都满足，false 表示存在不满足的依赖
     *
     * @example
     * ```kotlin
     * if (dependencies.checkAvailable()) {
     *     // 依赖满足
     * }
     * ```
     */
    fun checkAvailable(): Boolean {
        return MinecraftVersion.versionId in supportsRange &&
                datapacks.all { pack -> Bukkit.getDatapackManager().enabledPacks.any { it.name == pack } } &&
                plugins.all { Bukkit.getPluginManager().getPlugin(it) != null }
    }
}