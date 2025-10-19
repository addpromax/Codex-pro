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
package cc.polarastrum.aiyatsbus.impl.registration.v12100_paper

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import net.minecraft.world.item.enchantment.Enchantment

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.registration.v12100_paper.EnchantmentHelper
 *
 * @author mical
 * @since 2024/8/17 14:56
 */
object EnchantmentHelper {

    fun createCraftEnchantment(enchant: AiyatsbusEnchantmentBase, nms: Enchantment): Any {
        return AiyatsbusCraftEnchantment(enchant, nms)
    }
}