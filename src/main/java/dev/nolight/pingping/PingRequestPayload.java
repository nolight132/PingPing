package dev.nolight.pingping;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PingRequestPayload(PingTarget target) implements CustomPacketPayload {
	public static final Type<PingRequestPayload> TYPE = new Type<>(PingPing.id("ping_request"));

	public static final StreamCodec<ByteBuf, PingRequestPayload> CODEC = StreamCodec.composite(
			PingTarget.CODEC, PingRequestPayload::target,
			PingRequestPayload::new);

	@Override
	public Type<PingRequestPayload> type() {
		return TYPE;
	}
}
