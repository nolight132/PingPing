package dev.nolight.pingping.client;

import dev.nolight.pingping.PingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/** Vanilla-looking settings page, reachable from Mod Menu. */
public class PingOptionsScreen extends OptionsSubScreen {
	private final PingConfig config = PingConfig.get();

	public PingOptionsScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("pingping.options.title"));
	}

	@Override
	protected void addOptions() {
		this.list.addSmall(
				OptionInstance.createBoolean("pingping.options.pick_block_wins", config.pickBlockWins,
						value -> config.pickBlockWins = value),
				OptionInstance.createBoolean("pingping.options.sound", config.soundEnabled,
						value -> config.soundEnabled = value),
				OptionInstance.createBoolean("pingping.options.icons", config.showIcons,
						value -> config.showIcons = value),
				OptionInstance.createBoolean("pingping.options.edge_arrows", config.showEdgeArrows,
						value -> config.showEdgeArrows = value));
	}

	@Override
	public void removed() {
		config.save();
		super.removed();
	}

	@Override
	public void onClose() {
		config.save();
		super.onClose();
	}
}
