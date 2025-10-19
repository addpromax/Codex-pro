package net.momirealms.craftengine.bukkit.compatibility.slimeworld;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeWorld;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.serialization.DefaultChunkSerializer;
import net.momirealms.craftengine.core.world.chunk.storage.WorldDataStorage;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Map;

public class SlimeWorldDataStorage implements WorldDataStorage {
    private final WeakReference<SlimeWorld> slimeWorld;
    private final SlimeFormatStorageAdaptor adaptor;

    public SlimeWorldDataStorage(SlimeWorld slimeWorld, SlimeFormatStorageAdaptor adaptor) {
        this.slimeWorld = new WeakReference<>(slimeWorld);
        this.adaptor = adaptor;
    }

    public SlimeWorld getWorld() {
        return slimeWorld.get();
    }

    @Override
    public @NotNull CEChunk readChunkAt(@NotNull CEWorld world, @NotNull ChunkPos pos) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return new CEChunk(world, pos);
        Object tag = slimeChunk.getExtraData().get("craftengine");
        if (tag == null) return new CEChunk(world, pos);
        try {
            CompoundTag compoundTag = NBT.fromBytes(adaptor.byteArrayTagToBytes(tag));
            if (compoundTag == null) return new CEChunk(world, pos);
            return DefaultChunkSerializer.deserialize(world, pos, compoundTag);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read chunk tag from slime world. " + pos, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeChunkAt(@NotNull ChunkPos pos, @NotNull CEChunk chunk) {
        SlimeChunk slimeChunk = getWorld().getChunk(pos.x, pos.z);
        if (slimeChunk == null) return;
        CompoundTag nbt = DefaultChunkSerializer.serialize(chunk);
        if (nbt == null) {
            slimeChunk.getExtraData().remove("craftengine");
        } else {
            try {
                Object tag = adaptor.bytesToByteArrayTag(NBT.toBytes(nbt));
                Map<String, ?> data1 = slimeChunk.getExtraData();
                Map<String, Object> data2 = (Map<String, Object>) data1;
                data2.put("craftengine", tag);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write chunk tag to slime world. " + pos, e);
            }
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
