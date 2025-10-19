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
package cc.polarastrum.aiyatsbus.core.util

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import kotlin.math.max

/**
 * 向量工具类
 *
 * 提供向量操作相关的工具函数，包括安全速度设置、击退抗性处理等。
 * 确保实体速度在安全范围内，防止客户端崩溃和作弊。
 *
 * @author mical
 * @since 2024/4/3 00:20
 */
object Vectors {

    /**
     * 检查速度向量是否不安全
     *
     * 检查向量的各个分量是否超出安全范围（±4.0）。
     * 超出安全范围的速度可能导致客户端崩溃或异常行为。
     *
     * @param vector 要检查的向量
     * @return 如果超出安全范围则返回 true
     * 
     * @example
     * ```kotlin
     * // 检查安全速度
     * val velocity = Vector(5.0, 2.0, 1.0)
     * if (Vectors.isUnsafeVelocity(velocity)) {
     *     println("速度超出安全范围")
     * }
     * ```
     */
    private fun isUnsafeVelocity(vector: Vector): Boolean {
        val x = vector.blockX.toDouble()
        val y = vector.blockY.toDouble()
        val z = vector.blockZ.toDouble()
        return x > 4.0 || y > 4.0 || z > 4.0 || x < -4.0 || y < -4.0 || z < -4.0
    }

    /**
     * 获取安全的速度分量
     *
     * 将速度分量限制在安全范围内（-4.0 到 4.0）。
     * 防止单个分量超出安全限制。
     *
     * @param x 原始速度分量
     * @return 安全的速度分量
     */
    private fun getSafeVelocity(x: Double): Double {
        return if (x > 4.0) 4.0 else (max(x, -4.0))
    }

    /**
     * 转换为安全速度向量
     *
     * 将向量的所有分量限制在安全范围内。
     * 确保整个速度向量都在安全限制内。
     *
     * @param vector 原始向量
     * @return 安全的速度向量
     */
    private fun convertToSafeVelocity(vector: Vector): Vector {
        val x = vector.x
        val y = vector.y
        val z = vector.z
        return Vector(getSafeVelocity(x), getSafeVelocity(y), getSafeVelocity(z))
    }

    /**
     * 为实体添加速度
     *
     * 为实体添加速度向量，支持击退抗性检查和速度限制。
     * 自动处理击退抗性属性，确保速度在安全范围内。
     *
     * @param entity 目标实体
     * @param vector 要添加的速度向量
     * @param checkKnockback 是否检查击退抗性
     * 
     * @example
     * ```kotlin
     * // 基本用法
     * Vectors.addVelocity(player, Vector(1.0, 0.5, 1.0), true)
     * 
     * // 在技能系统中使用
     * fun executeKnockbackSkill(caster: Player, target: LivingEntity) {
     *     val direction = target.location.subtract(caster.location).toVector().normalize()
     *     val knockback = direction.multiply(2.0)
     *     Vectors.addVelocity(target, knockback, true)
     * }
     * 
     * // 在爆炸效果中使用
     * fun createExplosion(center: Location, radius: Double) {
     *     center.world.getNearbyEntities(center, radius, radius, radius).forEach { entity ->
     *         if (entity is LivingEntity) {
     *             val direction = entity.location.subtract(center).toVector().normalize()
     *             val force = direction.multiply(radius * 0.5)
     *             Vectors.addVelocity(entity, force, true)
     *         }
     *     }
     * }
     * 
     * // 在弹射器中使用
     * fun launchPlayer(player: Player, power: Double) {
     *     val launchVector = Vector(0.0, power, 0.0)
     *     Vectors.addVelocity(player, launchVector, false) // 不检查击退抗性
     * }
     * ```
     */
    fun addVelocity(entity: Entity, vector: Vector, checkKnockback: Boolean) {
        if (checkKnockback && entity is LivingEntity) {
            val instance = entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)
            if (instance != null) {
                val value = instance.value
                if (value >= 1) {
                    return
                }
                if (value > 0) {
                    vector.multiply(1 - value)
                }
            }
        }
        var newVelocity = vector
        if (isUnsafeVelocity(vector)) {
            newVelocity = convertToSafeVelocity(vector)
        }
        try {
            entity.velocity = newVelocity
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }
}