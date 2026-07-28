package dev.nolight.pingping.client;

import net.fabricmc.api.ClientModInitializer;

public class PingPingClient implements ClientModInitializer {
	@Override
	public void onInitialize() {
		ClientPings.register();
		PingMarkerRenderer.register();
	}
}
