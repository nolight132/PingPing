package dev.nolight.pingping;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server tells everyone that {@code sender} marked {@code entityId}. */
public record PingBroadcastPayload(int entityId, UUID sender) implements CustomPacketPayload {
	public static final Type<PingBroadcastPayload> TYPE = new Type<>(PingPing.id("ping_broadcast"));

	public static final StreamCodec<ByteBuf, PingBroadcastPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PingBroadcastPayload::entityId,
			UUIDUtil.STREAM_CODEC, PingBroadcastPayload::sender,
			PingBroadcastPayload::new);

	@Override
	public Type<PingBroadcastPayload> type() {
		return TYPE;
	}
}
