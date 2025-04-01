package net.surya.tutorialmod.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.surya.tutorialmod.points.data.NeuralChipProvider;
import net.surya.tutorialmod.points.data.PlayerPointsData;
import net.surya.tutorialmod.points.data.PlayerPointsProvider;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMiningMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(
        method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
        at = @At("RETURN"),
        cancellable = true
    )
    private void modifyMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        LOGGER.info("[TutorialMod] Mining speed mixin called");

        float originalSpeed = cir.getReturnValue();
        LOGGER.info("[TutorialMod] Original mining speed: {}", originalSpeed);

        Player player = (Player) (Object) this;

        NeuralChipProvider.getNeuralChipData(player).ifPresent(chipData -> {
            if (chipData.hasChip()) {
                LOGGER.info("[TutorialMod] Player has neural chip");

                player.getCapability(PlayerPointsProvider.PLAYER_POINTS).ifPresent(cap -> {
                    if (cap instanceof PlayerPointsData) {
                        PlayerPointsData pointsData = (PlayerPointsData) cap;
                        int miningLevel = pointsData.getUpgradeLevel("miningspeed");

                        if (miningLevel > 0) {
                            float multiplier = 1.0f + (miningLevel * 0.2f);
                            float newSpeed = originalSpeed * multiplier;

                            LOGGER.info("[TutorialMod] Mining level: {}", miningLevel);
                            LOGGER.info("[TutorialMod] Speed multiplier: {}", multiplier);
                            LOGGER.info("[TutorialMod] New mining speed: {}", newSpeed);

                            cir.setReturnValue(newSpeed);
                        }
                    }
                });
            }
        });
    }
}
