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

import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.jvm.Throws

/**
 * Aiyatsbus Minecraft API 接口
 *
 * 提供与 Minecraft 内部系统的交互接口。
 * 包含物品操作、方块破坏、组件转换等底层功能。
 *
 * @author mical
 * @since 2024/2/18 00:14
 */
interface AiyatsbusMinecraftAPI {

    /**
     * 获取物品在铁砧上的操作数
     *
     * @param item 物品
     * @return 操作数
     */
    fun getRepairCost(item: ItemStack): Int

    /**
     * 设置物品在铁砧上的操作数
     *
     * @param item 物品
     * @param cost 操作数
     */
    fun setRepairCost(item: ItemStack, cost: Int)

    /**
     * 创建物品堆栈
     *
     * 1.18.2 以下版本（不包含 1.18.2）中 ItemFactory#createItemStack 不存在，
     * 此函数用以替代。
     *
     * @param material 材料名称
     * @param tag 标签
     * @return 物品堆栈
     * @throws IllegalStateException 如果创建失败
     */
    @Throws(IllegalStateException::class)
    fun createItemStack(material: String, tag: String?): ItemStack

    /**
     * 为原版的 MerchantRecipeList 的物品显示更多附魔
     *
     * @param merchantRecipeList 商人配方列表
     * @param player 玩家
     */
    fun adaptMerchantRecipe(merchantRecipeList: Any, player: Player)

    /**
     * 将 Json 转成 IChatBaseComponent
     *
     * @param json JSON 字符串
     * @return IChatBaseComponent 对象
     */
    fun componentFromJson(json: String): Any

    /**
     * 将 IChatBaseComponent 转成 Json
     *
     * @param iChatBaseComponent IChatBaseComponent 对象
     * @return JSON 字符串
     */
    fun componentToJson(iChatBaseComponent: Any): String

    /**
     * 破坏方块
     *
     * 取代高版本 player.breakBlock 的函数，会触发 BlockBreakEvent。
     *
     * @param player 玩家
     * @param block 方块
     * @return 是否成功破坏
     */
    fun breakBlock(player: Player, block: Block): Boolean

    /**
     * 损坏物品堆栈
     *
     * 取代高版本 ItemStack#damage。
     *
     * @param item 物品堆栈
     * @param amount 损坏量
     * @param entity 实体
     * @return 损坏后的物品堆栈
     */
    fun damageItemStack(item: ItemStack, amount: Int, entity: LivingEntity): ItemStack

    fun getCursorItem(player: Player): Any?
}