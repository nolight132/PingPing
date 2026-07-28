package dev.nolight.pingping;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class PingServer {
	private static final Map<UUID, Budget> BUDGETS = new HashMap<>();

	private PingServer() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(PingRequestPayload.TYPE, (payload, context) ->
				handle(context.player(), payload.entityId()));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				BUDGETS.remove(handler.getPlayer().getUUID()));
	}

	private static void handle(ServerPlayer player, int entityId) {
		ServerLevel level = player.level();
		Entity target = level.getEntity(entityId);

		if (target == null || target.isRemoved()) {
			return;
		}

		if (target.distanceToSqr(player) > PingPing.MAX_PING_DISTANCE * PingPing.MAX_PING_DISTANCE) {
			return;
		}

		long tick = level.getGameTime();

		if (!BUDGETS.computeIfAbsent(player.getUUID(), uuid -> new Budget(tick)).tryConsume(tick)) {
			return;
		}

		PingBroadcastPayload broadcast = new PingBroadcastPayload(entityId, player.getUUID());

		for (ServerPlayer receiver : PlayerLookup.all(player.getServer())) {
			if (receiver.level() == level && ServerPlayNetworking.canSend(receiver, PingBroadcastPayload.TYPE)) {
				ServerPlayNetworking.send(receiver, broadcast);
			}
		}
	}

	/**
	 * Token bucket: a player may fire {@link PingPing#MAX_CHARGES} pings back to back, after which they have to
	 * wait for charges to trickle back in. The extra interval floor stops a whole bucket being dumped at once.
	 */
	private static final class Budget {
		private float charges = PingPing.MAX_CHARGES;
		private long refilledAt;
		private long lastPing = Long.MIN_VALUE;

		Budget(long tick) {
			this.refilledAt = tick;
		}

		boolean tryConsume(long tick) {
			charges = Math.min(PingPing.MAX_CHARGES,
					charges + (tick - refilledAt) / (float) PingPing.CHARGE_REFILL_TICKS);
			refilledAt = tick;

			if (charges < 1.0f || tick - lastPing < PingPing.MIN_PING_INTERVAL_TICKS) {
				return false;
			}

			charges -= 1.0f;
			lastPing = tick;
			return true;
		}
	}
}
