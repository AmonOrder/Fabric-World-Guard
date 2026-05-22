package net.fabricworldguard.mixin;

import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
        Explosion explosion = (Explosion) (Object) this;
        if (this.world == null || this.world.isClient) return;

        String worldId = this.world.getRegistryKey().getValue().toString();

        // 1. Защита блоков
        List<BlockPos> blocks = explosion.getAffectedBlocks();
        if (blocks != null) {
            blocks.removeIf(pos -> {
                Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, pos);
                return regionOpt.isPresent() && regionOpt.get().getFlags().getBoolean("explosions");
            });
        }

        // 2. Защита сущностей
        // Находим эпицентр взрыва, усредняя координаты блоков, которые он затрагивает
        if (blocks != null && !blocks.isEmpty()) {
            double avgX = blocks.stream().mapToDouble(BlockPos::getX).average().orElse(0);
            double avgY = blocks.stream().mapToDouble(BlockPos::getY).average().orElse(0);
            double avgZ = blocks.stream().mapToDouble(BlockPos::getZ).average().orElse(0);

            // Ищем сущности в радиусе 8 блоков от эпицентра
            Box explosionBox = new Box(avgX - 8, avgY - 8, avgZ - 8, avgX + 8, avgY + 8, avgZ + 8);
            List<Entity> entities = this.world.getOtherEntities(null, explosionBox);

            for (Entity entity : entities) {
                Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, entity.getBlockPos());
                if (regionOpt.isPresent() && regionOpt.get().getFlags().getBoolean("explosions")) {
                    entity.setInvulnerable(true); // Временно защищаем
                }
            }
        }
    }

    @Inject(method = "affectWorld(Z)V", at = @At("TAIL"))
    private void onExplosionFinished(boolean showParticles, CallbackInfo ci) {
        Explosion explosion = (Explosion) (Object) this;
        List<BlockPos> blocks = explosion.getAffectedBlocks();

        if (!blocks.isEmpty()) {
            double avgX = blocks.stream().mapToDouble(BlockPos::getX).average().orElse(0);
            double avgY = blocks.stream().mapToDouble(BlockPos::getY).average().orElse(0);
            double avgZ = blocks.stream().mapToDouble(BlockPos::getZ).average().orElse(0);

            Box explosionBox = new Box(avgX - 8, avgY - 8, avgZ - 8, avgX + 8, avgY + 8, avgZ + 8);
            List<Entity> entities = this.world.getOtherEntities(null, explosionBox);

            for (Entity entity : entities) {
                entity.setInvulnerable(false); // Возвращаем уязвимость
            }
        }
    }
}