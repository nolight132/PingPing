package dev.nolight.pingping.client;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class PingSounds {
	public static final SoundEvent FALLBACK = SoundEvents.AMETHYST_BLOCK_HIT;

	public static final List<String> SUGGESTED = List.of(
			"minecraft:block.amethyst_block.hit",
			"minecraft:block.amethyst_block.chime",
			"minecraft:block.note_block.pling",
			"minecraft:block.note_block.bell",
			"minecraft:block.note_block.bit",
			"minecraft:entity.arrow.hit_player",
			"minecraft:entity.experience_orb.pickup",
			"minecraft:entity.item.pickup",
			"minecraft:block.copper_bulb.turn_on",
			"minecraft:block.beacon.power_select",
			"minecraft:block.conduit.attack.target",
			"minecraft:item.trident.return",
			"minecraft:block.lever.click",
			"minecraft:ui.button.click");

	private PingSounds() {
	}

	public static SoundEvent resolve(String id) {
		if (id == null || id.isBlank()) {
			return FALLBACK;
		}

		Identifier parsed = Identifier.tryParse(id);
		return parsed == null ? FALLBACK : BuiltInRegistries.SOUND_EVENT.getOptional(parsed).orElse(FALLBACK);
	}
}
