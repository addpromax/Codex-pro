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
package cc.polarastrum.aiyatsbus.core.compat

import cc.polarastrum.aiyatsbus.core.util.isNull
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.max

/**
 * 物品保护检查器接口
 *
 * 定义物品保护检查接口，用于检查物品是否受保护，防止玩家拾取受保护的物品。
 * 原本为 QuickShop 的两个分支而设计，现在任何插件都可以来做适配。
 *
 * @author mical
 * @since 2024/8/19 09:41
 */
interface GuardItemChecker {

    /**
     * 检查是否为受保护的物品
     *
     * @param item 要检查的物品实体
     * @return 如果是受保护的物品则返回 true
     */
    fun checkIsGuardItem(item: Item): Boolean

    companion object {

        /** 已注册的集成检查器列表 */
        val registeredIntegrations = LinkedList<GuardItemChecker>()

        /**
         * 检查是否为受保护的物品
         *
         * 原本为 QuickShop 的两个分支而设计，现在任何插件都可以来做适配。
         *
         * @param item 要检查的物品实体
         * @param player 玩家，如果为 null 则不检查玩家相关事件
         * @return 如果是受保护的物品则返回 true
         * 
         * @example
         * ```kotlin
         * if (GuardItemChecker.checkIsGuardItem(item, player)) {
         *     // 物品受保护，不允许拾取
         * }
         * ```
         */
        fun checkIsGuardItem(item: Item, player: Player?): Boolean {
            if (!item.canPlayerPickup()) return true
            if (player != null && eventCanceled(item, player)) return true
            return registeredIntegrations.isNotEmpty() &&
                    registeredIntegrations.all { it.checkIsGuardItem(item) }
        }

        /**
         * 计算玩家背包剩余容量
         *
         * @param player 玩家
         * @param item 物品
         * @return 剩余容量
         * 
         * @example
         * ```kotlin
         * val capacity = GuardItemChecker.calculateItemCapacity(player, itemStack)
         * ```
         */
        fun calculateItemCapacity(player: Player, item: ItemStack): Int {
            val inventory = player.inventory
            var emptySlots = 0
            var availableSpace = 0

            for (stack in inventory.storageContents) {
                if (stack.isNull) {
                    emptySlots++
                    availableSpace += item.maxStackSize
                    continue
                }

                if (stack!!.isSimilar(item)) {
                    availableSpace += item.maxStackSize - stack.amount
                }
            }

            // 总剩余容量 = 空槽位容量 + 已有同类物品剩余空间
            val totalCapacity = emptySlots * item.maxStackSize + availableSpace

            // 返回实际可装数量（当请求量超过容量时返回剩余容量）
            return totalCapacity.coerceAtLeast(0)
        }

        /**
         * 依照服务的内顺序依次检测事件判断有无插件阻止
         *
         * @param item 物品实体
         * @param player 玩家
         * @return 如果有插件阻止拾取则返回 true
         */
        private fun eventCanceled(item: Item, player: Player): Boolean {
            val remaining = max(0, item.itemStack.amount - calculateItemCapacity(player, item.itemStack))
            runCatching {
                val e1 = PlayerAttemptPickupItemEvent(player, item, remaining).apply {
                    callEvent()
                }.isCancelled
                if (e1) return true
            }

            runCatching {
                val e2 = PlayerPickupItemEvent(player, item, remaining).apply {
                    callEvent()
                }.isCancelled
                if (e2) return true
            }

            runCatching {
                val e3 = EntityPickupItemEvent(player, item, remaining).apply {
                    callEvent()
                }.isCancelled
                if (e3) return true
            }

            return false
        }
    }
}