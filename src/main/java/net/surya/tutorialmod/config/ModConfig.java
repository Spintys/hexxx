package net.surya.tutorialmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import net.surya.tutorialmod.TutorialMod;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/tutorialmod";
    private static final String UPGRADES_CONFIG_FILE = "chip_upgrades.json";
    
    private static UpgradesConfig upgradesConfig;
    
    // Класс для хранения настроек улучшений
    public static class UpgradeSettings {
        public final int maxLevel;
        public final int cost;
        
        public UpgradeSettings(int maxLevel, int cost) {
            this.maxLevel = maxLevel;
            this.cost = cost;
        }
    }
    
    // Настройка улучшений
    public static Map<String, UpgradeSettings> UPGRADES = new HashMap<>();
    
    static {
        // Загрузка настроек по умолчанию для улучшений
        UPGRADES.put("health", new UpgradeSettings(5, 5));     // 5 уровней, стоимость 5 очков
        UPGRADES.put("resistance", new UpgradeSettings(3, 8)); // 3 уровня, стоимость 8 очков
        UPGRADES.put("miningspeed", new UpgradeSettings(5, 3)); // 5 уровней, стоимость 3 очка
    }
    
    public static void init() {
        // Создаем директорию для конфигов, если она не существует
        File configDir = new File(FMLPaths.GAMEDIR.get().toFile(), CONFIG_DIR);
        if (!configDir.exists() && !configDir.mkdirs()) {
            TutorialMod.LOGGER.error("Не удалось создать директорию конфигурации: " + configDir.getAbsolutePath());
            return;
        }
        
        // Инициализируем конфигурацию улучшений
        loadUpgradesConfig();
    }
    
    // Загружаем конфигурацию улучшений или создаем стандартную, если файл не существует
    private static void loadUpgradesConfig() {
        File configFile = new File(new File(FMLPaths.GAMEDIR.get().toFile(), CONFIG_DIR), UPGRADES_CONFIG_FILE);
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                upgradesConfig = GSON.fromJson(reader, UpgradesConfig.class);
                TutorialMod.LOGGER.info("Конфигурация улучшений загружена: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                TutorialMod.LOGGER.error("Ошибка при загрузке конфигурации улучшений", e);
                upgradesConfig = createDefaultConfig();
                saveUpgradesConfig();
            }
        } else {
            upgradesConfig = createDefaultConfig();
            saveUpgradesConfig();
        }
    }
    
    // Создаем стандартную конфигурацию
    private static UpgradesConfig createDefaultConfig() {
        UpgradesConfig config = new UpgradesConfig();
        
        // Стандартные стоимости улучшений
        config.upgradeCosts.put("health", 1);      // 1 очко за +1 к здоровью
        config.upgradeCosts.put("resistance", 1);  // 1 очко за +1% сопротивления урону
        config.upgradeCosts.put("miningspeed", 1); // 1 очко за +20% к скорости добычи
        
        // Максимальные уровни улучшений
        config.maxLevels.put("health", 20);        // Максимум 20 уровней здоровья
        config.maxLevels.put("resistance", 20);    // Максимум 20 уровней сопротивления
        config.maxLevels.put("miningspeed", 5);    // Максимум 5 уровней скорости добычи
        
        TutorialMod.LOGGER.info("Создана стандартная конфигурация улучшений");
        return config;
    }
    
    // Сохраняем конфигурацию в файл
    private static void saveUpgradesConfig() {
        File configFile = new File(new File(FMLPaths.GAMEDIR.get().toFile(), CONFIG_DIR), UPGRADES_CONFIG_FILE);
        
        try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(upgradesConfig, writer);
            TutorialMod.LOGGER.info("Конфигурация улучшений сохранена: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            TutorialMod.LOGGER.error("Ошибка при сохранении конфигурации улучшений", e);
        }
    }
    
    // Получаем стоимость улучшения
    public static int getUpgradeCost(String upgradeType) {
        return upgradesConfig.upgradeCosts.getOrDefault(upgradeType, 1);
    }
    
    // Получаем максимальный уровень улучшения
    public static int getMaxUpgradeLevel(String upgradeType) {
        return upgradesConfig.maxLevels.getOrDefault(upgradeType, 10);
    }
    
    // Обновляем конфигурацию из файла
    public static void reload() {
        loadUpgradesConfig();
        TutorialMod.LOGGER.info("Конфигурация улучшений перезагружена");
    }
    
    // Класс для хранения конфигурации улучшений
    public static class UpgradesConfig {
        public Map<String, Integer> upgradeCosts = new HashMap<>();
        public Map<String, Integer> maxLevels = new HashMap<>();
    }
} 