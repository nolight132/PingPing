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
import net.minecraft.world.phys.Vec3;

public final class PingServer {
	private static final Map<UUID, Budget> BUDGETS = new HashMap<>();

	private PingServer() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(PingRequestPayload.TYPE,
				(payload, context) -> handle(context.player(), payload.target()));

		ServerPlayNetworking.registerGlobalReceiver(PingColorPayload.TYPE,
				(payload, context) -> PingColors.applyColor(context.player(), payload.color()));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PingColors.join(handler.getPlayer()));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			BUDGETS.remove(handler.getPlayer().getUUID());
			PingColors.leave(handler.getPlayer().getUUID());
		});
	}

	private static void handle(ServerPlayer player, PingTarget target) {
		ServerLevel level = player.level();
		Vec3 spot = target.pos();

		if (target.isEntity()) {
			Entity entity = level.getEntity(target.entityId());

			if (entity == null || entity.isRemoved()) {
				return;
			}

			spot = entity.position();
			target = PingTarget.ofEntity(target.entityId(), spot, target.preview());
		}

		double reach = PingConfig.get().maxDistance + 8.0;

		if (spot.distanceToSqr(player.position()) > reach * reach) {
			return;
		}

		long tick = level.getGameTime();

		if (!BUDGETS.computeIfAbsent(player.getUUID(), uuid -> new Budget(tick)).tryConsume(tick)) {
			return;
		}

		PingBroadcastPayload broadcast = new PingBroadcastPayload(target, player.getUUID(), PingColors.of(player));

		for (ServerPlayer receiver : PlayerLookup.all(level.getServer())) {
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
		private float charges = PingConfig.get().maxCharges;
		private long refilledAt;
		private long lastPing;

		Budget(long tick) {
			this.refilledAt = tick;
			// Not Long.MIN_VALUE: `tick - lastPing` would overflow and reject every ping forever.
			this.lastPing = tick - PingConfig.get().minPingIntervalTicks;
		}

		boolean tryConsume(long tick) {
			PingConfig config = PingConfig.get();
			charges = Math.min(config.maxCharges,
					charges + (tick - refilledAt) / (float) config.chargeRefillTicks());
			refilledAt = tick;

			if (charges < 1.0f || tick - lastPing < PingConfig.get().minPingIntervalTicks) {
				return false;
			}

			charges -= 1.0f;
			lastPing = tick;
			return true;
		}
	}
}
