package dev.nolight.pingping;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPing implements ModInitializer {
	public static final String MOD_ID = "pingping";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Hard floor between two pings, so a single burst cannot be dumped in one tick. */
	public static final int MIN_PING_INTERVAL_TICKS = 2;

	/** Sent instead of a colour to mean "server, pick one for me". */
	public static final int AUTO_COLOR = -1;

	/**
	 * Readable, well separated hues handed out by UUID, so a player keeps the same colour across sessions and
	 * servers without anything being stored.
	 */
	private static final int[] PALETTE = {
			0xFFE066, 0xFF7A6B, 0x6BD5FF, 0x9CE86B, 0xC98BFF, 0xFFAA4D,
			0x6BFFC4, 0xFF8BD1, 0x8BA6FF, 0xE8DC6B, 0xFF6BA6, 0x6BFF8B,
			0xFFC48B, 0x8BE8FF, 0xD1FF6B, 0xB58BFF,
	};

	public static int autoColorFor(java.util.UUID player) {
		return PALETTE[Math.floorMod(player.hashCode(), PALETTE.length)];
	}

	/** Keeps a colour inside 24-bit RGB, since it arrives from a client. */
	public static int sanitiseColor(int color) {
		return color & 0xFFFFFF;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PingPayloads.register();
		PingServer.register();
	}
}
