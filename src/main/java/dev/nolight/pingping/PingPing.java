package dev.nolight.pingping;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPing implements ModInitializer {
	public static final String MOD_ID = "pingping";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Hard floor between two pings, so a single burst cannot be dumped in one tick. */
	public static final int MIN_PING_INTERVAL_TICKS = 2;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PingPayloads.register();
		PingServer.register();
	}
}
