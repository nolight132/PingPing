package dev.nolight.pingping.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/** Picks the little icon shown next to a marker. */
public final class PingIcons {
	private PingIcons() {
	}

	public static ItemStack forEntity(Entity entity) {
		if (entity instanceof ItemEntity item) {
			return item.getItem();
		}

		return SpawnEggItem.byId(entity.getType())
				.map(ItemStack::new)
				.orElse(ItemStack.EMPTY);
	}
}
