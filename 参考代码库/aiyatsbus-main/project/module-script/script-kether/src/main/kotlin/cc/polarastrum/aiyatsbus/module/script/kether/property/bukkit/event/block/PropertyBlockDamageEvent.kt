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
package cc.polarastrum.aiyatsbus.module.script.kether.property.bukkit.event.block

import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusGenericProperty
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusProperty
import org.bukkit.event.block.BlockDamageEvent
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.event.block.PropertyBlockDamageEvent
 *
 * @author yanshiqwq
 * @since 2024/4/2 00:15
 */

@AiyatsbusProperty(
    id = "block-damage-event",
    bind = BlockDamageEvent::class
)
class PropertyBlockDamageEvent : AiyatsbusGenericProperty<BlockDamageEvent>("block-damage-event") {

    override fun readProperty(instance: BlockDamageEvent, key: String): OpenResult {
        val property: Any? = when (key) {
            "player" -> instance.player
            "instaBreak", "insta-break", "instant-break" -> instance.instaBreak
            "itemInHand", "item-in-hand", "hand-item" -> instance.itemInHand
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: BlockDamageEvent, key: String, value: Any?): OpenResult {
        when (key) {
            "instaBreak", "insta-break", "instant-break" -> instance.instaBreak = value?.coerceBoolean() ?: return OpenResult.successful()
            else -> return OpenResult.failed()
        }
        return OpenResult.successful()
    }
}