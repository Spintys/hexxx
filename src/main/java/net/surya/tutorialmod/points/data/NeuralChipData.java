package net.surya.tutorialmod.points.data;

import net.minecraft.nbt.CompoundTag;

public class NeuralChipData {
    private boolean hasChip = false;
    
    public boolean hasChip() {
        return hasChip;
    }
    
    public void setHasChip(boolean hasChip) {
        this.hasChip = hasChip;
    }
    
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("hasChip", hasChip);
        return tag;
    }
    
    public void load(CompoundTag tag) {
        hasChip = tag.getBoolean("hasChip");
    }
} 