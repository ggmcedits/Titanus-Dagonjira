package com.dagonjira.client;

import com.dagonjira.DagonjiraMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Procedural geometry for Dagonjira's atomic breath and charge emission.
 *
 * <p>The beam is drawn the way the 2014 film's breath reads on screen: a white-hot core wrapped in
 * turbulent, rolling orange flame sheets, with flame "tongues" spiralling around it and a fireball where it lands.
 * Everything is additive + emissive, so it stays an atomic breath rather than a plain flamethrower.</p>
 */
public final class DagonjiraFlameEffects {
    private static final ResourceLocation FLAME = tex("dagonjira_beam_flame");
    private static final ResourceLocation CORE = tex("dagonjira_beam_inner");
    private static final ResourceLocation ORB = tex("dagonjira_orb");
    private static final ResourceLocation STREAK = tex("dagonjira_streak");

    private static final int FULL_BRIGHT = 15728880;
    private static final float TAU = (float) (Math.PI * 2.0D);

    private DagonjiraFlameEffects() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(DagonjiraMod.MODID, "textures/entities/" + name + ".png");
    }

    // ------------------------------------------------------------------------------------------------------------
    // ATOMIC BREATH
    // ------------------------------------------------------------------------------------------------------------

    /**
     * @param start    beam origin relative to the entity (the poseStack origin)
     * @param dir      normalised direction of the beam
     * @param length   beam length in blocks
     * @param progress 0..1 beam growth
     * @param time     entity tickCount + partialTick
     * @param scale    entity scale
     */
    public static void renderBeam(PoseStack ps, MultiBufferSource buffers, Vec3 start, Vec3 dir, float length,
                                  float progress, float time, float scale) {
        ps.pushPose();
        ps.translate(start.x, start.y, start.z);
        ps.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F),
                new Vector3f((float) dir.x, (float) dir.y, (float) dir.z)));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        float flicker = 0.88F + 0.12F * Mth.sin(time * 1.9F);
        float p = Mth.clamp(progress, 0.0F, 1.0F);

        // outer flame sheet: wide, very turbulent, deep orange
        VertexConsumer vc = buffers.getBuffer(DagonjiraRenderTypes.glow(FLAME));
        tube(vc, m, n, length, 1.45F * scale * p, 10, 0.34F, time, 0.15F, 1.7F, 0.11F, 1.0F, 0.42F, 0.06F, 0.55F * flicker, 0.0F);
        // middle flame sheet: orange -> yellow
        tube(vc, m, n, length, 1.0F * scale * p, 8, 0.2F, time, -0.22F, 2.6F, 0.19F, 1.0F, 0.68F, 0.14F, 0.78F * flicker, 2.1F);
        // flame tongues spiralling round the beam
        tongues(vc, m, n, length, 1.25F * scale * p, 6, time, 1.0F, 0.55F, 0.1F, 0.7F * flicker);

        // white-hot plasma core
        VertexConsumer core = buffers.getBuffer(DagonjiraRenderTypes.glow(CORE));
        tube(core, m, n, length, 0.58F * scale * p, 8, 0.09F, time, 0.3F, 3.4F, 0.25F, 1.0F, 0.95F, 0.7F, 0.95F * flicker, 4.2F);

        // fireballs: one at the mouth, a much bigger one where the breath lands
        VertexConsumer orb = buffers.getBuffer(DagonjiraRenderTypes.glow(ORB));
        orbCross(orb, m, n, 0.0F, 0.0F, 0.0F, 2.8F * scale * p, time, 1.0F, 0.8F, 0.35F, 0.9F);
        orbCross(orb, m, n, 0.0F, length, 0.0F, 7.0F * scale * p, time, 1.0F, 0.55F, 0.12F, 0.85F * flicker);
        orbCross(orb, m, n, 0.0F, length, 0.0F, 3.6F * scale * p, -time, 1.0F, 0.92F, 0.6F, 0.9F);

        ps.popPose();
    }

    private static float noise(float y, float th, float t, float phase) {
        return 0.5F * Mth.sin(y * 0.55F - t * 0.45F + 3.0F * th + phase)
                + 0.3F * Mth.sin(y * 1.3F - t * 0.85F - 2.0F * th)
                + 0.2F * Mth.sin(y * 2.7F - t * 1.5F + th);
    }

    /** Beam radius profile: narrow at the mouth, blooms over a few blocks, then slowly fans out. */
    private static float profile(float y) {
        float ramp = Mth.clamp(y / 3.0F, 0.0F, 1.0F);
        ramp = ramp * ramp * (3.0F - 2.0F * ramp);
        return (0.35F + 0.65F * ramp) * (1.0F + 0.5F * Math.min(1.0F, y / 40.0F));
    }

    private static void tube(VertexConsumer vc, Matrix4f m, Matrix3f n, float length, float baseRadius, int sides,
                             float turbulence, float time, float spin, float scrollSpeed, float vScale,
                             float r, float g, float b, float a, float phase) {
        if (baseRadius <= 0.001F || length <= 0.01F) {
            return;
        }
        float seg = Math.max(0.9F, length / 80.0F);
        int segs = Math.max(1, (int) Math.ceil(length / seg));
        float[][] rx = new float[segs + 1][sides];
        float[][] rz = new float[segs + 1][sides];
        float[] ys = new float[segs + 1];
        for (int k = 0; k <= segs; k++) {
            float y = Math.min(length, k * seg);
            ys[k] = y;
            float rad = baseRadius * profile(y);
            for (int i = 0; i < sides; i++) {
                float th = i * TAU / sides;
                float rr = rad * (1.0F + turbulence * noise(y, th, time, phase));
                float ang = th + time * spin * 0.1F + y * 0.04F;
                rx[k][i] = Mth.cos(ang) * rr;
                rz[k][i] = Mth.sin(ang) * rr;
            }
        }
        float scroll = time * scrollSpeed * 0.05F;
        for (int k = 0; k < segs; k++) {
            float v0 = ys[k] * vScale - scroll;
            float v1 = ys[k + 1] * vScale - scroll;
            // fade the far end of very long beams so it never ends in a hard cap
            float tail = 1.0F - 0.45F * Mth.clamp((ys[k + 1] - length * 0.7F) / Math.max(1.0F, length * 0.3F), 0.0F, 1.0F);
            for (int i = 0; i < sides; i++) {
                int j = (i + 1) % sides;
                float u0 = i / (float) sides;
                float u1 = (i + 1) / (float) sides;
                vert(vc, m, n, rx[k][i], ys[k], rz[k][i], u0, v0, r, g, b, a);
                vert(vc, m, n, rx[k][j], ys[k], rz[k][j], u1, v0, r, g, b, a);
                vert(vc, m, n, rx[k + 1][j], ys[k + 1], rz[k + 1][j], u1, v1, r, g, b, a * tail);
                vert(vc, m, n, rx[k + 1][i], ys[k + 1], rz[k + 1][i], u0, v1, r, g, b, a * tail);
            }
        }
    }

    private static void tongues(VertexConsumer vc, Matrix4f m, Matrix3f n, float length, float baseRadius, int count,
                                float time, float r, float g, float b, float a) {
        if (baseRadius <= 0.001F || length <= 0.01F) {
            return;
        }
        float seg = Math.max(0.9F, length / 70.0F);
        int segs = Math.max(1, (int) Math.ceil(length / seg));
        for (int c = 0; c < count; c++) {
            float base = c * TAU / count;
            for (int k = 0; k < segs; k++) {
                float y0 = Math.min(length, k * seg);
                float y1 = Math.min(length, (k + 1) * seg);
                float[] p0 = tonguePoint(y0, base, baseRadius, time, c);
                float[] p1 = tonguePoint(y1, base, baseRadius, time, c);
                float w0 = 0.55F * baseRadius * Mth.clamp(y0 / 2.0F, 0.0F, 1.0F) * (0.7F + 0.3F * Mth.sin(y0 * 0.8F - time * 0.9F + c));
                float w1 = 0.55F * baseRadius * Mth.clamp(y1 / 2.0F, 0.0F, 1.0F) * (0.7F + 0.3F * Mth.sin(y1 * 0.8F - time * 0.9F + c));
                float v0 = y0 * 0.14F - time * 0.09F;
                float v1 = y1 * 0.14F - time * 0.09F;
                // ribbon lying on the surface (width along the tangent) ...
                vert(vc, m, n, p0[0] - p0[2] * w0, y0, p0[1] + p0[3] * w0, 0.0F, v0, r, g, b, a);
                vert(vc, m, n, p0[0] + p0[2] * w0, y0, p0[1] - p0[3] * w0, 1.0F, v0, r, g, b, a);
                vert(vc, m, n, p1[0] + p1[2] * w1, y1, p1[1] - p1[3] * w1, 1.0F, v1, r, g, b, a);
                vert(vc, m, n, p1[0] - p1[2] * w1, y1, p1[1] + p1[3] * w1, 0.0F, v1, r, g, b, a);
            }
        }
    }

    /** returns {x, z, tangentX, tangentZ} of a flame tongue at height y. */
    private static float[] tonguePoint(float y, float base, float baseRadius, float time, int c) {
        float th = base + time * 0.045F + y * 0.11F;
        float rr = baseRadius * profile(y) * (1.0F + 0.35F * noise(y, th, time, c * 1.7F));
        float cos = Mth.cos(th);
        float sin = Mth.sin(th);
        return new float[]{cos * rr, sin * rr, -sin, cos};
    }

    /** Three crossed glow quads rotating about the beam axis: reads as a soft glowing ball from any angle. */
    private static void orbCross(VertexConsumer vc, Matrix4f m, Matrix3f n, float cx, float cy, float cz, float size,
                                 float time, float r, float g, float b, float a) {
        if (size <= 0.01F) {
            return;
        }
        float h = size * 0.5F;
        float rot = time * 0.07F;
        float cr = Mth.cos(rot) * h;
        float sr = Mth.sin(rot) * h;
        // vertical quad 1 and 2 (rotated about Y)
        quadVertical(vc, m, n, cx, cy, cz, cr, sr, h, r, g, b, a);
        quadVertical(vc, m, n, cx, cy, cz, -sr, cr, h, r, g, b, a);
        // horizontal quad
        vert(vc, m, n, cx - cr, cy, cz - sr, 0.0F, 0.0F, r, g, b, a);
        vert(vc, m, n, cx + sr, cy, cz - cr, 1.0F, 0.0F, r, g, b, a);
        vert(vc, m, n, cx + cr, cy, cz + sr, 1.0F, 1.0F, r, g, b, a);
        vert(vc, m, n, cx - sr, cy, cz + cr, 0.0F, 1.0F, r, g, b, a);
    }

    private static void quadVertical(VertexConsumer vc, Matrix4f m, Matrix3f n, float cx, float cy, float cz,
                                     float dx, float dz, float h, float r, float g, float b, float a) {
        vert(vc, m, n, cx - dx, cy - h, cz - dz, 0.0F, 1.0F, r, g, b, a);
        vert(vc, m, n, cx + dx, cy - h, cz + dz, 1.0F, 1.0F, r, g, b, a);
        vert(vc, m, n, cx + dx, cy + h, cz + dz, 1.0F, 0.0F, r, g, b, a);
        vert(vc, m, n, cx - dx, cy + h, cz - dz, 0.0F, 0.0F, r, g, b, a);
    }

    // ------------------------------------------------------------------------------------------------------------
    // CHARGE EMISSION (before the breath / during the nuclear pulse)
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Fire-coloured energy being drawn into the mouth (or, for the pulse, into the chest): a growing fireball with
     * streaks of orange/yellow light racing inward.
     *
     * @param center position relative to the entity (poseStack origin)
     * @param charge 0..1
     */
    public static void renderCharge(PoseStack ps, MultiBufferSource buffers, Vec3 center, float charge, float time,
                                    int entityId, float scale, boolean pulse) {
        if (charge <= 0.01F) {
            return;
        }
        ps.pushPose();
        ps.translate(center.x, center.y, center.z);
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        float pulseWobble = 1.0F + 0.08F * Mth.sin(time * 1.3F);
        VertexConsumer orb = buffers.getBuffer(DagonjiraRenderTypes.glow(ORB));
        float size = (pulse ? 3.0F + 9.0F * charge : 1.2F + 5.5F * charge) * scale * pulseWobble;
        orbCross(orb, m, n, 0.0F, 0.0F, 0.0F, size, time, 1.0F, 0.5F, 0.1F, 0.55F + 0.4F * charge);
        orbCross(orb, m, n, 0.0F, 0.0F, 0.0F, size * 0.55F, -time, 1.0F, 0.9F, 0.55F, 0.6F + 0.4F * charge);

        VertexConsumer streaks = buffers.getBuffer(DagonjiraRenderTypes.glow(STREAK));
        int count = (int) (14 + 46 * charge);
        float maxDist = (pulse ? 14.0F : 8.0F) * scale;
        float len = (pulse ? 3.2F : 2.0F) * scale;
        float width = (pulse ? 0.3F : 0.2F) * scale;
        for (int i = 0; i < count; i++) {
            Random rng = new Random(entityId * 7919L + i * 104729L);
            float theta = rng.nextFloat() * TAU;
            float cosPhi = rng.nextFloat() * 2.0F - 1.0F;
            float sinPhi = Mth.sqrt(1.0F - cosPhi * cosPhi);
            float dx = sinPhi * Mth.cos(theta);
            float dy = cosPhi;
            float dz = sinPhi * Mth.sin(theta);
            float f = (time * (0.035F + 0.03F * rng.nextFloat()) + rng.nextFloat()) % 1.0F;   // 0 far .. 1 arrived
            float dist = maxDist * (1.0F - f) * (0.55F + 0.45F * rng.nextFloat()) + 0.4F * scale;
            float fade = Mth.sin(f * (float) Math.PI);
            // streak runs from dist -> dist - len along the direction (towards the centre)
            float d0 = dist;
            float d1 = Math.max(0.0F, dist - len);
            // two perpendicular vectors to the direction
            float ux = -dz, uy = 0.0F, uz = dx;
            float ul = Mth.sqrt(ux * ux + uz * uz);
            if (ul < 1.0E-3F) {
                ux = 1.0F;
                uz = 0.0F;
                ul = 1.0F;
            }
            ux = ux / ul * width;
            uz = uz / ul * width;
            float vx = dy * uz - dz * uy;   // dir x u (already scaled by width)
            float vy = dz * ux - dx * uz;
            float vz = dx * uy - dy * ux;
            float al = (0.35F + 0.65F * charge) * fade;
            float gC = 0.55F + 0.45F * rng.nextFloat();
            for (int q = 0; q < 2; q++) {
                float sx = q == 0 ? ux : vx;
                float sy = q == 0 ? uy : vy;
                float sz = q == 0 ? uz : vz;
                vert(streaks, m, n, dx * d0 - sx, dy * d0 - sy, dz * d0 - sz, 0.0F, 0.0F, 1.0F, gC, 0.15F, al);
                vert(streaks, m, n, dx * d0 + sx, dy * d0 + sy, dz * d0 + sz, 1.0F, 0.0F, 1.0F, gC, 0.15F, al);
                vert(streaks, m, n, dx * d1 + sx, dy * d1 + sy, dz * d1 + sz, 1.0F, 1.0F, 1.0F, gC, 0.15F, al);
                vert(streaks, m, n, dx * d1 - sx, dy * d1 - sy, dz * d1 - sz, 0.0F, 1.0F, 1.0F, gC, 0.15F, al);
            }
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------------------------------------------------

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v,
                             float r, float g, float b, float a) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, Mth.clamp(a, 0.0F, 1.0F))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
