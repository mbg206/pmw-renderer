package me.mbg206.pmwrenderer.mixin;

import me.mbg206.pmwrenderer.Config;
import me.mbg206.pmwrenderer.renderer.ClassicRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientLevel.class, priority = 1)
public class ClientLevelMixin {
    @Inject(method="getSkyDarken", at=@At("RETURN"), cancellable = true)
    public void editSkyDarken(float partialTick, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (Config.renderMode == Config.RenderMode.SHADER) {
            return;
        }

        if (Config.enhancedFog) {
            float darken = ClassicRenderer.getSkyDarken(partialTick);
            callbackInfoReturnable.setReturnValue(callbackInfoReturnable.getReturnValue() * (1.0F - darken));
        }
    }
}
