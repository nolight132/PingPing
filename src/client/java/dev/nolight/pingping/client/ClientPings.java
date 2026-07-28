package dev.nolight.pingping.client;

import dev.nolight.pingping.PingBroadcastPayload;
import dev.nolight.pingping.PingConfig;
import dev.nolight.pingping.PingPing;
import dev.nolight.pingping.PingRequestPayload;
import dev.nolight.pingping.PingTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Tracks live pings and turns middle clicks into ping requests. */
public final class ClientPings {
	// Not Entity#isPickable: that is false for anything non-living, which silently excluded dropped items,
	// boats and minecarts from ever being a ping target.
	private static final Predicate<Entity> PINGABLE = entity -> !entity.isSpectator() && !entity.isRemoved();

	private static final List<ActivePing> ACTIVE = new ArrayList<>();

	private static long clientTick;

	private ClientPings() {
	}

	/** A ping being displayed. {@code entityId} is {@link PingTarget#NO_ENTITY} for a plain world position. */
	public record ActivePing(int entityId, Vec3 pos, long expiresAt) {
		public Vec3 currentPos(ClientLevel level, float partialTick) {
			if (entityId == PingTarget.NO_ENTITY) {
				return pos;
			}

			Entity entity = level.getEntity(entityId);
			return entity == null ? pos : entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.65, 0.0);
		}
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(PingBroadcastPayload.TYPE, (payload, context) ->
				accept(context.client(), payload.target()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			clientTick++;
			ACTIVE.removeIf(ping -> ping.expiresAt() <= clientTick);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ACTIVE.clear();
			clientTick = 0;
		});
	}

	private static void accept(Minecraft client, PingTarget target) {
		ClientLevel level = client.level;
		LocalPlayer player = client.player;

		if (level == null || player == null) {
			return;
		}

		ACTIVE.removeIf(ping -> target.isEntity() && ping.entityId() == target.entityId());
		ACTIVE.add(new ActivePing(target.entityId(), target.pos(), clientTick + PingConfig.get().lifetimeTicks()));

		// Non-positional: every listener hears their own ping at their own ears, never someone else's from afar.
		if (PingConfig.get().soundEnabled && target.pos().distanceToSqr(player.position()) <= PingConfig.get().soundRadius * PingConfig.get().soundRadius) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6f, 0.5f));
		}
	}

	public static List<ActivePing> active() {
		return ACTIVE;
	}

	/**
	 * Called from the pick-block hook. Returns {@code true} when the click was consumed as a ping and vanilla
	 * pick block should be skipped.
	 */
	public static boolean tryPing(boolean overridePickBlock) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		ClientLevel level = client.level;

		if (player == null || level == null) {
			return false;
		}

		if (!overridePickBlock && PingConfig.get().pickBlockWins && vanillaPickWouldWork(client, player)) {
			return false;
		}

		PingTarget target = findTarget(player, level);

		if (target != null) {
			ClientPlayNetworking.send(new PingRequestPayload(target));
		}

		// Swallow the click either way: the player asked for a ping, not for a block.
		return true;
	}

	/**
	 * Picks what the crosshair means. An entity actually under the crosshair wins; otherwise the closest entity
	 * inside a small cone around the view snaps in; failing that the ping lands on the block being looked at.
	 */
	private static PingTarget findTarget(LocalPlayer player, ClientLevel level) {
		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1.0f);
		Vec3 far = eye.add(view.scale(PingConfig.get().maxDistance));

		HitResult precise = ProjectileUtil.getHitResultOnViewVector(player, PINGABLE, PingConfig.get().maxDistance);

		if (precise instanceof EntityHitResult entityHit) {
			return entity(entityHit.getEntity());
		}

		BlockHitResult blockHit = level.clip(
				new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double limit = blockHit.getType() == HitResult.Type.MISS
				? PingConfig.get().maxDistance
				: blockHit.getLocation().distanceTo(eye);

		Entity snapped = snapToEntity(player, level, eye, view, limit);

		if (snapped != null) {
			return entity(snapped);
		}

		if (blockHit.getType() != HitResult.Type.MISS) {
			// Centre of the struck block, so every client can resolve the same block for its preview icon
			// and the marker does not wobble along the face the ray happened to clip.
			return PingTarget.ofPosition(Vec3.atCenterOf(blockHit.getBlockPos()));
		}

		return null;
	}

	private static Entity snapToEntity(LocalPlayer player, ClientLevel level, Vec3 eye, Vec3 view, double limit) {
		double minDot = Math.cos(Math.toRadians(PingConfig.get().snapConeDegrees));
		// Cone half-width grows with range, but stays bounded so a 256-block ping is not a world-sized query.
		double margin = Math.min(limit * Math.tan(Math.toRadians(PingConfig.get().snapConeDegrees)) + 1.0, 12.0);
		AABB search = player.getBoundingBox().expandTowards(view.scale(limit)).inflate(margin);

		Entity best = null;
		double bestDot = minDot;

		for (Entity candidate : level.getEntities(player, search, PINGABLE)) {
			Vec3 toCandidate = candidate.getBoundingBox().getCenter().subtract(eye);
			double distance = toCandidate.length();

			if (distance < 1.0e-4 || distance > limit + candidate.getBbWidth()) {
				continue;
			}

			double dot = toCandidate.scale(1.0 / distance).dot(view);

			if (dot <= bestDot) {
				continue;
			}

			BlockHitResult blocked = level.clip(new ClipContext(eye, candidate.getBoundingBox().getCenter(),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

			if (blocked.getType() != HitResult.Type.MISS) {
				continue;
			}

			best = candidate;
			bestDot = dot;
		}

		return best;
	}

	private static PingTarget entity(Entity entity) {
		return PingTarget.ofEntity(entity.getId(), entity.position());
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

	public static long clientTick() {
		return clientTick;
	}
}
