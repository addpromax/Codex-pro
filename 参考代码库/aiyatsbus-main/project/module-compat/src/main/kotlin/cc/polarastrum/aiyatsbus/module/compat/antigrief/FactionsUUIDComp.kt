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
import com.massivecraft.factions.FactionsPlugin
import com.massivecraft.factions.listeners.FactionsBlockListener
import com.massivecraft.factions.listeners.FactionsEntityListener
import com.massivecraft.factions.perms.PermissibleActions
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.util.unsafeLazy

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.FactionsUUIDComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class FactionsUUIDComp : AntiGrief {

    val plugin by unsafeLazy {
        FactionsPlugin.getInstance()
    }

    override fun canPlace(player: Player, location: Location): Boolean {
        if (!plugin.worldUtil().isEnabled(location.world)) return true
        return FactionsBlockListener.playerCanBuildDestroyBlock(player, location, PermissibleActions.BUILD, false)
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        if (!plugin.worldUtil().isEnabled(location.world)) return true
        return FactionsBlockListener.playerCanBuildDestroyBlock(player, location, PermissibleActions.DESTROY, false)
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        if (!plugin.worldUtil().isEnabled(location.world)) return true
        return FactionsBlockListener.playerCanBuildDestroyBlock(player, location, PermissibleActions.CONTAINER, false)
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        if (!plugin.worldUtil().isEnabled(entity.world)) return true
        return FactionsBlockListener.playerCanBuildDestroyBlock(player, entity.location, PermissibleActions.CONTAINER, false)
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        if (!plugin.worldUtil().isEnabled(entity.world)) return true
        return FactionsEntityListener.canDamage(player, entity, false)
    }

    override fun getAntiGriefPluginName(): String {
        return "FactionsUUID"
    }

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(FactionsUUIDComp())
        }
    }
} 