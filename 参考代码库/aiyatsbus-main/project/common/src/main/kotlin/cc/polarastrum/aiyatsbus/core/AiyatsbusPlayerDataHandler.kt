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

import cc.polarastrum.aiyatsbus.core.data.PlayerData
import org.bukkit.entity.Player

/**
 * Aiyatsbus 玩家数据处理器接口
 *
 * 负责管理玩家的附魔数据和状态。
 * 提供玩家数据的加载、保存和获取功能。
 *
 * @author mical
 * @since 2024/2/18 12:57
 */
interface AiyatsbusPlayerDataHandler {

    /**
     * 加载玩家数据
     *
     * @param player 玩家
     */
    fun load(player: Player)

    /**
     * 保存玩家数据
     *
     * @param player 玩家
     */
    fun save(player: Player)

    /**
     * 获取玩家数据
     *
     * @param player 玩家
     * @return 玩家数据
     */
    fun get(player: Player): PlayerData
}