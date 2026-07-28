package dev.nolight.pingping.client;

import dev.nolight.pingping.PingPing;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
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
 * the model's head part, so variants, overlays and modded mobs all come out right.
 *
 * <p>The head is found by walking the model's part tree for a part named {@code head} rather than by asking for
 * {@code HeadedModel}: only five vanilla models implement that interface, which left almost every mob without a
 * preview. Private fields are reached by reflection on purpose — a decoration must never take the game down, and
 * a mixin accessor that fails to bind is a hard crash at class-load time.
 */
public final class PingIcons {
	/** A polygon counts as the face when its normal points at the viewer. */
	private static final float FRONT_NORMAL_Z = -0.9f;

	private static final String HEAD_PART = "head";

	/** Depth limit while hunting for the head, so a pathological model cannot spin the search. */
	private static final int MAX_SEARCH_DEPTH = 8;

	private static final Field MODEL_FIELD = field(LivingEntityRenderer.class, "model");
	private static final Field ROOT_FIELD = field(Model.class, "root");
	private static final Field CUBES_FIELD = field(ModelPart.class, "cubes");
	private static final Field CHILDREN_FIELD = field(ModelPart.class, "children");

	/** UVs never move once a model is built, so each renderer is resolved once. */
	private static final Map<Object, List<Rect>> FACES = new IdentityHashMap<>();

	private record Rect(float u0, float u1, float v0, float v1) {
	}

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
		if (MODEL_FIELD == null || ROOT_FIELD == null || CUBES_FIELD == null) {
			return false;
		}

		EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

		if (!(renderer instanceof LivingEntityRenderer) || !(state instanceof LivingEntityRenderState living)) {
			return false;
		}

		List<Rect> faces = FACES.computeIfAbsent(renderer, PingIcons::resolveFaces);

		if (faces.isEmpty()) {
			return false;
		}

		Identifier texture;

		try {
			texture = ((LivingEntityRenderer) renderer).getTextureLocation(living);
		} catch (RuntimeException e) {
			return false;
		}

		if (texture == null) {
			return false;
		}

		// In declaration order, so a hat or fur overlay lands on top of the base face just as it does in world.
		for (Rect rect : faces) {
			graphics.blit(texture, x, y, size, size, rect.u0(), rect.u1(), rect.v0(), rect.v1());
		}

		return true;
	}

	private static List<Rect> resolveFaces(Object renderer) {
		List<Rect> faces = new ArrayList<>();

		try {
			Object model = MODEL_FIELD.get(renderer);

			if (!(model instanceof Model)) {
				return faces;
			}

			if (!(ROOT_FIELD.get(model) instanceof ModelPart root)) {
				return faces;
			}

			ModelPart head = findHead(root, 0);
			collectFrontFaces(head == null ? root : head, faces, head == null);
		} catch (ReflectiveOperationException | RuntimeException e) {
			PingPing.LOGGER.debug("[pingping] could not resolve a face for {}", renderer.getClass().getName(), e);
		}

		return faces;
	}

	/** Breadth of the tree matters more than depth: the head is normally a direct child of the root. */
	private static ModelPart findHead(ModelPart part, int depth) throws ReflectiveOperationException {
		if (CHILDREN_FIELD == null || depth > MAX_SEARCH_DEPTH) {
			return null;
		}

		if (!(CHILDREN_FIELD.get(part) instanceof Map<?, ?> children)) {
			return null;
		}

		for (Map.Entry<?, ?> entry : children.entrySet()) {
			if (HEAD_PART.equals(entry.getKey()) && entry.getValue() instanceof ModelPart found) {
				return found;
			}
		}

		for (Object child : children.values()) {
			if (child instanceof ModelPart part1) {
				ModelPart found = findHead(part1, depth + 1);

				if (found != null) {
					return found;
				}
			}
		}

		return null;
	}

	/**
	 * Collects the front faces of a part's own cubes. Headless mobs — slimes, ghasts, shulkers — have their
	 * geometry on the body instead, so the search may descend to find something to show.
	 */
	private static void collectFrontFaces(ModelPart part, List<Rect> into, boolean descend)
			throws ReflectiveOperationException {
		if (!(CUBES_FIELD.get(part) instanceof List<?> cubes)) {
			return;
		}

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

				if (u1 > u0 && v1 > v0) {
					into.add(new Rect(u0, u1, v0, v1));
				}
			}
		}

		if (!into.isEmpty() || !descend || CHILDREN_FIELD == null) {
			return;
		}

		if (CHILDREN_FIELD.get(part) instanceof Map<?, ?> children) {
			for (Object child : children.values()) {
				if (child instanceof ModelPart part1) {
					collectFrontFaces(part1, into, true);

					if (!into.isEmpty()) {
						return;
					}
				}
			}
		}
	}
}
