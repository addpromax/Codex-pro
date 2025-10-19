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
package cc.polarastrum.aiyatsbus.module.ingame.command.subcommand

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.registry.Rarity
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggestPlayers
import taboolib.common.util.random
import taboolib.module.kether.isInt
import taboolib.platform.util.giveItem

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.command.subcommand.Random
 *
 * @author mical
 * @since 2024/3/3 19:04
 */
val randomSubCommand = subCommand {
    dynamic("rarity") {
        suggestion<CommandSender> { _, _ -> Rarity.map { it.key } + Rarity.map { it.value.name } }
        execute<CommandSender> { sender, args, _ -> handleRandom(sender, null, aiyatsbusRarity(args["rarity"])!!) }
        dynamic("level", true) {
            suggestionUncheck<CommandSender> { _, _ -> listOf("等级", "level", "random", "随机") }
            execute<CommandSender> { sender, args, _ -> handleRandom(sender, null, aiyatsbusRarity(args["rarity"])!!, args["level"]) }
            dynamic("player", true) {
                suggestPlayers()
                execute<CommandSender> { sender, args, _ -> handleRandom(sender, args["player"], aiyatsbusRarity(args["rarity"])!!, args["level"]) }
            }
        }
    }
}

private fun handleRandom(sender: CommandSender, who: String?, rarity: Rarity, level: String? = "100") {
    val enchant = rarity.drawEt() ?: run {
        sender.sendLang("command-subCommands-random-rarity")
        return
    }
    val lv = when {
        level == "random" || level == "随机" -> random(1, enchant.basicData.maxLevel)
        level?.isInt() == true -> level.toInt().coerceAtLeast(1).coerceAtMost(enchant.basicData.maxLevel)
        else -> {
            sender.sendLang("command-subCommands-random-number")
            return
        }
    }
    (who?.let { Bukkit.getPlayer(it) } ?: (sender as? Player))?.let { receiver ->
        receiver.giveItem(enchant.book(lv))
        sender.sendLang("command-subCommands-random-sender", receiver.name to "name", enchant.displayName(lv) to "enchantment")
        receiver.sendLang("command-subCommands-random-receiver", enchant.displayName(lv) to "enchantment")
    } ?: sender.sendLang("command-subCommands-random-fail")
}