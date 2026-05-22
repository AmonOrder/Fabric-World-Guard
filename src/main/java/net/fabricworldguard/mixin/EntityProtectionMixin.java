package net.fabricworldguard.mixin;

import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Entity.class)
public class EntityProtectionMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // Проверяем, является ли источник взрывом
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            Entity entity = (Entity) (Object) this;
            String worldId = entity.getWorld().getRegistryKey().getValue().toString();

            // Если сущность в защищенном регионе
            Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, entity.getBlockPos());
            if (regionOpt.isPresent() && regionOpt.get().getFlags().getBoolean("explosions")) {
                cir.setReturnValue(false); // Отменяем урон полностью
            }
        }
    }
}
