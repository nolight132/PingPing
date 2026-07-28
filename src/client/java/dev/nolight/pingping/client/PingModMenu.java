package dev.nolight.pingping.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class PingModMenu implements ModMenuApi {
	private static final String YACL = "yet_another_config_lib_v3";

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		// YACL is client-only, so it cannot be a hard dependency without locking the mod out of dedicated servers.
		// Without it there is simply no settings button; the config file still works.
		if (!FabricLoader.getInstance().isModLoaded(YACL)) {
			return screen -> null;
		}

		return screen -> PingConfigScreen.create(screen);
	}
}
