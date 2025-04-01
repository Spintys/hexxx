package net.surya.tutorialmod.points.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.surya.tutorialmod.points.PointsUpgradeEffects;
import net.surya.tutorialmod.points.data.NeuralChipProvider;
import net.surya.tutorialmod.points.data.PlayerDataManager;
import net.surya.tutorialmod.points.data.PlayerPointsData;
import net.surya.tutorialmod.points.data.PlayerPointsProvider;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class PointsCommand {
    // Список доступных улучшений
    private static final List<String> AVAILABLE_UPGRADES = Arrays.asList(
            "strength", // Сила
            "speed",    // Скорость
            "health",   // Здоровье
            "luck"      // Удача
    );

    // Метод для регистрации команды через событие Forge
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        registerPointsCommand(event.getDispatcher());
    }

    public static void register() {
        // Метод оставлен для совместимости, регистрация теперь происходит через событие
    }

    private static void registerPointsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("points")
                        .executes(PointsCommand::showPoints)
                        .then(Commands.literal("upgrades")
                                .executes(PointsCommand::showUpgrades))
                        .then(Commands.literal("upgrade")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            AVAILABLE_UPGRADES.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(PointsCommand::upgradeSkill)))
        );
    }

    private static int showPoints(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Проверяем наличие чипа
            return NeuralChipProvider.getNeuralChipData(player).map(chipData -> {
                if (!chipData.hasChip()) {
                    // Пытаемся восстановить из файла
                    if (!PlayerDataManager.restorePlayerData(player)) {
                        context.getSource().sendFailure(Component.translatable("message.tutorialmod.neural_chip_required")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                }
                
                PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                    int level = pointsData.getLevel();
                    int points = pointsData.getPoints();
                    int experience = pointsData.getExperience();
                    int xpForNextLevel = pointsData.getXPForNextLevel();
                    
                    // Отправляем информацию о текущих очках
                    context.getSource().sendSuccess(() -> Component.literal("=== Информация о прогрессе ===")
                            .withStyle(ChatFormatting.GOLD), false);
                    context.getSource().sendSuccess(() -> Component.literal("Уровень: " + level)
                            .withStyle(ChatFormatting.GREEN), false);
                    context.getSource().sendSuccess(() -> Component.literal("Очки улучшений: " + points)
                            .withStyle(ChatFormatting.AQUA), false);
                    context.getSource().sendSuccess(() -> Component.literal("Опыт: " + experience + "/" + xpForNextLevel)
                            .withStyle(ChatFormatting.YELLOW), false);
                    
                    // Процент до следующего уровня
                    float percentage = (float) experience / xpForNextLevel * 100;
                    context.getSource().sendSuccess(() -> Component.literal("Прогресс: " + 
                            String.format("%.1f%%", percentage)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                    
                    // Визуализация прогресса
                    StringBuilder progressBar = new StringBuilder("[");
                    int progressBlocks = (int) (percentage / 5);
                    for (int i = 0; i < 20; i++) {
                        if (i < progressBlocks) {
                            progressBar.append("|");
                        } else {
                            progressBar.append(" ");
                        }
                    }
                    progressBar.append("]");
                    
                    // Показываем UUID игрока для понимания привязки данных
                    UUID playerId = player.getUUID();
                    context.getSource().sendSuccess(() -> Component.literal("UUID: " + playerId.toString())
                            .withStyle(ChatFormatting.DARK_GRAY), false);
                    
                    context.getSource().sendSuccess(() -> Component.literal(progressBar.toString())
                            .withStyle(ChatFormatting.WHITE), false);
                });
                
                return 1;
            }).orElse(0);
        }
        
        return 0;
    }

    private static int showUpgrades(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Проверяем наличие чипа
            return NeuralChipProvider.getNeuralChipData(player).map(chipData -> {
                if (!chipData.hasChip()) {
                    // Пытаемся восстановить из файла
                    if (!PlayerDataManager.restorePlayerData(player)) {
                        context.getSource().sendFailure(Component.translatable("message.tutorialmod.neural_chip_required")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                }
                
                PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                    context.getSource().sendSuccess(() -> Component.literal("=== Доступные улучшения ===")
                            .withStyle(ChatFormatting.GOLD), false);
                    
                    for (String upgradeType : AVAILABLE_UPGRADES) {
                        int level = pointsData.getUpgradeLevel(upgradeType);
                        context.getSource().sendSuccess(() -> Component.literal("- " + upgradeType + ": " + level)
                                .withStyle(ChatFormatting.GREEN), false);
                    }
                    
                    context.getSource().sendSuccess(() -> Component.literal("Используйте /points upgrade <тип> для улучшения")
                            .withStyle(ChatFormatting.GRAY), false);
                });
                
                return 1;
            }).orElse(0);
        }
        
        return 0;
    }
    
    private static int upgradeSkill(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Проверяем наличие чипа
            return NeuralChipProvider.getNeuralChipData(player).map(chipData -> {
                if (!chipData.hasChip()) {
                    // Пытаемся восстановить из файла
                    if (!PlayerDataManager.restorePlayerData(player)) {
                        context.getSource().sendFailure(Component.translatable("message.tutorialmod.neural_chip_required")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                }
                
                String upgradeType = StringArgumentType.getString(context, "type");
                
                if (!AVAILABLE_UPGRADES.contains(upgradeType)) {
                    context.getSource().sendFailure(Component.literal("Недопустимый тип улучшения!"));
                    return 0;
                }
                
                PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                    if (pointsData.getPoints() <= 0) {
                        context.getSource().sendFailure(Component.literal("У вас недостаточно очков улучшения!"));
                        return;
                    }
                    
                    // Применяем улучшение
                    if (pointsData.upgrade(upgradeType)) {
                        // Применяем эффект улучшения к игроку
                        applyUpgradeEffect(player, upgradeType, pointsData.getUpgradeLevel(upgradeType));
                        
                        // Сохраняем данные после улучшения
                        savePlayerData(player);
                        
                        context.getSource().sendSuccess(() -> Component.literal("Вы улучшили " + upgradeType + 
                                " до уровня " + pointsData.getUpgradeLevel(upgradeType))
                                .withStyle(ChatFormatting.GREEN), true);
                    }
                });
                
                return 1;
            }).orElse(0);
        }
        
        return 0;
    }
    
    // Сохраняем данные игрока в файл
    private static void savePlayerData(ServerPlayer player) {
        NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
            boolean hasChip = chipData.hasChip();
            
            PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                PlayerDataManager.savePlayerData(
                    player,
                    hasChip,
                    pointsData.getLevel(),
                    pointsData.getExperience(),
                    pointsData.getPoints(),
                    pointsData.getUpgrades()
                );
            });
        });
    }
    
    private static void applyUpgradeEffect(ServerPlayer player, String upgradeType, int level) {
        // Используем класс PointsUpgradeEffects для применения эффектов
        switch (upgradeType) {
            case "strength":
                PointsUpgradeEffects.applyStrengthUpgrade(player, level);
                break;
            case "speed":
                PointsUpgradeEffects.applySpeedUpgrade(player, level);
                break;
            case "health":
                PointsUpgradeEffects.applyHealthUpgrade(player, level);
                break;
            case "luck":
                PointsUpgradeEffects.applyLuckUpgrade(player, level);
                break;
        }
        
        player.sendSystemMessage(Component.literal("Ваше умение " + upgradeType + " улучшено до уровня " + level)
                .withStyle(ChatFormatting.GREEN));
    }
} 