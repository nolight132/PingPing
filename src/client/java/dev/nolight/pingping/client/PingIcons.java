package dev.nolight.pingping.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Flat head sprites, cropped straight out of each mob's own texture at draw time.
 *
 * <p>Every downloadable mob-head pack found was all-rights-reserved, and the game already ships a texture for
 * every entity, so nothing is redistributed here: the crop reads whatever texture the entity's renderer is
 * already using, which also picks up variants and modded mobs for free.
 */
public final class PingIcons {
	/**
	 * Front face of a head box. Minecraft lays a box out as top/bottom on the first row and
	 * right/front/left/back below, so the front face starts at {@code texOffs + depth}.
	 */
	private record Head(int u, int v, int width, int height, int textureWidth, int textureHeight) {
	}

	private static final Head HUMANOID = new Head(8, 8, 8, 8, 64, 64);

	private static final Map<EntityType<?>, Head> HEADS = new HashMap<>();

	static {
		HEADS.put(EntityTypes.CREEPER, new Head(8, 8, 8, 8, 64, 32));
		HEADS.put(EntityTypes.ENDERMAN, new Head(8, 8, 8, 8, 64, 32));
		HEADS.put(EntityTypes.PIG, new Head(8, 8, 8, 8, 64, 32));
		HEADS.put(EntityTypes.COW, new Head(6, 6, 8, 8, 64, 32));
		HEADS.put(EntityTypes.MOOSHROOM, new Head(6, 6, 8, 8, 64, 32));
		HEADS.put(EntityTypes.SHEEP, new Head(8, 8, 6, 6, 64, 32));
		HEADS.put(EntityTypes.CHICKEN, new Head(3, 3, 4, 6, 64, 32));
		HEADS.put(EntityTypes.SPIDER, new Head(40, 12, 8, 8, 64, 32));
		HEADS.put(EntityTypes.CAVE_SPIDER, new Head(40, 12, 8, 8, 64, 32));
		HEADS.put(EntityTypes.WOLF, new Head(6, 6, 6, 6, 64, 32));
		HEADS.put(EntityTypes.CAT, new Head(5, 5, 5, 4, 64, 32));
		HEADS.put(EntityTypes.OCELOT, new Head(5, 5, 5, 4, 64, 32));
		HEADS.put(EntityTypes.VILLAGER, new Head(8, 8, 8, 10, 64, 64));
		HEADS.put(EntityTypes.ZOMBIE_VILLAGER, new Head(8, 8, 8, 10, 64, 64));
		HEADS.put(EntityTypes.WANDERING_TRADER, new Head(8, 8, 8, 10, 64, 64));
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
	 * Draws the entity's face at {@code (x, y)}, top-left, at {@code size} pixels square.
	 *
	 * @return whether anything was drawn
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static boolean face(GuiGraphicsExtractor graphics, Entity entity, EntityRenderState state, int x, int y,
			int size) {
		EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

		if (!(renderer instanceof LivingEntityRenderer) || !(state instanceof LivingEntityRenderState living)) {
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

		Head head = HEADS.getOrDefault(entity.getType(), HUMANOID);

		graphics.blit(texture, x, y, size, size,
				head.u() / (float) head.textureWidth(),
				(head.u() + head.width()) / (float) head.textureWidth(),
				head.v() / (float) head.textureHeight(),
				(head.v() + head.height()) / (float) head.textureHeight());
		return true;
	}
}
