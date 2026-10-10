package com.dagonjira.entity;

import com.dagonjira.DagonjiraMod;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Makes Dagonjira hit a little softer than Dagonzilla (melee, tail, stomp and beam alike). */
@Mod.EventBusSubscriber(modid = DagonjiraMod.MODID)
public class DagonjiraEvents {
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity source = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (source instanceof DagonjiraEntity || direct instanceof DagonjiraEntity) {
            event.setAmount(event.getAmount() * DagonjiraEntity.DAMAGE_MULTIPLIER);
        }
    }
}
