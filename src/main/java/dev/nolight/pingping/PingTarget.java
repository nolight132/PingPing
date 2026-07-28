package dev.nolight.pingping;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * What a ping points at: an entity the marker follows, a whole block, or a bare spot in the world. The three are
 * distinct because only a block has something sensible to preview.
 */
public record PingTarget(int entityId, Vec3 pos, boolean block) {
	public static final int NO_ENTITY = -1;

	public static final StreamCodec<ByteBuf, PingTarget> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, target -> target.entityId() + 1,
			Vec3.STREAM_CODEC, PingTarget::pos,
			ByteBufCodecs.BOOL, PingTarget::block,
			(id, pos, block) -> new PingTarget(id - 1, pos, block));

	public static PingTarget ofEntity(int entityId, Vec3 pos) {
		return new PingTarget(entityId, pos, false);
	}

	public static PingTarget ofBlock(Vec3 pos) {
		return new PingTarget(NO_ENTITY, pos, true);
	}

	public static PingTarget ofPoint(Vec3 pos) {
		return new PingTarget(NO_ENTITY, pos, false);
	}

	public boolean isEntity() {
		return entityId != NO_ENTITY;
	}
}
