package net.surya.tutorialmod.points.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.surya.tutorialmod.TutorialMod;
import net.surya.tutorialmod.config.ModConfig;
import net.surya.tutorialmod.points.PointsUpgradeEffects;
import net.surya.tutorialmod.points.data.NeuralChipProvider;
import net.surya.tutorialmod.points.data.PlayerDataManager;
import net.surya.tutorialmod.points.data.PlayerPointsProvider;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class ChipUpgradeCommand {
    private static final List<String> CHIP_UPGRADES = Arrays.asList(
            "chiphealth",      
            "resistance",     
            "miningspeed"     
    );

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        registerChipUpgradeCommand(event.getDispatcher());
        registerAdminCommands(event.getDispatcher());
    }

    private static void registerChipUpgradeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("chipupgrade")
                        .executes(ChipUpgradeCommand::showChipUpgrades)
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(ChipUpgradeCommand::reloadConfig))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    CHIP_UPGRADES.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ChipUpgradeCommand::upgradeChip))
        );
    }
    
    private static void registerAdminCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("chippoints")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ChipUpgradeCommand::givePoints))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ChipUpgradeCommand::setPoints))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    CHIP_UPGRADES.forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(ChipUpgradeCommand::resetUpgrade))
                                        .executes(ChipUpgradeCommand::resetAllUpgrades)))
        );
    }

    private static int showChipUpgrades(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            return NeuralChipProvider.getNeuralChipData(player).map(chipData -> {
                if (!chipData.hasChip()) {
                    if (!PlayerDataManager.restorePlayerData(player)) {
                        context.getSource().sendFailure(Component.translatable("message.tutorialmod.neural_chip_required")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                }
                
                PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                    context.getSource().sendSuccess(() -> Component.literal("=== Улучшения нейрочипа ===")
                            .withStyle(ChatFormatting.GOLD), false);
                    
                    int points = pointsData.getPoints();
                    context.getSource().sendSuccess(() -> Component.literal("Доступные очки: " + points)
                            .withStyle(ChatFormatting.AQUA), false);
                    
                    int healthLevel = pointsData.getUpgradeLevel("chiphealth");
                    int healthMax = ModConfig.getMaxUpgradeLevel("health");
                    int healthCost = ModConfig.getUpgradeCost("health");
                    context.getSource().sendSuccess(() -> Component.literal("- Здоровье: " + healthLevel + "/" + healthMax + 
                            " (стоимость: " + healthCost + " очк.)")
                            .withStyle(healthLevel >= healthMax ? ChatFormatting.RED : ChatFormatting.GREEN), false);
                    
                    int resistanceLevel = pointsData.getUpgradeLevel("resistance");
                    int resistanceMax = ModConfig.getMaxUpgradeLevel("resistance");
                    int resistanceCost = ModConfig.getUpgradeCost("resistance");
                    context.getSource().sendSuccess(() -> Component.literal("- Уроноустойчивость: " + resistanceLevel + "/" + resistanceMax + 
                            " (" + (resistanceLevel * 4) + "% снижения урона, стоимость: " + resistanceCost + " очк.)")
                            .withStyle(resistanceLevel >= resistanceMax ? ChatFormatting.RED : ChatFormatting.GREEN), false);
                    
                    int miningSpeedLevel = pointsData.getUpgradeLevel("miningspeed");
                    int miningSpeedMax = ModConfig.getMaxUpgradeLevel("miningspeed");
                    int miningSpeedCost = ModConfig.getUpgradeCost("miningspeed");
                    context.getSource().sendSuccess(() -> Component.literal("- Скорость добычи: " + miningSpeedLevel + "/" + miningSpeedMax + 
                            " (стоимость: " + miningSpeedCost + " очк.)")
                            .withStyle(miningSpeedLevel >= miningSpeedMax ? ChatFormatting.RED : ChatFormatting.GREEN), false);
                    
                    context.getSource().sendSuccess(() -> Component.literal("Используйте /chipupgrade <тип> для улучшения")
                            .withStyle(ChatFormatting.GRAY), false);
                });
                
                return 1;
            }).orElse(0);
        }
        
        return 0;
    }
    
    private static int upgradeChip(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            return NeuralChipProvider.getNeuralChipData(player).map(chipData -> {
                if (!chipData.hasChip()) {
                    if (!PlayerDataManager.restorePlayerData(player)) {
                        context.getSource().sendFailure(Component.translatable("message.tutorialmod.neural_chip_required")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                }
                
                String upgradeType = StringArgumentType.getString(context, "type");
                
                if (!CHIP_UPGRADES.contains(upgradeType)) {
                    context.getSource().sendFailure(Component.literal("Недопустимый тип улучшения чипа!"));
                    return 0;
                }
                
                PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                    int currentLevel = pointsData.getUpgradeLevel(upgradeType);
                    int maxLevel = ModConfig.getMaxUpgradeLevel(upgradeType.equals("chiphealth") ? "health" : upgradeType);
                    int cost = ModConfig.getUpgradeCost(upgradeType.equals("chiphealth") ? "health" : upgradeType);
                    
                    if (currentLevel >= maxLevel) {
                        context.getSource().sendFailure(Component.literal("Достигнут максимальный уровень для этого улучшения!"));
                        return;
                    }
                    
                    if (pointsData.getPoints() < cost) {
                        context.getSource().sendFailure(Component.literal("У вас недостаточно очков улучшения! Требуется: " + cost));
                        return;
                    }
                    
                    pointsData.setPoints(pointsData.getPoints() - cost);
                    
                    pointsData.setUpgradeLevel(upgradeType, currentLevel + 1);
                    
                    if (upgradeType.equals("chiphealth")) {
                        PlayerDataManager.scheduleHealing(player);
                    }
                    
                    savePlayerData(player);
                    
                    String upgradeName = upgradeType.equals("chiphealth") ? "здоровья" : upgradeType.equals("resistance") ? "уроноустойчивости" : "скорости добычи";
                    context.getSource().sendSuccess(() -> Component.literal("Вы улучшили уровень " + upgradeName + 
                            " до " + (currentLevel + 1) + "/" + maxLevel)
                            .withStyle(ChatFormatting.GREEN), true);
                });
                
                return 1;
            }).orElse(0);
        }
        
        return 0;
    }
    
    private static int givePoints(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            
            return NeuralChipProvider.getNeuralChipData(targetPlayer).map(chipData -> {
                if (!chipData.hasChip()) {
                    if (!PlayerDataManager.restorePlayerData(targetPlayer)) {
                        chipData.setHasChip(true);
                    }
                }
                
                PlayerPointsProvider.getPlayerPoints(targetPlayer).ifPresent(pointsData -> {
                    pointsData.setPoints(pointsData.getPoints() + amount);
                    
                    savePlayerData(targetPlayer);
                    
                    context.getSource().sendSuccess(() -> Component.literal("Выдано " + amount + " очков улучшения игроку " + 
                            targetPlayer.getName().getString())
                            .withStyle(ChatFormatting.GREEN), true);
                    
                    targetPlayer.sendSystemMessage(Component.literal("Вы получили " + amount + " очков улучшения нейрочипа!")
                            .withStyle(ChatFormatting.GREEN));
                });
                
                return 1;
            }).orElse(0);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Ошибка при выдаче очков: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int setPoints(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            
            return NeuralChipProvider.getNeuralChipData(targetPlayer).map(chipData -> {
                if (!chipData.hasChip()) {
                    if (!PlayerDataManager.restorePlayerData(targetPlayer)) {
                        chipData.setHasChip(true);
                    }
                }
                
                PlayerPointsProvider.getPlayerPoints(targetPlayer).ifPresent(pointsData -> {
                    pointsData.setPoints(amount);
                    
                    savePlayerData(targetPlayer);
                    
                    context.getSource().sendSuccess(() -> Component.literal("Установлено " + amount + " очков улучшения игроку " + 
                            targetPlayer.getName().getString())
                            .withStyle(ChatFormatting.GREEN), true);
                    
                    targetPlayer.sendSystemMessage(Component.literal("Ваши очки улучшения нейрочипа установлены на " + amount + "!")
                            .withStyle(ChatFormatting.GREEN));
                });
                
                return 1;
            }).orElse(0);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Ошибка при установке очков: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int resetUpgrade(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String upgradeType = StringArgumentType.getString(context, "type");
            
            if (!CHIP_UPGRADES.contains(upgradeType)) {
                context.getSource().sendFailure(Component.literal("Недопустимый тип улучшения: " + upgradeType));
                return 0;
            }
            
            return NeuralChipProvider.getNeuralChipData(targetPlayer).map(chipData -> {
                if (!chipData.hasChip()) {
                    context.getSource().sendFailure(Component.literal("У игрока нет нейрочипа!"));
                    return 0;
                }
                
                PlayerPointsProvider.getPlayerPoints(targetPlayer).ifPresent(pointsData -> {
                    int oldLevel = pointsData.getUpgradeLevel(upgradeType);
                    pointsData.setUpgradeLevel(upgradeType, 0);
                    
                    final int refundAmount = calculateRefund(upgradeType, oldLevel);
                    
                    pointsData.setPoints(pointsData.getPoints() + refundAmount);
                    
                    if (upgradeType.equals("chiphealth")) {
                        PlayerDataManager.scheduleHealing(targetPlayer);
                    }
                    
                    savePlayerData(targetPlayer);
                    
                    String upgradeName = upgradeType.equals("chiphealth") ? "здоровья" : upgradeType.equals("resistance") ? "уроноустойчивости" : "скорости добычи";
                    context.getSource().sendSuccess(() -> Component.literal("Сброшен уровень " + upgradeName + 
                            " для игрока " + targetPlayer.getName().getString() + " и возвращено " + refundAmount + " очков")
                            .withStyle(ChatFormatting.GREEN), true);
                    
                    targetPlayer.sendSystemMessage(Component.literal("Ваш уровень " + upgradeName + 
                            " был сброшен администратором. Вы получили назад " + refundAmount + " очков.")
                            .withStyle(ChatFormatting.GOLD));
                });
                
                return 1;
            }).orElse(0);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Ошибка при сбросе улучшения: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int resetAllUpgrades(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            
            return NeuralChipProvider.getNeuralChipData(targetPlayer).map(chipData -> {
                if (!chipData.hasChip()) {
                    context.getSource().sendFailure(Component.literal("У игрока нет нейрочипа!"));
                    return 0;
                }
                
                PlayerPointsProvider.getPlayerPoints(targetPlayer).ifPresent(pointsData -> {
                    final int refundAmount;
                    
                    int totalRefund = 0;
                    for (String upgradeType : CHIP_UPGRADES) {
                        int oldLevel = pointsData.getUpgradeLevel(upgradeType);
                        pointsData.setUpgradeLevel(upgradeType, 0);
                        
                        totalRefund += calculateRefund(upgradeType, oldLevel);
                    }
                    
                    refundAmount = totalRefund;
                    
                    pointsData.setPoints(pointsData.getPoints() + refundAmount);
                    
                    PlayerDataManager.scheduleHealing(targetPlayer);
                    
                    savePlayerData(targetPlayer);
                    
                    context.getSource().sendSuccess(() -> Component.literal("Сброшены все улучшения нейрочипа для игрока " + 
                            targetPlayer.getName().getString() + " и возвращено " + refundAmount + " очков")
                            .withStyle(ChatFormatting.GREEN), true);
                    
                    targetPlayer.sendSystemMessage(Component.literal("Все улучшения вашего нейрочипа были сброшены администратором. " + 
                            "Вы получили назад " + refundAmount + " очков.")
                            .withStyle(ChatFormatting.GOLD));
                });
                
                return 1;
            }).orElse(0);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Ошибка при сбросе улучшений: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        ModConfig.reload();
        context.getSource().sendSuccess(() -> Component.literal("Конфигурация улучшений перезагружена!")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
    
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

    private static int calculateRefund(String upgradeType, int oldLevel) {
        int refund = 0;
        String costKey = upgradeType.equals("chiphealth") ? "health" : upgradeType.equals("resistance") ? "resistance" : "miningspeed";
        
        for (int i = 1; i <= oldLevel; i++) {
            refund += ModConfig.getUpgradeCost(costKey);
        }
        
        return refund;
    }
} 