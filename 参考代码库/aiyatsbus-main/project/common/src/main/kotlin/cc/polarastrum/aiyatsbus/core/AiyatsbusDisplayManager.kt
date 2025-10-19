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

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.configuration.Configuration

/**
 * Aiyatsbus 显示管理器接口
 *
 * 负责管理附魔的显示效果，包括物品名称、描述、光效等。
 * 提供附魔展示和取消展示的功能。
 *
 * @author mical
 * @since 2024/2/17 21:59
 */
interface AiyatsbusDisplayManager {

    /**
     * 获取显示模块配置
     *
     * @return 显示设置配置
     */
    fun getSettings(): Settings

    /**
     * 按照配置中设定的品质排行顺序整理附魔
     *
     * @param enchants 要排序的附魔映射
     * @return 排序后的附魔映射
     */
    fun sortEnchants(enchants: Map<AiyatsbusEnchantment, Int>): LinkedHashMap<AiyatsbusEnchantment, Int>

    /**
     * 展示附魔，展示是给玩家看的，玩家必须存在
     *
     * @param item 要展示的物品
     * @param player 目标玩家
     * @return 展示后的物品
     */
    fun display(item: ItemStack, player: Player): ItemStack

    /**
     * 取消展示
     *
     * @param item 要取消展示的物品
     * @param player 目标玩家
     * @return 取消展示后的物品
     */
    fun undisplay(item: ItemStack, player: Player): ItemStack

    /**
     * 显示设置接口
     *
     * 定义附魔显示的各种配置选项。
     */
    interface Settings {

        /** 配置文件 */
        var conf: Configuration

        /** 是否启用内置附魔显示 */
        var enable: Boolean

        /** 默认附魔前缀 */
        var defaultPrevious: String

        /** 默认附魔后缀 */
        var defaultSubsequent: String

        /** 附魔词条数空余 */
        var capabilityLine: String

        /** 按等级排序，由高到低 */
        var sortByLevel: Boolean

        /** 品质顺序 */
        val rarityOrder: List<String>

        /** 开启合并显示 */
        var combine: Boolean

        /** 当物品附魔超过多少时开启合并显示 */
        var combineMinimal: Int

        /** 每次合并几个附魔，也就是每行显示几个 */
        var combineAmount: Int

        /** 合并布局 */
        var combineLayout: List<String>

        /** Lore 显示顺序（物品本身有 Lore 时） */
        var hasLoreFormation: List<String>

        /** Lore 显示顺序（物品本身没有 Lore 时） */
        var withoutLoreFormation: List<String>

        /** 是否独立在最后单行显示拥有特殊显示的附魔 */
        var separateSpecial: Boolean
    }
}