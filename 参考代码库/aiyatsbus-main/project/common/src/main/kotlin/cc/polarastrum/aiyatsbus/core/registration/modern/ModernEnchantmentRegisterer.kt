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
package cc.polarastrum.aiyatsbus.core.registration.modern

import cc.polarastrum.aiyatsbus.core.registration.AiyatsbusEnchantmentRegisterer

/**
 * 1.20.3+ 附魔注册器接口
 *
 * 继承自 AiyatsbusEnchantmentRegisterer，提供 1.20.3+ 版本的附魔注册功能。
 * 支持替换注册表等高级功能，适用于 1.20.3+ 的 Minecraft 版本。
 * 通过注册表的冻结和解冻机制，确保附魔注册的安全性。
 *
 * @author mical
 * @since 2024/2/17 15:22
 */
interface ModernEnchantmentRegisterer : AiyatsbusEnchantmentRegisterer {

    /**
     * 解冻注册表
     *
     * 在注册新附魔前解冻注册表，允许修改。
     */
    fun unfreezeRegistry()

    /**
     * 替换注册表
     *
     * 替换 Bukkit 的附魔注册表，确保自定义附魔能够正确注册和使用。
     * 这是 1.20.3+ 版本特有的功能。
     */
    fun replaceRegistry()

    /**
     * 冻结注册表
     *
     * 注册完成后冻结注册表，防止意外修改。
     */
    fun freezeRegistry()
}