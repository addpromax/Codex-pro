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
package cc.polarastrum.aiyatsbus.module.script.kether.property.bukkit.entity

import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusGenericProperty
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusProperty
import org.bukkit.permissions.ServerOperator
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.entity.PropertyServerOperator
 *
 * @author yanshiqwq
 * @since 2024/4/4 10:41
 */
@AiyatsbusProperty(
    id = "server-operator",
    bind = ServerOperator::class
)
class PropertyServerOperator : AiyatsbusGenericProperty<ServerOperator>("server-operator") {

    override fun readProperty(instance: ServerOperator, key: String): OpenResult {
        val property: Any? = when (key) {
            "isOp", "op" -> instance.isOp
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: ServerOperator, key: String, value: Any?): OpenResult {
        when (key) {
            "isOp", "op" -> {
                instance.isOp = value?.coerceBoolean() ?: return OpenResult.successful()
            }
            else -> return OpenResult.failed()
        }
        return OpenResult.successful()
    }
}