/*
 * This file is part of ParrotX, licensed under the MIT License.
 *
 *  Copyright (c) 2020 Legoshi
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
package cc.polarastrum.aiyatsbus.module.ingame.ui.internal.feature

import org.bukkit.inventory.ItemStack
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.MenuFeature
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.data.ActionContext
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.data.BuildContext
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.function.value
import cc.polarastrum.aiyatsbus.module.ingame.ui.internal.registry.MenuFunctions

object FunctionalFeature : MenuFeature() {

    override val name: String = "Functional"

    override fun build(context: BuildContext): ItemStack = MenuFunctions[keyword(context.extra)]?.build(context) ?: context.icon

    override fun handle(context: ActionContext) {
        MenuFunctions[keyword(context.extra)]?.handle(context)
    }

    fun keyword(extra: Map<*, *>): String = extra.value("keyword")

}