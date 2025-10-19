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

import org.bukkit.entity.Entity
import java.util.LinkedList

/**
 * NPC 检查器接口
 *
 * 用于检查实体是否为 NPC（非玩家角色）。
 * 支持多种 NPC 插件的兼容性检查，如 Citizens、MyPet 等。
 * 通过注册多个检查器实现，可以同时支持多种 NPC 插件。
 *
 * @author mical
 * @since 2024/9/4 20:47
 */
interface NPCChecker {

    /**
     * 检查实体是否为 NPC
     *
     * 根据具体的 NPC 插件实现，检查指定的实体是否为 NPC。
     * 不同的 NPC 插件可能有不同的检查方式。
     *
     * @param entity 要检查的实体
     * @return 如果实体是 NPC 则返回 true，否则返回 false
     */
    fun checkIfIsNPC(entity: Entity): Boolean

    /**
     * NPC 检查器伴生对象
     *
     * 提供全局的 NPC 检查功能，管理所有已注册的检查器。
     */
    companion object {

        /** 已注册的 NPC 检查器列表 */
        val registeredIntegrations = LinkedList<NPCChecker>()

        /**
         * 检查实体是否为 NPC
         *
         * 使用所有已注册的检查器检查实体。
         * 只要有一个检查器认为该实体是 NPC，就返回 true。
         *
         * @param entity 要检查的实体
         * @return 如果实体是 NPC 则返回 true，否则返回 false
         */
        fun checkIfIsNPC(entity: Entity): Boolean {
            return registeredIntegrations.isNotEmpty() &&
                    registeredIntegrations.any { it.checkIfIsNPC(entity) }
        }
    }
}