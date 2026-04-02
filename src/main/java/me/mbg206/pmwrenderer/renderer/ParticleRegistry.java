package me.mbg206.pmwrenderer.renderer;

import me.mbg206.pmwrenderer.PMWRendering;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = PMWRendering.MODID, value= Dist.CLIENT)
public class ParticleRegistry extends SpriteSourceProvider {
    private static final String ER_MOD_ID = "coroutil";

    public static TextureAtlasSprite cloudTexture;
    private static final ResourceLocation cloudTextureR = ResourceLocation.fromNamespaceAndPath(ER_MOD_ID, "particle/cloud256");

    public ParticleRegistry(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, PMWRendering.MODID, existingFileHelper);
    }

    private void addSprite(ResourceLocation location) {
        this.atlas(SpriteSourceProvider.PARTICLES_ATLAS).addSource(new SingleFile(location, Optional.empty()));
    }

    @Override
    protected void gather() {
        this.addSprite(cloudTextureR);
    }


    @SubscribeEvent
    public static void getRegisteredParticles(TextureAtlasStitchedEvent event) {
        if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)) {
            return;
        }

        ParticleRegistry.cloudTexture = event.getAtlas().getSprite(cloudTextureR);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        gen.addProvider(event.includeClient(), new ParticleRegistry(packOutput, event.getLookupProvider(), existingFileHelper));
    }

}
