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
package cc.polarastrum.aiyatsbus.core

import org.bukkit.command.CommandSender

/**
 * Aiyatsbus 语言系统接口
 *
 * 提供多语言支持和文本管理功能。
 * 支持发送、获取和格式化多语言文本，支持前缀和参数替换。
 *
 * @author mical
 * @since 2024/4/2 20:09
 */
interface AiyatsbusLanguage {

    /**
     * 发送语言文本
     *
     * 向指定发送者发送格式化的语言文本。
     *
     * @param sender 消息接收者
     * @param key 语言键
     * @param args 格式化参数
     */
    fun sendLang(sender: CommandSender, key: String, vararg args: Any)

    /**
     * 获取语言文本
     *
     * 获取格式化的语言文本字符串。
     *
     * @param sender 用于确定语言的发送者
     * @param key 语言键
     * @param args 格式化参数
     * @return 格式化后的文本字符串
     */
    fun getLang(sender: CommandSender, key: String, vararg args: Any): String

    /**
     * 获取可空语言文本
     *
     * 获取格式化的语言文本，如果键不存在则返回 null。
     *
     * @param sender 用于确定语言的发送者
     * @param key 语言键
     * @param args 格式化参数
     * @return 格式化后的文本字符串，如果键不存在则返回 null
     */
    fun getLangOrNull(sender: CommandSender, key: String, vararg args: Any): String?

    /**
     * 获取语言文本列表
     *
     * 获取格式化的语言文本列表。
     *
     * @param sender 用于确定语言的发送者
     * @param key 语言键
     * @param args 格式化参数
     * @return 格式化后的文本列表
     */
    fun getLangList(sender: CommandSender, key: String, vararg args: Any): List<String>
}