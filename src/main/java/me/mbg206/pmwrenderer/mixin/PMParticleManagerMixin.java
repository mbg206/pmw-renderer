package me.mbg206.pmwrenderer.mixin;

import dev.protomanly.pmweather.particle.ParticleManager;
import me.mbg206.pmwrenderer.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ParticleManager.class)
public class PMParticleManagerMixin {
    @ModifyArg(method="render", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd(F)V"))
    public float setShaderFogEnd(float end) {
        if (Config.renderMode == Config.RenderMode.CLASSIC) {
            return end * 2F;
        }

        return end;
    }
}
