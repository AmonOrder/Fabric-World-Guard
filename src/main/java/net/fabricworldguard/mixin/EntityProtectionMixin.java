package net.fabricworldguard.mixin;

import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Entity.class)
public class EntityProtectionMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity target = (Entity) (Object) this;
        if (target.getWorld().isClient()) return;

        String worldId = target.getWorld().getRegistryKey().getValue().toString();

        // 1. ЗАЩИТА БЛОКОВ И СУЩНОСТЕЙ ОТ ВЗРЫВОВ
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            if (RegionManager.isActionDenied(worldId, target.getBlockPos(), java.util.UUID.randomUUID(), "explosions")) {
                cir.setReturnValue(false);
                return;
            }
        }

        // 2. ЗАЩИТА ОТ ПВП ПРИ КЛИКЕ И СНАРЯДАХ
        if (target instanceof PlayerEntity && source.getAttacker() instanceof PlayerEntity attacker) {
            if (attacker.hasPermissionLevel(3)) return;

            if (isPvPDisabled(worldId, target, attacker)) {
                attacker.sendMessage(Text.literal("§c[WG] PvP в этом регионе запрещено!"), true);
                cir.setReturnValue(false);
            }
        }
    }

    private boolean isPvPDisabled(String worldId, Entity target, PlayerEntity attacker) {
        Optional<Region> defenderRegion = RegionManager.getRegionAt(worldId, target.getBlockPos());
        if (defenderRegion.isPresent() && !defenderRegion.get().getFlags().getBoolean("pvp")) {
            return true;
        }
        Optional<Region> attackerRegion = RegionManager.getRegionAt(worldId, attacker.getBlockPos());
        return attackerRegion.isPresent() && !attackerRegion.get().getFlags().getBoolean("pvp");
    }
}