package dev.nolight.pingping;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client asks the server to mark an entity. */
public record PingRequestPayload(int entityId) implements CustomPacketPayload {
	public static final Type<PingRequestPayload> TYPE = new Type<>(PingPing.id("ping_request"));

	public static final StreamCodec<ByteBuf, PingRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PingRequestPayload::entityId,
			PingRequestPayload::new);

	@Override
	public Type<PingRequestPayload> type() {
		return TYPE;
	}
}
