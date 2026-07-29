package dev.nolight.pingping;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PingColorPayload(int color) implements CustomPacketPayload {
	public static final Type<PingColorPayload> TYPE = new Type<>(PingPing.id("ping_color"));

	public static final StreamCodec<ByteBuf, PingColorPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, PingColorPayload::color,
			PingColorPayload::new);

	@Override
	public Type<PingColorPayload> type() {
		return TYPE;
	}
}
