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
import org.bukkit.entity.AbstractVillager
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import world.bentobox.bentobox.BentoBox
import world.bentobox.bentobox.api.user.User
import world.bentobox.bentobox.lists.Flags
import world.bentobox.bentobox.util.Util

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.compat.antigrief.BentoBoxComp
 *
 * @author mical
 * @since 2024/4/5 15:03
 */
class BentoBoxComp : AntiGrief {

    override fun canPlace(player: Player, location: Location): Boolean {
        return BentoBox.getInstance()
            .islands
            .getIslandAt(location)
            .map { it.isAllowed(User.getInstance(player), Flags.PLACE_BLOCKS) }
            .orElse(true)
    }

    override fun canBreak(player: Player, location: Location): Boolean {
        return BentoBox.getInstance()
            .islands
            .getIslandAt(location)
            .map { it.isAllowed(User.getInstance(player), Flags.BREAK_BLOCKS) }
            .orElse(true)
    }

    override fun canInteract(player: Player, location: Location): Boolean {
        return BentoBox.getInstance()
            .islands
            .getIslandAt(location)
            .map { it.isAllowed(User.getInstance(player), Flags.CONTAINER) }
            .orElse(true)
    }

    override fun canInteractEntity(player: Player, entity: Entity): Boolean {
        // I am not sure if I should use Flags.HURT_xxx or Flags.CONTAINER
        return entityOperation(player, entity)
    }

    override fun canDamage(player: Player, entity: Entity): Boolean {
        return entityOperation(player, entity) && (entity !is Player || entity.getWorld().pvp)
    }

    private fun entityOperation(player: Player, entity: Entity): Boolean {
        val type = if (Util.isPassiveEntity(entity))
            Flags.HURT_ANIMALS
        else if (entity is AbstractVillager)
            Flags.HURT_VILLAGERS
        else if (Util.isHostileEntity(entity))
            Flags.HURT_MONSTERS
        else Flags.CONTAINER

        return BentoBox.getInstance()
            .islands
            .getIslandAt(entity.location)
            .map { it.isAllowed(User.getInstance(player), type) }
            .orElse(true)
    }

    override fun getAntiGriefPluginName(): String {
        return "BentoBox"
    }

    companion object {

        @Awake(LifeCycle.ACTIVE)
        fun init() {
            AntiGriefChecker.registerNewCompatibility(BentoBoxComp())
        }
    }
}