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
package cc.polarastrum.aiyatsbus.module.script.kether.property.paper.destroystokyo.event.player

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusGenericProperty
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusProperty
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.paper.event.player.PlayerElytraBoostEvent
 *
 * @author mical
 * @since 2024/7/17 14:24
 */
@AiyatsbusProperty(
    id = "player-elytra-boost-event",
    bind = PlayerElytraBoostEvent::class
)
class PropertyPlayerElytraBoostEvent : AiyatsbusGenericProperty<PlayerElytraBoostEvent>("player-elytra-boost-event") {

    override fun readProperty(instance: PlayerElytraBoostEvent, key: String): OpenResult {
        val property: Any? = when (key) {
            "itemStack", "item-stack", "item" -> instance.itemStack
            "firework" -> instance.firework
            "shouldConsume", "should-consume", "consume" -> instance.shouldConsume()
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: PlayerElytraBoostEvent, key: String, value: Any?): OpenResult {
        when (key) {
            "shouldConsume", "should-consume", "consume" -> instance.setShouldConsume(value?.coerceBoolean() ?: return OpenResult.successful())
            else -> return OpenResult.failed()
        }
        return OpenResult.successful()
    }
}