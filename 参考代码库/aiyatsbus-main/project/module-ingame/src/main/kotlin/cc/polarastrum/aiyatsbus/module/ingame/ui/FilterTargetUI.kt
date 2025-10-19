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

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.FilterStatement
import cc.polarastrum.aiyatsbus.core.FilterType
import cc.polarastrum.aiyatsbus.core.data.registry.Target
import cc.polarastrum.aiyatsbus.core.sendLang
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.MenuComponent
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.config.MenuConfiguration
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.feature.util.MenuFunctionBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import cc.polarastrum.aiyatsbus.core.util.variables
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.load
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.pages
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.skull
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

@MenuComponent("FilterTarget")
object FilterTargetUI {

    @Config("core/ui/filter_target.yml", autoReload = true)
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
        player.record(UIType.FILTER_TARGET)
        player.openMenu<PageableChest<Target>>(config.title().component().buildColored().toLegacyText()) {
//            virtualize()

            val (shape, templates) = config
            rows(shape.rows)
            val slots = shape["FilterTarget:filter"].toList()
            slots(slots)
            elements { Target.values.toList() }

            load(shape, templates, player, "FilterTarget:filter", "Previous", "Next")
            pages(shape, templates)

            val template = templates.require("FilterTarget:filter")
            onGenerate(async = true) { _, element, index, slot ->
                template(slot, index) {
                    this["target"] = element
                    this["player"] = player
                }
            }
            onClick { event, element -> templates[event.rawSlot]?.handle(this, event, "target" to element) }
        }
    }

    @MenuComponent
    private val filter = MenuFunctionBuilder {
        onBuild { (_, _, _, _, icon, args) ->
            val target = args["target"] as Target
            val player = args["player"] as Player

            when (Aiyatsbus.api().getEnchantmentFilter().getStatement(player, FilterType.TARGET, target.id)) {
                FilterStatement.ON -> icon.type = Material.LIME_STAINED_GLASS_PANE
                FilterStatement.OFF -> icon.type = Material.RED_STAINED_GLASS_PANE
                else -> {}
            }

            icon.variables {
                when (it) {
                    "name" -> listOf(target.name)
                    "amount" -> listOf((Aiyatsbus.api().getEnchantmentManager().getEnchants().values.count { it.targets.contains(target) }).toString())
                    else -> emptyList()
                }
            }.skull(target.skull)
        }

        onClick { (_, _, _, event, args) ->
            val clickType = event.clickEvent().click
            val player = event.clicker
            val target = args["target"] as Target

            when (clickType) {
                ClickType.LEFT, ClickType.RIGHT -> {
                    Aiyatsbus.api().getEnchantmentFilter().clearFilter(player, FilterType.TARGET, target.id)
                    Aiyatsbus.api().getEnchantmentFilter().addFilter(
                        player, FilterType.TARGET, target.id,
                        when (clickType) {
                            ClickType.RIGHT -> FilterStatement.OFF
                            else -> FilterStatement.ON
                        }
                    )
                    open(player)
                }

                ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT -> {
                    Aiyatsbus.api().getEnchantmentFilter().clearFilter(player, FilterType.TARGET, target.id)
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
            Aiyatsbus.api().getEnchantmentFilter().clearFilter(player, FilterType.TARGET)
            open(player)
        }
    }
}