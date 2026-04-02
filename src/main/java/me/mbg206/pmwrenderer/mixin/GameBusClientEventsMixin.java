package me.mbg206.pmwrenderer.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WindEngine;
import me.mbg206.pmwrenderer.Config;
import me.mbg206.pmwrenderer.renderer.ClassicRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameBusClientEvents.class)
public class GameBusClientEventsMixin {
    @Shadow
    public static WeatherHandler weatherHandler;

    @Inject(method="fogEvent", at=@At("HEAD"), cancellable=true)
    private static void fogEvent(ViewportEvent.RenderFog event, CallbackInfo ci) {
        if (Config.renderMode == Config.RenderMode.SHADER) {
            return;
        }

        if (Config.enhancedFog) {
            ci.cancel();
            float fog = ClassicRenderer.getFog((float) event.getPartialTick());
            float mul = 1F - fog;

            RenderSystem.setShaderFogStart(Math.max(RenderSystem.getShaderFogStart() * mul, 20F));
            RenderSystem.setShaderFogEnd(Math.max(RenderSystem.getShaderFogEnd() * mul, 40F));

        }
    }

    // disable mist
    @Redirect(method="doRainParticles", at=@At(value="INVOKE", target="Ldev/protomanly/pmweather/weather/WindEngine;getWind(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/Level;ZZZZ)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 getWind(Vec3 position, Level level, boolean ignoreStorms, boolean ignoreTornadoes, boolean windCheck, boolean windAnyway) {
        if (Config.enableMist) {
            return WindEngine.getWind(position, level, ignoreStorms, ignoreTornadoes, windCheck, windAnyway);
        }
        return new Vec3(0, 0, 0);
    }

    // disable splash
    @Inject(method="doRainParticles", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"), cancellable = true)
    private static void setSpawnAreaSize(CallbackInfo ci) {
        if (!Config.enableSplash) {
            ci.cancel();
        }
    }
}
