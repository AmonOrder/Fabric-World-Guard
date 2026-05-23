package net.fabricworldguard.mixin;

import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onPlayerAttack(Entity target, CallbackInfo ci) {
        PlayerEntity attacker = (PlayerEntity) (Object) this;
        if (attacker.getWorld().isClient()) return;
        if (attacker.hasPermissionLevel(3)) return; // Админам можно всё

        // Если один игрок бьет другого игрока
        if (target instanceof PlayerEntity) {
            String worldId = attacker.getWorld().getRegistryKey().getValue().toString();

            // Проверяем регион жертвы
            Optional<Region> defenderRegion = RegionManager.getRegionAt(worldId, target.getBlockPos());
            if (defenderRegion.isPresent() && !defenderRegion.get().getFlags().getBoolean("pvp")) {
                attacker.sendMessage(Text.literal("§c[WG] PvP в этом регионе запрещено!"), true);
                ci.cancel();
                return;
            }

            // Проверяем регион нападающего
            Optional<Region> attackerRegion = RegionManager.getRegionAt(worldId, attacker.getBlockPos());
            if (attackerRegion.isPresent() && !attackerRegion.get().getFlags().getBoolean("pvp")) {
                attacker.sendMessage(Text.literal("§c[WG] Вы не можете атаковать, находясь в безопасной зоне!"), true);
                ci.cancel();
            }
        }
    }
}