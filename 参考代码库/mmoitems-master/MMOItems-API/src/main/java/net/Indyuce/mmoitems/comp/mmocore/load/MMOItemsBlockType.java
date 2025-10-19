package net.Indyuce.mmoitems.comp.mmocore.load;

import io.lumine.mythic.lib.api.MMOLineConfig;
import net.Indyuce.mmocore.api.block.BlockInfo.RegeneratingBlock;
import net.Indyuce.mmocore.api.block.BlockType;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.block.CustomBlock;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MMOItemsBlockType implements BlockType {
	private final int id;

	public MMOItemsBlockType(MMOLineConfig config) {
		config.validate("id");

		id = config.getInt("id");
	}

	public MMOItemsBlockType(CustomBlock block) {
		id = block.getId();
	}

	public int getBlockId() {
		return id;
	}

	public static boolean matches(Block block) {
		return MMOItems.plugin.getCustomBlocks().isMushroomBlock(block.getType());
	}

    @Override
    public @NotNull String display() {
        return "MMOItems{" + id + "}";
    }

	@Override
	public void place(RegeneratingBlock regeneratingBlock) {
		Location loc = regeneratingBlock.getLocation();
		CustomBlock block = MMOItems.plugin.getCustomBlocks().getBlock(id);

		loc.getBlock().setType(block.getState().getType());
		loc.getBlock().setBlockData(block.getState().getBlockData());
	}

	@Override
	public void regenerate(RegeneratingBlock regeneratingBlock) {
		place(regeneratingBlock);
	}

	@Override
	public boolean breakRestrictions(Block block) {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MMOItemsBlockType that = (MMOItemsBlockType) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
