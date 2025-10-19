package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractVanillaLootManager implements VanillaLootManager {
    protected final Map<Integer, VanillaLoot> blockLoots = new HashMap<>();
    // TODO More entity NBT
    protected final Map<Key, VanillaLoot> entityLoots = new HashMap<>();

    public AbstractVanillaLootManager() {
    }

    @Override
    public void unload() {
        this.blockLoots.clear();
        this.entityLoots.clear();
    }

    @Override
    public Optional<VanillaLoot> getBlockLoot(int vanillaBlockState) {
        return Optional.ofNullable(this.blockLoots.get(vanillaBlockState));
    }

    @Override
    public Optional<VanillaLoot> getEntityLoot(Key entity) {
        return Optional.ofNullable(this.entityLoots.get(entity));
    }
}
