package dev.nolight.pingping;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client asks the server to mark an entity or a spot in the world. {@code color} is the sender's preference, or
 * {@link PingPing#AUTO_COLOR} to let the server choose.
 */
public record PingRequestPayload(PingTarget target, int color) implements CustomPacketPayload {
	public static final Type<PingRequestPayload> TYPE = new Type<>(PingPing.id("ping_request"));

	public static final StreamCodec<ByteBuf, PingRequestPayload> CODEC = StreamCodec.composite(
			PingTarget.CODEC, PingRequestPayload::target,
			ByteBufCodecs.INT, PingRequestPayload::color,
			PingRequestPayload::new);

	@Override
	public Type<PingRequestPayload> type() {
		return TYPE;
	}
}
