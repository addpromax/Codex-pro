/*
 * This file is part of EcoEnchants, licensed under the GPL-3.0 License.
 *
 *  Copyright (C) 2024 Auxilor
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
package cc.polarastrum.aiyatsbus.impl.registration.v12103_paper

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import net.minecraft.world.item.enchantment.Enchantment
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.enchantments.CraftEnchantment

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.registration.v12104_paper.EnchantmentHelper
 *
 * @author mical
 * @since 2025/2/14 16:31
 */
object EnchantmentHelper {

    fun createCraftEnchantment(key: NamespacedKey, nms: Enchantment?): Any? {
        return CraftEnchantment(key, nms ?: return null)
    }

    fun createVanillaCraftEnchantment(enchant: AiyatsbusEnchantmentBase, nms: Enchantment): Any {
        return VanillaCraftEnchantment(enchant, nms)
    }

    fun createAiyatsbusCraftEnchantment(enchant: AiyatsbusEnchantmentBase, nms: Enchantment): Any {
        return AiyatsbusCraftEnchantment(enchant, nms)
    }
}