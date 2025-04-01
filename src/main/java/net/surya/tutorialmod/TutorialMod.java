package net.surya.tutorialmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import net.surya.tutorialmod.points.PointsEvents;
import net.surya.tutorialmod.points.PointsUpgradeEffects;
import net.surya.tutorialmod.points.data.NeuralChipData;
import net.surya.tutorialmod.points.data.NeuralChipProvider;
import net.surya.tutorialmod.points.data.PlayerDataManager;
import net.surya.tutorialmod.points.data.PlayerPointsData;
import net.surya.tutorialmod.points.data.PlayerPointsProvider;
import net.surya.tutorialmod.networking.ModMessages;
import net.surya.tutorialmod.item.ModCreativeModeTabs;
import net.surya.tutorialmod.item.ModItems;
import net.surya.tutorialmod.config.ModConfig;

@Mod(TutorialMod.MOD_ID)
public class TutorialMod
{
    public static final String MOD_ID = "tutorialmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    static {
        LOGGER.info("Initializing TutorialMod with Mixins support");
        Mixins.addConfiguration("mixins.tutorialmod.json");
    }

    public TutorialMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PointsEvents.class);
        MinecraftForge.EVENT_BUS.register(PointsUpgradeEffects.class);
        
        modEventBus.addListener(this::addCreative);
        
        ModConfig.init();
        
        LOGGER.info("TutorialMod is using Mixins for gameplay modifications");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            ModMessages.register();
            LOGGER.info("Points system initialized");
        });
    }
    
    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.register(PlayerPointsData.class);
        
        event.register(NeuralChipData.class);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Points system ready on server");
        
        PlayerDataManager.init(event.getServer());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }
    }
}