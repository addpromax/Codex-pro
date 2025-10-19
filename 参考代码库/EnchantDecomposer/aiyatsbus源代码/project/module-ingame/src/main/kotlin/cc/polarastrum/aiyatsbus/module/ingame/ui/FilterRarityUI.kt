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
package cc.polarastrum.aiyatsbus.module.ingame.ui

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.registry.Rarity
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.MenuComponent
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.config.MenuConfiguration
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.feature.util.MenuFunctionBuilder
import cc.polarastrum.aiyatsbus.core.util.variables
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.load
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.pages
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.skull
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import taboolib.module.chat.component
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.UIType
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.record
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import kotlin.collections.set
import kotlin.system.measureTimeMillis

@MenuComponent("FilterRarity")
object FilterRarityUI {

    @Config("core/ui/filter_rarity.yml", autoReload = true)
    private lateinit var source: Configuration
    private lateinit var config: MenuConfiguration

    fun initialize() {
        config = MenuConfiguration(source)
    }

    @Awake(LifeCycle.ENABLE)
    fun init() {
        source.onReload {
            measureTimeMillis { config = MenuConfiguration(source) }
                .let { console().sendLang("configuration-reload", source.file!!.name, it) }
        }
    }

    fun open(player: Player) {
        player.record(UIType.FILTER_RARITY)
        player.openMenu<PageableChest<Rarity>>(config.title().component().buildColored().toLegacyText()) {
//            virtualize()
            val (shape, templates) = config
            rows(shape.rows)
            val slots = shape["FilterRarity:filter"].toList()
            slots(slots)
            elements { Rarity.values.toList() }

            load(shape, templates, player, "FilterRarity:filter", "Previous", "Next")
            pages(shape, templates)

            val template = templates.require("FilterRarity:filter")
            onGenerate(async = true) { _, element, index, slot ->
                template(slot, index) {
                    this["rarity"] = element
                    this["player"] = player
                }
            }
            onClick { event, element -> templates[event.rawSlot]?.handle(this, event, "rarity" to element) }
        }
    }

    @MenuComponent
    private val filter = MenuFunctionBuilder {
        onBuild { (_, _, _, _, icon, args) ->
            val rarity = args["rarity"] as Rarity
            val player = args["player"] as Player

            when (Aiyatsbus.api().getEnchantmentFilter().getStatement(player, FilterType.RARITY, rarity.id)) {
                FilterStatement.ON -> icon.type = Material.LIME_STAINED_GLASS_PANE
                FilterStatement.OFF -> icon.type = Material.RED_STAINED_GLASS_PANE
                else -> {}
            }

            icon.variables {
                when (it) {
                    "name", "rarity_display" -> listOf(rarity.displayName())
                    "amount" -> listOf(aiyatsbusEts(rarity).size.toString())
                    else -> emptyList()
                }
            }.skull(rarity.skull)
        }

        onClick { (_, _, _, event, args) ->
            val clickType = event.clickEvent().click
            val player = event.clicker
            val rarity = args["rarity"] as Rarity

            when (clickType) {
                ClickType.LEFT, ClickType.RIGHT -> {
                    Aiyatsbus.api().getEnchantmentFilter().clearFilter(player, FilterType.RARITY, rarity.id)
                    Aiyatsbus.api().getEnchantmentFilter().addFilter(
                        player, FilterType.RARITY, rarity.id,
                        when (clickType) {
                            ClickType.RIGHT -> FilterStatement.OFF
                            else -> FilterStatement.ON
                        }
                    )
                    open(player)
                }

                ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT -> {
                    Aiyatsbus.api().getEnchantmentFilter().clearFilter(player, FilterType.RARITY, rarity.id)
                    open(player)
                }

                else -> {}
            }
        }
    }

    @MenuComponent
    private val reset = MenuFunctionBuilder {
        onClick { (_, _, _, event, _) ->
            val player = event.clicker
            Aiyatsbus.api().getEnchantmentFilter().clearFilter(player, FilterType.RARITY)
            open(player)
        }
    }
}