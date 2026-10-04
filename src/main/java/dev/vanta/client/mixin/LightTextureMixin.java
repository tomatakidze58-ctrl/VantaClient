package dev.vanta.client.mixin;

import dev.vanta.client.VantaClient;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Inject(method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vanta$fullbright(CallbackInfoReturnable<Float> cir) {
        if (VantaClient.MODULES.on("Fullbright")) cir.setReturnValue(1.0F);
    }

    @Inject(method = "getBrightness(FI)F", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vanta$fullbrightAmbient(CallbackInfoReturnable<Float> cir) {
        if (VantaClient.MODULES.on("Fullbright")) cir.setReturnValue(1.0F);
    }
}
