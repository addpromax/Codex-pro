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
package cc.polarastrum.aiyatsbus.module.script.kether.action.game

import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.entity.Firework
import taboolib.module.kether.KetherParser
import taboolib.module.kether.combinationParser
import taboolib.module.kether.player
import taboolib.module.nms.spawnEntity
import taboolib.platform.util.toBukkitLocation

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.action.game.ActionFirework
 *
 * @author mical
 * @since 2025/1/27 23:06
 */
object ActionFirework {

    /*
        // 生成烟花
        val firework = location.world.spawnEntity(location, EntityType.FIREWORK) as Firework
        val meta = firework.fireworkMeta

        // 设置烟花效果
        val effect = FireworkEffect.builder()
            .withColor(Color.RED)
            .withFade(Color.ORANGE)
            .with(FireworkEffect.Type.BALL_LARGE)
            .trail(true)
            .build()

        meta.addEffect(effect)
        meta.power = 1
        firework.fireworkMeta = meta

        // 立即引爆烟花
        firework.detonate()
     */

    val colorMap = mapOf(
        // 中文名称
        "红" to Color.RED,
        "绿" to Color.GREEN,
        "蓝" to Color.BLUE,
        "白" to Color.WHITE,
        "黑" to Color.BLACK,
        "黄" to Color.YELLOW,
        "紫" to Color.PURPLE,
        "橙" to Color.ORANGE,
        "青" to Color.AQUA,
        "灰" to Color.GRAY,

        // 英文名称
        "red" to Color.RED,
        "green" to Color.GREEN,
        "blue" to Color.BLUE,
        "white" to Color.WHITE,
        "black" to Color.BLACK,
        "yellow" to Color.YELLOW,
        "purple" to Color.PURPLE,
        "orange" to Color.ORANGE,
        "aqua" to Color.AQUA,
        "gray" to Color.GRAY,

        // 常见的额外颜色
        "pink" to Color.fromRGB(255, 192, 203),
        "粉" to Color.fromRGB(255, 192, 203),
        "lime" to Color.LIME,
        "青柠" to Color.LIME,
        "maroon" to Color.MAROON,
        "褐" to Color.MAROON,
        "navy" to Color.NAVY,
        "深蓝" to Color.NAVY,
        "olive" to Color.OLIVE,
        "橄榄" to Color.OLIVE,
        "silver" to Color.SILVER,
        "银" to Color.SILVER,
        "teal" to Color.TEAL,
        "青绿" to Color.TEAL
    )

    @KetherParser(["build-firework"], shared = true)
    fun fireworkBuilder() = combinationParser {
        it.group(
            any(),
            command("type", then = text()),
            command("power", then = int()).option(),
            command("flicker", then = bool()).option(),
            command("trail", then = bool()).option(),
            command("main-colors", then = any()).option(),
            command("fade-colors", then = any()).option()
        ).apply(it) { location, type, power, flicker, trail, mainColors, fadeColors ->
            now { createFirework(resolveLocation(location), type, power, flicker, trail, resolveColors(mainColors), resolveColors(fadeColors)) }
        }
    }

    @KetherParser(["detonate-firework"], shared = true)
    fun detonateFirework() = combinationParser {
        it.group(type<Firework>()).apply(it) { firework -> now { firework.detonate() } }
    }

    private fun resolveLocation(loc: Any?): Location {
        return when (loc) {
            is taboolib.common.util.Location -> loc.toBukkitLocation()
            is Location -> loc
            is String -> {
                val (world, x, y, z) = loc.split(' ', limit = 4)
                Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
            }

            else -> throw UnsupportedOperationException("resolve location")
        }
    }

    private fun resolveColors(any: Any?): List<Color> {
        if (any == null) return emptyList()
        return when (any) {
            is String -> listOf(colorMap[any.lowercase()]!!)
            is Color -> listOf(any)
            is List<*> -> any.map { if (it is Color) it else colorMap[it.toString().lowercase()]!! }
            else -> throw UnsupportedOperationException("resolve colors")
        }
    }

    private fun createFirework(
        location: Location,
        with: String, // BALL 小型球状效果, BALL_LARGE 大型球状效果, BURST 爆裂效果, CREEPER 苦力怕脸型效果, STAR 星形效果
        power: Int?,
        flicker: Boolean?,
        trail: Boolean?,
        mainColors: List<Color>,
        fadeColors: List<Color>,
    ): Firework {
        return createFirework(
            location,
            FireworkEffect.Type.valueOf(with),
            power,
            flicker,
            trail,
            mainColors,
            fadeColors
        )
    }

    private fun createFirework(
        location: Location,
        with: FireworkEffect.Type, // BALL 小型球状效果, BALL_LARGE 大型球状效果, BURST 爆裂效果, CREEPER 苦力怕脸型效果, STAR 星形效果
        power: Int?,
        flicker: Boolean?,
        trail: Boolean?,
        mainColors: List<Color>,
        fadeColors: List<Color>,
    ): Firework {
        val builder = FireworkEffect.builder()
            .with(with) // 类型必须设置
        // 以下都是可选设置选项
        flicker?.let { builder.flicker(it) }
        trail?.let { builder.trail(it) }
        if (mainColors.isNotEmpty()) {
            builder.withColor(mainColors)
        }
        if (fadeColors.isNotEmpty()) {
            builder.withColor(fadeColors)
        }
        return location.spawnEntity(Firework::class.java) { firework ->
            firework.fireworkMeta = firework.fireworkMeta.apply {
                addEffect(builder.build())
                power?.let { this.power = it }
            }
        }
    }
}