package net.surya.tutorialmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.surya.tutorialmod.networking.ModMessages;
import net.surya.tutorialmod.networking.packet.ExperienceGainS2CPacket;
import net.surya.tutorialmod.points.data.NeuralChipData;
import net.surya.tutorialmod.points.data.NeuralChipProvider;
import net.surya.tutorialmod.points.data.PlayerPointsProvider;

import javax.annotation.Nullable;
import java.util.List;

public class NeuralChipItem extends Item {
    public NeuralChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            NeuralChipProvider.getNeuralChipData(player).ifPresent(neuralChipData -> {
                if (!neuralChipData.hasChip()) {
                    neuralChipData.setHasChip(true);
                    
                    if (!player.getAbilities().instabuild) {
                        itemStack.shrink(1);
                    }
                    
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                            1.0F, 1.0F);
                    
                    player.displayClientMessage(Component.translatable("message.tutorialmod.neural_chip_implanted")
                            .withStyle(ChatFormatting.GREEN), true);
                    
                    PlayerPointsProvider.getPlayerPoints(player).ifPresent(pointsData -> {
                        ModMessages.sendToPlayer(new ExperienceGainS2CPacket(
                                0, pointsData.getExperience(), 
                                pointsData.getXPForNextLevel(), pointsData.getLevel(), 
                                pointsData.getPoints()), serverPlayer);
                    });
                } else {
                    player.displayClientMessage(Component.translatable("message.tutorialmod.neural_chip_already_implanted")
                            .withStyle(ChatFormatting.RED), true);
                }
            });
        }
        
        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        components.add(Component.translatable("tooltip.tutorialmod.neural_chip")
                .withStyle(ChatFormatting.GRAY));
        components.add(Component.translatable("tooltip.tutorialmod.neural_chip_usage")
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        
        super.appendHoverText(stack, level, components, flag);
    }
} 