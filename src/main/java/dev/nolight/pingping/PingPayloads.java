package dev.nolight.pingping;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PingPayloads {
	private PingPayloads() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(PingRequestPayload.TYPE, PingRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PingBroadcastPayload.TYPE, PingBroadcastPayload.CODEC);
	}
}
