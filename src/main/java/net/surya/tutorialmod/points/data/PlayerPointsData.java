package net.surya.tutorialmod.points.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerPointsData {
    private int points = 0;
    private int experience = 0;
    private int level = 0;
    private final Map<String, Integer> upgrades = new HashMap<>();

    private static final int BASE_XP_FOR_NEXT_LEVEL = 100;
    private static final float LEVEL_SCALING_FACTOR = 1.5f;

    public void addExperience(int amount) {
        this.experience += amount;
        checkLevelUp();
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    private void checkLevelUp() {
        int xpForNextLevel = getXPForNextLevel();
        while (experience >= xpForNextLevel) {
            experience -= xpForNextLevel;
            level++;
            points += 1;
            xpForNextLevel = getXPForNextLevel();
        }
    }

    public int getXPForNextLevel() {
        return (int)(BASE_XP_FOR_NEXT_LEVEL * Math.pow(LEVEL_SCALING_FACTOR, level));
    }

    public boolean upgrade(String upgradeType) {
        if (points <= 0) {
            return false;
        }
        
        int currentLevel = upgrades.getOrDefault(upgradeType, 0);
        upgrades.put(upgradeType, currentLevel + 1);
        points--;
        return true;
    }

    public int getUpgradeLevel(String upgradeType) {
        return upgrades.getOrDefault(upgradeType, 0);
    }
    
    public void setUpgradeLevel(String upgradeType, int level) {
        upgrades.put(upgradeType, level);
    }

    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }

    public int getExperience() {
        return experience;
    }
    
    public void setExperience(int experience) {
        this.experience = experience;
    }

    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public Map<String, Integer> getUpgrades() {
        return new HashMap<>(upgrades);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("points", points);
        tag.putInt("experience", experience);
        tag.putInt("level", level);
        
        CompoundTag upgradesTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
            upgradesTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("upgrades", upgradesTag);
        
        return tag;
    }

    public void load(CompoundTag tag) {
        points = tag.getInt("points");
        experience = tag.getInt("experience");
        level = tag.getInt("level");
        
        upgrades.clear();
        CompoundTag upgradesTag = tag.getCompound("upgrades");
        for (String key : upgradesTag.getAllKeys()) {
            upgrades.put(key, upgradesTag.getInt(key));
        }
    }
} 