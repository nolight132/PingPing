package dev.nolight.pingping.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * The HUD pass has no camera of its own, so the level pass stashes what it needs to project world positions
 * into screen space.
 */
public final class CameraCapture {
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static final Matrix4f VIEW = new Matrix4f();

	private static Vec3 position = Vec3.ZERO;
	private static boolean ready;

	private CameraCapture() {
	}

	public static void register() {
		LevelRenderEvents.START_MAIN.register(context -> {
			CameraRenderState camera = context.levelState().cameraRenderState;

			if (camera == null || camera.projectionMatrix == null || camera.pos == null) {
				return;
			}

			position = camera.pos;
			PROJECTION.set(camera.projectionMatrix);
			// Same construction vanilla uses for the level view matrix.
			VIEW.identity()
					.rotateX((float) Math.toRadians(camera.xRot))
					.rotateY((float) Math.toRadians(camera.yRot + 180.0f));
			ready = true;
		});
	}

	public static boolean isReady() {
		return ready;
	}

	/**
	 * Projects a world position to GUI coordinates.
	 *
	 * @return {@code x}, {@code y} in GUI pixels and {@code z} > 0 when the point is in front of the camera,
	 *         or {@code null} if no camera has been captured yet
	 */
	public static Vector4f project(Vec3 world, int guiWidth, int guiHeight) {
		if (!ready) {
			return null;
		}

		Vector4f clip = new Vector4f(
				(float) (world.x - position.x),
				(float) (world.y - position.y),
				(float) (world.z - position.z),
				1.0f);

		VIEW.transform(clip);
		float depth = -clip.z;
		PROJECTION.transform(clip);

		if (Math.abs(clip.w) < 1.0e-6f) {
			return new Vector4f(0.0f, 0.0f, depth, 0.0f);
		}

		float ndcX = clip.x / clip.w;
		float ndcY = clip.y / clip.w;

		return new Vector4f(
				(ndcX * 0.5f + 0.5f) * guiWidth,
				(0.5f - ndcY * 0.5f) * guiHeight,
				depth,
				1.0f);
	}
}
