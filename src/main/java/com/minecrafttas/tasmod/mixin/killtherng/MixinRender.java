package com.minecrafttas.tasmod.mixin.killtherng;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.RandomBase;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

@Mixin(Render.class)
public abstract class MixinRender {

	@Unique
	private long previousSeed = 0;
	@Shadow
	private RenderManager renderManager;

	@Inject(method = "renderName", at = @At(value = "HEAD"))
	public void inject_renderName(Entity entity, double d, double e, double f, CallbackInfo ci) {
		if (System.getProperty("tasmod.killtherng.entitytrace", "false").equals("true")) {
			Entity serverEntity = TASmod.getServerInstance().getEntityFromUuid(entity.getUniqueID());
			if (serverEntity == null)
				return;
			RandomBase random = (RandomBase) serverEntity.rand;
			long seed = random.getSeed();
			long distance = -random.distance(random.getInitialSeed());
			GlStateManager.alphaFunc(516, 0.1F);
			this.renderEntityName2(entity, d, e + 0.69D, f, Long.toString(random.getInitialSeed()), 64);
			this.renderEntityName2(entity, d, e + 0.46D, f, Long.toString(seed), 64);
			this.renderEntityName2(entity, d, e + 0.23D, f, Long.toString(distance), 64);
		}
	}

	protected void renderEntityName2(Entity entityIn, double x, double y, double z, String name, double distanceSq) {
		this.renderLivingLabel2(entityIn, name, x, y, z, 64);
	}

	protected void renderLivingLabel2(Entity entityIn, String str, double x, double y, double z, int maxDistance) {
		double d = entityIn.getDistanceSq(this.renderManager.renderViewEntity);
		if (!(d > maxDistance * maxDistance)) {
			boolean bl = entityIn.isSneaking();
			float f = this.renderManager.playerViewY;
			float g = this.renderManager.playerViewX;
			boolean bl2 = this.renderManager.options.thirdPersonView == 2;
			float h = entityIn.height + 0.5F - (bl ? 0.25F : 0.0F);
			int i = "deadmau5".equals(str) ? -10 : 0;
			EntityRenderer.drawNameplate(this.renderManager.getFontRenderer(), str, (float) x, (float) y + h, (float) z, i, f, g, bl2, bl);
		}
	}
}
