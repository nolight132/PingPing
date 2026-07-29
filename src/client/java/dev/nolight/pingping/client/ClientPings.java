package dev.nolight.pingping.client;

import dev.nolight.pingping.PingBroadcastPayload;
import dev.nolight.pingping.PingColorPayload;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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

	public record ActivePing(int entityId, Vec3 pos, boolean block, boolean preview, int color, long expiresAt) {
		public Vec3 currentPos(ClientLevel level, float partialTick) {
			if (entityId == PingTarget.NO_ENTITY) {
				return pos;
			}

			Entity entity = level.getEntity(entityId);
			return entity == null ? pos : entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.65, 0.0);
		}
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(PingBroadcastPayload.TYPE,
				(payload, context) -> accept(context.client(), payload.target(), payload.sender(), payload.color()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			clientTick++;
			ACTIVE.removeIf(ping -> ping.expiresAt() <= clientTick);
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sendColor());

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ACTIVE.clear();
			clientTick = 0;
		});
	}

	/** Tells the server which colour to paint this player's markers and locator bar dot. */
	public static void sendColor() {
		if (!ClientPlayNetworking.canSend(PingColorPayload.TYPE)) {
			return;
		}

		PingConfig config = PingConfig.get();
		ClientPlayNetworking.send(new PingColorPayload(
				config.useServerColor ? PingPing.AUTO_COLOR : PingPing.sanitiseColor(config.customColor)));
	}

	private static void accept(Minecraft client, PingTarget target, java.util.UUID sender, int color) {
		ClientLevel level = client.level;
		LocalPlayer player = client.player;

		if (level == null || player == null) {
			return;
		}

		ACTIVE.removeIf(ping -> target.isEntity() && ping.entityId() == target.entityId());
		ACTIVE.add(new ActivePing(target.entityId(), target.pos(), target.block(), target.preview(), color,
				clientTick + PingConfig.get().lifetimeTicks()));

		if (PingConfig.get().soundEnabled && shouldHear(level, player, sender)) {
			playPing(client, level, player, target.pos());
		}
	}

	private static void playPing(Minecraft client, ClientLevel level, LocalPlayer player, Vec3 spot) {
		PingConfig config = PingConfig.get();
		SoundEvent sound = PingSounds.resolve(config.soundId);
		float volume = (float) config.soundVolume;
		float pitch = (float) config.soundPitch;

		Vec3 ear = player.getEyePosition();
		Vec3 offset = spot.subtract(ear);
		double away = offset.length();

		if (!config.directionalSound || away < 1.0e-4) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(sound, volume, pitch));
			return;
		}

		double reach = Math.max(config.maxDistance, 1.0);
		double mapped = config.soundSphereRadius * Math.sqrt(Math.min(away / reach, 1.0));
		Vec3 at = ear.add(offset.scale(mapped / away));

		level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.MASTER, volume, pitch, false);
	}

	/**
	 * You always hear your own ping, however far away you marked something. Someone else's only carries if they
	 * are within earshot of you — the radius is between the two players, not between you and the marker.
	 */
	private static boolean shouldHear(ClientLevel level, LocalPlayer player, java.util.UUID sender) {
		if (player.getUUID().equals(sender)) {
			return true;
		}

		Player origin = level.getPlayerByUUID(sender);

		if (origin == null) {
			return false;
		}

		double radius = PingConfig.get().soundRadius;
		return origin.position().distanceToSqr(player.position()) <= radius * radius;
	}

	public static List<ActivePing> active() {
		return ACTIVE;
	}

	public static boolean tryPing(boolean forceBlock, boolean sneaking) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		ClientLevel level = client.level;

		if (player == null || level == null) {
			return false;
		}

		PingConfig config = PingConfig.get();
		boolean preview = config.previewTarget.wants(sneaking);

		if (forceBlock) {
			send(worldTarget(player, level, true, preview));
			return true;
		}

		Entity entity = findEntity(player, level);

		if (entity != null) {
			send(PingTarget.ofEntity(entity.getId(), entity.position(), preview));
			return true;
		}

		// Sneaking is a deliberate ping, so it always beats pick block; otherwise pick block gets its turn first.
		if (config.pickBlockWins && !sneaking && vanillaPickWouldWork(client, player)) {
			return false;
		}

		if (sneaking || !config.blockPingNeedsSneak) {
			send(worldTarget(player, level, true, preview));
		} else if (config.freePointPing) {
			send(worldTarget(player, level, false, false));
		}

		return true;
	}

	private static void send(PingTarget target) {
		if (target != null) {
			ClientPlayNetworking.send(new PingRequestPayload(target));
		}
	}

	/**
	 * An entity actually under the crosshair wins; otherwise the closest one inside a small cone around the view
	 * snaps in, which is what makes marking something 60 blocks away practical.
	 */
	private static Entity findEntity(LocalPlayer player, ClientLevel level) {
		double range = PingConfig.get().maxDistance;
		HitResult precise = ProjectileUtil.getHitResultOnViewVector(player, PINGABLE, range);

		if (precise instanceof EntityHitResult entityHit) {
			return entityHit.getEntity();
		}

		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1.0f);
		BlockHitResult blockHit = clip(player, level, eye, view, range);
		double limit = blockHit.getType() == HitResult.Type.MISS ? range : blockHit.getLocation().distanceTo(eye);

		return snapToEntity(player, level, eye, view, limit);
	}

	/**
	 * Where a non-entity marker lands. As a block it snaps to the whole block and gains a preview; as a plain point
	 * it stays exactly where the ray struck, so the marker sits on the pixel that was aimed at.
	 */
	private static PingTarget worldTarget(LocalPlayer player, ClientLevel level, boolean asBlock, boolean preview) {
		double range = PingConfig.get().maxDistance;
		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1.0f);
		BlockHitResult blockHit = clip(player, level, eye, view, range);

		if (blockHit.getType() == HitResult.Type.MISS) {
			return asBlock ? null : PingTarget.ofPoint(eye.add(view.scale(range)));
		}

		if (!asBlock) {
			return PingTarget.ofPoint(blockHit.getLocation());
		}

		return PingTarget.ofBlock(PingConfig.get().snapBlockToCentre
				? Vec3.atCenterOf(blockHit.getBlockPos())
				: blockHit.getLocation(), preview);
	}

	private static BlockHitResult clip(LocalPlayer player, ClientLevel level, Vec3 eye, Vec3 view, double range) {
		return level.clip(new ClipContext(eye, eye.add(view.scale(range)),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
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
