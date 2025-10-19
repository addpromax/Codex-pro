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
import cc.polarastrum.aiyatsbus.core.AiyatsbusPlayerDataHandler
import cc.polarastrum.aiyatsbus.core.StandardPriorities
import cc.polarastrum.aiyatsbus.core.data.PlayerData
import cc.polarastrum.aiyatsbus.core.util.get
import cc.polarastrum.aiyatsbus.core.util.reloadable
import cc.polarastrum.aiyatsbus.core.util.set
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.platform.util.onlinePlayers
import java.util.*

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.DefaultAiyatsbusPlayerDataHandler
 *
 * @author mical
 * @since 2024/2/18 13:02
 */
class DefaultAiyatsbusPlayerDataHandler : AiyatsbusPlayerDataHandler {

    private val data = mutableMapOf<UUID, PlayerData>()

    override fun load(player: Player) {
        data[player.uniqueId] = PlayerData(player["aiyatsbus_data", PersistentDataType.STRING])
    }

    override fun save(player: Player) {
        data[player.uniqueId]?.let { player["aiyatsbus_data", PersistentDataType.STRING] = it.serialize() }
    }

    override fun get(player: Player): PlayerData {
        return data[player.uniqueId]!!
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusPlayerDataHandler>(DefaultAiyatsbusPlayerDataHandler())
            reloadable {
                registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.PLAYER_DATA) {
                    onlinePlayers.forEach(PlatformFactory.getAPI<AiyatsbusPlayerDataHandler>()::load)
                }
            }
        }

        @Schedule(period = 600L)
        fun tick() {
            onlinePlayers.forEach { Aiyatsbus.api().getPlayerDataHandler().save(it) }
        }

        @SubscribeEvent(priority = EventPriority.MONITOR)
        fun e(e: PlayerJoinEvent) {
            Aiyatsbus.api().getPlayerDataHandler().load(e.player)
        }

        @SubscribeEvent(priority = EventPriority.MONITOR)
        fun e(e: PlayerQuitEvent) {
            Aiyatsbus.api().getPlayerDataHandler().save(e.player)
        }
    }
}