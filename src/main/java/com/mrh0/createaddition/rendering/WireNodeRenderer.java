package com.mrh0.createaddition.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.energy.IWireNode;
import com.mrh0.createaddition.energy.WireType;
import com.mrh0.createaddition.event.ClientEventHandler;
import com.mrh0.createaddition.index.CAPartials;
import com.mrh0.createaddition.util.ClientMinecraftWrapper;
import com.mrh0.createaddition.util.Util;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * PORT-NOTE(26.1): extract/submit. Each wire (and the wire being placed) is extracted as a {@link Wire} with its
 * endpoints, type and light; submit draws it through {@code submitCustomGeometry} in {@link CARenderType#WIRE}, and
 * festive lights as Create Fly SuperByteBuffer states. The Sable sub-level path is gone with the Sable compat (see
 * PORTING-26.1.md). The render bounding box lives on the block entities ({@link #wireBounds}).
 */
public class WireNodeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, WireNodeRenderer.WireRenderState> {
	public WireNodeRenderer(BlockEntityRendererProvider.Context context) {
	}

	private static final float HANG = 0.5f;

	public record Wire(float tx, float ty, float tz, float x, float y, float z, WireType type, float dis,
					   int blockLightTo, int blockLightFrom, int skyLightTo, int skyLightFrom, BlockState state) {
	}

	public static class WireRenderState extends BlockEntityRenderState {
		public final List<Wire> wires = new ArrayList<>();
		public final List<SuperByteBufferRenderState> lights = new ArrayList<>();
	}

	@Override
	public WireRenderState createRenderState() {
		return new WireRenderState();
	}

	// The wires drawn here reach far past the connector block, so cull by the wires rather than the block.
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** The wires' bounds, for the block entity's render bounding box. */
	public static AABB wireBounds(BlockEntity be) {
		IWireNode te = (IWireNode) be;
		BlockPos pos = be.getBlockPos();
		int minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
		int maxX = minX, maxY = minY, maxZ = minZ;
		for (int i = 0; i < te.getNodeCount(); i++) {
			BlockPos other = te.getNodePos(i);
			if (other == null) continue;
			minX = Math.min(minX, other.getX());
			minY = Math.min(minY, other.getY());
			minZ = Math.min(minZ, other.getZ());
			maxX = Math.max(maxX, other.getX());
			maxY = Math.max(maxY, other.getY());
			maxZ = Math.max(maxZ, other.getZ());
		}
		// One block of margin covers the sag.
		AABB box = new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 2, maxZ + 2);
		if (be.getLevel() != null && be.getLevel().isClientSide() && ClientEventHandler.clientRenderHeldWire) {
			// A wire being placed runs to the player.
			LocalPlayer player = ClientMinecraftWrapper.getPlayer();
			Util.Triple<BlockPos, Integer, WireType> wireNode = player == null ? null : Util.getWireNodeOfSpools(player.getInventory().getSelectedItem());
			if (wireNode != null && wireNode.a.equals(pos)) box = box.minmax(player.getBoundingBox());
		}
		return box;
	}

	// Measured to the nearest part of the wires, so a long wire doesn't vanish when its connector is far away.
	@Override
	public boolean shouldRender(T be, Vec3 cameraPos) {
		double distance = getViewDistance();
		return wireBounds(be).distanceToSqr(cameraPos) < distance * distance;
	}

	@Override
	public void extractRenderState(T be, WireRenderState state, float partialTicks, Vec3 cameraPos, @Nullable CrumblingOverlay crumblingOverlay) {
		SmartBlockEntityRenderer.extractBase(be, state, crumblingOverlay);
		state.wires.clear();
		state.lights.clear();
		IWireNode te = (IWireNode) be;
		Level level = be.getLevel();
		if (level == null) return;

		for (int i = 0; i < te.getNodeCount(); i++) {
			if (!te.hasConnection(i)) continue;
			Vec3 d1 = te.getNodeOffset(i);
			float ox1 = ((float) d1.x());
			float oy1 = ((float) d1.y());
			float oz1 = ((float) d1.z());

			// The other end may be outside the client's loaded chunks; aim at its block centre then.
			IWireNode wn = te.getWireNode(i);
			Vec3 d2 = wn != null ? wn.getNodeOffset(te.getOtherNodeIndex(i)) : Vec3.ZERO; // get other
			float ox2 = ((float) d2.x());
			float oy2 = ((float) d2.y());
			float oz2 = ((float) d2.z());
			BlockPos other = te.getNodePos(i);

			float tx = other.getX() - te.getPos().getX();
			float ty = other.getY() - te.getPos().getY();
			float tz = other.getZ() - te.getPos().getZ();
			float dis = distanceFromZero(tx, ty, tz);
			addWire(state, be, other, tx + .5f + ox2, ty + .5f + oy2, tz + .5f + oz2,
					-tx - ox2 + ox1, -ty - oy2 + oy1, -tz - oz2 + oz1, te.getNodeType(i), dis);
		}

		if (ClientEventHandler.clientRenderHeldWire) {
			LocalPlayer player = ClientMinecraftWrapper.getPlayer();
			if (player == null) return;
			Util.Triple<BlockPos, Integer, WireType> wireNode = Util.getWireNodeOfSpools(player.getInventory().getSelectedItem());
			if (wireNode == null) return;

			BlockPos nodePos = wireNode.a;
			int nodeIndex = wireNode.b;
			WireType wireType = wireNode.c;
			if (!nodePos.equals(te.getPos())) return;

			Vec3 d1 = te.getNodeOffset(nodeIndex);
			float ox1 = ((float) d1.x());
			float oy1 = ((float) d1.y());
			float oz1 = ((float) d1.z());

			Vec3 playerPos = player.getPosition(partialTicks);
			float tx = (float) playerPos.x - te.getPos().getX();
			float ty = (float) playerPos.y - te.getPos().getY();
			float tz = (float) playerPos.z - te.getPos().getZ();
			float dis = distanceFromZero(tx, ty, tz);
			addWire(state, be, player.blockPosition(), tx + .5f, ty + .5f, tz + .5f, -tx + ox1, -ty + oy1, -tz + oz1, wireType, dis);
		}
	}

	private static void addWire(WireRenderState state, BlockEntity be, BlockPos other, float tx, float ty, float tz,
								float x, float y, float z, WireType type, float dis) {
		if (type == null) return;
		Level level = be.getLevel();
		BlockPos pos = be.getBlockPos();
		int i = level.getBrightness(LightLayer.BLOCK, pos);
		int j = level.getBrightness(LightLayer.BLOCK, other);
		int k = level.getBrightness(LightLayer.SKY, pos);
		int l = level.getBrightness(LightLayer.SKY, other);
		Wire wire = new Wire(tx, ty, tz, x, y, z, type, dis, j, i, l, k, be.getBlockState());
		state.wires.add(wire);
		if (type.isFestive()) {
			boolean main = x + y + z > 0;
			for (int index = 1; index <= 24; index++) {
				SuperByteBufferRenderState light = light(wire, index, main);
				if (light != null) state.lights.add(light);
			}
		}
	}

	@Override
	public void submit(WireRenderState state, PoseStack stack, SubmitNodeCollector queue, CameraRenderState camera) {
		for (Wire wire : state.wires) {
			stack.pushPose();
			stack.translate(wire.tx(), wire.ty(), wire.tz());
			queue.submitCustomGeometry(stack, CARenderType.WIRE, (pose, consumer) -> wireRender(consumer, pose.pose(), wire));
			stack.popPose();
		}
		for (SuperByteBufferRenderState light : state.lights) light.submit(stack, queue);
	}

	public static void wireRender(VertexConsumer consumer, Matrix4f matrix4f, Wire w) {
		float x = w.x(), y = w.y(), z = w.z();
		float f = (float) (Mth.fastInvSqrt(x * x + z * z) * 0.025F / 2.0F);
		float o1 = z * f;
		float o2 = x * f;
		wirePart(consumer, matrix4f, x, y, z, w.blockLightTo(), w.blockLightFrom(), w.skyLightTo(), w.skyLightFrom(), 0.025F, 0.025F, o1, o2, w.type(), w.dis(), 0, 1f, 0f, -1f, 0f);
		wirePart(consumer, matrix4f, x, y, z, w.blockLightTo(), w.blockLightFrom(), w.skyLightTo(), w.skyLightFrom(), 0.025F, 0.0F, o1, o2, w.type(), w.dis(), 1, 1f, 0f, -1f, 0f);
	}

	public static void wirePart(VertexConsumer vertBuilder, Matrix4f matrix, float x, float y, float z, int l1, int l2,
			int l3, int l4, float a, float b, float o1, float o2, WireType type, float dis, int lightOffset, float hangFactor,
			float gdx, float gdy, float gdz) {
		for (int j = 0; j < 24; ++j) {
			float f = (float) j / 23.0F;
			int k = (int) Mth.lerp(f, (float) l1, (float) l2);
			int l = (int) Mth.lerp(f, (float) l3, (float) l4);
			int light = LightCoordsUtil.pack(k, l);

			wireVert(vertBuilder, matrix, light, x, y, z, a, b, 24, j, false, o1, o2, type, dis, lightOffset, hangFactor, gdx, gdy, gdz);
			wireVert(vertBuilder, matrix, light, x, y, z, a, b, 24, j + 1, true, o1, o2, type, dis, lightOffset + 1, hangFactor, gdx, gdy, gdz);
		}
	}

	static Color[] colors = {Color.RED, Color.GREEN, new Color(0f, 0f, 1f, 1f)};
	static float LIGHT_Y_OFFSET = -0.03f;

	/** A festive light along the wire, in the wire's local space (translated to the far end), or null. */
	private static @Nullable SuperByteBufferRenderState light(Wire w, int index, boolean main) {
		int count = 24;
		if (index % 3 != 0 || index == 1 || index == count) return null;
		float gdx = 0f, gdy = -1f, gdz = 0f;
		float x = w.x(), y = w.y(), z = w.z();
		float part = (float) index / (float) count;
		float v = x * (-gdx) + y * (-gdy) + z * (-gdz);
		float catenaryScalar = (v > 0.0F ? v * part * part : v - v * (1.0F - part) * (1.0F - part));
		float hangScalar = hang(divf(index, count), w.dis()) + LIGHT_Y_OFFSET;
		float fx = (catenaryScalar + hangScalar) * (-gdx) + (x - v * (-gdx)) * part;
		float fy = (catenaryScalar + hangScalar) * (-gdy) + (y - v * (-gdy)) * part;
		float fz = (catenaryScalar + hangScalar) * (-gdz) + (z - v * (-gdz)) * part;
		return CachedBuffers.partial(CAPartials.SMALL_LIGHT, w.state())
				.color(colors[(main ? 2 - (index / 3) % 3 : (index / 3) % 3)])
				.light(LightCoordsUtil.FULL_BRIGHT)
				.translate(w.tx() + fx, w.ty() + fy, w.tz() + fz)
				.extractRenderState();
	}

	private static float divf(int a, int b) {
		return (float) a / (float) b;
	}

	private static float hang(float f, float dis) {
		return (float) Math.sin(-f * (float) Math.PI) * (HANG * dis / (float) CommonConfig.SMALL_CONNECTOR_MAX_LENGTH.get());
	}

	public static float distanceFromZero(float x, float y, float z) {
		return (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
	}

	public static void wireVert(VertexConsumer vertBuilder, Matrix4f matrix, int light, float x, float y, float z,
			float a, float b, int count, int index, boolean sw, float o1, float o2, WireType type, float dis, int lightOffset, float hangFactor,
			float gdx, float gdy, float gdz) {
		int cr = type.getRed();
		int cg = type.getGreen();
		int cb = type.getBlue();
		if (index % 2 == 0) {
			cr *= 0.7F;
			cg *= 0.7F;
			cb *= 0.7F;
		}

		float part = (float) index / (float) count;
		// Component of wire direction along "up" (-gravity), used for catenary shaping
		float v = x * (-gdx) + y * (-gdy) + z * (-gdz);
		float catenaryScalar = (v > 0.0F ? v * part * part : v - v * (1.0F - part) * (1.0F - part));
		float hangScalar = hangFactor * hang(divf(index, count), dis);
		float fx = (catenaryScalar + hangScalar) * (-gdx) + (x - v * (-gdx)) * part;
		float fy = (catenaryScalar + hangScalar) * (-gdy) + (y - v * (-gdy)) * part;
		float fz = (catenaryScalar + hangScalar) * (-gdz) + (z - v * (-gdz)) * part;

		//System.out.println((fx + o1) +":"+ (fy + n1 - n2) +":"+ (fz - o2));


		if(Math.abs(x) + Math.abs(z) < Math.abs(y)) {
			boolean p = b > 0;
			float c = 0.015f;

			if (!sw) {
				vertBuilder.addVertex(matrix, fx -c, fy, fz + (p?-c:c)).setColor(cr, cg, cb, 255).setLight(light);
			}

			vertBuilder.addVertex(matrix, fx + c, fy, fz + (p?c:-c)).setColor(cr, cg, cb, 255).setLight(light);
			if (sw) {
				vertBuilder.addVertex(matrix, fx -c, fy, fz + (p?-c:c)).setColor(cr, cg, cb, 255).setLight(light);
			}
		}
		else {
			if (!sw) {
				vertBuilder.addVertex(matrix, fx + o1, fy + a - b, fz - o2).setColor(cr, cg, cb, 255).setLight(light);
			}

			vertBuilder.addVertex(matrix, fx - o1, fy + b, fz + o2).setColor(cr, cg, cb, 255).setLight(light);
			if (sw) {
				vertBuilder.addVertex(matrix, fx + o1, fy + a - b, fz - o2).setColor(cr, cg, cb, 255).setLight(light);
			}
		}
	}
}
