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
package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import taboolib.common.util.unsafeLazy

/**
 * Aiyatsbus 启动器
 * 
 * 负责 Aiyatsbus 系统的启动和初始化。
 * 注册 API 实例，确保系统正常运行。
 *
 * @author mical
 * @since 2024/2/17 16:19
 */
object DefaultAiyatsbusBooster {

    /** API 实例，延迟初始化 */
    val api by unsafeLazy { DefaultAiyatsbusAPI() }

    /**
     * 启动 Aiyatsbus 系统
     * 
     * 注册 API 实例到全局 Aiyatsbus 对象中
     */
    fun startup() {
        Aiyatsbus.register(api)
    }
}