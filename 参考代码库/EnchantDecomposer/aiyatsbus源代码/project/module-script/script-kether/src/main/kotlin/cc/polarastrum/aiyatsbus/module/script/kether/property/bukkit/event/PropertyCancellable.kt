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
package cc.polarastrum.aiyatsbus.module.script.kether.property.bukkit.event

import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusGenericProperty
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusProperty
import org.bukkit.event.Cancellable
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.event.PropertyCancellable
 *
 * @author mical
 * @since 2024/3/10 13:24
 */
@AiyatsbusProperty(
    id = "cancellable",
    bind = Cancellable::class
)
class PropertyCancellable : AiyatsbusGenericProperty<Cancellable>("cancellable") {

    override fun readProperty(instance: Cancellable, key: String): OpenResult {
        val property: Any? = when (key) {
            "isCancelled", "is-cancelled", "cancelled", "cancel" -> instance.isCancelled
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: Cancellable, key: String, value: Any?): OpenResult {
        when (key) {
            "isCancelled", "is-cancelled", "cancelled", "cancel" -> instance.isCancelled = value?.coerceBoolean() ?: return OpenResult.successful()
            else -> return OpenResult.failed()
        }
        return OpenResult.successful()
    }
}