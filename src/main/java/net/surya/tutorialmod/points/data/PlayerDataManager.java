package net.surya.tutorialmod.points.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.surya.tutorialmod.TutorialMod;
import net.surya.tutorialmod.points.PointsUpgradeEffects;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер для сохранения данных игроков в файлы на диске в формате JSON
 * Привязка к UUID игрока
 */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PLAYER_DATA_PATH = "player_data";
    private static File playerDataDir;
    
    // Кэш данных игроков, чтобы не обращаться к диску постоянно
    private static final Map<UUID, PlayerSaveData> playerDataCache = new ConcurrentHashMap<>();
    
    // Список игроков, ожидающих восстановления данных после смерти
    private static final Map<UUID, Integer> pendingRestores = new ConcurrentHashMap<>();
    
    // Список игроков, ожидающих восстановления здоровья и эффектов
    private static final Map<UUID, Integer> pendingHealing = new ConcurrentHashMap<>();
    
    // Игроки, для которых данные восстановлены и ожидается событие явного возрождения
    private static final Set<UUID> readyForHealing = ConcurrentHashMap.newKeySet();
    
    /**
     * Инициализирует директорию для данных
     */
    public static void init(MinecraftServer server) {
        File worldDir = server.getWorldPath(new net.minecraft.world.level.storage.LevelResource("")).toFile();
        playerDataDir = new File(worldDir, PLAYER_DATA_PATH);
        
        if (!playerDataDir.exists()) {
            boolean created = playerDataDir.mkdirs();
            if (!created) {
                TutorialMod.LOGGER.error("Не удалось создать директорию для данных игроков");
            }
        }
        
        TutorialMod.LOGGER.info("Директория данных игроков инициализирована: " + playerDataDir.getAbsolutePath());
    }
    
    /**
     * Сохраняет данные игрока в файл
     */
    public static void savePlayerData(ServerPlayer player, boolean hasChip, int level, int experience, int points, Map<String, Integer> upgrades) {
        UUID playerId = player.getUUID();
        String playerName = player.getName().getString();
        
        PlayerSaveData saveData = new PlayerSaveData();
        saveData.uuid = playerId.toString();
        saveData.name = playerName;
        saveData.chip.installed = hasChip;
        saveData.chip.data.level = level;
        saveData.chip.data.experience = experience;
        saveData.chip.data.points = points;
        saveData.chip.data.upgrades = new HashMap<>(upgrades);
        
        // Сохраняем текущую позицию игрока
        Vec3 pos = player.position();
        saveData.chip.data.location = new double[] { pos.x, pos.y, pos.z };
        
        // Обновляем кэш
        playerDataCache.put(playerId, saveData);
        
        // Сохраняем в файл
        String fileName = getFileName(playerId);
        try (FileWriter writer = new FileWriter(new File(playerDataDir, fileName))) {
            GSON.toJson(saveData, writer);
            TutorialMod.LOGGER.debug("Данные игрока сохранены: " + playerName + " (UUID: " + playerId + ")");
        } catch (IOException e) {
            TutorialMod.LOGGER.error("Ошибка при сохранении данных игрока: " + playerName, e);
        }
    }
    
    /**
     * Загружает данные игрока из файла по UUID
     */
    public static PlayerSaveData loadPlayerData(UUID playerId) {
        // Проверяем кэш
        if (playerDataCache.containsKey(playerId)) {
            return playerDataCache.get(playerId);
        }
        
        String fileName = getFileName(playerId);
        File dataFile = new File(playerDataDir, fileName);
        
        if (!dataFile.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            PlayerSaveData saveData = GSON.fromJson(reader, PlayerSaveData.class);
            // Обновляем кэш
            playerDataCache.put(playerId, saveData);
            return saveData;
        } catch (IOException e) {
            TutorialMod.LOGGER.error("Ошибка при загрузке данных игрока: " + playerId, e);
            return null;
        }
    }
    
    /**
     * Планирует восстановление данных игрока через 3 тика после смерти
     */
    public static void scheduledRestore(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Проверяем, был ли у игрока чип
        PlayerSaveData saveData = loadPlayerData(playerId);
        if (saveData != null && saveData.chip.installed) {
            // Планируем восстановление через 3 тика
            pendingRestores.put(playerId, 3);
            TutorialMod.LOGGER.info("Запланировано восстановление данных для игрока " + 
                     player.getName().getString() + " (UUID: " + playerId + ")");
        }
    }
    
    /**
     * Отмечает игрока готовым к восстановлению здоровья после явного события возрождения
     */
    public static void markReadyForHealing(ServerPlayer player) {
        if (player == null) return;
        
        UUID playerId = player.getUUID();
        TutorialMod.LOGGER.info("Попытка восстановления здоровья для игрока " + player.getName().getString());
        
        // Проверяем, есть ли у игрока чип
        boolean hasChip = NeuralChipProvider.getNeuralChipData(player)
                .map(NeuralChipData::hasChip)
                .orElse(false);
        
        if (!hasChip) {
            // Пытаемся восстановить из файла
            if (!restorePlayerData(player)) {
                TutorialMod.LOGGER.info("Не удалось восстановить данные для игрока " + 
                        player.getName().getString() + " (UUID: " + playerId + ")");
                return;
            }
        }
        
        // Планируем восстановление здоровья через 10 тиков
        pendingHealing.put(playerId, 10);
        TutorialMod.LOGGER.info("Восстановление здоровья запланировано для игрока " + 
                player.getName().getString() + " (UUID: " + playerId + ")");
        
        // Удаляем из списка ожидающих, если он там был
        readyForHealing.remove(playerId);
    }
    
    /**
     * Обработчик тиков для восстановления данных игроков после смерти
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Создаем копию ключей, чтобы можно было безопасно модифицировать оригинальную карту
        // Обработка восстановления данных
        for (UUID playerId : new HashMap<>(pendingRestores).keySet()) {
            int ticksLeft = pendingRestores.get(playerId) - 1;
            
            if (ticksLeft <= 0) {
                // Время истекло, пора восстанавливать данные
                MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        restorePlayerData(player);
                        TutorialMod.LOGGER.info("Данные восстановлены для игрока " + 
                                player.getName().getString() + " (UUID: " + playerId + ")");
                        
                        // После восстановления данных, отмечаем игрока как готового к восстановлению здоровья
                        // Но ждём явного события возрождения
                        readyForHealing.add(playerId);
                        TutorialMod.LOGGER.info("Игрок " + player.getName().getString() + 
                                " готов к восстановлению здоровья после явного возрождения");
                    }
                }
                
                // Удаляем игрока из списка ожидания
                pendingRestores.remove(playerId);
            } else {
                // Уменьшаем счетчик
                pendingRestores.put(playerId, ticksLeft);
            }
        }
        
        // Обработка восстановления здоровья и эффектов
        for (UUID playerId : new HashMap<>(pendingHealing).keySet()) {
            int ticksLeft = pendingHealing.get(playerId) - 1;
            
            if (ticksLeft <= 0) {
                // Время истекло, восстанавливаем здоровье и эффекты
                MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        // Применяем эффекты модификаторов
                        PointsUpgradeEffects.applyAllUpgradeEffects(player);
                        
                        // Отложенное восстановление здоровья - через 2 тика после применения эффектов
                        server.tell(new net.minecraft.server.TickTask(2, () -> {
                            // Проверяем, что игрок все еще в игре
                            ServerPlayer p = server.getPlayerList().getPlayer(playerId);
                            if (p != null) {
                                float beforeHealth = p.getHealth();
                                float beforeMaxHealth = p.getMaxHealth();
                                
                                // Восстанавливаем здоровье полностью
                                p.setHealth(p.getMaxHealth());
                                
                                TutorialMod.LOGGER.info("Здоровье восстановлено для игрока " + 
                                        p.getName().getString() + " (UUID: " + playerId + ") " +
                                        "Было: " + beforeHealth + "/" + beforeMaxHealth + ", " +
                                        "Стало: " + p.getHealth() + "/" + p.getMaxHealth());
                            }
                        }));
                    }
                }
                
                // Удаляем игрока из списка ожидания
                pendingHealing.remove(playerId);
            } else {
                // Уменьшаем счетчик
                pendingHealing.put(playerId, ticksLeft);
            }
        }
    }
    
    /**
     * Планирует восстановление здоровья и эффектов через 10 тиков
     * Используется при выполнении команд улучшения и при входе в игру
     */
    public static void scheduleHealing(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Планируем восстановление здоровья через 10 тиков
        pendingHealing.put(playerId, 10);
        TutorialMod.LOGGER.info("Запланировано восстановление здоровья для игрока " + 
                player.getName().getString() + " (UUID: " + playerId + ")");
    }
    
    /**
     * Восстанавливает данные игрока из файла
     * @return true если данные были восстановлены
     */
    public static boolean restorePlayerData(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerSaveData saveData = loadPlayerData(playerId);
        
        if (saveData != null && saveData.chip.installed) {
            // Проверяем, есть ли уже у игрока чип
            boolean hadChip = NeuralChipProvider.getNeuralChipData(player)
                    .map(NeuralChipData::hasChip)
                    .orElse(false);
            
            if (!hadChip) {
                TutorialMod.LOGGER.info("Восстанавливаем нейронный чип для игрока: " + player.getName().getString());
                // Восстанавливаем данные о чипе
                NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
                    chipData.setHasChip(true);
                });
            }
            
            // Восстанавливаем данные о прокачке
            PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                PlayerSaveData.ChipData chipData = saveData.chip.data;
                // Восстанавливаем базовые параметры
                pointsData.setLevel(chipData.level);
                pointsData.setExperience(chipData.experience);
                pointsData.setPoints(chipData.points);
                
                // Восстанавливаем улучшения
                for (Map.Entry<String, Integer> entry : chipData.upgrades.entrySet()) {
                    String upgradeType = entry.getKey();
                    int level = entry.getValue();
                    
                    // Устанавливаем уровень улучшения напрямую
                    pointsData.setUpgradeLevel(upgradeType, level);
                    
                    // Логируем что мы восстанавливаем важные улучшения чипа
                    if (upgradeType.equals("chiphealth") || upgradeType.equals("resistance")) {
                        TutorialMod.LOGGER.info("Восстановлено улучшение чипа " + upgradeType + 
                                " уровня " + level + " для игрока " + player.getName().getString());
                    }
                }
                
                // Не применяем эффекты сразу, они будут применены позже по таймеру
                // PointsUpgradeEffects.applyAllUpgradeEffects(player);
            });
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Получает имя файла для UUID игрока
     */
    private static String getFileName(UUID playerId) {
        return playerId.toString() + ".json";
    }
    
    /**
     * Структура данных, соответствующая JSON формату, указанному в требованиях
     */
    public static class PlayerSaveData {
        public String uuid;
        public String name;
        public ChipInfo chip = new ChipInfo();
        
        public static class ChipInfo {
            public boolean installed;
            public ChipData data = new ChipData();
        }
        
        public static class ChipData {
            public int level;
            public int experience;
            public int points;
            public Map<String, Integer> upgrades = new HashMap<>();
            public double[] location = new double[3]; // x, y, z
        }
    }
    
    public static void savePlayerData(ServerPlayer player) {
        if (player == null) return;
        
        NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
            boolean hasChip = chipData.hasChip();
            
            PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                savePlayerData(
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
} 