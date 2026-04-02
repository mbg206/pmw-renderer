package me.mbg206.pmwrenderer.mixin;

import dev.protomanly.pmweather.render.RenderEvents;
import dev.protomanly.pmweather.shaders.ModShaders;
import me.mbg206.pmwrenderer.Config;
import net.minecraft.client.Camera;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderEvents.class)
public class RenderEventsMixin {
    @Redirect(method="render", at=@At(value="INVOKE", target="Ldev/protomanly/pmweather/shaders/ModShaders;renderShaders(FLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
    private static void disableShaderRendering(float depthTextureId, Camera dhProj, Matrix4f dhProjInv, Matrix4f e) {
        if (Config.renderMode == Config.RenderMode.SHADER) {
            ModShaders.renderShaders(depthTextureId, dhProj, dhProjInv, e);
        }
    }



    @Redirect(method="render", at=@At(value="FIELD", target="Lnet/neoforged/neoforge/client/event/RenderLevelStageEvent$Stage;AFTER_WEATHER:Lnet/neoforged/neoforge/client/event/RenderLevelStageEvent$Stage;", opcode= Opcodes.GETSTATIC))
    private static RenderLevelStageEvent.Stage getTargetStage() {
        //Tesselator.getInstance().clear();
        if (Config.bleedFixMode != Config.BleedFixMode.DISABLE && Config.renderMode == Config.RenderMode.SHADER) {
            // order:
            // AFTER_SKY
            // AFTER_SOLID_BLOCKS
            // AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS
            // AFTER_CUTOUT_BLOCKS
            // AFTER_ENTITIES
            // AFTER_BLOCK_ENTITIES
            // AFTER_TRANSLUCENT_BLOCKS
            // AFTER_TRIPWIRE_BLOCKS
            // AFTER_PARTICLES
            // AFTER_WEATHER
            // AFTER_LEVEL

            return Config.bleedFixMode == Config.BleedFixMode.FULL ?
                    RenderLevelStageEvent.Stage.AFTER_SKY : RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
        }

        return RenderLevelStageEvent.Stage.AFTER_WEATHER;
    }

}
