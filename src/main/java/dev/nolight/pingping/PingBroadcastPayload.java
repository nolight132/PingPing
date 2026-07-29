package dev.nolight.pingping;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PingBroadcastPayload(PingTarget target, UUID sender, int serverColor, int customColor)
		implements
			CustomPacketPayload {
	public static final Type<PingBroadcastPayload> TYPE = new Type<>(PingPing.id("ping_broadcast"));

	public static final StreamCodec<ByteBuf, PingBroadcastPayload> CODEC = StreamCodec.composite(
			PingTarget.CODEC, PingBroadcastPayload::target,
			UUIDUtil.STREAM_CODEC, PingBroadcastPayload::sender,
			ByteBufCodecs.INT, PingBroadcastPayload::serverColor,
			ByteBufCodecs.INT, PingBroadcastPayload::customColor,
			PingBroadcastPayload::new);

	@Override
	public Type<PingBroadcastPayload> type() {
		return TYPE;
	}
}
