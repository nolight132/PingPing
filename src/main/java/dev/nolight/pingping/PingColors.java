package dev.nolight.pingping;

import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;

public final class PingColors {
	private PingColors() {
	}

	public static int locatorBar(LivingEntity entity) {
		return entity.waypointIcon().cloneAndAssignStyle(entity).color
				.orElseGet(() -> ARGB.setBrightness(ARGB.color(255, entity.getUUID().hashCode()), 0.9f))
				& 0xFFFFFF;
	}
}
