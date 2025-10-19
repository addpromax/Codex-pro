package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public interface Furniture {
    void initializeColliders();

    WorldPosition position();

    boolean isValid();

    void destroy();

    void destroySeats();

    Optional<Seat> findFirstAvailableSeat(int targetEntityId);

    boolean removeOccupiedSeat(Vector3f seat);

    default boolean removeOccupiedSeat(Seat seat) {
        return this.removeOccupiedSeat(seat.offset());
    }

    boolean tryOccupySeat(Seat seat);

    UUID uuid();

    int baseEntityId();

    @NotNull AnchorType anchorType();

    @NotNull Key id();

    @NotNull CustomFurniture config();

    boolean hasExternalModel();

    void spawnSeatEntityForPlayer(Player player, Seat seat);

    FurnitureExtraData extraData();

    void setExtraData(FurnitureExtraData extraData);

    void save();
}
