package com.dagonjira.client;

import com.dagonjira.DagonjiraMod;
import com.github.alexthe666.citadel.animation.Animation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.benstitans.DagonzillaEntity;
import net.mcreator.benstitans.DagonzillaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Fire-orange / yellow replacement for Dagonzilla's glow layer. Uses Ben's Titans'
 * DagonzillaModel#renderProgressiveGlow (phase 0..1 = how much of the spine is lit,
 * phase 10+ = travelling brightness wave) with Dagonjira's own glow textures.
 */
public class DagonjiraGlowLayer extends RenderLayer<DagonzillaEntity, DagonzillaModel> {
    private static final int FULL_BRIGHT = 15728880;

    private static final ResourceLocation GLOW = tex("dagonjira_glow");
    private static final ResourceLocation GLOW_POWERED = tex("dagonjira_glow_powered");
    private static final ResourceLocation THERMO_SKIN = tex("dagonjira_thermonuclear");
    private static final ResourceLocation GLOW_THERMO = tex("dagonjira_thermonuclear_glow");
    private static final ResourceLocation GLOW_THERMO_POWERED = tex("dagonjira_thermonuclear_glow_powered");

    public DagonjiraGlowLayer(RenderLayerParent<DagonzillaEntity, DagonzillaModel> parent) {
        super(parent);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(DagonjiraMod.MODID, "textures/entities/" + name + ".png");
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, DagonzillaEntity e, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        DagonzillaModel model = this.getParentModel();
        boolean thermo = e.getNuclearStacks() > 0;
        float t = e.tickCount + partialTick;

        // thermonuclear: scorched skin fades in over the first few stacks
        if (thermo) {
            float a = Mth.clamp(e.getNuclearStacks() / 3.0F, 0.0F, 1.0F);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(THERMO_SKIN));
            model.renderToBuffer(ps, vc, light, LivingEntityRenderer.getOverlayCoords(e, 0.0F), 1.0F, 1.0F, 1.0F, a);
        }

        Animation anim = e.getAnimation();
        float tick = e.getAnimationTick() + partialTick;
        float powered = 0.0F;
        float fill = 1.0F;
        float sweep = -1.0F;

        if (e.isFiring() || e.getBeamProgress(partialTick) > 0.0F) {
            powered = 1.0F;
        } else if (anim == DagonzillaEntity.ANIMATION_PREPARE_BREATH) {
            powered = 1.0F;
            fill = Mth.clamp(tick / 100.0F, 0.05F, 1.0F);
            sweep = tick * 0.06F;
        } else if (anim == DagonzillaEntity.ANIMATION_NUCLEAR_PULSE) {
            powered = 1.0F;
            fill = Mth.clamp(tick / 142.0F, 0.05F, 1.0F);
            sweep = tick * 0.06F;
        } else if (anim == DagonzillaEntity.ANIMATION_ROAR_1 || anim == DagonzillaEntity.ANIMATION_ROAR_2) {
            powered = 0.6F + 0.25F * Mth.sin(t * 0.5F);
        } else if (e.getCharge() >= DagonzillaEntity.MAX_CHARGE) {
            powered = 0.5F + 0.3F * Mth.sin(t * 0.3F);
        }

        float passive = thermo ? 0.85F + 0.15F * Mth.sin(t * 0.12F) : 0.45F + 0.1F * Mth.sin(t * 0.08F);

        glow(model, ps, buffers, thermo ? GLOW_THERMO : GLOW, 1.0F, passive);
        if (powered > 0.0F) {
            glow(model, ps, buffers, thermo ? GLOW_THERMO_POWERED : GLOW_POWERED, fill, powered);
        }
        if (sweep >= 0.0F) {
            glow(model, ps, buffers, thermo ? GLOW_THERMO_POWERED : GLOW_POWERED, 10.0F + sweep, 1.0F);
        }
    }

    private static void glow(DagonzillaModel model, PoseStack ps, MultiBufferSource buffers, ResourceLocation tex,
                             float phase, float alpha) {
        VertexConsumer vc = buffers.getBuffer(DagonjiraRenderTypes.glow(tex));
        model.renderProgressiveGlow(ps, vc, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, phase, alpha, alpha, alpha, alpha);
    }
}
