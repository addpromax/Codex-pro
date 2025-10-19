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

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.containers.Flags
import com.bekvon.bukkit.residence.listeners.ResidenceEntityListener
import com.bekvon.bukkit.residence.protection.FlagPermissions
import com.bekvon.bukkit.residence.utils.Utils
import cc.polarastrum.aiyatsbus.core.compat.AntiGrief
import cc.polarastrum.aiyatsbus.core.compat.AntiGriefChecker
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import java.util.*

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.compat.antigrief.ResidenceComp
 *
 * @author mical
 * @since 2024/4/4 13:28
 */
class ResidenceComp : AntiGrief {

    override fun canPlace(player: Player, location: Location): Boolean {
        return Optional.ofNullable(Residence.getInstance().residenceManager.getByLoc(location))
            .map { claimedResidence ->
                claimedResidence.permissions.playerHas(player, Flags.place, true)
            }
            .orElse(true)
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return Optional.ofNullable(Residence.getInstance().residenceManager.getByLoc(location))
            .map { claimedResidence ->
                claimedResidence.permissions.playerHas(player, Flags.destroy, true)
            }
            .orElse(true)
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return Optional.ofNullable(Residence.getInstance().residenceManager.getByLoc(location))
            .map { claimedResidence ->
                claimedResidence.permissions.playerHas(player, Flags.use, true)
            }
            .orElse(true)
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return Optional.ofNullable(Residence.getInstance().residenceManager.getByLoc(entity.location))
            .map { claimedResidence ->
                claimedResidence.permissions.playerHas(player, Flags.build, true)
            }
            .orElse(true)
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return Optional.ofNullable(Residence.getInstance().residenceManager.getByLoc(entity.location))
            .map { claimedResidence ->
                if (entity is Player) {
                    val src =
                        Residence.getInstance().getPermsByLoc(player.location)
                            .has(Flags.pvp, FlagPermissions.FlagCombo.TrueOrNone)
                    val target =
                        Residence.getInstance().getPermsByLoc(entity.getLocation())
                            .has(Flags.pvp, FlagPermissions.FlagCombo.TrueOrNone)
                    return@map src && target && player.world.pvp
                }
                if (Utils.isAnimal(entity)) {
                    return@map claimedResidence.permissions
                        .playerHas(player, Flags.animalkilling, true)
                } else if (ResidenceEntityListener.isMonster(entity)) {
                    return@map claimedResidence.permissions
                        .playerHas(player, Flags.mobkilling, true)
                }
                null
            }
            .orElse(true)!!
    }

    override fun getAntiGriefPluginName(): String {
        return "Residence"
    }

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(ResidenceComp())
        }
    }
}