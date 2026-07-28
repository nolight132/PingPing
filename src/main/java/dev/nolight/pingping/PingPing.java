package dev.nolight.pingping;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPing implements ModInitializer {
	public static final String MOD_ID = "pingping";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** How long a ping stays on screen. */
	public static final int PING_LIFETIME_TICKS = 80;

	/** Pings a player may fire back to back before the bucket runs dry. */
	public static final int MAX_CHARGES = 5;

	/** Ticks needed to regenerate one charge, so an emptied bucket is full again after five seconds. */
	public static final int CHARGE_REFILL_TICKS = 20;

	/** Hard floor between two pings, so a single burst cannot be dumped in one tick. */
	public static final int MIN_PING_INTERVAL_TICKS = 2;

	/** Maximum distance at which a player can mark an entity. */
	public static final double MAX_PING_DISTANCE = 64.0;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PingPayloads.register();
		PingServer.register();
		LOGGER.info("[pingping] main entrypoint initialised");
	}
}
