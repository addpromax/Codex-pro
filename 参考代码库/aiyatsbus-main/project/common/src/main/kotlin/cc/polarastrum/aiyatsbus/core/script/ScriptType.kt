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
 * 脚本类型枚举
 *
 * 定义了 Aiyatsbus 支持的脚本语言类型。
 * 每种类型都有对应的脚本处理器实现。
 * 支持多种脚本语言，满足不同的开发需求。
 *
 * @author mical
 * @since 2025/6/22 12:54
 */
enum class ScriptType {

    /** Kether 脚本语言，基于 TabooLib 的脚本系统 */
    KETHER,

    /** JavaScript 脚本语言，支持标准的 JavaScript 语法 */
    JAVASCRIPT,

    /** Fluxon 脚本语言，服了脚本 */
    FLUXON
}