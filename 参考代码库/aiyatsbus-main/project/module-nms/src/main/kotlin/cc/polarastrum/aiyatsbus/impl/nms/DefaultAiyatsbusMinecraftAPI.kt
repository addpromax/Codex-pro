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
@file:Suppress("PrivatePropertyName", "SameParameterValue", "unused")

package cc.polarastrum.aiyatsbus.impl.nms

import cc.polarastrum.aiyatsbus.core.AiyatsbusMinecraftAPI
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import cc.polarastrum.aiyatsbus.impl.nms.v12005_nms.NMS12005
import io.papermc.paper.adventure.AdventureComponent
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.util.Codec
import net.minecraft.core.BlockPosition
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.chat.IChatBaseComponent
import net.minecraft.world.entity.EnumItemSlot
import net.minecraft.world.item.trading.MerchantRecipeList
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.nms.MinecraftVersion.isUniversalCraftBukkit
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.NMSItemTag
import java.io.IOException

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.nms.DefaultAiyatsbusMinecraftAPI
 *
 * @author mical
 * @since 2024/2/18 00:21
 */
class DefaultAiyatsbusMinecraftAPI : AiyatsbusMinecraftAPI {

    init {
        // 预热
        if (versionId >= 12005) {
            NMS12005.instance
        }
    }

    private val NBT_CODEC: Codec<Any, String, IOException, IOException> = PaperAdventure::class.java.getProperty("NBT_CODEC", isStatic = true)!!

    override fun getRepairCost(item: ItemStack): Int {
        return if (versionId >= 12005) {
            NMS12005.instance.getRepairCost(item)
        } else {
            (NMSItemTag.asNMSCopy(item) as NMSItemStack).baseRepairCost
        }
    }

    override fun setRepairCost(item: ItemStack, cost: Int) {
        if (versionId >= 12005) {
            NMS12005.instance.setRepairCost(item, cost)
        } else {
            (NMSItemTag.asNMSCopy(item) as NMSItemStack).setRepairCost(cost)
        }
    }

    override fun createItemStack(material: String, tag: String?): ItemStack {
        return try {
            if (versionId >= 11802) {
                Bukkit.getItemFactory().createItemStack(material + tag)
            } else {
                val mat = material.split(":")[1].uppercase()
                val bkItem = ItemStack(Material.valueOf(mat), 1)
                if (tag.isNullOrEmpty()) {
                    bkItem
                } else {
                    val nmsItem = NMSItemTag.asNMSCopy(bkItem)
                    val nbt = NBT_CODEC.decode(tag) as NBTTagCompound
                    (nmsItem as NMSItemStack).tag = nbt
                    NMSItemTag.asBukkitCopy(nmsItem)
                }
            }
        } catch (t: Throwable) {
            throw IllegalStateException(t)
        }
    }

    override fun adaptMerchantRecipe(merchantRecipeList: Any, player: Player) {

        if (versionId >= 12005) {
            return NMS12005.instance.adaptMerchantRecipe(merchantRecipeList, player)
        }

        fun adapt(item: Any, player: Player): Any {
            val bkItem = NMSItemTag.asBukkitCopy(item)
            if (bkItem.isNull) return item
            return NMSItemTag.asNMSCopy(bkItem.toDisplayMode(player))
        }

        val previous = merchantRecipeList as MerchantRecipeList
        for (i in 0 until previous.size) {
            with(previous[i]!!) {
                setProperty("baseCostA", adapt(baseCostA, player) as NMSItemStack)
                setProperty("costB", adapt(costB, player) as NMSItemStack)
                setProperty("result", adapt(result, player) as NMSItemStack)
            }
        }
    }

    override fun componentFromJson(json: String): Any {
        return CraftChatMessage.fromJSON(json)
    }

    override fun componentToJson(iChatBaseComponent: Any): String {
        // 逆天 paper 1.21.4+
        if (isUniversalCraftBukkit && versionId >= 12104) {
            if (iChatBaseComponent is AdventureComponent) {
                return GsonComponentSerializer.gson().serialize(iChatBaseComponent.`adventure$component`())
            }
        }
        return CraftChatMessage.toJSON(iChatBaseComponent as IChatBaseComponent)
    }

    override fun breakBlock(player: Player, block: Block): Boolean {
        return (player as CraftPlayer).handle.gameMode.destroyBlock(BlockPosition(block.x, block.y, block.z))
    }

    override fun damageItemStack(item: ItemStack, amount: Int, entity: LivingEntity): ItemStack {
        var stack = item
        val nmsStack = if (stack is CraftItemStack) {
            val handle = stack.getProperty<NMSItemStack>("handle")
            if (handle == null || handle.isEmpty) {
                return stack
            }
            handle
        } else {
            CraftItemStack.asNMSCopy(stack).also {
                stack = CraftItemStack.asCraftMirror(it)
            }
        }
        damageItemStack(nmsStack, amount, null, entity)
        return stack
    }

    /*
    /**
     * CraftLivingEntity&damageItemStack(EquipmentSlot, int)
     */
    fun damageItemStack(slot: EquipmentSlot, amount: Int, entity: LivingEntity) {
        if (MinecraftVersion.isUniversal) {
            val nmsSlot = CraftEquipmentSlot20.getNMS(slot)
            damageItemStack((entity as CraftLivingEntity20).handle.getItemBySlot(nmsSlot), amount, nmsSlot, entity)
        } else {
            val nmsSlot = CraftEquipmentSlot16.getNMS(slot)
            damageItemStack((entity as CraftLivingEntity16).handle.getEquipment(nmsSlot), amount, nmsSlot, entity)
        }
    }

     */

    /**
     * CraftLivingEntity#damageItemStack0
     */
    private fun damageItemStack(nmsStack: Any, amount: Int, enumItemSlot: Any?, entity: LivingEntity) {
        nmsStack as NMSItemStack
        // 1.20.4 -> hurtAndBreak(int, EntityLiving, Consumer<EntityLiving>)
        // 1.20.5, 1.21 -> hurtAndBreak(int, EntityLiving, EnumItemSlot), 自动广播事件
        if (versionId >= 12005) {
            NMS12005.instance.hurtAndBreak(nmsStack, amount, entity)
        } else {
            nmsStack.hurtAndBreak(amount, (entity as CraftLivingEntity).handle) { entityLiving ->
                (enumItemSlot as? EnumItemSlot)?.let { entityLiving.broadcastBreakEvent(it) }
            }
        }
    }

    override fun getCursorItem(player: Player): Any? {
        return (player as CraftPlayer).handle.containerMenu.carried
    }
}

typealias NMSItemStack = net.minecraft.world.item.ItemStack