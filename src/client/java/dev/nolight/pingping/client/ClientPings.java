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

	public record ActivePing(int entityId, Vec3 pos, boolean block, int serverColor, int customColor, long expiresAt) {
		public int color() {
			PingConfig config = PingConfig.get();
			return config.syncAllColors || customColor == PingPing.AUTO_COLOR ? serverColor : customColor;
		}

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
				(payload, context) -> accept(context.client(), payload.target(), payload.sender(),
						payload.serverColor(), payload.customColor()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			clientTick++;
			ACTIVE.removeIf(ping -> ping.expiresAt() <= clientTick);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ACTIVE.clear();
			clientTick = 0;
		});
	}

	private static void accept(Minecraft client, PingTarget target, java.util.UUID sender, int serverColor,
			int customColor) {
		ClientLevel level = client.level;
		LocalPlayer player = client.player;

		if (level == null || player == null) {
			return;
		}

		ACTIVE.removeIf(ping -> target.isEntity() && ping.entityId() == target.entityId());
		ACTIVE.add(new ActivePing(target.entityId(), target.pos(), target.block(), serverColor, customColor,
				clientTick + PingConfig.get().lifetimeTicks()));

		if (PingConfig.get().soundEnabled && shouldHear(level, player, sender)) {
			// Non-positional: played at the listener's own ears, so nobody hears anyone else's ping from afar.
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6f, 0.5f));
		}
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

	private static int outgoingColor() {
		PingConfig config = PingConfig.get();
		return config.useServerColor ? PingPing.AUTO_COLOR : PingPing.sanitiseColor(config.customColor);
	}

	public static List<ActivePing> active() {
		return ACTIVE;
	}

	/**
	 * Called from the pick-block hook. Returns {@code true} when the click was consumed as a ping and vanilla
	 * pick block should be skipped.
	 */
	public static boolean tryPing(boolean sneaking) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		ClientLevel level = client.level;

		if (player == null || level == null) {
			return false;
		}

		PingConfig config = PingConfig.get();

		// Sneak is the block gesture: it marks whatever block is being aimed at and nothing else.
		if (sneaking) {
			PingTarget block = worldTarget(player, level, true);

			if (block != null) {
				ClientPlayNetworking.send(new PingRequestPayload(block, outgoingColor()));
			}

			return true;
		}

		Entity entity = findEntity(player, level);

		if (entity != null) {
			ClientPlayNetworking.send(
					new PingRequestPayload(PingTarget.ofEntity(entity.getId(), entity.position()), outgoingColor()));
			return true;
		}

		// Nothing alive under the crosshair, so pick block gets its turn before we fall back to a bare marker.
		if (config.pickBlockWins && vanillaPickWouldWork(client, player)) {
			return false;
		}

		PingTarget fallback = config.blockPingNeedsSneak
				? (config.freePointPing ? worldTarget(player, level, false) : null)
				: worldTarget(player, level, true);

		if (fallback != null) {
			ClientPlayNetworking.send(new PingRequestPayload(fallback, outgoingColor()));
		}

		return true;
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
	private static PingTarget worldTarget(LocalPlayer player, ClientLevel level, boolean asBlock) {
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
				: blockHit.getLocation());
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
