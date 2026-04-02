package me.mbg206.pmwrenderer.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.protomanly.pmweather.config.ServerConfig;
import me.mbg206.pmwrenderer.PMWRendering;
import me.mbg206.pmwrenderer.renderer.ClassicRenderer;
import me.mbg206.pmwrenderer.renderer.ParticleManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid= PMWRendering.MODID, value= Dist.CLIENT)
public class RendererDebugCommand {
    @SubscribeEvent
    private static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                literal("pmwrenderer").requires(CommandSourceStack::isPlayer).then(
                        literal("debug_cloud").then(
                                literal("aloft").executes(ctx -> spawnDebugCloud(ctx, DebugPosition.AT_PLAYER_ALOFT))
                        ).executes(ctx -> spawnDebugCloud(ctx, DebugPosition.AT_PLAYER))
                )

        );
    }

    private static int spawnDebugCloud(final CommandContext<CommandSourceStack> context, DebugPosition posType) {
        ParticleManager particleManager = ClassicRenderer.getParticleManager();
        if (particleManager == null) {
            setSuccess(context, "No ParticleManager found!");
        }

        else {
            CommandSourceStack source = context.getSource();
            Vec3 playerPos = source.getPosition();
            Vec3 position = switch (posType) {
                case AT_PLAYER -> source.getPosition();
                case AT_PLAYER_ALOFT -> new Vec3(playerPos.x(), ServerConfig.layer0Height, playerPos.z());
            };

            particleManager.spawnCloud(position.x, position.y, position.z, -0.25, -0.25, ClassicRenderer.getCloudColor());
            setSuccess(context, "Spawned particle!");
        }

        return Command.SINGLE_SUCCESS;
    }

    private enum DebugPosition {
        AT_PLAYER,
        AT_PLAYER_ALOFT
    }

    private static void setSuccess(final CommandContext<CommandSourceStack> context, String message) {
        // Component.translatable(key)
        context.getSource().sendSuccess(() -> Component.literal(message), true);
    }
}
