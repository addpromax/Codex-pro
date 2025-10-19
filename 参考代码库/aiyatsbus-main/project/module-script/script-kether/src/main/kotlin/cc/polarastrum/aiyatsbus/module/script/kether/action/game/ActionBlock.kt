package cc.polarastrum.aiyatsbus.module.script.kether.action.game

import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import taboolib.module.kether.KetherParser
import taboolib.module.kether.combinationParser

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.action.game.ActionBlock
 *
 * @author mical
 * @since 2025/1/11 23:22
 */
object ActionBlock {

    @KetherParser(["get-block-drops"], shared = true)
    fun dropsParser() = combinationParser {
        it.group(
            type<Block?>(),
            command("item", then = type<ItemStack?>()).option(),
            command("entity", then = type<Entity?>()).option()
        ).apply(it) { block, item, entity ->
            now {
                block?.getDrops(item, entity) ?: emptyList()
            }
        }
    }
}