package me.mbg206.pmwrenderer.mixin;

import dev.protomanly.pmweather.config.ClientConfig;
import me.mbg206.pmwrenderer.Config;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method="getDepthFar", at=@At("RETURN"), cancellable = true)
    public void editDepthFar(CallbackInfoReturnable<Float> ci) {
        if (Config.renderMode == Config.RenderMode.CLASSIC) {
            ci.setReturnValue((float) (ClientConfig.maxParticleSpawnDistanceFromPlayer + 256));
        }
    }
}
