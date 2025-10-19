/*
 * This file is part of Vulpecula, licensed under the MIT License.
 *
 *  Copyright (c) 2018 Bkm016
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package cc.polarastrum.aiyatsbus.module.script.kether.action.game.item

import cc.polarastrum.aiyatsbus.module.script.kether.util.playerOrNull
import cc.polarastrum.aiyatsbus.module.script.kether.util.toBukkit
import taboolib.platform.util.giveItem

/**
 * Vulpecula
 * top.lanscarlos.vulpecula.bacikal.action.item
 *
 * @author Lanscarlos
 * @since 2023-03-27 23:11
 */
object ActionItemGive : ActionItem.Resolver {

    override val name: Array<String> = arrayOf("give")

    override fun resolve(reader: ActionItem.Reader): ActionItem.Handler<out Any?> {
        return reader.transfer {
            combine(
                source(),
                optional("to", then = player()),
                optional("with", "repeat", then = int(), def = 1)
            ) { item, player, repeat ->
                val target = player ?: this.playerOrNull()?.toBukkit() ?: error("No player selected.")
                target.giveItem(item, repeat)
                item
            }
        }
    }
}