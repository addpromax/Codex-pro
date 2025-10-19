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

import com.google.gson.JsonParser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import taboolib.module.chat.ComponentText
import taboolib.module.chat.component

/**
 * 组件工具类
 * 
 * 提供文本组件转换和处理的工具函数。
 * 支持旧版颜色代码到 Adventure 组件的转换。
 * 兼容不同版本的 Minecraft 文本系统。
 *
 * @author mical
 * @since 2024/2/17 17:07
 */

/**
 * 旧版 JSON 解析器实例
 * 
 * 用于兼容旧版 Gson API，旧版没有 parseString 静态方法。
 * 提供向后兼容的 JSON 解析功能。
 */
val JSON_PARSER = JsonParser()

/**
 * 旧版文本序列化器
 * 
 * 配置为使用 § 符号作为颜色代码前缀，支持十六进制颜色格式。
 * 用于将旧版颜色代码转换为 Adventure 组件。
 * 
 * 配置说明：
 * - character('\u00a7'): 使用 § 符号作为颜色代码前缀
 * - useUnusualXRepeatedCharacterHexFormat(): 支持 #RRGGBB 格式的十六进制颜色
 * - hexColors(): 启用十六进制颜色支持
 */
val LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
    .character('\u00a7')
    .useUnusualXRepeatedCharacterHexFormat()
    .hexColors()
    .build()

/**
 * 将旧版颜色代码文本转换为 Adventure 组件
 * 
 * 将包含 &a、&b 等颜色代码的字符串转换为 Adventure 文本组件。
 * 支持所有标准的 Minecraft 颜色代码和格式代码。
 *
 * @return 转换后的 Adventure 组件
 * 
 * @example
 * ```kotlin
 * // 基本颜色转换
 * "&aHello &bWorld".legacyToAdventure() // 返回带颜色的组件
 * 
 * // 复杂格式
 * "&l&c&oBold Red Italic Text".legacyToAdventure()
 * 
 * // 十六进制颜色
 * "&#FF5733Custom Color".legacyToAdventure()
 * 
 * // 在消息系统中使用
 * fun sendColoredMessage(player: Player, message: String) {
 *     val component = message.legacyToAdventure()
 *     player.sendMessage(component)
 * }
 * 
 * // 在物品名称中使用
 * fun createColoredItem(material: Material, name: String): ItemStack {
 *     val item = ItemStack(material)
 *     val meta = item.itemMeta
 *     meta.setDisplayName(name.legacyToAdventure())
 *     item.itemMeta = meta
 *     return item
 * }
 * ```
 */
fun String.legacyToAdventure(): Component {
    return LEGACY_COMPONENT_SERIALIZER.deserialize(this)
}

/**
 * 将字符串列表构建为复合文本组件
 * 
 * 将字符串列表转换为带颜色的组件文本列表。
 * 适用于物品描述、聊天消息等多行文本。
 *
 * @return 组件文本列表，如果输入为 null 则返回空列表
 * 
 * @example
 * ```kotlin
 * // 基本用法
 * listOf("&aLine 1", "&bLine 2").toBuiltComponent() // 返回带颜色的组件列表
 * 
 * // 物品描述
 * val lore = listOf(
 *     "&7这是一个特殊的物品",
 *     "&e稀有度: &6传说",
 *     "&b效果: &f增加攻击力"
 * )
 * val componentLore = lore.toBuiltComponent()
 * 
 * // 在 GUI 中使用
 * fun createGuiTitle(title: String, subtitle: String?): Component {
 *     val lines = mutableListOf(title)
 *     if (subtitle != null) {
 *         lines.add("&7$subtitle")
 *     }
 *     return lines.toBuiltComponent().firstOrNull() ?: Component.empty()
 * }
 * 
 * // 处理 null 输入
 * val nullList: List<String>? = null
 * val components = nullList.toBuiltComponent() // 返回空列表
 * ```
 */
fun List<String>?.toBuiltComponent(): List<ComponentText> {
    return this?.map { it.component().buildColored() } ?: emptyList()
}