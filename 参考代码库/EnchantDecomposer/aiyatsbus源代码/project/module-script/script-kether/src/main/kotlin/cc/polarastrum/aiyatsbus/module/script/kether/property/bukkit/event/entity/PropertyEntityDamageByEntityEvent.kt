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
package cc.polarastrum.aiyatsbus.module.script.kether.property.bukkit.event.entity

import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusGenericProperty
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusProperty
import org.bukkit.event.entity.EntityDamageByEntityEvent
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.event.entity.PropertyEntityDamageByEntityEvent
 *
 * @author mical
 * @since 2024/3/23 17:46
 */
@AiyatsbusProperty(
    id = "entity-damage-by-entity-event",
    bind = EntityDamageByEntityEvent::class
)
class PropertyEntityDamageByEntityEvent : AiyatsbusGenericProperty<EntityDamageByEntityEvent>("entity-damage-by-entity-event") {

    override fun readProperty(instance: EntityDamageByEntityEvent, key: String): OpenResult {
        val property: Any? = when (key) {
            "damager", "attacker" -> instance.damager
            "isCritical", "is-critical", "critical" -> instance.isCritical
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: EntityDamageByEntityEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}