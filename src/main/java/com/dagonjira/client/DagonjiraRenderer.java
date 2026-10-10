package com.dagonjira.client;

import com.dagonjira.DagonjiraMod;
import com.github.alexthe666.citadel.animation.Animation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mcreator.benstitans.DagonzillaEntity;
import net.mcreator.benstitans.DagonzillaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Re-uses Ben's Titans' Dagonzilla model + animations (so every animation works unchanged) with:
 * the Dagonjira skin (2014-style dorsal fins), a scaled-down pose, fire-coloured glow layer,
 * and our own flame-style atomic breath / charge emission.
 *
 * Generic type is DagonzillaEntity because DagonzillaModel is an EntityModel&lt;DagonzillaEntity&gt;.
 */
public class DagonjiraRenderer extends MobRenderer<DagonzillaEntity, DagonzillaModel> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(DagonjiraMod.MODID, "textures/entities/dagonjira.png");

    public DagonjiraRenderer(EntityRendererProvider.Context context) {
        super(context, new DagonzillaModel(), 4.0F);
        this.addLayer(new DagonjiraGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(DagonzillaEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(DagonzillaEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.getScale();
        poseStack.scale(s, s, s);
    }

    @Override
    public boolean shouldRender(DagonzillaEntity entity, Frustum frustum, double x, double y, double z) {
        // the beam can be far longer than the body, so never cull while it is visible
        return entity.getBeamProgress(1.0F) > 0.0F || super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(DagonzillaEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        this.shadowRadius = 4.0F * entity.getScale();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);

        float time = entity.tickCount + partialTick;
        float scale = entity.getScale();

        float beamProgress = entity.getBeamProgress(partialTick);
        Vec3 end = entity.getClientBeamEndPosition(partialTick);

        if (beamProgress > 0.0F && entity.isAlive() && end != null) {
            Vec3 mouth = mouthOffset(entity, partialTick);
            Vec3 wobble = new Vec3(Mth.sin(time * 4.0F) * 0.075D, Mth.sin(time * 4.0F + 1.2F) * 0.075D,
                    Mth.sin(time * 4.0F + 2.4F) * 0.075D);
            Vec3 diff = end.subtract(entity.getPosition(partialTick).add(mouth));
            float length = (float) diff.length();
            if (length > 0.01F) {
                DagonjiraFlameEffects.renderBeam(poseStack, buffers, mouth.add(wobble), diff.scale(1.0D / length),
                        length, beamProgress, time, scale);
            }
        } else if (!entity.isFiring()) {
            Animation anim = entity.getAnimation();
            float tick = entity.getAnimationTick() + partialTick;
            if (anim == DagonzillaEntity.ANIMATION_PREPARE_BREATH) {
                DagonjiraFlameEffects.renderCharge(poseStack, buffers, mouthOffset(entity, partialTick),
                        Mth.clamp(tick / 100.0F, 0.0F, 1.0F), time, entity.getId(), scale, false);
            } else if (anim == DagonzillaEntity.ANIMATION_NUCLEAR_PULSE) {
                DagonjiraFlameEffects.renderCharge(poseStack, buffers, new Vec3(0.0D, 6.0D * scale, 0.0D),
                        Mth.clamp(tick / 142.0F, 0.0F, 1.0F), time, entity.getId(), scale, true);
            }
        }
    }

    /** Mouth position relative to the entity, computed exactly like Ben's Titans does, then scaled. */
    private Vec3 mouthOffset(DagonzillaEntity entity, float partialTick) {
        float bodyYaw = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        Vec3 mouth = this.getModel().getMouthPosition(new Vec3(0.0D, 0.1D, 0.0D));
        return mouth.yRot((float) (Math.PI - bodyYaw * 0.0174533F)).scale(entity.getScale());
    }
}
