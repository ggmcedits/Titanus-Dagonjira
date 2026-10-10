package com.dagonjira;

import com.dagonjira.entity.DagonjiraEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Dagonjira: an addon for Ben's Titans that adds a smaller, slightly weaker Dagonzilla variant.
 */
@Mod(DagonjiraMod.MODID)
public class DagonjiraMod {
    public static final String MODID = "dagonjira";

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Same registered size as Dagonzilla (5 x 11.7). DagonjiraEntity#getScale() shrinks the live hitbox,
    // model, beam origin and shadow by the same factor so everything stays consistent.
    public static final RegistryObject<EntityType<DagonjiraEntity>> DAGONJIRA = ENTITIES.register("dagonjira",
            () -> EntityType.Builder.<DagonjiraEntity>of(DagonjiraEntity::new, MobCategory.MONSTER)
                    .sized(5.0F, 11.7F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .build("dagonjira"));

    public static final RegistryObject<Item> DAGONJIRA_SPAWN_EGG = ITEMS.register("dagonjira_spawn_egg",
            () -> new ForgeSpawnEggItem(DAGONJIRA, 0x23262B, 0xFF7A1A, new Item.Properties()));

    public DagonjiraMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onCreativeTab);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(DAGONJIRA.get(), DagonjiraEntity.createDagonjiraAttributes().build());
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(DAGONJIRA_SPAWN_EGG);
        }
    }
}
