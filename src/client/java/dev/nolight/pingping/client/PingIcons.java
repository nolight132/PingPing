package dev.nolight.pingping.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The little preview above a marker. Mobs get a live portrait rather than a spawn egg: vanilla only ships seven
 * head items, so anything else would need third-party art and still miss modded mobs.
 */
public final class PingIcons {
	/** Upright, facing the viewer, as vanilla poses the inventory preview. */
	private static final Quaternionf UPRIGHT = new Quaternionf().rotateZ((float) Math.PI);

	private static final Quaternionf TILT = new Quaternionf().rotateX(-0.12f);

	private PingIcons() {
	}

	public static ItemStack itemFor(Entity entity) {
		return entity instanceof ItemEntity item ? item.getItem() : ItemStack.EMPTY;
	}

	public static ItemStack itemForBlock(BlockGetter level, Vec3 pos) {
		BlockState state = level.getBlockState(BlockPos.containing(pos));
		return state.isAir() ? ItemStack.EMPTY : new ItemStack(state.getBlock().asItem());
	}

	/**
	 * Draws {@code entity} framed on its head inside the given box.
	 *
	 * @param size edge length of the square the portrait is clipped to
	 */
	public static void portrait(GuiGraphicsExtractor graphics, EntityRenderState state, int centreX, int centreY,
			int size) {
		float head = Math.max(state.eyeHeight, 0.25f);
		// Fit roughly the top third of the entity, so tall mobs still read as a face and not a full body.
		float scale = size / (head * 0.9f);

		graphics.entity(
				state,
				scale,
				new Vector3f(0.0f, -head, 0.0f),
				UPRIGHT,
				TILT,
				centreX - size / 2, centreY - size / 2,
				centreX + size / 2, centreY + size / 2);
	}
}
