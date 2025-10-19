package net.Indyuce.mmoitems.api.item.util.identify;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class IdentifiedItem {
    private final NBTItem item;

    public IdentifiedItem(NBTItem item) {
        this.item = item;
    }

    /**
     * The identified item is stored in an item NBTTag, identifying the
     * item basically replaces the item for the one saved in the NBT
     */
    @NotNull
    public ItemStack identify() {
        return deserialize(item.getString("MMOITEMS_UNIDENTIFIED_ITEM"));
    }

    @NotNull
    private ItemStack deserialize(String stringInput) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(stringInput));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack stack = (ItemStack) dataInput.readObject();
            dataInput.close();

            /*
             * For some reason, unidentified items keep having slightly different NBT tags
             * than items generated from mob drops or the GUI, I suppose it has to do with
             * the serialization-deserialization, It seems to get fixed when rebuilding
             * the item stack though.
             *
             * Its annoying because it prevents stacking.
             */
            NBTItem toRebuild = NBTItem.get(stack);
            if (toRebuild.hasType()) {

                // Rebuild
                LiveMMOItem rebuilt = new LiveMMOItem(stack);
                return rebuilt.newBuilder().build();
            }

            return stack;
        } catch (ClassNotFoundException | IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
