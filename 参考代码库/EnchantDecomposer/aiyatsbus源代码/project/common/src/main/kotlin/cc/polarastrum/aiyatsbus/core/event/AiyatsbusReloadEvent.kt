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
package cc.polarastrum.aiyatsbus.core.event

import taboolib.platform.type.BukkitProxyEvent

/**
 * Aiyatsbus 重载事件
 *
 * 当 Aiyatsbus 插件执行重载操作时触发此事件。
 * 允许其他插件监听重载过程，并根据需要执行相应的处理逻辑。
 *
 * @author mical
 * @since 2025/6/27 00:06
 */
class AiyatsbusReloadEvent: BukkitProxyEvent() {

    /** 是否允许取消事件，重载事件不允许取消 */
    override val allowCancelled: Boolean = false
}