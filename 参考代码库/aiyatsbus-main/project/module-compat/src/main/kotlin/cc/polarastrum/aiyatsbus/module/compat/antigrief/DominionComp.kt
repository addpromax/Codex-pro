/*
 * This file is part of AntiGriefLib, licensed under the MIT License.
 *
 *  Copyright (c) 2024 XiaoMoMi
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.

 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package cc.polarastrum.aiyatsbus.module.compat.antigrief

import cc.polarastrum.aiyatsbus.core.compat.AntiGrief
import cc.polarastrum.aiyatsbus.core.compat.AntiGriefChecker
import cn.lunadeer.dominion.api.DominionAPI
import cn.lunadeer.dominion.api.dtos.flag.Flags
import cn.lunadeer.dominion.api.dtos.flag.PriFlag
import org.bukkit.Location
import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.DominionComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class DominionComp : AntiGrief {

    override fun canPlace(player: Player, location: Location): Boolean {
        return checkPermission(player, location, Flags.PLACE)
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return checkPermission(player, location, Flags.BREAK_BLOCK)
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return checkPermission(player, location, Flags.PLACE)
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return checkPermission(player, entity.location, Flags.PLACE)
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        val dto = DominionAPI.getInstance().getDominion(entity.location) ?: return true
        return if (entity is Player) {
            DominionAPI.getInstance().checkEnvironmentFlag(dto, Flags.MONSTER_DAMAGE)
        } else if (entity is Villager) {
            DominionAPI.getInstance().checkPrivilegeFlag(dto, Flags.VILLAGER_KILLING, player)
        } else if (entity is Animals) {
            DominionAPI.getInstance().checkPrivilegeFlag(dto, Flags.ANIMAL_KILLING, player)
        } else {
            false
        }
    }

    private fun checkPermission(player: Player, location: Location, flag: PriFlag): Boolean {
        // Dominion 的权限检查逻辑
        // 这里需要根据具体的 Dominion API 来实现
        val dto = DominionAPI.getInstance().getDominion(location) ?: return true
        return DominionAPI.getInstance().checkPrivilegeFlag(dto, flag, player)
    }

    override fun getAntiGriefPluginName(): String {
        return "Dominion"
    }

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(DominionComp())
        }
    }
} 