/*
 * This file is part of Vulpecula, licensed under the MIT License.
 *
 *  Copyright (c) 2018 Bkm016
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
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package cc.polarastrum.aiyatsbus.module.script.kether.property.taboolib

import cc.polarastrum.aiyatsbus.core.util.coerceDouble
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusGenericProperty
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusProperty
import taboolib.common.OpenResult
import taboolib.common.util.Vector

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.taboolib.PropertyVector
 *
 * @author Lanscarlos
 * @since 2023-03-22 14:02
 */
@AiyatsbusProperty(
    id = "vector-taboo",
    bind = Vector::class
)
class PropertyVector : AiyatsbusGenericProperty<Vector>("vector-taboo") {

    override fun readProperty(instance: Vector, key: String): OpenResult {
        val property: Any = when (key) {
            "clone" -> instance.clone()
            "bukkit", "bk" -> org.bukkit.util.Vector(instance.x, instance.y, instance.z)
            "blockX", "block-x" -> instance.blockX
            "blockY", "block-y" -> instance.blockY
            "blockZ", "block-z" -> instance.blockZ
            "x" -> instance.x
            "y" -> instance.y
            "z" -> instance.z
            "length" -> instance.length()
            "lengthSquared", "length-squared", "length-sq", "squared", "sq" -> instance.lengthSquared()
            "isNormalized", "normalized" -> instance.isNormalized
            "normalize" -> instance.clone().normalize()
            "zero" -> instance.clone().zero()
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: Vector, key: String, value: Any?): OpenResult {
        when (key) {
            "x" -> {
                instance.x = value?.coerceDouble() ?: return OpenResult.successful()
            }
            "y" -> {
                instance.y = value?.coerceDouble() ?: return OpenResult.successful()
            }
            "z" -> {
                instance.z = value?.coerceDouble() ?: return OpenResult.successful()
            }
            else -> return OpenResult.failed()
        }
        return OpenResult.successful()
    }
}