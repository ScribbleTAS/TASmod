package com.minecrafttas.tasmod.mixin.killtherng;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.KTRNGRandom;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

@SuppressWarnings("rawtypes")
@Mixin(RenderLivingBase.class)
public abstract class MixinRenderLivingBase extends Render {

	@Unique
	private long previousSeed = 0;

	protected MixinRenderLivingBase(RenderManager renderManager) {
		super(renderManager);
	}

	@SuppressWarnings("unchecked")
	@Inject(method = "renderName", at = @At(value = "HEAD"))
	public void inject_renderName(EntityLivingBase entity, double d, double e, double f, CallbackInfo ci) {
		Entity serverEntity = TASmod.getServerInstance().getEntityFromUuid(entity.getUniqueID());
		KTRNGRandom random = (KTRNGRandom) serverEntity.rand;
		long seed = random.getSeed();
		long distance = -random.distance(previousSeed);
		GlStateManager.alphaFunc(516, 0.1F);
		this.renderEntityName(entity, d, e + 0.23D, f, Long.toString(seed), 64);
		this.renderEntityName(entity, d, e + 0D, f, Long.toString(distance), 64);
	}
}
