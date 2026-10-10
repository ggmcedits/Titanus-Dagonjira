package com.dagonjira.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Additive, emissive, double-sided render type used for every glowing part of Dagonjira
 * (glow layers, flame beam, charge orb and rays). Extends RenderType only to reach its protected render-state shards.
 */
public abstract class DagonjiraRenderTypes extends RenderType {
    private static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>();

    private DagonjiraRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                 boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    /** Additive "fire" glow: texture colour * vertex colour added on top of whatever is behind it, no culling. */
    public static RenderType glow(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, tex -> RenderType.create(
                "dagonjira_glow",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                262144,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_EYES_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)));
    }
}
