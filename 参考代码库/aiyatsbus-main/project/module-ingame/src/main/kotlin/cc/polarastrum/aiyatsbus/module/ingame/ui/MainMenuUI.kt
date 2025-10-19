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

import cc.polarastrum.aiyatsbus.core.StandardPriorities
import cc.polarastrum.aiyatsbus.core.sendLang
import cc.polarastrum.aiyatsbus.core.util.reloadable
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.*
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.config.MenuConfiguration
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.feature.util.MenuFunctionBuilder
import cc.polarastrum.aiyatsbus.core.util.variable
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.registry.MenuFunctions
import org.bukkit.entity.Player
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.module.chat.component
import kotlin.system.measureTimeMillis

@MenuComponent("Menu")
object MainMenuUI {

    @Config("core/ui/menu.yml", autoReload = true)
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
        player.record(UIType.MAIN_MENU)
        player.openMenu<Chest>(config.title().component().buildColored().toLegacyText()) {
            val (shape, templates) = config
            rows(shape.rows)
            map(*shape.array)

            load(shape, templates, player)
        }
    }

    @MenuComponent
    private val enchant_search = MenuFunctionBuilder {
        onClick { (_, _, _, event, _) ->
            EnchantSearchUI.open(event.clicker)
        }
    }

    @MenuComponent
    private val item_check = MenuFunctionBuilder {
        onClick { (_, _, _, event, _) ->
            ItemCheckUI.open(event.clicker, null, ItemCheckUI.CheckMode.LOAD)
        }
    }

    @MenuComponent
    private val anvil = MenuFunctionBuilder {
        onClick { (_, _, _, event, _) ->
            AnvilUI.open(event.clicker)
        }
    }

    @MenuComponent
    private val favorites = MenuFunctionBuilder {
        onClick { (_, _, _, event, _) ->
            FavoritesUI.open(event.clicker)
        }
    }

    @Awake(LifeCycle.LOAD)
    fun load() {
        reloadable {
            registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.MENU) {
                MenuFunctions.unregister("Back")
                MenuFunctions.register("Back", false) { MenuFunctionBuilder {
                    onBuild { (_, _, _, _, icon, args) -> icon.variable("last", listOf((args["player"] as Player).last())) }
                    onClick { (_, _, _, event, _) -> event.clicker.back() }
                } }
                AnvilUI.initialize()
                EnchantInfoUI.initialize()
                EnchantSearchUI.initialize()
                FavoritesUI.initialize()
                FilterGroupUI.initialize()
                FilterRarityUI.initialize()
                FilterTargetUI.initialize()
                ItemCheckUI.initialize()
                this.initialize()
            }
        }
    }
}