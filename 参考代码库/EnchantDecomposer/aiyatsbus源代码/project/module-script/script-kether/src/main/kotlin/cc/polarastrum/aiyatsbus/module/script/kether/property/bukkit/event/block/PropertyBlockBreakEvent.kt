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
import org.bukkit.event.block.BlockBreakEvent
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.event.block.PropertyBlockBreakEvent
 *
 * @author mical
 * @since 2024/3/10 13:35
 */
@AiyatsbusProperty(
    id = "block-break-event",
    bind = BlockBreakEvent::class
)
class PropertyBlockBreakEvent : AiyatsbusGenericProperty<BlockBreakEvent>("block-break-event") {

    override fun readProperty(instance: BlockBreakEvent, key: String): OpenResult {
        val property: Any? = when (key) {
            "player" -> instance.player
            "isDropItems", "is-drop-items", "drop-items" -> instance.isDropItems
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: BlockBreakEvent, key: String, value: Any?): OpenResult {
        when (key) {
            "isDropItems", "is-drop-items", "drop-items" -> instance.isDropItems = value?.coerceBoolean() ?: return OpenResult.successful()
            else -> return OpenResult.failed()
        }
        return OpenResult.successful()
    }
}