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

import cc.polarastrum.aiyatsbus.core.util.coerceInt
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.chat.colored
import taboolib.module.chat.uncolored

/**
 * 附魔基本数据类
 *
 * 包含附魔的基础配置信息，这些数据是附魔系统必需的。
 * 定义了附魔的启用状态、禁用世界、标识符、名称和最大等级等基本属性。
 *
 * @author mical
 * @since 2024/2/17 14:37
 */
data class BasicData(
    /** 配置根节点 */
    private val root: ConfigurationSection,
    /** 是否启用该附魔，默认为 true */
    val enable: Boolean = root.getBoolean("enable", true),
    /** 禁用该附魔的世界列表，在这些世界中附魔不会生效 */
    val disableWorlds: List<String> = root.getStringList("disable-worlds").ifEmpty { root.getStringList("disable_worlds") },
    /** 附魔的唯一标识符 */
    val id: String = root.getString("id")!!,
    /** 附魔的显示名称 */
    val name: String = root.getString("name")!!,
    /** 附魔的最大等级，默认为 1 */
    val maxLevel: Int = (root["max-level"]?.coerceInt(1) ?: root.getInt("max_level", 1))
) {

    /**
     * 原始名称（去除颜色代码）
     *
     * uncolored() 是通过 bungee-chat 的 ChatColor 去色的，
     * 所以 TabooLib 提供的颜色格式必须先通过 colored() 转为 bungee-chat 的颜色格式，
     * 再通过 uncolored() 去色。
     */
    val originName: String = name.colored().uncolored()

    /**
     * 名称是否包含颜色代码
     *
     * 通过比较原始名称和去色后的名称来判断是否包含颜色代码。
     */
    val nameHasColor: Boolean = originName != name
}