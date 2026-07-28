package dev.nolight.pingping.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Vanilla folds view bob into the projection matrix rather than the level pose, so reproducing a marker's exact
 * screen position means running the very same bob maths instead of re-deriving it.
 */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
	@Invoker("bobHurt")
	void pingping$bobHurt(CameraRenderState camera, PoseStack poseStack);

	@Invoker("bobView")
	void pingping$bobView(CameraRenderState camera, PoseStack poseStack);
}
