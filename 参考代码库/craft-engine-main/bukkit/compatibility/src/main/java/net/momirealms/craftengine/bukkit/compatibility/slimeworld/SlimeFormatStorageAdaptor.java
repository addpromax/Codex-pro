package net.momirealms.craftengine.bukkit.compatibility.slimeworld;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent;
import com.infernalsuite.asp.api.world.SlimeWorld;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldManager;
import net.momirealms.craftengine.core.world.chunk.storage.CachedStorage;
import net.momirealms.craftengine.core.world.chunk.storage.DefaultStorageAdaptor;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class SlimeFormatStorageAdaptor extends DefaultStorageAdaptor implements Listener {
    private final WorldManager worldManager;
    private final Class<?> byteArrayTagClass = ReflectionUtils.getClazz("net{}kyori{}adventure{}nbt{}ByteArrayBinaryTag".replace("{}", "."));
    private final Method method$ByteArrayBinaryTag$byteArrayBinaryTag = ReflectionUtils.getStaticMethod(byteArrayTagClass, byteArrayTagClass, byte.class.arrayType());
    private final Method method$ByteArrayBinaryTag$value = ReflectionUtils.getMethod(byteArrayTagClass, byte.class.arrayType());

    @EventHandler
    public void onWorldLoad(LoadSlimeWorldEvent event) {
        org.bukkit.World world = Bukkit.getWorld(event.getSlimeWorld().getName());
        this.worldManager.loadWorld(this.worldManager.createWorld(this.worldManager.wrap(world),
                Config.enableChunkCache() ? new CachedStorage<>(new SlimeWorldDataStorage(event.getSlimeWorld(), this)) : new SlimeWorldDataStorage(event.getSlimeWorld(), this)));
    }

    public SlimeFormatStorageAdaptor(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    public SlimeWorld getWorld(String name) {
        return AdvancedSlimePaperAPI.instance().getLoadedWorld(name);
    }

    // 请注意，在加载事件的时候，无法通过AdvancedSlimePaperAPI.instance().getLoadedWorld来判断是否为slime世界
    @Override
    public @NotNull WorldDataStorage adapt(@NotNull World world) {
        SlimeWorld slimeWorld = getWorld(world.name());
        if (slimeWorld == null) {
            return super.adapt(world);
        }
        return new SlimeWorldDataStorage(slimeWorld, this);
    }

    public byte[] byteArrayTagToBytes(Object byteArrayTag) {
        try {
            return (byte[]) method$ByteArrayBinaryTag$value.invoke(byteArrayTag);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to convert byte array tag to byte[]", e);
        }
    }

    public Object bytesToByteArrayTag(byte[] bytes) {
        try {
            return method$ByteArrayBinaryTag$byteArrayBinaryTag.invoke(null, (Object) bytes);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to convert byte array tag to byte[]", e);
        }
    }
}
