package me.mbg206.pmwrenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = PMWRendering.MODID, value = Dist.CLIENT)
public class Config {
    public enum RenderMode {
        SHADER,
        CLASSIC,
        DISABLE
    }

    public enum BleedFixMode {
        FULL,
        LIGHT,
        DISABLE
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.EnumValue<RenderMode> RENDER_MODE = BUILDER
            .translation("config.render_mode")
            .comment("Which mode to use for rendering clouds.\n\nSHADER: Use the original mod's shaders (no change to cloud rendering)\nCLASSIC: Use this mod's particle rendering system\nDISABLE: Disable all cloud rendering")
            .defineEnum("render_mode", RenderMode.SHADER);
    public static RenderMode renderMode;

    private static final ModConfigSpec.BooleanValue ENABLE_MIST = BUILDER
            .translation("config.enable_mist")
            .comment("Enables/disables mist particles in heavy rain")
            .define("enable_mist", true);
    public static boolean enableMist;

    private static final ModConfigSpec.BooleanValue ENABLE_SPLASH = BUILDER
            .translation("config.enable_splash")
            .comment("Enables/disables rain splash particles")
            .define("enable_splash", true);
    public static boolean enableSplash;

    private static final ModConfigSpec.BooleanValue ENHANCED_FOG = BUILDER
            .translation("config.enhanced_fog")
            .comment("Enables more intense fog/darkness effects for precipitation. Does nothing on SHADER rendering mode")
            .define("enhanced_fog", true);
    public static boolean enhancedFog;

    private static final ModConfigSpec.IntValue UPDATE_EVERY_TICKS = BUILDER
            .translation("config.update_every_ticks")
            .comment("Rate at which storms and cloud map are updated")
            .defineInRange("update_every_ticks", 200, 40, 400);
    public static int updateEveryTicks;

    private static final ModConfigSpec.IntValue PARTICLE_CROSSOVER = BUILDER
            .translation("config.particle_crossover")
            .comment("Crossover ticks for particle fade/switchover. Should only need modified if using a resource pack")
            .defineInRange("particle_crossover", 60, 1, 100);
    public static int particleCrossover;

    private static final ModConfigSpec.EnumValue<BleedFixMode> FIX_BLEED = BUILDER
            .translation("config.fix_bleed")
            .comment("Fixes bleeding issues on SHADER rendering mode. Disables shader culling (major performance hit! may be reworked in the future...)\n\nFULL: Disables all culling, biggest performance hit\nLIGHT: Keeps solid block culling, only fixes bleeding around small blocks/entities/beacons/etc.\nDISABLE: No changes to PMW culling behavior")
            .defineEnum("bleed_fix_mode", BleedFixMode.DISABLE);
    public static BleedFixMode bleedFixMode;

    static final ModConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        renderMode = RENDER_MODE.get();
        enableMist = ENABLE_MIST.get();
        enableSplash = ENABLE_SPLASH.get();
        enhancedFog = ENHANCED_FOG.get();
        updateEveryTicks = UPDATE_EVERY_TICKS.get();
        particleCrossover = PARTICLE_CROSSOVER.get();
        bleedFixMode = FIX_BLEED.get();
    }
}
