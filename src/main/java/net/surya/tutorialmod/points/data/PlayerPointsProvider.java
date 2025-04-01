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

public class PlayerPointsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<PlayerPointsData> PLAYER_POINTS = CapabilityManager.get(new CapabilityToken<>(){});
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(TutorialMod.MOD_ID, "points");

    private PlayerPointsData points = null;
    private final LazyOptional<PlayerPointsData> optional = LazyOptional.of(this::createPlayerPoints);

    private PlayerPointsData createPlayerPoints() {
        if (points == null) {
            points = new PlayerPointsData();
        }
        return points;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return PLAYER_POINTS.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return createPlayerPoints().save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerPoints().load(nbt);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerPointsData.class);
    }

    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(IDENTIFIER, new PlayerPointsProvider());
        }
    }

    public static LazyOptional<PlayerPointsData> getPlayerPoints(Player player) {
        return player.getCapability(PLAYER_POINTS);
    }
} 