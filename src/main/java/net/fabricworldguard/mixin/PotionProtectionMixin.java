package net.fabricworldguard.mixin;

import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class) // Меняем цель на базовый класс живых существ
public class PotionProtectionMixin {

    // Используем "addStatusEffect" — если среда разработки всё равно подчеркнет метод красным,
    // Mixin-процессор на этапе компиляции сам подберет правильное имя (addStatusEffect / addEffect)
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddStatusEffect(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Нас интересуют ТОЛЬКО игроки на стороне сервера
        if (entity instanceof ServerPlayerEntity player) {
            if (player.getWorld().isClient()) return;
            if (player.hasPermissionLevel(3)) return; // Админам можно любые эффекты

            // Проверяем, вредный ли эффект (яд, иссушение, замедление и т.д.)
            if (effect.getEffectType().getCategory() == StatusEffectCategory.HARMFUL) {
                String worldId = player.getWorld().getRegistryKey().getValue().toString();

                // Ищем регион под игроком
                Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, player.getBlockPos());
                if (regionOpt.isPresent()) {
                    // Если PvP выключено — блокируем наложение вредного эффекта
                    if (!regionOpt.get().getFlags().getBoolean("pvp")) {
                        cir.setReturnValue(false); // Отменяем применение эффекта к игроку
                    }
                }
            }
        }
    }
}