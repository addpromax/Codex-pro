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
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.land.Land
import org.kingdoms.constants.player.KingdomPlayer
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import java.util.*

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.compat.antigrief.KingdomsComp
 *
 * @author mical
 * @since 2025/2/14 14:55
 */
class KingdomsComp : AntiGrief {

    override fun canPlace(player: Player, location: Location): Boolean {
        return checkPermission(player, location)
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return checkPermission(player, location)
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return checkPermission(player, location)
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        return checkPermission(player, entity.location)
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return checkPermission(player, entity.location)
    }

    private fun checkPermission(player: Player, location: Location): Boolean {
        // Kingdoms 的权限检查逻辑
        // 这里需要根据具体的 Kingdoms API 来实现
        val land = Land.getLand(location)
        if (land == null || !land.isClaimed) return true
        return Optional.ofNullable(land.kingdom)
            .map { kingdom -> KingdomPlayer.getKingdomPlayer(player).kingdom === kingdom }
            .orElse(true)
    }

    override fun getAntiGriefPluginName(): String {
        return "Kingdoms"
    }

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(KingdomsComp())
        }
    }
} 