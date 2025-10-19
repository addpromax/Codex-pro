package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.plugin.config.Config;

import java.util.UUID;

public class ResourcePackUtils {

    public static Object createPacket(UUID uuid, String url, String hash) {
        return FastNMS.INSTANCE.constructor$ClientboundResourcePackPushPacket(uuid, url, hash, Config.kickOnDeclined(), ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt()));
    }
}
