package me.mbg206.pmwrenderer.renderer;

import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.seasons.SeasonHandler;
import dev.protomanly.pmweather.util.ShaderCompatibleNoise;
import dev.protomanly.pmweather.weather.WeatherHandler;
import me.mbg206.pmwrenderer.Config;
import me.mbg206.pmwrenderer.PMWRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Vector2f;
import org.joml.Vector3f;

@EventBusSubscriber(modid= PMWRendering.MODID, value= Dist.CLIENT)
public class ClassicRenderer {
    private static ParticleManager particleManager;
    public static ParticleManager getParticleManager() {
        return particleManager;
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        Level level = (Level) event.getLevel();
        if (!level.isClientSide()) {
            return; // is this ever reached?
        }
        if (ServerConfig.validDimensions.contains(level.dimension())) {
            particleManager = new ParticleManager((ClientLevel) level);
            particleManager.start();
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        particleManager = null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }

        if (particleManager != null) {
            particleManager.tick();
        }
    }


    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent event) {
        if (particleManager == null) {
            return;
        }

        particleManager.stop();
        GameBusClientEvents.particleManager.clearParticles();

        if (Config.renderMode == Config.RenderMode.CLASSIC) {
            particleManager.start();
        }
    }

    public static float getFog(float partialTick) {
        if (particleManager == null) {
            return 0F;
        }

        return particleManager.getFog(partialTick);
    }
    public static float getSkyDarken(float partialTick) {
        if (particleManager == null) {
            return 0F;
        }

        return particleManager.getSkyDarken(partialTick);
    }


    // dev.protomanly.pmweather.shaders.ModShaders.renderShaders()
    public static Vec3 getCloudColor() {
        float sunAng = Minecraft.getInstance().level.getSunAngle(0);
        double sunY = Math.cos(sunAng);
        return (new Vec3(1.0, 0.992, 0.957)).scale(1.05)
            .lerp(new Vec3(0.741, 0.318, 0.227), Math.pow(1.0 - sunY, 2.5))
            .lerp(new Vec3(0.314, 0.408, 0.525), Math.clamp((sunY + 0.1) / -0.1, 0.0, 1.0));
    }

    // dev.protomanly.pmweather.weather.Clouds.getCloudDensity()
    public static float getBasicCloudDensity(WeatherHandler weatherHandler, float x, float z) {
        float c = 0F;
        long seed = weatherHandler.seed;
        float worldTime = (float)weatherHandler.getWorld().getGameTime() + (float)seed / 1.0E14F;
        Vector3f noisePos = new Vector3f(x + worldTime, (float)ServerConfig.layer0Height, z + worldTime);
        Vector3f cloudNoisePos = new Vector3f(x + worldTime * 0.5F, (float)ServerConfig.layer0Height, z + worldTime * 0.5F);
        Vector3f overrideNoisePos = new Vector3f(x + worldTime * 0.25F, (float)ServerConfig.layer0Height, z + worldTime * 0.25F);
        float overrideNoise = Math.clamp(ShaderCompatibleNoise.noise2D((new Vector2f(overrideNoisePos.x, overrideNoisePos.z)).div(200.0F)) + 1.0F, 0.0F, 2.0F) / 2.0F;
        float densityNoise = Math.min(ShaderCompatibleNoise.noise2D((new Vector2f(cloudNoisePos.x, cloudNoisePos.z)).div(400.0F)), 1.0F);
        float cloudNoise = Math.min(ShaderCompatibleNoise.noise2D((new Vector2f(noisePos.x, noisePos.z)).div(30.0F)), 1.0F);
        float heightNoise = ShaderCompatibleNoise.noise2D((new Vector2f(noisePos.x, noisePos.z)).div(90.0F));
        float bgCloudHeight = Mth.lerp(Math.clamp((heightNoise + 1.0F) * 0.5F, 0.0F, 1.0F), 300.0F, 850.0F);
        float seasonEffect = SeasonHandler.getSeasonEffectSine(weatherHandler.getWorld(), 3.5F) + 1.0F;
        double overcastDampen = (double)seasonEffect * 0.15;
        float overcastP = (float)Math.max(ServerConfig.overcastPercent - overcastDampen, 0.0);
        float v = Math.clamp(densityNoise - (1.0F - overcastP), 0.0F, 1.0F);
        c += (float)Math.max(Math.sqrt(Math.sqrt((double)v)), (double)0.0F);
        c *= Mth.lerp(v, Math.clamp(cloudNoise - 0.1F + v, 0.0F, 1.0F), 1.0F);
        c = (float)Math.sqrt(c) * 0.5F;
        c *= Mth.lerp((float)Math.sqrt(v), bgCloudHeight / 850.0F, 1.0F);
        c *= overrideNoise;

        float m = (float)Math.max(ServerConfig.overcastPercent - overcastDampen, 0.0);
        return Mth.clamp(c * m, 0.0F, 1.0F);
    }
}
