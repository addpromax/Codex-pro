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
package cc.polarastrum.aiyatsbus.core.script

/**
 * Aiyatsbus 脚本处理器接口
 *
 * 负责管理不同类型的脚本处理器，如 Kether、JavaScript、Fluxon 等。
 * 提供统一的脚本处理接口，支持多种脚本语言的执行。
 * 根据脚本类型返回对应的处理器实例。
 *
 * @author mical
 * @since 2025/6/22 00:13
 */
interface AiyatsbusScriptHandler {

    /**
     * 获取指定类型的脚本处理器
     *
     * 根据脚本类型返回对应的处理器实例。
     * 支持 Kether、JavaScript、Fluxon 等多种脚本类型。
     *
     * @param type 脚本类型
     * @return 对应的脚本处理器实例
     */
    fun getScriptHandler(type: ScriptType): ScriptHandler
}