package dev.nolight.pingping.client;

import dev.nolight.pingping.PingBroadcastPayload;
import dev.nolight.pingping.PingPing;
import dev.nolight.pingping.PingRequestPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Tracks which entities are currently marked, and turns middle clicks into ping requests. */
public final class ClientPings {
	private static final Predicate<Entity> PINGABLE = entity -> !entity.isSpectator() && entity.isPickable();

	/** entity id -> tick at which the marker disappears. */
	private static final Map<Integer, Long> ACTIVE = new HashMap<>();

	private static long clientTick;

	private ClientPings() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(PingBroadcastPayload.TYPE, (payload, context) ->
				accept(context.client(), payload.entityId()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			clientTick++;
			ACTIVE.values().removeIf(expiry -> expiry <= clientTick);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ACTIVE.clear();
			clientTick = 0;
		});
	}

	private static void accept(Minecraft client, int entityId) {
		ClientLevel level = client.level;

		if (level == null) {
			return;
		}

		Entity target = level.getEntity(entityId);

		if (target == null) {
			return;
		}

		PingPing.LOGGER.info("[pingping] client received ping for entity {}", entityId);
		ACTIVE.put(entityId, clientTick + PingPing.PING_LIFETIME_TICKS);
		level.playLocalSound(target, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
	}

	public static Map<Integer, Long> active() {
		return ACTIVE;
	}

	public static long clientTick() {
		return clientTick;
	}

	/**
	 * Called from the pick-block hook. Returns {@code true} when the click was consumed as a ping and vanilla
	 * pick block should be skipped.
	 */
	public static boolean tryPing(boolean overridePickBlock) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;

		if (player == null || client.level == null) {
			return false;
		}

		PingPing.LOGGER.info("[pingping] middle click: crosshair={} creative={} override={}",
				client.hitResult == null ? "null" : client.hitResult.getType(), player.isCreative(), overridePickBlock);

		if (!overridePickBlock && vanillaPickWouldWork(client, player)) {
			PingPing.LOGGER.info("[pingping] deferring to vanilla pick block");
			return false;
		}

		HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, PINGABLE, PingPing.MAX_PING_DISTANCE);
		PingPing.LOGGER.info("[pingping] ping raycast hit {}", hit.getType());

		if (hit instanceof EntityHitResult entityHit) {
			PingPing.LOGGER.info("[pingping] sending request for entity {}", entityHit.getEntity().getId());
			ClientPlayNetworking.send(new PingRequestPayload(entityHit.getEntity().getId()));
		}

		// Swallow the click either way: the player asked for a ping, not for a block.
		return true;
	}

	/** Vanilla pick block is only meaningful on blocks, or on entities while in creative (spawn eggs). */
	private static boolean vanillaPickWouldWork(Minecraft client, LocalPlayer player) {
		HitResult hit = client.hitResult;

		if (hit == null) {
			return false;
		}

		return switch (hit.getType()) {
			case BLOCK -> true;
			case ENTITY -> player.isCreative();
			case MISS -> false;
		};
	}
}
