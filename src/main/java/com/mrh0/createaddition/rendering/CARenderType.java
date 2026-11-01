package com.mrh0.createaddition.rendering;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrh0.createaddition.CreateAddition;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * PORT-NOTE(26.1): render types are built from RenderPipelines now. The wire is vanilla's leash shader with QUADS
 * (vanilla's own leash pipeline is a triangle strip), no culling, lightmap.
 */
public final class CARenderType {
	private CARenderType() {
	}

	public static final RenderPipeline WIRE_PIPELINE = registerPipeline("wire", RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
			.withVertexShader("core/rendertype_leash")
			.withFragmentShader("core/rendertype_leash")
			.withSampler("Sampler2")
			.withCull(false)
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
			.withDepthStencilState(DepthStencilState.DEFAULT));

	@SuppressWarnings("deprecation")
	public static final RenderType WIRE = RenderType.create(CreateAddition.MODID + ":wire",
			RenderSetup.builder(WIRE_PIPELINE).useLightmap().createRenderSetup());

	private static RenderPipeline registerPipeline(String id, RenderPipeline.Builder builder) {
		Identifier location = CreateAddition.asResource("pipeline/" + id);
		RenderPipeline pipeline = builder.withLocation(location).build();
		RenderPipelines.PIPELINES_BY_LOCATION.put(location, pipeline);
		return pipeline;
	}
}
