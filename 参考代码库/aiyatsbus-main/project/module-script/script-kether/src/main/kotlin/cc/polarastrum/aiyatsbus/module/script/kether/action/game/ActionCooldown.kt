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

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import cc.polarastrum.aiyatsbus.core.asLang
import cc.polarastrum.aiyatsbus.core.util.addCd
import cc.polarastrum.aiyatsbus.core.util.checkCd
import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.module.script.kether.AiyatsbusParser
import cc.polarastrum.aiyatsbus.module.script.kether.aiyatsbus
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.chat.component
import taboolib.module.nms.sendRawActionBar

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.action.ActionCooldown
 *
 * @author mical
 * @since 2024/3/11 22:55
 */
object ActionCooldown {

    /**
     * a-cd check &enchant &player &sec true/false true/false
     * 第一个布尔值(必写): 是否通报给玩家
     * 第二个布尔值(可选): 是否以动作栏形式发送给玩家
     * a-cd add &enchant &player
     */
    @AiyatsbusParser(["a-cd"])
    fun aCdCheck() = aiyatsbus {
        when (nextToken()) {
            "check" -> {
                combine(any(), player(), double(), bool(true), optional("action-bar", then = bool())) { ench, player, sec, send, actionBar ->
                    return@combine player.checkCd((ench as AiyatsbusEnchantment).basicData.id, sec).let {
                        if (!it.first && send) {
                            val message = player.asLang("messages-misc-cool_down", it.second to "second")
                                .component().buildColored()
                            if (actionBar.coerceBoolean(AiyatsbusSettings.coolDownInActionBar)) {
                                player.sendRawActionBar(message.toRawMessage())
                            } else {
                                message.sendTo(adaptPlayer(player))
                            }
                        }
                        it.first
                    }
                }
            }
            "add" -> {
                combine(any(), player()) { ench, player ->
                    player.addCd((ench as AiyatsbusEnchantment).basicData.id)
                }
            }
            else -> error("unknown a-cd operation")
        }
    }
}