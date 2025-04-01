package net.surya.tutorialmod.points;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.surya.tutorialmod.TutorialMod;
import net.surya.tutorialmod.points.data.NeuralChipProvider;
import net.surya.tutorialmod.points.data.PlayerPointsData;
import net.surya.tutorialmod.points.data.PlayerPointsProvider;
import net.surya.tutorialmod.points.data.PlayerDataManager;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class PointsUpgradeEffects {
    private static final UUID STRENGTH_MODIFIER_UUID = UUID.fromString("606de09a-9c2d-11ee-b9d1-0242ac120002");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("6c0d60e6-9c2d-11ee-b9d1-0242ac120002");
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("751ae16a-9c2d-11ee-b9d1-0242ac120002");
    private static final UUID LUCK_MODIFIER_UUID = UUID.fromString("7ed1e1f6-9c2d-11ee-b9d1-0242ac120002");
    private static final UUID RESISTANCE_MODIFIER_UUID = UUID.fromString("8a9ec71e-ac51-4d7c-9e0f-2f8a9eb3690f");

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
                if (chipData.hasChip()) {
                    applyAllUpgradeEffects(player);
                }
            });
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();
            
            TutorialMod.LOGGER.info("Игрок " + player.getName().getString() + " возродился. " +
                    "Текущее здоровье: " + currentHealth + "/" + maxHealth + ". " +
                    "Запускаем процесс восстановления здоровья");
            
            PlayerDataManager.markReadyForHealing(player);
            
            NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
                if (chipData.hasChip()) {
                    TutorialMod.LOGGER.info("Игрок " + player.getName().getString() + " имеет чип, запланировано восстановление здоровья");
                } else {
                    TutorialMod.LOGGER.info("Игрок " + player.getName().getString() + " НЕ имеет чип!");
                }
            });
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START && 
                event.player instanceof ServerPlayer player && player.tickCount % 100 == 0) {
            
            NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
                if (chipData.hasChip()) {
                    applyAllUpgradeEffects(player);
                }
            });
        }
    }
    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
                if (chipData.hasChip()) {
                    PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                        int resistanceLevel = pointsData.getUpgradeLevel("resistance");
                        if (resistanceLevel > 0) {
                            float damageReduction = resistanceLevel * 0.04f;
                            float newDamage = event.getAmount() * (1.0f - Math.min(0.8f, damageReduction));
                            
                            if (newDamage != event.getAmount()) {
                                TutorialMod.LOGGER.debug("Урон снижен с " + event.getAmount() + " до " + newDamage + 
                                        " для игрока " + player.getName().getString() + " (уровень уроноустойчивости: " + 
                                        resistanceLevel + ")");
                                event.setAmount(newDamage);
                            }
                        }
                    });
                }
            });
        }
    }
    
    public static void applyAllUpgradeEffects(ServerPlayer player) {
        PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
            int strengthLevel = pointsData.getUpgradeLevel("strength");
            int speedLevel = pointsData.getUpgradeLevel("speed");
            int healthLevel = pointsData.getUpgradeLevel("health");
            int luckLevel = pointsData.getUpgradeLevel("luck");
            int chipHealthLevel = pointsData.getUpgradeLevel("chiphealth");
            
            applyStrengthUpgrade(player, strengthLevel);
            applySpeedUpgrade(player, speedLevel);
            applyHealthUpgrade(player, healthLevel + chipHealthLevel);
            applyLuckUpgrade(player, luckLevel);
            
            applyResistanceUpgrade(player, pointsData.getUpgradeLevel("resistance"));
            
            TutorialMod.LOGGER.info("Применены эффекты улучшений для игрока " + player.getName().getString());
        });
    }
    
    public static void applyStrengthUpgrade(ServerPlayer player, int level) {
        if (level <= 0) return;
        
        player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(STRENGTH_MODIFIER_UUID);
        
        double attackBonus = level * 0.5;
        AttributeModifier modifier = new AttributeModifier(
                STRENGTH_MODIFIER_UUID,
                "points_strength_boost",
                attackBonus,
                AttributeModifier.Operation.ADDITION
        );
        
        player.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(modifier);
    }
    
    public static void applySpeedUpgrade(ServerPlayer player, int level) {
        if (level <= 0) return;
        
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_UUID);
        
        double speedBonus = level * 0.05;
        AttributeModifier modifier = new AttributeModifier(
                SPEED_MODIFIER_UUID,
                "points_speed_boost",
                speedBonus,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        
        player.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(modifier);
    }
    
    public static void applyHealthUpgrade(ServerPlayer player, int level) {
        if (level <= 0) return;
        
        player.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_MODIFIER_UUID);
        
        double healthBonus = level * 2.0;
        AttributeModifier modifier = new AttributeModifier(
                HEALTH_MODIFIER_UUID,
                "points_health_boost",
                healthBonus,
                AttributeModifier.Operation.ADDITION
        );
        
        player.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(modifier);
    }
    
    public static void applyLuckUpgrade(ServerPlayer player, int level) {
        if (level <= 0) return;
        
        player.getAttribute(Attributes.LUCK).removeModifier(LUCK_MODIFIER_UUID);
        
        double luckBonus = level * 1.0;
        AttributeModifier modifier = new AttributeModifier(
                LUCK_MODIFIER_UUID,
                "points_luck_boost",
                luckBonus,
                AttributeModifier.Operation.ADDITION
        );
        
        player.getAttribute(Attributes.LUCK).addPermanentModifier(modifier);
    }

    public static void applyResistanceUpgrade(ServerPlayer player, int level) {
        if (level <= 0) return;
        
        player.getAttribute(Attributes.ARMOR).removeModifier(RESISTANCE_MODIFIER_UUID);
        
        double resistanceBonus = level * 0.05;
        AttributeModifier modifier = new AttributeModifier(
                RESISTANCE_MODIFIER_UUID,
                "points_resistance_boost",
                resistanceBonus,
                AttributeModifier.Operation.ADDITION
        );
        
        player.getAttribute(Attributes.ARMOR).addPermanentModifier(modifier);
    }
}