package dev.nolight.pingping;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class PingColors {
	private static final int[] PALETTE = {
			0xFFE066, 0xFF7A6B, 0x6BD5FF, 0x9CE86B, 0xC98BFF, 0xFFAA4D,
			0x6BFFC4, 0xFF8BD1, 0x8BA6FF, 0xE8DC6B, 0xFF6BA6, 0x6BFF8B,
			0xFFC48B, 0x8BE8FF, 0xD1FF6B, 0xB58BFF,
	};

	private static final Map<UUID, Integer> CHOSEN = new HashMap<>();
	private static final Map<UUID, Integer> HANDED_OUT = new HashMap<>();

	private PingColors() {
	}

	public static int of(ServerPlayer player) {
		Integer chosen = CHOSEN.get(player.getUUID());
		return chosen != null ? chosen : HANDED_OUT.getOrDefault(player.getUUID(), PALETTE[0]);
	}

	public static int sanitiseColor(int color) {
		return color & 0xFFFFFF;
	}

	public static void applyColor(ServerPlayer player, int color) {
		CHOSEN.put(player.getUUID(), sanitiseColor(color));
		apply(player);
	}

	public static void join(ServerPlayer player) {
		HANDED_OUT.put(player.getUUID(), free(player));
		apply(player);
	}

	public static void leave(UUID player) {
		CHOSEN.remove(player);
		HANDED_OUT.remove(player);
	}

	/**
	 * Starts where the player's own id points so a colour is stable between sessions, then walks on until it finds
	 * one nobody online is already using.
	 */
	private static int free(ServerPlayer player) {
		int start = Math.floorMod(player.getUUID().hashCode(), PALETTE.length);

		for (int step = 0; step < PALETTE.length; step++) {
			int candidate = PALETTE[(start + step) % PALETTE.length];

			if (!taken(player, candidate)) {
				return candidate;
			}
		}

		return PALETTE[start];
	}

	private static boolean taken(ServerPlayer asking, int color) {
		for (ServerPlayer other : asking.level().getServer().getPlayerList().getPlayers()) {
			if (other != asking && of(other) == color) {
				return true;
			}
		}

		return false;
	}

	/** Vanilla's own recipe for a live icon change: drop the waypoint, edit it, put it back. */
	private static void apply(ServerPlayer player) {
		ServerLevel level = player.level();
		level.getWaypointManager().untrackWaypoint(player);
		player.waypointIcon().color = Optional.of(of(player));

		if (player.isTransmittingWaypoint()) {
			level.getWaypointManager().trackWaypoint(player);
		}
	}
}
