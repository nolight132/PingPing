package dev.nolight.pingping.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nolight.pingping.PingPing;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class PingKeys {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(PingPing.id("main"));

	private static final String YACL = "yet_another_config_lib_v3";

	private static KeyMapping ping;
	private static KeyMapping pingBlock;
	private static KeyMapping settings;

	private PingKeys() {
	}

	public static void register() {
		ping = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.pingping.ping", InputConstants.Type.MOUSE, 2, CATEGORY));
		pingBlock = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.pingping.ping_block", InputConstants.UNKNOWN.getValue(), CATEGORY));
		settings = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.pingping.settings", InputConstants.UNKNOWN.getValue(), CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(PingKeys::tick);
	}

	public static boolean replacesPickBlock() {
		Minecraft client = Minecraft.getInstance();
		return ping != null && !ping.isUnbound() && ping.same(client.options.keyPickItem);
	}

	public static boolean forceBlock() {
		return pingBlock != null && pingBlock.isDown();
	}

	public static boolean sneaking() {
		Minecraft client = Minecraft.getInstance();
		return client.player != null && client.player.isShiftKeyDown();
	}

	private static void tick(Minecraft client) {
		while (settings.consumeClick()) {
			if (FabricLoader.getInstance().isModLoaded(YACL)) {
				client.setScreenAndShow(PingConfigScreen.create(null));
			}
		}

		if (client.player == null || client.level == null) {
			drain(ping);
			drain(pingBlock);
			return;
		}

		if (pingBlock.same(ping)) {
			drain(pingBlock);
		} else {
			while (pingBlock.consumeClick()) {
				ClientPings.tryPing(true, sneaking());
			}
		}

		if (replacesPickBlock()) {
			drain(ping);
		} else {
			while (ping.consumeClick()) {
				ClientPings.tryPing(forceBlock(), sneaking());
			}
		}
	}

	private static void drain(KeyMapping key) {
		while (key.consumeClick()) {
		}
	}
}
