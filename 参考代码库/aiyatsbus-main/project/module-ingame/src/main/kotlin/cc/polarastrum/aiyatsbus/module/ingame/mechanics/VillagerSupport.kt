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
package cc.polarastrum.aiyatsbus.module.ingame.mechanics

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.CheckType
import cc.polarastrum.aiyatsbus.core.data.registry.Group
import org.bukkit.event.entity.VillagerAcquireTradeEvent
import org.bukkit.inventory.MerchantRecipe
import taboolib.common.LifeCycle
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.util.random
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.nms.MinecraftVersion

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.listener.mechanism.VillagerListener
 *
 * @author mical
 * @since 2024/2/22 23:35
 */
@ConfigNode(bind = "core/mechanisms/villager.yml")
object VillagerSupport {

    @Config("core/mechanisms/villager.yml", autoReload = true)
    lateinit var conf: Configuration
        private set

    @ConfigNode("enable")
    var enableEnchantTrade = true

    @ConfigNode("group")
    var tradeGroup = "可交易附魔"

    @ConfigNode("amount")
    var amount = 2

    @ConfigNode("max_level_limit")
    var maxLevelLimit = -1

    init {
        registerLifeCycleTask(LifeCycle.ENABLE) {
            conf.onReload {
                console().sendLang("configuration-reload", conf.file!!.name, 0)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun acquireTrade(e: VillagerAcquireTradeEvent) {
        val origin = e.recipe
        val result = origin.result.clone()

        if (result.fixedEnchants.isEmpty()) return
        if (!enableEnchantTrade) {
            e.isCancelled = true
            return
        }

        result.clearEts()
        repeat(amount) {
            val drawEt = (Group[tradeGroup]?.enchantments ?: listOf()).filter {
                it.limitations.checkAvailable(CheckType.MERCHANT, result).isSuccess && it.alternativeData.isTradeable && !it.inaccessible
            }.drawEt() ?: return@repeat
            val level = random(1, drawEt.alternativeData.getTradeLevelLimit(drawEt.basicData.maxLevel, maxLevelLimit))
            result.addEt(drawEt, level)
        }
        if (result.fixedEnchants.isEmpty())
            e.isCancelled = true

        origin.run origin@{
            e.recipe = (if (MinecraftVersion.versionId >= 11801) {
                MerchantRecipe(
                    result,
                    uses,
                    maxUses,
                    hasExperienceReward(),
                    villagerExperience,
                    priceMultiplier,
                    demand,
                    specialPrice,
                    shouldIgnoreDiscounts()
                )
            } else MerchantRecipe(
                result,
                uses,
                maxUses,
                hasExperienceReward(),
                villagerExperience,
                priceMultiplier,
                shouldIgnoreDiscounts()
            )).also { it.ingredients = this@origin.ingredients }
        }
    }
}