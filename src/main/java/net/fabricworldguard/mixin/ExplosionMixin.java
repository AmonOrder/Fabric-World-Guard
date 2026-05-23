package net.fabricworldguard.mixin;

import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(Explosion.class)
public class ExplosionMixin {

    @Shadow @Final private World world;

    @Inject(method = "affectWorld(Z)V", at = @At("HEAD"))
    private void onExplosionAffect(boolean showParticles, CallbackInfo ci) {
        if (this.world == null || this.world.isClient) return;

        String worldId = this.world.getRegistryKey().getValue().toString();
        Explosion explosion = (Explosion) (Object) this;

        List<BlockPos> blocks = explosion.getAffectedBlocks();
        if (blocks != null) {
            blocks.removeIf(pos -> {
                Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, pos);
                // Если регион есть и флаг explosions == false (выключен), удаляем блок из списка разрушений
                return regionOpt.isPresent() && !regionOpt.get().getFlags().getBoolean("explosions");
            });
        }
    }
}