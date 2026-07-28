package dev.nolight.pingping.client;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.nolight.pingping.PingConfig;
import net.minecraft.client.gui.screens.Screen;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

/** The settings screen, built with YACL so it matches the look of Sodium and friends. */
public final class PingConfigScreen {
	private static final PingConfig DEFAULTS = new PingConfig();

	private PingConfigScreen() {
	}

	public static Screen create(Screen parent) {
		PingConfig config = PingConfig.get();

		return YetAnotherConfigLib.createBuilder()
				.title(text("title"))
				.category(targeting(config))
				.category(appearance(config))
				.category(limits(config))
				.save(config::save)
				.build()
				.generateScreen(parent);
	}

	private static ConfigCategory targeting(PingConfig config) {
		return ConfigCategory.createBuilder()
				.name(text("category.targeting"))
				.group(OptionGroup.createBuilder()
						.name(text("group.blocks"))
						.option(bool("block_needs_sneak", DEFAULTS.blockPingNeedsSneak, () -> config.blockPingNeedsSneak,
								value -> config.blockPingNeedsSneak = value))
						.option(bool("snap_block", DEFAULTS.snapBlockToCentre, () -> config.snapBlockToCentre,
								value -> config.snapBlockToCentre = value))
						.option(bool("free_point", DEFAULTS.freePointPing, () -> config.freePointPing,
								value -> config.freePointPing = value))
						.build())
				.group(OptionGroup.createBuilder()
						.name(text("group.entities"))
						.option(doubleSlider("snap_cone", DEFAULTS.snapConeDegrees, () -> config.snapConeDegrees,
								value -> config.snapConeDegrees = value, 0.0, 20.0, 0.5))
						.option(doubleSlider("max_distance", DEFAULTS.maxDistance, () -> config.maxDistance,
								value -> config.maxDistance = value, 16.0, 512.0, 16.0))
						.option(bool("pick_block_wins", DEFAULTS.pickBlockWins, () -> config.pickBlockWins,
								value -> config.pickBlockWins = value))
						.build())
				.build();
	}

	private static ConfigCategory appearance(PingConfig config) {
		return ConfigCategory.createBuilder()
				.name(text("category.appearance"))
				.group(OptionGroup.createBuilder()
						.name(text("group.marker"))
						.option(doubleSlider("marker_scale", DEFAULTS.markerScale, () -> config.markerScale,
								value -> config.markerScale = value, 0.25, 2.0, 0.05))
						.option(intSlider("preview_size", DEFAULTS.previewSize, () -> config.previewSize,
								value -> config.previewSize = value, 6, 32, 1))
						.option(bool("show_icons", DEFAULTS.showIcons, () -> config.showIcons, value -> config.showIcons = value))
						.option(bool("edge_arrows", DEFAULTS.showEdgeArrows, () -> config.showEdgeArrows,
								value -> config.showEdgeArrows = value))
						.build())
				.group(OptionGroup.createBuilder()
						.name(text("group.sound"))
						.option(bool("sound", DEFAULTS.soundEnabled, () -> config.soundEnabled, value -> config.soundEnabled = value))
						.option(doubleSlider("sound_radius", DEFAULTS.soundRadius, () -> config.soundRadius,
								value -> config.soundRadius = value, 0.0, 256.0, 8.0))
						.build())
				.build();
	}

	private static ConfigCategory limits(PingConfig config) {
		return ConfigCategory.createBuilder()
				.name(text("category.limits"))
				.group(OptionGroup.createBuilder()
						.name(text("group.limits"))
						.description(OptionDescription.of(text("group.limits.note")))
						.option(doubleSlider("lifetime", DEFAULTS.lifetimeSeconds, () -> config.lifetimeSeconds,
								value -> config.lifetimeSeconds = value, 1.0, 30.0, 0.5))
						.option(intSlider("max_charges", DEFAULTS.maxCharges, () -> config.maxCharges,
								value -> config.maxCharges = value, 1, 20, 1))
						.option(doubleSlider("refill", DEFAULTS.refillSeconds, () -> config.refillSeconds,
								value -> config.refillSeconds = value, 1.0, 60.0, 1.0))
						.build())
				.build();
	}

	private static Option<Boolean> bool(String key, boolean fallback, Supplier<Boolean> getter,
			Consumer<Boolean> setter) {
		return Option.<Boolean>createBuilder()
				.name(text("option." + key))
				.description(OptionDescription.of(text("option." + key + ".desc")))
				.binding(fallback, getter, setter)
				.controller(option -> BooleanControllerBuilder.create(option).coloured(true).yesNoFormatter())
				.build();
	}

	private static Option<Double> doubleSlider(String key, double fallback, Supplier<Double> getter,
			Consumer<Double> setter, double min, double max, double step) {
		return Option.<Double>createBuilder()
				.name(text("option." + key))
				.description(OptionDescription.of(text("option." + key + ".desc")))
				.binding(fallback, getter, setter)
				.controller(option -> DoubleSliderControllerBuilder.create(option).range(min, max).step(step))
				.build();
	}

	private static Option<Integer> intSlider(String key, int fallback, Supplier<Integer> getter,
			Consumer<Integer> setter, int min, int max, int step) {
		return Option.<Integer>createBuilder()
				.name(text("option." + key))
				.description(OptionDescription.of(text("option." + key + ".desc")))
				.binding(fallback, getter, setter)
				.controller(option -> IntegerSliderControllerBuilder.create(option).range(min, max).step(step))
				.build();
	}

	private static Component text(String key) {
		return Component.translatable("pingping.options." + key);
	}
}
