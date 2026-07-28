package dev.nolight.pingping.client.mixin;

import dev.nolight.pingping.client.ClientPings;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftPickMixin {
	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
	private void pingping$pingInsteadOfPick(CallbackInfo ci) {
		Minecraft client = (Minecraft) (Object) this;
		boolean override = client.player != null && client.player.isShiftKeyDown();

		if (ClientPings.tryPing(override)) {
			ci.cancel();
		}
	}
}
