package net.F53.HorseBuff.mixin.Server;

import net.F53.HorseBuff.config.ModConfig;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// Disable movement checks for Horses, fixing MC-100830
@Mixin(value = ServerPlayNetworkHandler.class, priority = 960)
public class MovementCheck {
    @Shadow
    public ServerPlayerEntity player;

    @ModifyConstant(method = "onVehicleMove(Lnet/minecraft/network/packet/c2s/play/VehicleMoveC2SPacket;)V", constant = @Constant(doubleValue = 0.0625D))
    private double horseNoMovementCheck(double value) {
        if (this.player.getRootVehicle() instanceof AbstractHorseEntity && ModConfig.getInstance().rubberBand)
            return Double.POSITIVE_INFINITY;
        return value;
    }
}
