package net.F53.HorseBuff.mixin.PortalHorse;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.F53.HorseBuff.HorseBuffInit;
import net.F53.HorseBuff.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class TickNether {

    @Shadow
    protected boolean inNetherPortal;

    @Shadow
    protected int netherPortalTime;

    @Shadow
    protected abstract void tickPortalCooldown();

    @Shadow
    public abstract boolean hasVehicle();

    @Inject(method = "tickPortal()V", at = @At("HEAD"))
    public void riderTravel(CallbackInfo ci) {
        Entity player = (Entity) (Object) this;
        if (player.world instanceof ServerWorld && player instanceof PlayerEntity) {
            if (player.getVehicle() != null) {
                int maxPortalTime = player.getMaxNetherPortalTime();
                if (inNetherPortal) {
                    MinecraftServer minecraftServer = ((ServerWorld) player.world).getServer();
                    ServerWorld destination = minecraftServer.getWorld(player.world.getRegistryKey() == World.NETHER ? World.OVERWORLD : World.NETHER);
                    if (destination != null && minecraftServer.isNetherAllowed() && netherPortalTime++ >= maxPortalTime) {
                        // Get Vehicle
                        Entity vehicle = player.getVehicle();

                        // Split
                        vehicle.detach();

                        // in some cases some of these values are null, causing problems, just don't teleport if they are null
                        if (player.getPos() != null && vehicle.getPos() != null) {
                            // Get UUIDs
                            UUID vehicleUUID = vehicle.getUuid();
                            UUID playerUUID = player.getUuid();

                            // Change player Dim
                            player.resetPortalCooldown();
                            player.moveToWorld(destination);

                            // Change vehicle Dim
                            vehicle.resetPortalCooldown();
                            vehicle.moveToWorld(destination);

                            // Safely rejoin player and vehicle once the game is ready
                            HorseBuffInit.tpAndRemount(playerUUID, vehicleUUID, destination, 0);
                        }
                    }
                    inNetherPortal = false;
                } else {
                    if (this.netherPortalTime > 0) {
                        this.netherPortalTime -= 4;
                    }
                    if (this.netherPortalTime < 0) {
                        this.netherPortalTime = 0;
                    }
                }
                tickPortalCooldown();
            }
        }
    }

    @ModifyReturnValue(method = "canUsePortals", at = @At(value = "RETURN"))
    public boolean allowPortalTravel(boolean original) {
        if (ModConfig.getInstance().portalPatch) {
            return true;
        }
        return original;
    }

    // elsewhere, we allow vehicles to be marked as in nether portal, so we have to deny them teleporting, so we can teleport them ourselves
    @WrapOperation(method = "tickPortal()V", at = @At(value = "INVOKE", target = "net/minecraft/entity/Entity.hasVehicle ()Z"))
    public boolean denyVehicleTravel(Entity instance, Operation<Boolean> original) {
        // if portalPatch, deny travel
        if (instance.hasPassengers() && ModConfig.getInstance().portalPatch) {
            return true;
        } else {
            return original.call(instance);
        }
    }

    @ModifyConstant(method = "tickPortal()V", constant = @Constant(intValue = 4))
    public int netherPortalTime(int constant) {
        if (this.hasVehicle()) {
            return 0;
        }
        return constant;
    }
}