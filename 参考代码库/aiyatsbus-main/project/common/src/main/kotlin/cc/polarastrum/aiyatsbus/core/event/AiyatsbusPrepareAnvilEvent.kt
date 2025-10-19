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
package cc.polarastrum.aiyatsbus.core.event

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.type.BukkitProxyEvent

/**
 * Aiyatsbus 铁砧准备事件
 *
 * 当玩家在铁砧中放置物品准备合成时触发此事件。
 * 允许插件修改铁砧合成的结果和名称。
 * 提供对铁砧合成过程的完全控制。
 *
 * @param left 左侧物品（主要物品）
 * @param right 右侧物品（材料物品），可能为 null
 * @param result 合成结果，可以被修改
 * @param name 物品名称，可以被修改
 * @param player 触发事件的玩家
 *
 * @author mical
 * @since 2024/5/3 16:02
 */
class AiyatsbusPrepareAnvilEvent(
    /** 左侧物品（主要物品） */
    val left: ItemStack, 
    /** 右侧物品（材料物品），可能为 null */
    val right: ItemStack?, 
    /** 合成结果，可以被修改 */
    var result: ItemStack?, 
    /** 物品名称，可以被修改 */
    var name: String?, 
    /** 触发事件的玩家 */
    val player: Player
) : BukkitProxyEvent() {

    /** 是否允许取消事件 */
    override val allowCancelled: Boolean = true
}