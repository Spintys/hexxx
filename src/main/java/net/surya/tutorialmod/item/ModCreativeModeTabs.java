package net.surya.tutorialmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.surya.tutorialmod.TutorialMod;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TutorialMod.MOD_ID);
    
    public static final RegistryObject<CreativeModeTab> CYBERNETICS_TAB = CREATIVE_MODE_TABS.register("cybernetics",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.NEURAL_CHIP.get()))
                    .title(Component.translatable("itemGroup.tutorialmod.cybernetics"))
                    .displayItems((pParameters, pOutput) -> {
                        // Добавляем нейронный чип в вкладку
                        pOutput.accept(ModItems.NEURAL_CHIP.get());
                    })
                    .build());
    
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
} 