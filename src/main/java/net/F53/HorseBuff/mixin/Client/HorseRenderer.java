package net.F53.HorseBuff.mixin.Client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.F53.HorseBuff.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.*;

import static net.F53.HorseBuff.HorseBuffInit.getOpacity;
import static net.F53.HorseBuff.HorseBuffInit.isJeb;

@Mixin(value = LivingEntityRenderer.class, priority = 960)
public abstract class HorseRenderer<T extends LivingEntity, M extends EntityModel<T>> {

    private boolean isHorse;

    private float opacity;

    private float r;
    private float g;
    private float b;

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
    at = @At("HEAD"))
    void fetchOpacityAndJeb(T livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        isHorse = false;
        opacity = 1;
        r = 1;
        g = 1;
        b = 1;
        if (livingEntity instanceof AbstractHorseEntity) {
            isHorse = true;
            if (ModConfig.getInstance().pitchFade.enabled && livingEntity.hasPassenger(MinecraftClient.getInstance().player)) {
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                assert player != null;
                opacity = getOpacity(player);
            }
            if (isJeb(livingEntity)) {
                float hueOffset = (livingEntity.getUuid().hashCode() % 5000) / 5000f + (System.currentTimeMillis() % 5000) / 5000f;
                Color color = new Color(Color.HSBtoRGB(hueOffset, 0.8f, 1));
                r = color.getRed() / 255f;
                g = color.getGreen() / 255f;
                b = color.getBlue() / 255f;
            }
        }
    }

    @WrapOperation(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/entity/LivingEntity;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    RenderLayer makeRenderLayerTranslucent(LivingEntityRenderer<T, ? extends EntityModel<T>> instance, T entity, boolean showBody, boolean translucent, boolean showOutline, Operation<RenderLayer> original) {
        if (showBody && entity instanceof AbstractHorseEntity && opacity < 1) {
            return RenderLayer.getItemEntityTranslucentCull(instance.getTexture(entity));
        } else {
            return original.call(instance, entity, showBody, translucent, showOutline);
        }
    }

    @ModifyArgs(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
    at = @At(value = "INVOKE", target = "net/minecraft/client/render/entity/model/EntityModel.render (Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"))
    void setOpacityAndChromaForRender(Args args) {
        if (isHorse) {
            args.set(4, r);
            args.set(5, g);
            args.set(6, b);
            args.set(7, Math.min(args.get(7), opacity));
        }
    }
}
