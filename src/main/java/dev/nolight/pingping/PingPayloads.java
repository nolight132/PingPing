package dev.nolight.pingping;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PingPayloads {
	private PingPayloads() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(PingRequestPayload.TYPE, PingRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(PingBroadcastPayload.TYPE, PingBroadcastPayload.CODEC);
	}
}
