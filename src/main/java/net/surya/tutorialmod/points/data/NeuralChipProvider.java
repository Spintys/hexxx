package net.surya.tutorialmod.points.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.surya.tutorialmod.TutorialMod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeuralChipProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<NeuralChipData> NEURAL_CHIP = CapabilityManager.get(new CapabilityToken<>(){});
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(TutorialMod.MOD_ID, "neural_chip");

    private NeuralChipData chipData = null;
    private final LazyOptional<NeuralChipData> optional = LazyOptional.of(this::createChipData);

    private NeuralChipData createChipData() {
        if (chipData == null) {
            chipData = new NeuralChipData();
        }
        return chipData;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return NEURAL_CHIP.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return createChipData().save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createChipData().load(nbt);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(NeuralChipData.class);
    }

    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(IDENTIFIER, new NeuralChipProvider());
        }
    }

    public static LazyOptional<NeuralChipData> getNeuralChipData(Player player) {
        return player.getCapability(NEURAL_CHIP);
    }
} 