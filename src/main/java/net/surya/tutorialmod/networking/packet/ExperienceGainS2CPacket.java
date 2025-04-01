package net.surya.tutorialmod.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExperienceGainS2CPacket {
    private final int expGained;
    private final int currentExp;
    private final int expForNext;
    private final int level;
    private final int points;

    public ExperienceGainS2CPacket(int expGained, int currentExp, int expForNext, int level, int points) {
        this.expGained = expGained;
        this.currentExp = currentExp;
        this.expForNext = expForNext;
        this.level = level;
        this.points = points;
    }

    public ExperienceGainS2CPacket(FriendlyByteBuf buf) {
        this.expGained = buf.readInt();
        this.currentExp = buf.readInt();
        this.expForNext = buf.readInt();
        this.level = buf.readInt();
        this.points = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(expGained);
        buf.writeInt(currentExp);
        buf.writeInt(expForNext);
        buf.writeInt(level);
        buf.writeInt(points);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                LocalPlayer player = Minecraft.getInstance().player;
                
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.tutorialmod.exp_gain", expGained), true);
                    
                    if (points > 0) {
                        Minecraft.getInstance().gui.setTitle(Component.translatable("message.tutorialmod.level_up", level));
                        Minecraft.getInstance().gui.setSubtitle(Component.translatable("message.tutorialmod.points_available", points));
                        Minecraft.getInstance().gui.setTimes(10, 70, 20);
                    }
                }
            });
        });
        return true;
    }
} 