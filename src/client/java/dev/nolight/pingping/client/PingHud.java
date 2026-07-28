package dev.nolight.pingping.client;

import dev.nolight.pingping.PingPing;
import dev.nolight.pingping.PingTarget;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
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

	/** Markers use a smaller face than the rest of the HUD. */
	private static final float TEXT_SCALE = 0.75f;

	private static final float ICON_SCALE = 0.6f;

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
					drawMarker(graphics, font, screen.x, screen.y, distance, icon(level, ping));
				} else {
					drawEdgeArrow(graphics, font, screen, width, height, distance);
				}
			}
		});
	}

	private static ItemStack icon(ClientLevel level, ClientPings.ActivePing ping) {
		if (ping.entityId() == PingTarget.NO_ENTITY) {
			return ItemStack.EMPTY;
		}

		Entity entity = level.getEntity(ping.entityId());
		return entity == null ? ItemStack.EMPTY : PingIcons.forEntity(entity);
	}

	/** Diamond sits on the target, distance rides above it, icon above that. */
	private static void drawMarker(GuiGraphicsExtractor graphics, Font font, float x, float y, Component distance,
			ItemStack icon) {
		Matrix3x2fStack pose = graphics.pose();

		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(TEXT_SCALE, TEXT_SCALE);
		graphics.centeredText(font, DIAMOND, 0, -font.lineHeight / 2, TEXT_COLOR);
		backdrop(graphics, font, distance, 0, -font.lineHeight - font.lineHeight / 2 - 2);
		pose.popMatrix();

		if (!icon.isEmpty()) {
			pose.pushMatrix();
			pose.translate(x, y - (font.lineHeight + font.lineHeight / 2 + 4) * TEXT_SCALE);
			pose.scale(ICON_SCALE, ICON_SCALE);
			graphics.item(icon, -8, -16);
			pose.popMatrix();
		}
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
		pose.scale(TEXT_SCALE, TEXT_SCALE);
		// The glyph points right at zero rotation.
		graphics.centeredText(font, ARROW, 0, -font.lineHeight / 2, TEXT_COLOR);
		pose.popMatrix();

		pose.pushMatrix();
		pose.translate(x - dirX * 14.0f, y - dirY * 14.0f);
		pose.scale(TEXT_SCALE, TEXT_SCALE);
		backdrop(graphics, font, distance, 0, -font.lineHeight / 2);
		pose.popMatrix();
	}

	private static void backdrop(GuiGraphicsExtractor graphics, Font font, Component text, int centreX, int y) {
		int half = font.width(text) / 2;
		graphics.fill(centreX - half - 2, y - 1, centreX + half + 2, y + font.lineHeight, BACKDROP_COLOR);
		graphics.centeredText(font, text, centreX, y, TEXT_COLOR);
	}
}
