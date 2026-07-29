package dev.nolight.pingping.client;

import net.fabricmc.api.ClientModInitializer;

public class PingPingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPings.register();
		PingKeys.register();
		CameraCapture.register();
		PingHud.register();
	}
}
