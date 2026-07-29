package dev.nolight.pingping.client.mixin;

import dev.nolight.pingping.client.ClientPings;
import dev.nolight.pingping.client.PingKeys;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftPickMixin {
	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
	private void pingping$pingInsteadOfPick(CallbackInfo ci) {
		if (PingKeys.replacesPickBlock() && ClientPings.tryPing(PingKeys.blockRequested())) {
			ci.cancel();
		}
	}
}
