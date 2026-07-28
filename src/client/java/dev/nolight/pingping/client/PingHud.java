package dev.nolight.pingping.client;

import dev.nolight.pingping.PingConfig;
import dev.nolight.pingping.PingPing;
import dev.nolight.pingping.PingTarget;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.player.LocalPlayer;
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

	private static final int TEXT_COLOR = 0xFFFFE066;
	private static final int BACKDROP_COLOR = 0x80000000;

	private static final int PREVIEW_SIZE = 10;

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

				if (onScreen) {
					drawMarker(graphics, font, screen.x, screen.y, distance, ping, level, partialTick);
				} else if (PingConfig.get().showEdgeArrows) {
					drawEdgeArrow(graphics, font, screen, width, height, distance);
				}
			}
		});
	}

	/** Diamond sits on the target, distance rides above it, preview above that. */
	private static void drawMarker(GuiGraphicsExtractor graphics, Font font, float x, float y, Component distance,
			ClientPings.ActivePing ping, ClientLevel level, float partialTick) {
		float scale = (float) PingConfig.get().markerScale;
		Matrix3x2fStack pose = graphics.pose();

		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		graphics.centeredText(font, DIAMOND, 0, -font.lineHeight / 2, TEXT_COLOR);
		backdrop(graphics, font, distance, 0, -font.lineHeight - font.lineHeight / 2 - 2);
		pose.popMatrix();

		if (PingConfig.get().showIcons) {
			// Positioned on the matrix in floats: rounding the pixel here made previews twitch as the player moved.
			pose.pushMatrix();
			pose.translate(x, y - (font.lineHeight * 2.0f + 3.0f) * scale - PREVIEW_SIZE * scale * 0.5f);
			pose.scale(scale, scale);
			drawPreview(graphics, ping, level, partialTick);
			pose.popMatrix();
		}
	}

	/** Items and blocks show their icon; anything else alive shows a live portrait of itself. */
	/** Blocks and dropped items show their icon; anything else shows its face cropped from its own texture. */
	private static void drawPreview(GuiGraphicsExtractor graphics, ClientPings.ActivePing ping, ClientLevel level,
			float partialTick) {
		if (ping.entityId() == PingTarget.NO_ENTITY) {
			blitItem(graphics, PingIcons.itemForBlock(level, ping.pos()));
			return;
		}

		Entity entity = level.getEntity(ping.entityId());

		if (entity == null) {
			return;
		}

		ItemStack stack = PingIcons.itemFor(entity);

		if (!stack.isEmpty()) {
			blitItem(graphics, stack);
			return;
		}

		EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher()
				.extractEntity(entity, partialTick);
		PingIcons.face(graphics, entity, state, -PREVIEW_SIZE / 2, -PREVIEW_SIZE / 2, PREVIEW_SIZE);
	}

	/** Item icons are authored at 16px, so shrink them to the preview box. */
	private static void blitItem(GuiGraphicsExtractor graphics, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		float shrink = PREVIEW_SIZE / 16.0f;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(shrink, shrink);
		graphics.item(stack, -8, -8);
		pose.popMatrix();
	}

	private static void drawEdgeArrow(GuiGraphicsExtractor graphics, Font font, Vector4f screen, int width,
			int height, Component distance) {
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
		graphics.centeredText(font, ARROW, 0, -font.lineHeight / 2, TEXT_COLOR);
		pose.popMatrix();

		pose.pushMatrix();
		pose.translate(x - dirX * 14.0f, y - dirY * 14.0f);
		pose.scale((float) PingConfig.get().markerScale, (float) PingConfig.get().markerScale);
		backdrop(graphics, font, distance, 0, -font.lineHeight / 2);
		pose.popMatrix();
	}

	private static void backdrop(GuiGraphicsExtractor graphics, Font font, Component text, int centreX, int y) {
		int half = font.width(text) / 2;
		graphics.fill(centreX - half - 2, y - 1, centreX + half + 2, y + font.lineHeight, BACKDROP_COLOR);
		graphics.centeredText(font, text, centreX, y, TEXT_COLOR);
	}
}
