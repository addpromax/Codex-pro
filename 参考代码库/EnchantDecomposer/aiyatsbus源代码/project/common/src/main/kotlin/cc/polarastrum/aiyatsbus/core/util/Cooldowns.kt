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

import cc.polarastrum.aiyatsbus.core.cooldown
import org.bukkit.entity.Player
import taboolib.common5.format

/**
 * 冷却工具类
 * 
 * 提供玩家冷却时间管理的工具函数。
 * 支持添加、移除、检查和清除冷却时间。
 * 使用系统时间戳进行精确的冷却计算。
 *
 * @author mical
 * @since 2024/2/17 17:07
 */

/**
 * 为玩家添加冷却时间
 * 
 * 记录指定冷却键的当前时间戳。
 * 冷却时间以毫秒为单位存储，确保精确的时间控制。
 *
 * @param key 冷却键名，用于标识不同的冷却类型
 * 
 * @example
 * ```kotlin
 * player.addCd("teleport") // 添加传送冷却
 * player.addCd("attack")   // 添加攻击冷却
 * ```
 */
fun Player.addCd(key: String) {
    cooldown[key] = System.currentTimeMillis()
}

/**
 * 移除玩家的指定冷却
 * 
 * 从冷却记录中删除指定的冷却键。
 * 移除后该冷却将不再生效。
 *
 * @param key 要移除的冷却键名
 * 
 * @example
 * ```kotlin
 * player.removeCd("teleport") // 移除传送冷却
 * ```
 */
fun Player.removeCd(key: String) {
    cooldown.remove(key)
}

/**
 * 清除玩家所有冷却
 * 
 * 移除该玩家的所有冷却记录。
 * 谨慎使用，可能影响游戏平衡。
 * 
 * @example
 * ```kotlin
 * player.clearCd() // 清除所有冷却
 * ```
 */
fun Player.clearCd() {
    cooldown.clear()
}

/**
 * 检查玩家冷却状态
 * 
 * 检查指定冷却是否结束，并返回剩余时间。
 * 如果冷却未开始或已结束，返回 true 和 0.0。
 * 如果正在冷却中，返回 false 和剩余秒数。
 *
 * @param key 冷却键名
 * @param cd 冷却时间（秒）
 * @return 冷却状态对，first 为是否冷却结束（true 表示结束），second 为剩余时间（秒）
 * 
 * @example
 * ```kotlin
 * val (isReady, remaining) = player.checkCd("teleport", 30.0)
 * if (isReady) {
 *     // 冷却结束，可以执行传送
 *     player.teleport(targetLocation)
 * } else {
 *     // 还在冷却中，显示剩余时间
 *     player.sendMessage("传送冷却中，剩余 ${remaining} 秒")
 * }
 * ```
 */
fun Player.checkCd(key: String, cd: Double): Pair<Boolean, Double> {
    if (!cooldown.containsKey(key))
        return true to 0.0
    val tmp = (cd - (System.currentTimeMillis() - cooldown[key]!!) / 1000.0).format(1)
    return if (tmp <= 0.0) true to -1.0 else false to maxOf(tmp, 0.0)
}