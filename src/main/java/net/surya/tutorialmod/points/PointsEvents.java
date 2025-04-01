package net.surya.tutorialmod.points;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.surya.tutorialmod.TutorialMod;
import net.surya.tutorialmod.networking.ModMessages;
import net.surya.tutorialmod.networking.packet.ExperienceGainS2CPacket;
import net.surya.tutorialmod.points.data.NeuralChipData;
import net.surya.tutorialmod.points.data.NeuralChipProvider;
import net.surya.tutorialmod.points.data.PlayerDataManager;
import net.surya.tutorialmod.points.data.PlayerPointsData;
import net.surya.tutorialmod.points.data.PlayerPointsProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class PointsEvents {
    private static final Map<String, Integer> ORE_RATINGS = new HashMap<>();
    private static final Map<UUID, Integer> HEALING_TICKS = new HashMap<>();
    
    static {
        ORE_RATINGS.put("coal", 1);
        ORE_RATINGS.put("copper", 2);
        ORE_RATINGS.put("iron", 3);
        ORE_RATINGS.put("gold", 4);
        ORE_RATINGS.put("redstone", 3);
        ORE_RATINGS.put("lapis", 4);
        ORE_RATINGS.put("diamond", 8);
        ORE_RATINGS.put("emerald", 10);
        ORE_RATINGS.put("quartz", 2);
        ORE_RATINGS.put("debris", 15);
        ORE_RATINGS.put("glowstone", 2);
    }
    
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerPointsData.class);
        event.register(NeuralChipData.class);
    }
    
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerPointsProvider.PLAYER_POINTS).isPresent()) {
                event.addCapability(new ResourceLocation(TutorialMod.MOD_ID, "properties"), new PlayerPointsProvider());
            }
            if (!event.getObject().getCapability(NeuralChipProvider.NEURAL_CHIP).isPresent()) {
                event.addCapability(new ResourceLocation(TutorialMod.MOD_ID, "neural_chip"), new NeuralChipProvider());
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            savePlayerData(player);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            ServerPlayer player = (ServerPlayer) event.player;
            UUID playerId = player.getUUID();
            
            if (HEALING_TICKS.containsKey(playerId)) {
                int remainingTicks = HEALING_TICKS.get(playerId);
                if (remainingTicks <= 0) {
                    PointsUpgradeEffects.applyAllUpgradeEffects(player);
                    HEALING_TICKS.remove(playerId);
                } else {
                    HEALING_TICKS.put(playerId, remainingTicks - 1);
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
                if (PlayerDataManager.restorePlayerData(player)) {
                    PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                        PointsUpgradeEffects.applyAllUpgradeEffects(player);
                        
                        ModMessages.sendToPlayer(new ExperienceGainS2CPacket(0, pointsData.getExperience(), 
                                                                              pointsData.getXPForNextLevel(), pointsData.getLevel(), 
                                                                              pointsData.getPoints()), player);
                    });
                }
            });
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            savePlayerData(player);
        }
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

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
                if (!chipData.hasChip()) {
                    if (!PlayerDataManager.restorePlayerData(player)) {
                        return;
                    }
                }
                
                BlockState state = event.getState();
                Block block = state.getBlock();
                String blockId = block.getDescriptionId();
                
                int expValue = 0;
                for (Map.Entry<String, Integer> entry : ORE_RATINGS.entrySet()) {
                    if (blockId.contains(entry.getKey())) {
                        expValue = entry.getValue();
                        break;
                    }
                }
                
                if (expValue > 0) {
                    final int finalExpValue = expValue;
                    PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                        int level = pointsData.getLevel();
                        float levelMultiplier = Math.max(0.1f, 1.0f - (level * 0.01f));
                        int adjustedExp = Math.max(1, (int)(finalExpValue * levelMultiplier));
                        
                        pointsData.addExperience(adjustedExp);
                        ModMessages.sendToPlayer(new ExperienceGainS2CPacket(adjustedExp, pointsData.getExperience(), 
                                                                            pointsData.getXPForNextLevel(), pointsData.getLevel(), 
                                                                            pointsData.getPoints()), player);
                        
                        savePlayerData(player);
                    });
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onEntityKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
                if (!chipData.hasChip()) {
                    if (!PlayerDataManager.restorePlayerData(player)) {
                        return;
                    }
                }
                
                LivingEntity killed = event.getEntity();
                if (killed instanceof Player) {
                    return;
                }
                
                float health = killed.getMaxHealth();
                float armor = killed.getArmorValue();
                
                int expValue = (int)Math.ceil((health + armor) / 2);
                
                PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                    int level = pointsData.getLevel();
                    float levelMultiplier = Math.max(0.1f, 1.0f - (level * 0.01f));
                    int adjustedExp = Math.max(1, (int)(expValue * levelMultiplier));
                    
                    pointsData.addExperience(adjustedExp);
                    ModMessages.sendToPlayer(new ExperienceGainS2CPacket(adjustedExp, pointsData.getExperience(), 
                                                                        pointsData.getXPForNextLevel(), pointsData.getLevel(), 
                                                                        pointsData.getPoints()), player);
                    
                    savePlayerData(player);
                });
            });
        }
    }
} 