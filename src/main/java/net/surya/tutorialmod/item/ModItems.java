package net.surya.tutorialmod.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.surya.tutorialmod.TutorialMod;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, TutorialMod.MOD_ID);
            
    public static final RegistryObject<Item> NEURAL_CHIP = ITEMS.register("neural_chip",
            () -> new NeuralChipItem(new Item.Properties().stacksTo(1)));
            
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
} 