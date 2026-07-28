package dev.nolight.pingping;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * What a ping points at. {@link #entityId()} is {@link #NO_ENTITY} for a plain world position, otherwise the
 * marker follows that entity and {@code pos} is only the fallback used until the entity is known on a client.
 */
public record PingTarget(int entityId, Vec3 pos) {
	public static final int NO_ENTITY = -1;

	public static final StreamCodec<ByteBuf, PingTarget> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, target -> target.entityId() + 1,
			Vec3.STREAM_CODEC, PingTarget::pos,
			(id, pos) -> new PingTarget(id - 1, pos));

	public static PingTarget ofEntity(int entityId, Vec3 pos) {
		return new PingTarget(entityId, pos);
	}

	public static PingTarget ofPosition(Vec3 pos) {
		return new PingTarget(NO_ENTITY, pos);
	}

	public boolean isEntity() {
		return entityId != NO_ENTITY;
	}
}
