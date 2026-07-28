package dev.nolight.pingping.client;

import java.lang.reflect.Field;
import java.util.List;
import dev.nolight.pingping.PingPing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Flat face sprites cut straight out of each mob's own texture at draw time.
 *
 * <p>No sprite pack is bundled: every downloadable set of mob-head icons found was all-rights-reserved, and
 * nothing needs redistributing anyway. The UV rectangle is not guessed either — it is read off the front face of
 * the model's own head part, so variants, overlays and modded mobs all come out right.
 *
 * <p>The two private fields are reached by reflection rather than mixin accessors on purpose: a decoration must
 * never take the game down with it, and an @Accessor that fails to bind is a hard crash at class-load time.
 */
public final class PingIcons {
	/** A polygon counts as the face when its normal points at the viewer. */
	private static final float FRONT_NORMAL_Z = -0.9f;

	private static final Field MODEL_FIELD = field(LivingEntityRenderer.class, "model");
	private static final Field CUBES_FIELD = field(ModelPart.class, "cubes");

	private static Field field(Class<?> owner, String name) {
		try {
			Field found = owner.getDeclaredField(name);
			found.setAccessible(true);
			return found;
		} catch (ReflectiveOperationException | RuntimeException e) {
			PingPing.LOGGER.warn("[pingping] no {}#{}, face previews disabled", owner.getSimpleName(), name);
			return null;
		}
	}

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
	 * Draws the entity's face into a {@code size} square with its top-left at {@code (x, y)}.
	 *
	 * @return whether anything was drawn
	 */
	public static boolean face(GuiGraphicsExtractor graphics, Entity entity, EntityRenderState state, int x, int y,
			int size) {
		EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

		if (!(renderer instanceof LivingEntityRenderer) || !(state instanceof LivingEntityRenderState living)) {
			return false;
		}

		if (MODEL_FIELD == null || CUBES_FIELD == null) {
			return false;
		}

		Identifier texture;
		ModelPart head;
		List<?> cubes;

		try {
			if (!(MODEL_FIELD.get(renderer) instanceof HeadedModel headed)) {
				return false;
			}

			texture = ((LivingEntityRenderer) renderer).getTextureLocation(living);
			head = headed.getHead();

			if (texture == null || head == null) {
				return false;
			}

			cubes = (List<?>) CUBES_FIELD.get(head);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}

		if (cubes == null) {
			return false;
		}

		boolean drawn = false;

		// Cubes in declaration order, so a hat or fur overlay lands on top of the base face just as it does in world.
		for (Object raw : cubes) {
			if (!(raw instanceof ModelPart.Cube cube)) {
				continue;
			}

			for (ModelPart.Polygon polygon : cube.polygons) {
				if (polygon.normal().z() > FRONT_NORMAL_Z) {
					continue;
				}

				float u0 = Float.MAX_VALUE;
				float u1 = -Float.MAX_VALUE;
				float v0 = Float.MAX_VALUE;
				float v1 = -Float.MAX_VALUE;

				for (ModelPart.Vertex vertex : polygon.vertices()) {
					u0 = Math.min(u0, vertex.u());
					u1 = Math.max(u1, vertex.u());
					v0 = Math.min(v0, vertex.v());
					v1 = Math.max(v1, vertex.v());
				}

				if (u1 <= u0 || v1 <= v0) {
					continue;
				}

				graphics.blit(texture, x, y, size, size, u0, u1, v0, v1);
				drawn = true;
			}
		}

		return drawn;
	}
}
