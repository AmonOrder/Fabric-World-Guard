package net.fabricworldguard.mixin;

import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

// Указываем цель через внутренний класс эндермена со знаком $
@Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PickUpBlockGoal")
public class EndermanGriefMixin {

    @Shadow(aliases = {"field_7074", "enderman"})
    @Final
    private EndermanEntity enderman;

    @Inject(method = "canStart()Z", at = @At("HEAD"), cancellable = true)
    private void onTryPickUpBlock(CallbackInfoReturnable<Boolean> cir) {
        if (this.enderman == null || this.enderman.getWorld() == null) return;

        String worldId = this.enderman.getWorld().getRegistryKey().getValue().toString();
        BlockPos pos = this.enderman.getBlockPos();
        Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, pos);

        // Если регион есть и защита включена (true), запрещаем эндермену брать блок
        if (regionOpt.isPresent() && regionOpt.get().getFlags().getBoolean("enderman-grief")) {
            cir.setReturnValue(false);
        }
    }
}