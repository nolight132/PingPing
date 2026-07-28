package dev.nolight.pingping.client;

import dev.nolight.pingping.PingPing;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Vector4f;

/** Draws every live ping: a label where it is on screen, an arrow at the edge when it is not. */
public final class PingHud {
	private static final int TEXT_COLOR = 0xFFFFE066;
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

				int metres = (int) Math.round(world.distanceTo(player.getEyePosition()));
				Component distance = Component.translatable("pingping.marker.distance", metres);
				boolean onScreen = screen.z > 0.0f && screen.w != 0.0f
						&& screen.x >= EDGE_INSET && screen.x <= width - EDGE_INSET
						&& screen.y >= EDGE_INSET && screen.y <= height - EDGE_INSET;

				if (onScreen) {
					drawMarker(graphics, font, (int) screen.x, (int) screen.y, ping.label(), distance);
				} else {
					drawEdgeArrow(graphics, font, screen, width, height, distance);
				}
			}
		});
	}

	private static void drawMarker(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Font font, int x, int y,
			Component label, Component distance) {
		graphics.centeredText(font, Component.literal("▼"), x, y - font.lineHeight - 2, TEXT_COLOR);
		backdrop(graphics, font, label, x, y + 2);
		backdrop(graphics, font, distance, x, y + 2 + font.lineHeight + 1);
	}

	private static void drawEdgeArrow(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Font font,
			Vector4f screen, int width, int height, Component distance) {
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

		float halfW = width / 2.0f - EDGE_INSET;
		float halfH = height / 2.0f - EDGE_INSET;
		float scale = Math.min(
				halfW / Math.max(Math.abs(dirX), 1.0e-3f),
				halfH / Math.max(Math.abs(dirY), 1.0e-3f));

		float x = centreX + dirX * scale;
		float y = centreY + dirY * scale;

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		// The glyph points right at zero rotation.
		pose.rotate((float) Math.atan2(dirY, dirX));
		graphics.centeredText(font, Component.literal("➤"), 0, -font.lineHeight / 2, TEXT_COLOR);
		pose.popMatrix();

		backdrop(graphics, font, distance, (int) (centreX + dirX * (scale - 14.0f)),
				(int) (centreY + dirY * (scale - 14.0f)) - font.lineHeight / 2);
	}

	private static void backdrop(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Font font, Component text,
			int centreX, int y) {
		int half = font.width(text) / 2;
		graphics.fill(centreX - half - 2, y - 1, centreX + half + 2, y + font.lineHeight, BACKDROP_COLOR);
		graphics.centeredText(font, text, centreX, y, TEXT_COLOR);
	}
}
