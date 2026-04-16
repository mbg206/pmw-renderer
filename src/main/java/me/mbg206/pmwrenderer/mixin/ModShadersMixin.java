package me.mbg206.pmwrenderer.mixin;

import dev.protomanly.pmweather.shaders.ModShaders;
import me.mbg206.pmwrenderer.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModShaders.class)
public class ModShadersMixin {
    @Inject(method="renderShaders", at=@At("HEAD"), cancellable = true)
    private static void disableShaderRendering(CallbackInfo ci) {
        if (Config.renderMode != Config.RenderMode.SHADER) {
            ci.cancel();
        }
    }
}
