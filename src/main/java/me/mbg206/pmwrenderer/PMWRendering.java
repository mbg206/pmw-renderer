package me.mbg206.pmwrenderer;

import com.mojang.logging.LogUtils;
import dev.protomanly.pmweather.addons.AddonHelper;
import dev.protomanly.pmweather.addons.AddonInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(PMWRendering.MODID)
@OnlyIn(Dist.CLIENT)
public class PMWRendering {
    public static final String MODID = "pmwrenderer";
    private static final Logger LOGGER = LogUtils.getLogger();


    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public PMWRendering(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        AddonHelper.registerAddon(new AddonInfo(modContainer, List.of("0.16")));
    }


}
