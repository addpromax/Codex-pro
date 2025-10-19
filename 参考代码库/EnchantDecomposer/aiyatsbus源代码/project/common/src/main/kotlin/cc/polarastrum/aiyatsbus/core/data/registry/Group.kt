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
package cc.polarastrum.aiyatsbus.core.data.registry

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.aiyatsbusEt
import cc.polarastrum.aiyatsbus.core.aiyatsbusEts
import cc.polarastrum.aiyatsbus.core.aiyatsbusRarity
import cc.polarastrum.aiyatsbus.core.data.Dependencies
import cc.polarastrum.aiyatsbus.core.data.Dependency
import cc.polarastrum.aiyatsbus.core.data.Registry
import cc.polarastrum.aiyatsbus.core.data.RegistryItem
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * 附魔组类
 *
 * 用于将多个附魔组织在一起，管理附魔之间的互斥关系和共存限制。
 * 支持排除特定附魔、设置最大共存数量等功能。
 * 可以通过品质批量添加附魔，也可以通过名称单独指定。
 *
 * @param root 配置根节点，包含所有附魔组的配置信息
 * @param dependencies 依赖项配置，定义该附魔组的前置条件
 * @param name 附魔组名称，默认为配置节点名称
 * @param exclude 排除的附魔列表，这些附魔不会出现在该组中
 * @param enchantments 包含的附魔列表，该组管理的所有附魔
 * @param skull 头颅材质值，用于自定义头颅显示
 * @param maxCoexist 最大共存数量，限制该组中附魔可以同时存在的最大数量，默认为 1
 * @param inaccessible 是否不可访问，为 true 时玩家无法获得该附魔组的附魔
 *
 * @author mical
 * @since 2025/6/20 20:16
 */
data class Group @JvmOverloads constructor(
    /** 配置根节点 */
    private val root: ConfigurationSection,
    /** 依赖项配置，定义该附魔组的前置条件 */
    override val dependencies: Dependencies = Dependencies(root.getConfigurationSection("dependencies")),
    /** 附魔组名称，默认为配置节点名称 */
    val name: String = root.name,
    /** 排除的附魔列表，这些附魔不会出现在该组中 */
    val exclude: List<AiyatsbusEnchantment> = root.getStringList("exclude").mapNotNull(::aiyatsbusEt),
    /** 包含的附魔列表，该组管理的所有附魔 */
    val enchantments: List<AiyatsbusEnchantment> = root.getStringList("enchants").mapNotNull(::aiyatsbusEt)
        .toMutableList().also {
            it += root.getStringList("rarities")
                .map { aiyatsbusRarity(it)?.let { r -> aiyatsbusEts(r) } ?: listOf() }.flatten()
        }.filter { it !in exclude },
    /** 头颅材质值，用于自定义头颅显示 */
    val skull: String = root.getString(
        "skull",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRiODlhZDA2ZDMxOGYwYWUxZWVhZjY2MGZlYTc4YzM0ZWI1NWQwNWYwMWUxY2Y5OTlmMzMxZmIzMmQzODk0MiJ9fX0="
    )!!,
    /** 最大共存数量，限制该组中附魔可以同时存在的最大数量，默认为 1 */
    val maxCoexist: Int = root.getInt("max_coexist", 1),
    /** 是否不可访问，为 true 时玩家无法获得该附魔组的附魔 */
    val inaccessible: Boolean = root.getBoolean("inaccessible", false),
) : RegistryItem(root), Dependency {

    /**
     * 附魔组注册器伴生对象
     *
     * 负责从配置文件中加载和管理附魔组数据。
     * 提供全局访问点来获取和管理所有附魔组。
     */
    companion object : Registry<Group>("group", { section -> Group(section) }) {

        /** 附魔组配置文件，自动重载配置变更 */
        @Config("enchants/group.yml", autoReload = true)
        override lateinit var config: Configuration
            private set
    }
}