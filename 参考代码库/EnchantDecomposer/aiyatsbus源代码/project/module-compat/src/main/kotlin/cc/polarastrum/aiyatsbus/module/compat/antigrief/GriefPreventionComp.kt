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
import me.ryanhamshire.GriefPrevention.Claim
import me.ryanhamshire.GriefPrevention.ClaimPermission
import me.ryanhamshire.GriefPrevention.GriefPrevention
import me.ryanhamshire.GriefPrevention.PlayerData
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.GriefPreventionComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class GriefPreventionComp : AntiGrief {

    override fun canPlace(player: Player, location: Location): Boolean {
        return checkPermission(player, location, ClaimPermission.Build)
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return checkPermission(player, location, ClaimPermission.Build)
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return checkPermission(player, location, ClaimPermission.Inventory)
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return checkPermission(player, entity.location, ClaimPermission.Inventory)
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return checkPermission(player, entity.location, ClaimPermission.Inventory)
    }

    private fun checkPermission(player: Player, location: Location, permission: ClaimPermission): Boolean {
        val playerData = GriefPrevention.instance.dataStore.getPlayerData(player.uniqueId)
        val claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, playerData.lastClaim)
        if (claim == null || playerData.ignoreClaims) return true
        playerData.lastClaim = claim
        return claim.checkPermission(player, permission, null) == null
    }

    override fun getAntiGriefPluginName(): String {
        return "GriefPrevention"
    }

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(GriefPreventionComp())
        }
    }
} 