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

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.platform.util.bukkitPlugin

/**
 * 反破坏插件兼容接口
 *
 * 定义与反破坏插件的兼容接口，提供方块放置、破坏、交互等权限检查功能。
 * 支持多种反破坏插件的统一接入。
 *
 * @author mical
 * @since 2024/4/4 12:53
 */
interface AntiGrief {

    /**
     * 检查玩家是否可以放置方块
     *
     * @param player 玩家
     * @param location 放置位置
     * @return 如果可以放置则返回 true
     */
    fun canPlace(player: Player, location: Location): Boolean

    /**
     * 检查玩家是否可以破坏方块
     *
     * @param player 玩家
     * @param location 破坏位置
     * @return 如果可以破坏则返回 true
     */
    fun canBreak(player: Player, location: Location): Boolean

    /**
     * 检查玩家是否可以与方块交互
     *
     * @param player 玩家
     * @param location 交互位置
     * @return 如果可以交互则返回 true
     */
    fun canInteract(player: Player, location: Location): Boolean

    /**
     * 检查玩家是否可以与实体交互
     *
     * @param player 玩家
     * @param entity 目标实体
     * @return 如果可以交互则返回 true
     */
    fun canInteractEntity(player: Player, entity: Entity): Boolean

    /**
     * 检查玩家是否可以伤害实体
     *
     * @param player 玩家
     * @param entity 目标实体
     * @return 如果可以伤害则返回 true
     */
    fun canDamage(player: Player, entity: Entity): Boolean

    /**
     * 获取插件名称
     *
     * @return 反破坏插件的名称
     */
    fun getAntiGriefPluginName(): String

    /**
     * 检查插件是否存在
     *
     * @return 如果插件存在则返回 true
     */
    fun checkRunning(): Boolean {
        if (getAntiGriefPluginName().contains('.')) {
            return try {
                Class.forName(getAntiGriefPluginName())
                true
            } catch (_: ClassNotFoundException) {
                false
            }
        }
        return bukkitPlugin.server.pluginManager.getPlugin(getAntiGriefPluginName()) != null
    }
}