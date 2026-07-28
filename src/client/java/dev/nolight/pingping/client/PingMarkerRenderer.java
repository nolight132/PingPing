package dev.nolight.pingping.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Draws a billboarded marker above every entity that is currently pinged. */
public final class PingMarkerRenderer {
	private static final Component MARKER = Component.literal("◆").withStyle(ChatFormatting.YELLOW);

	/** Gap between the entity's head and the marker. */
	private static final double HEAD_CLEARANCE = 0.7;

	private PingMarkerRenderer() {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			Map<Integer, Long> active = ClientPings.active();

			if (active.isEmpty()) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
			ClientLevel level = client.level;

			if (level == null) {
				return;
			}

			PoseStack poseStack = context.poseStack();
			SubmitNodeCollector collector = context.submitNodeCollector();
			CameraRenderState camera = context.levelState().cameraRenderState;
			Vec3 cameraPos = camera.pos;
			float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

			for (int entityId : active.keySet()) {
				Entity entity = level.getEntity(entityId);

				if (entity == null) {
					continue;
				}

				Vec3 pos = entity.getPosition(partialTick).subtract(cameraPos);

				poseStack.pushPose();
				poseStack.translate(pos.x, pos.y, pos.z);
				collector.submitNameTag(
						poseStack,
						new Vec3(0.0, entity.getBbHeight() + HEAD_CLEARANCE, 0.0),
						0,
						MARKER,
						true,
						LightCoordsUtil.FULL_BRIGHT,
						camera);
				poseStack.popPose();
			}
		});
	}
}
