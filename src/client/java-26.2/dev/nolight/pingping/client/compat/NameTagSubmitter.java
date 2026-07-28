package dev.nolight.pingping.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * 26.2 dropped the camera-distance argument that {@code submitNameTag} took in 26.1, so the call lives here and
 * has one implementation per game branch.
 */
public final class NameTagSubmitter {
	private NameTagSubmitter() {
	}

	public static void submit(SubmitNodeCollector collector, PoseStack poseStack, Vec3 offset, Component text,
			int lightCoords, double distanceToCameraSq, CameraRenderState camera) {
		collector.submitNameTag(poseStack, offset, 0, text, true, lightCoords, camera);
	}
}
