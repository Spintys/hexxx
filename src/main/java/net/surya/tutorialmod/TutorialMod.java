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

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TutorialMod.MOD_ID)
public class TutorialMod
{
    public static final String MOD_ID = "tutorialmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    static {
        // Явно выводим информацию о миксинах в лог
        LOGGER.info("Initializing TutorialMod with Mixins support");
    }

    public TutorialMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Регистрируем предметы и вкладки
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        // Регистрируем слушатели событий
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        // Регистрируем события
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PointsEvents.class);
        MinecraftForge.EVENT_BUS.register(PointsUpgradeEffects.class);
        
        // Остальные регистрации
        modEventBus.addListener(this::addCreative);
        
        // Инициализация конфигурации
        ModConfig.init();
        
        // Подтверждаем, что миксины должны быть загружены
        LOGGER.info("TutorialMod is using Mixins for gameplay modifications");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            // Регистрируем сетевые пакеты
            ModMessages.register();
            LOGGER.info("Points system initialized");
        });
    }
    
    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        // Регистрируем capability для хранения данных игрока
        event.register(PlayerPointsData.class);
        
        // Регистрируем capability для нейронного чипа
        event.register(NeuralChipData.class);
    }

    // Метод для добавления предметов в творческую вкладку
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        // Не требуется дополнительных действий, так как мы создали собственную вкладку
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Points system ready on server");
        
        // Инициализируем систему сохранения данных игроков
        PlayerDataManager.init(event.getServer());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Инициализация клиентской части, если нужно
        }
    }
}