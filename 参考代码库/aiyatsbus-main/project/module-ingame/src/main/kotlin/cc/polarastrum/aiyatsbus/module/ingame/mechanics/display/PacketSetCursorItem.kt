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
package cc.polarastrum.aiyatsbus.module.ingame.mechanics.display

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.toRevertMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.NMSItemTag
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.PacketSendEvent

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.listener.packet.PacketSetCursorItem
 *
 * @author xiaozhangup
 * @since 2025/7/2 23:47
 */
object PacketSetCursorItem {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun e(e: PacketSendEvent) {
        // Spigot 与 Paper 同名
        if (e.packet.name == "ClientboundSetCursorItemPacket") {
            val origin = e.packet.read<Any>("contents")!!
            val bkItem = NMSItemTag.asBukkitCopy(origin)
            if (versionId >= 12105) {
                var nmsCursorItem: Any? = Aiyatsbus.api().getMinecraftAPI().getCursorItem(e.player)
                if (nmsCursorItem == null) {
                    e.packet.write("contents", nmsCursorItem)
                    return
                }
                nmsCursorItem = NMSItemTag.asBukkitCopy(nmsCursorItem)
                nmsCursorItem = nmsCursorItem.toDisplayMode(e.player)
                nmsCursorItem = NMSItemTag.asNMSCopy(nmsCursorItem)
                e.packet.write("contents", nmsCursorItem)
            } else {
                if (bkItem.isNull) return
                val adapted = NMSItemTag.asNMSCopy(bkItem.toDisplayMode(e.player))
                e.packet.write("contents", adapted)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun e(e: PacketReceiveEvent) {
        if (versionId >= 12105) return
        val name = e.packet.name
        if (name == "PacketPlayInWindowClick" || name == "ServerboundContainerClickPacket") {
            val origin = e.packet.read<Any>("carriedItem")!!
            val bkItem = NMSItemTag.asBukkitCopy(origin)
            if (bkItem.isNull) return
            val adapted = NMSItemTag.asNMSCopy(bkItem.toRevertMode(e.player))
            e.packet.write("carriedItem", adapted)
        }

//        if (versionId >= 12005) {
//            val rawSlot = e.packet.read<Short>("slotNum")!!.toInt()
//            val serverPlayer = e.player.invokeMethod<Any>("getHandle")!!
//            val containerMenu = serverPlayer.getProperty<Any>("containerMenu")!!
//            val bukkitView = containerMenu.invokeMethod<Any>("getBukkitView") as InventoryView
//            val cursorItem = bukkitView.getItem(rawSlot)
//
//            val registryAccess = (Bukkit.getWorlds().first() as CraftWorld).handle.registryAccess()
//            val registryOps: RegistryOps<HashCode> =
//                registryAccess.createSerializationContext(HashOps.CRC32C_INSTANCE)
//
//            val hashOpsGenerator: HashedPatchMap.HashGenerator = object : HashedPatchMap.HashGenerator {
//
//                override fun apply(t: TypedDataComponent<*>): Int {
//                    return t.encodeValue(registryOps).getOrThrow { string ->
//                        IllegalArgumentException("")
//                    }.asInt()
//                }
//            }
//
//            // 拿起物品
//            if (cursorItem.isNull || cursorItem.fixedEnchants.isEmpty()) {
//                return
//            }
//
//            // 获取到的物品即为原始物品, 意味着不需要再进行 toRevertMode
//            val hashedStack =
//                HashedStack.create(NMSItemTag.asNMSCopy(cursorItem!!) as ItemStack, hashOpsGenerator)
//            e.packet.write("carriedItem", hashedStack)
//            return
//        }
    }
}