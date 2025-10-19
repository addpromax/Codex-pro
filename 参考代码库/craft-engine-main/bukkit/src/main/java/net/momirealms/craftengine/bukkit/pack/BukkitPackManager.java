package net.momirealms.craftengine.bukkit.pack;

import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackGenerateEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.feature.ReloadCommand;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.ResourcePackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.impl.NoneHost;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitPackManager extends AbstractPackManager implements Listener {
    public static final String FAKE_URL = "https://127.0.0.1:65536";
    private final BukkitCraftEngine plugin;

    public BukkitPackManager(BukkitCraftEngine plugin) {
        super(plugin, (rf, zp) -> {
            AsyncResourcePackGenerateEvent endEvent = new AsyncResourcePackGenerateEvent(rf, zp);
            EventUtils.fireAndForget(endEvent);
        });
        this.plugin = plugin;
    }

    @Override
    public void delayedInit() {
        super.delayedInit();
        Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (Config.sendPackOnJoin() && !VersionHelper.isOrAbove1_20_2()) {
            Player player = plugin.adapt(event.getPlayer());
            this.sendResourcePack(player);
        }
    }

    @Override
    public void load() {
        if (ReloadCommand.RELOAD_PACK_FLAG || CraftEngine.instance().isInitializing()) {
            super.load();
            if (Config.sendPackOnJoin() && VersionHelper.isOrAbove1_20_2() && !(resourcePackHost() instanceof NoneHost)) {
                this.modifyServerSettings();
            }
        }
    }

    public void modifyServerSettings() {
        try {
            Object settings = CoreReflections.field$DedicatedServer$settings.get(CoreReflections.method$MinecraftServer$getServer.invoke(null));
            Object properties = CoreReflections.field$DedicatedServerSettings$properties.get(settings);
            Object info;
            if (VersionHelper.isOrAbove1_20_3()) {
                info = CoreReflections.constructor$ServerResourcePackInfo.newInstance(new UUID(0, 0), FAKE_URL, "", Config.kickOnDeclined(), ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt()));
            } else {
                info = CoreReflections.constructor$ServerResourcePackInfo.newInstance(FAKE_URL, "", Config.kickOnDeclined(), ComponentUtils.adventureToMinecraft(Config.resourcePackPrompt()));
            }
            CoreReflections.field$DedicatedServerProperties$serverResourcePackInfo.set(properties, Optional.of(info));
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to update resource pack settings", e);
        }
    }

    @Override
    public void unload() {
        super.unload();
        if (ReloadCommand.RELOAD_PACK_FLAG) {
            if (VersionHelper.isOrAbove1_20_2()) {
                this.resetServerSettings();
            }
        }
    }

    @Override
    public void disable() {
        super.disable();
        HandlerList.unregisterAll(this);
        this.resetServerSettings();
    }

    public void resetServerSettings() {
        try {
            Object settings = CoreReflections.field$DedicatedServer$settings.get(CoreReflections.method$MinecraftServer$getServer.invoke(null));
            Object properties = CoreReflections.field$DedicatedServerSettings$properties.get(settings);
            CoreReflections.field$DedicatedServerProperties$serverResourcePackInfo.set(properties, Optional.empty());
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to reset resource pack settings", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncResourcePackGenerate(AsyncResourcePackGenerateEvent event) {
        if (!Config.autoUpload()) return;
        uploadResourcePack();
    }

    @Override
    public void uploadResourcePack() {
        resourcePackHost().upload(Config.fileToUpload()).whenComplete((d, e) -> {
            if (e != null) {
                CraftEngine.instance().logger().warn("Failed to upload resource pack", e);
                return;
            }
            if (!Config.sendPackOnUpload()) return;
            CraftEngine.instance().logger().info("Complete uploading resource pack");
            for (BukkitServerPlayer player : this.plugin.networkManager().onlineUsers()) {
                sendResourcePack(player);
            }
        });
    }

    @Override
    public void sendResourcePack(Player player) {
        CompletableFuture<List<ResourcePackDownloadData>> future = resourcePackHost().requestResourcePackDownloadLink(player.uuid());
        future.thenAccept(dataList -> {
            if (player.isOnline()) {
                player.unloadCurrentResourcePack();
                if (dataList.isEmpty()) {
                    return;
                }
                if (dataList.size() == 1) {
                    ResourcePackDownloadData data = dataList.get(0);
                    player.sendPacket(ResourcePackUtils.createPacket(data.uuid(), data.url(), data.sha1()), true);
                    player.addResourcePackUUID(data.uuid());
                } else {
                    List<Object> packets = new ArrayList<>();
                    for (ResourcePackDownloadData data : dataList) {
                        packets.add(ResourcePackUtils.createPacket(data.uuid(), data.url(), data.sha1()));
                        player.addResourcePackUUID(data.uuid());
                    }
                    player.sendPackets(packets, true);
                }
            }
        }).exceptionally(throwable -> {
            CraftEngine.instance().logger().warn("Failed to send resource pack to player " + player.name(), throwable);
            return null;
        });
    }
}
