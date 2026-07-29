package dev.nolight.pingping.client;

import dev.nolight.pingping.PingConfig;
import dev.nolight.pingping.PingPing;
import dev.nolight.pingping.PingTarget;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Vector4f;

/** Draws every live ping: a diamond where it is on screen, an arrow at the edge when it is not. */
public final class PingHud {
	private static final Component DIAMOND = Component.literal("◆");
	private static final Component ARROW = Component.literal("➤");

	private static final int BACKDROP_COLOR = 0x80000000;

	/** How far the edge arrows sit from the screen border. */
	private static final int EDGE_INSET = 26;

	private PingHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(PingPing.id("pings"), (graphics, deltaTracker) -> {
			if (ClientPings.active().isEmpty() || !CameraCapture.isReady()) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
			ClientLevel level = client.level;
			LocalPlayer player = client.player;

			if (level == null || player == null) {
				return;
			}

			Font font = client.font;
			int width = graphics.guiWidth();
			int height = graphics.guiHeight();
			float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

			for (ClientPings.ActivePing ping : ClientPings.active()) {
				Vec3 world = ping.currentPos(level, partialTick);
				Vector4f screen = CameraCapture.project(world, width, height);

				if (screen == null) {
					continue;
				}

				Component distance = Component.translatable("pingping.marker.distance",
						Math.round(world.distanceTo(player.getEyePosition())));

				boolean onScreen = screen.z > 0.0f && screen.w != 0.0f
						&& screen.x >= EDGE_INSET && screen.x <= width - EDGE_INSET
						&& screen.y >= EDGE_INSET && screen.y <= height - EDGE_INSET;

				int color = 0xFF000000 | ping.color();

				if (onScreen) {
					drawMarker(graphics, font, screen.x, screen.y, distance, ping, level, partialTick, color,
							(float) PingConfig.get().markerScale * distanceFactor(world, height));
				} else if (PingConfig.get().showEdgeArrows) {
					drawEdgeArrow(graphics, font, screen, width, height, distance, color);
				}
			}
		});
	}

	private static float distanceFactor(Vec3 world, int guiHeight) {
		PingConfig config = PingConfig.get();

		if (!config.scaleWithDistance) {
			return 1.0f;
		}

		float perspective = CameraCapture.pixelsPerBlock(world, guiHeight) * EntityRenderer.NAMETAG_SCALE;
		return (float) Math.min(config.maxMarkerScale, Math.max(config.minMarkerScale, perspective));
	}

	private static void drawMarker(GuiGraphicsExtractor graphics, Font font, float x, float y, Component distance,
			ClientPings.ActivePing ping, ClientLevel level, float partialTick, int color, float scale) {
		Matrix3x2fStack pose = graphics.pose();

		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		graphics.centeredText(font, DIAMOND, 0, -font.lineHeight / 2, color);
		backdrop(graphics, font, distance, 0, -font.lineHeight - font.lineHeight / 2 - 2, color);
		pose.popMatrix();

		PingConfig config = PingConfig.get();

		if (config.showIcons) {
			float drawPx = Math.max(2.0f, config.previewSize * scale);
			float centreY = y - (font.lineHeight * 2.0f + 3.0f) * scale - drawPx * 0.5f;
			drawPreview(graphics, ping, level, partialTick, x, centreY, previewBox(config), drawPx);
		}
	}

	private static int previewBox(PingConfig config) {
		double widest = config.scaleWithDistance ? Math.max(config.maxMarkerScale, 1.0) : 1.0;
		return Math.max(2, (int) Math.ceil(config.previewSize * config.markerScale * widest));
	}

	private static void drawPreview(GuiGraphicsExtractor graphics, ClientPings.ActivePing ping, ClientLevel level,
			float partialTick, float centreX, float centreY, int boxPx, float drawPx) {
		if (ping.entityId() == PingTarget.NO_ENTITY) {
			if (ping.block()) {
				blitItem(graphics, PingIcons.itemForBlock(level, ping.pos()), centreX, centreY, drawPx);
			}

			return;
		}

		Entity entity = level.getEntity(ping.entityId());

		if (entity == null) {
			return;
		}

		ItemStack stack = PingIcons.itemFor(entity);

		if (!stack.isEmpty()) {
			blitItem(graphics, stack, centreX, centreY, drawPx);
			return;
		}

		PingIcons.entity(graphics, entity, partialTick, centreX, centreY, boxPx, drawPx);
	}

	private static void blitItem(GuiGraphicsExtractor graphics, ItemStack stack, float centreX, float centreY,
			float size) {
		if (stack.isEmpty()) {
			return;
		}

		float shrink = size / 16.0f;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(centreX, centreY);
		pose.scale(shrink, shrink);
		graphics.item(stack, -8, -8);
		pose.popMatrix();
	}

	private static void drawEdgeArrow(GuiGraphicsExtractor graphics, Font font, Vector4f screen, int width,
			int height, Component distance, int color) {
		float centreX = width / 2.0f;
		float centreY = height / 2.0f;

		float dirX = screen.x - centreX;
		float dirY = screen.y - centreY;

		// Behind the camera the projection mirrors the point, so flip it back to the correct side.
		if (screen.z <= 0.0f) {
			dirX = -dirX;
			dirY = -dirY;
		}

		float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);

		if (length < 1.0e-3f) {
			dirX = 0.0f;
			dirY = 1.0f;
			length = 1.0f;
		}

		dirX /= length;
		dirY /= length;

		float scale = Math.min(
				(width / 2.0f - EDGE_INSET) / Math.max(Math.abs(dirX), 1.0e-3f),
				(height / 2.0f - EDGE_INSET) / Math.max(Math.abs(dirY), 1.0e-3f));

		float x = centreX + dirX * scale;
		float y = centreY + dirY * scale;

		Matrix3x2fStack pose = graphics.pose();

		pose.pushMatrix();
		pose.translate(x, y);
		pose.rotate((float) Math.atan2(dirY, dirX));
		pose.scale((float) PingConfig.get().markerScale, (float) PingConfig.get().markerScale);
		// The glyph points right at zero rotation.
		graphics.centeredText(font, ARROW, 0, -font.lineHeight / 2, color);
		pose.popMatrix();

		pose.pushMatrix();
		pose.translate(x - dirX * 14.0f, y - dirY * 14.0f);
		pose.scale((float) PingConfig.get().markerScale, (float) PingConfig.get().markerScale);
		backdrop(graphics, font, distance, 0, -font.lineHeight / 2, color);
		pose.popMatrix();
	}

	private static void backdrop(GuiGraphicsExtractor graphics, Font font, Component text, int centreX, int y,
			int color) {
		int half = font.width(text) / 2;
		graphics.fill(centreX - half - 2, y - 1, centreX + half + 2, y + font.lineHeight, BACKDROP_COLOR);
		graphics.centeredText(font, text, centreX, y, color);
	}
}
