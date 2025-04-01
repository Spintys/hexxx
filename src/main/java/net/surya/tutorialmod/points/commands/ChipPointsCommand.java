package net.surya.tutorialmod.points.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.surya.tutorialmod.TutorialMod;
import net.surya.tutorialmod.points.data.NeuralChipProvider;
import net.surya.tutorialmod.points.data.PlayerDataManager;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class ChipPointsCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        registerRespawnCommand(event.getDispatcher());
    }

    private static void registerRespawnCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("chiprespawn")
                        .executes(ChipPointsCommand::respawnWithChip)
        );
    }

    private static int respawnWithChip(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();
            
            TutorialMod.LOGGER.info("Игрок " + player.getName().getString() + " вызвал команду восстановления. " +
                    "Текущее здоровье: " + currentHealth + "/" + maxHealth);
            
            return NeuralChipProvider.getNeuralChipData(player).map(chipData -> {
                if (!chipData.hasChip()) {
                    if (!PlayerDataManager.restorePlayerData(player)) {
                        context.getSource().sendFailure(Component.translatable("message.tutorialmod.neural_chip_required")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                }
                
                PlayerDataManager.markReadyForHealing(player);
                
                context.getSource().sendSuccess(() -> Component.literal("Запущено восстановление систем нейрочипа...")
                        .withStyle(ChatFormatting.GREEN), true);
                
                return 1;
            }).orElse(0);
        }
        
        return 0;
    }
} 