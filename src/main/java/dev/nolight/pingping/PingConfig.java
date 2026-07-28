package dev.nolight.pingping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Everything tunable, in {@code config/pingping.json}. The limits (distance, charges, lifetime) are enforced by
 * whichever side runs the logical server; the rest is per-client decoration.
 */
public final class PingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static PingConfig instance;

	// --- limits, read by the logical server ---
	public double maxDistance = 256.0;
	public double lifetimeSeconds = 4.0;
	public int maxCharges = 5;
	public double refillSeconds = 5.0;

	// --- targeting, client side ---
	public double snapConeDegrees = 6.0;
	public boolean pickBlockWins = true;

	/** When true a block can only be marked while sneaking; a plain click then only ever finds entities. */
	public boolean blockPingNeedsSneak = true;

	/** Sneak-marked blocks sit at the block's centre instead of the exact spot the ray struck. */
	public boolean snapBlockToCentre = true;

	/** Without sneak and with no entity in sight, drop a marker on the precise spot being aimed at. */
	public boolean freePointPing = true;

	// --- presentation, client side ---
	public boolean soundEnabled = true;
	public double soundRadius = 48.0;
	public boolean showIcons = true;
	public boolean showEdgeArrows = true;
	public double markerScale = 0.75;

	/** Edge length in pixels of the preview above a marker. */
	public int previewSize = 12;

	public static PingConfig get() {
		if (instance == null) {
			instance = load();
		}

		return instance;
	}

	public int lifetimeTicks() {
		return (int) Math.max(1, Math.round(lifetimeSeconds * 20.0));
	}

	/** Ticks to regenerate a single charge, derived from how long a full bucket takes to come back. */
	public int chargeRefillTicks() {
		return (int) Math.max(1, Math.round(refillSeconds * 20.0 / Math.max(1, maxCharges)));
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("pingping.json");
	}

	private static PingConfig load() {
		Path path = path();

		if (Files.exists(path)) {
			try {
				PingConfig loaded = GSON.fromJson(Files.readString(path), PingConfig.class);

				if (loaded != null) {
					return loaded;
				}
			} catch (IOException | RuntimeException e) {
				PingPing.LOGGER.warn("[pingping] could not read {}, falling back to defaults", path, e);
			}
		}

		PingConfig fresh = new PingConfig();
		fresh.save();
		return fresh;
	}

	public void save() {
		try {
			Files.createDirectories(path().getParent());
			Files.writeString(path(), GSON.toJson(this));
		} catch (IOException e) {
			PingPing.LOGGER.warn("[pingping] could not write {}", path(), e);
		}
	}
}
