package dev.nolight.pingping.client;

import dev.nolight.pingping.PingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PingIcons {
	private static final float HEAD_SPAN = 0.45f;

	private static final float MIN_SPAN = 0.55f;

	private static final float FULL_BODY_MARGIN = 1.15f;

	private PingIcons() {
	}

	public static ItemStack itemFor(Entity entity) {
		return entity instanceof ItemEntity item ? item.getItem() : ItemStack.EMPTY;
	}

	public static ItemStack itemForBlock(BlockGetter level, Vec3 pos) {
		BlockState state = level.getBlockState(BlockPos.containing(pos));
		return state.isAir() ? ItemStack.EMPTY : new ItemStack(state.getBlock().asItem());
	}

	public static void entity(GuiGraphicsExtractor graphics, Entity entity, float partialTick, int x0, int y0, int x1,
			int y1) {
		EntityRenderState state;

		try {
			state = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, partialTick);
		} catch (RuntimeException e) {
			return;
		}

		state.shadowPieces.clear();
		state.outlineColor = EntityRenderState.NO_OUTLINE;
		state.nameTag = null;
		state.scoreText = null;
		state.leashStates = null;
		state.displayFireAnimation = false;

		float modelScale = 1.0f;

		if (state instanceof LivingEntityRenderState living) {
			modelScale = Math.max(living.scale, 1.0e-3f);
			living.bodyRot = 180.0f;
			living.yRot = 0.0f;
			living.xRot = 0.0f;
			living.boundingBoxWidth /= modelScale;
			living.boundingBoxHeight /= modelScale;
			living.scale = 1.0f;
		}

		float height = Math.max(state.boundingBoxHeight, 0.1f);
		boolean fullBody = PingConfig.get().previewFullBody || !(state instanceof LivingEntityRenderState);
		float span;
		float anchor;

		if (fullBody) {
			span = Math.max(state.boundingBoxWidth, height) * FULL_BODY_MARGIN;
			anchor = height / 2.0f;
		} else {
			span = Math.max(height * HEAD_SPAN, Math.min(MIN_SPAN, height));
			anchor = Mth.clamp(Math.min(state.eyeHeight / modelScale, height - span * 0.4f), span * 0.5f, height);
		}

		graphics.entity(state, (x1 - x0) / span, new Vector3f(0.0f, anchor, 0.0f),
				new Quaternionf().rotateZ((float) Math.PI), null, x0, y0, x1, y1);
	}
}
