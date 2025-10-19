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

import org.bukkit.Location
import org.bukkit.block.Block
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 方块工具类
 *
 * 提供方块操作相关的工具函数，包括矿脉检测等。
 * 使用广度优先搜索算法进行高效的方块探索。
 *
 * @author mical
 * @since 2024/8/27 17:35
 */
object BlockUtils {

    /**
     * 方块方向数组
     * 
     * 包含六个基本方向和对角线方向的三维坐标偏移。
     * 用于在三维空间中探索相邻方块。
     * 基本方向：上下左右前后
     * 对角线方向：八个角落位置
     */
    private val blockDirections = listOf(
        // 六个基本方向
        doubleArrayOf(0.0, 1.0, 0.0),   // 上
        doubleArrayOf(0.0, -1.0, 0.0),  // 下
        doubleArrayOf(1.0, 0.0, 0.0),   // 东
        doubleArrayOf(-1.0, 0.0, 0.0),  // 西
        doubleArrayOf(0.0, 0.0, -1.0),  // 北
        doubleArrayOf(0.0, 0.0, 1.0),   // 南
        // 对角线方向
        doubleArrayOf(1.0, 1.0, 0.0),   // 东北上
        doubleArrayOf(-1.0, -1.0, 0.0), // 西南下
        doubleArrayOf(1.0, -1.0, 0.0),  // 东南下
        doubleArrayOf(-1.0, 1.0, 0.0),  // 西北上
        doubleArrayOf(0.0, 1.0, -1.0),  // 北上
        doubleArrayOf(0.0, -1.0, 1.0),  // 南下
        doubleArrayOf(0.0, -1.0, -1.0), // 北下
        doubleArrayOf(0.0, 1.0, 1.0),   // 南上
        doubleArrayOf(1.0, 0.0, -1.0),  // 东北
        doubleArrayOf(-1.0, 0.0, 1.0),  // 西南
        doubleArrayOf(-1.0, 0.0, -1.0), // 西北
        doubleArrayOf(1.0, 0.0, 1.0)    // 东南
    )

    /**
     * 获取矿脉
     * 
     * 使用广度优先搜索（BFS）算法探索与指定方块相连的所有相同类型方块。
     * 适用于矿物挖掘、液体流动等场景。
     * 支持限制获取数量，提高性能。
     *
     * @param block 起始方块
     * @param amount 获取方块位置数量，-1 则为获取完整的矿脉
     * @return 矿脉中所有方块的位置列表（不包含起始方块）
     * 
     * @example
     * ```kotlin
     * // 获取完整的钻石矿脉
     * val diamondVein = BlockUtils.getVein(diamondBlock)
     * 
     * // 限制获取数量，提高性能
     * val limitedVein = BlockUtils.getVein(coalBlock, 10)
     * 
     * // 用于矿物挖掘插件
     * val oreVein = BlockUtils.getVein(oreBlock, 50)
     * oreVein.forEach { location ->
     *     location.block.type = Material.AIR
     *     world.dropItem(location, oreItem)
     * }
     * ```
     */
    fun getVein(block: Block, amount: Int? = null): List<Location> {
        val max = amount ?: -1
        val mines = LinkedList<Location>()
        val queue = ConcurrentLinkedQueue<Location>()
        // 从破坏的方块位置开始，使用广度优先搜索 BFS 算法探索所有相同矿物位置
        queue += block.location
        while (!queue.isEmpty()) {
            // 如果达到了数量限制
            if (max != -1 && mines.size >= max) break
            // 获取当前位置
            val current = queue.poll()
            // 遍历向外一层的方块，添加同种方块（矿物）
            blockDirections.map { current.clone().add(it[0], it[1], it[2]) }
                .filter { it.block.type == current.block.type && it !in mines }
                .forEach {
                    queue += it
                    mines += it
                }
        }
        // 防止重复挖掘自身
        return mines.filter { it != block.location }
    }
}