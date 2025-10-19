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

import org.bukkit.NamespacedKey
import java.io.File

/**
 * Aiyatsbus 附魔管理器接口
 *
 * 提供附魔的注册、加载、查询和管理功能。
 * 定义了附魔系统的核心管理操作，包括附魔的增删改查等。
 *
 * @author mical
 * @since 2024/2/17 16:16
 */
interface AiyatsbusEnchantmentManager {

    /**
     * 根据命名空间键获取附魔
     *
     * @param key 附魔的命名空间键
     * @return 对应的附魔实例，如果不存在则返回 null
     */
    fun getEnchant(key: NamespacedKey): AiyatsbusEnchantment?

    /**
     * 根据命名空间键的字符串形式获取附魔
     *
     * @param key 附魔的字符串键
     * @return 对应的附魔实例，如果不存在则返回 null
     */
    fun getEnchant(key: String): AiyatsbusEnchantment?

    /**
     * 根据名称获取附魔
     *
     * @param name 附魔的名称
     * @return 对应的附魔实例，如果不存在则返回 null
     */
    fun getByName(name: String): AiyatsbusEnchantment?

    /**
     * 获取全部附魔
     *
     * @return 所有已注册附魔的映射表，键为命名空间键，值为附魔实例
     */
    fun getEnchants(): Map<NamespacedKey, AiyatsbusEnchantment>

    /**
     * 注册附魔
     *
     * @param enchantment 要注册的附魔实例
     */
    fun register(enchantment: AiyatsbusEnchantmentBase)

    /**
     * 取消注册附魔
     *
     * @param enchantment 要取消注册的附魔实例
     */
    fun unregister(enchantment: AiyatsbusEnchantment)

    /**
     * 加载附魔
     *
     * 从配置文件中加载所有附魔数据
     */
    fun loadEnchantments()

    /**
     * 从文件中加载附魔
     *
     * @param file 包含附魔配置的文件
     */
    fun loadFromFile(file: File)

    /**
     * 删除附魔
     *
     * 清空所有已注册的附魔数据
     */
    fun clearEnchantments()
}