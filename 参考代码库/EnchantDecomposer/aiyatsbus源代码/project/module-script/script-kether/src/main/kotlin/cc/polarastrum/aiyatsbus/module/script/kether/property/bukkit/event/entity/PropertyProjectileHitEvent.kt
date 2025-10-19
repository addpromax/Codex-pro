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
import org.bukkit.event.entity.ProjectileHitEvent
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.event.entity.PropertyProjectileHitEvent
 *
 * @author mical
 * @since 2024/4/5 21:08
 */
@AiyatsbusProperty(
    id = "projectile-hit-event",
    bind = ProjectileHitEvent::class
)
class PropertyProjectileHitEvent : AiyatsbusGenericProperty<ProjectileHitEvent>("projectile-hit-event") {

    override fun readProperty(instance: ProjectileHitEvent, key: String): OpenResult {
        val property: Any? = when (key) {
            "hitBlock", "hit-block" -> instance.hitBlock
            "hitBlockFace", "hit-block-face" -> instance.hitBlockFace
            "hitEntity", "hit-entity" -> instance.hitEntity
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: ProjectileHitEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}