package net.fabricworldguard;

import net.fabricworldguard.command.RegionCommand;
import net.fabricworldguard.data.FlagConfig;
import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.fabricworldguard.data.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Optional;

public class FabricWorldGuard implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("FabricWorldGuard");
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("[FabricWorldGuard] Инициализация защитных систем...");

        ModConfig.load();
        FlagConfig.load();

        // Читает регионы и генерирует/читает worldguard-limits.json
        RegionManager.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                RegionCommand.register(dispatcher));

        // --- СИСТЕМА ЗАЩИТЫ ОТ УРОНА (PVP И МИРНЫЕ МОБЫ) ---
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.getWorld().isClient()) return true;

            String worldId = entity.getWorld().getRegistryKey().getValue().toString();

            // 1. ЗАЩИТА МИРНЫХ МОБОВ (Животных)
            if (entity instanceof PassiveEntity) {
                Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, entity.getBlockPos());
                if (regionOpt.isPresent()) {
                    return !regionOpt.get().getFlags().getBoolean("mob-protection");
                }
            }

            // 2. БЛОКИРОВКА PVP (Игрок бьет Игрока)
            if (entity instanceof PlayerEntity && source.getAttacker() instanceof PlayerEntity attacker) {
                if (attacker.hasPermissionLevel(3)) return true; // Админам можно пвпшиться везде

                // Проверяем регион, в котором стоит жертва
                Optional<Region> defenderRegion = RegionManager.getRegionAt(worldId, entity.getBlockPos());
                if (defenderRegion.isPresent()) {
                    if (!defenderRegion.get().getFlags().getBoolean("pvp")) {
                        attacker.sendMessage(Text.literal("§c[WG] В этом регионе PvP запрещено!"), true);
                        return false;
                    }
                }

                // Проверяем регион, в котором стоит атакующий (чтобы не расстреливали из безопасной зоны)
                Optional<Region> attackerRegion = RegionManager.getRegionAt(worldId, attacker.getBlockPos());
                if (attackerRegion.isPresent()) {
                    if (!attackerRegion.get().getFlags().getBoolean("pvp")) {
                        attacker.sendMessage(Text.literal("§c[WG] Вы не можете атаковать, находясь в безопасной зоне!"), true);
                        return false;
                    }
                }
            }

            return true; // Во всех остальных случаях урон разрешен
        });

        // --- БРОНЕБОЙНАЯ СИСТЕМА КОНТРОЛЯ ГРАНИЦ (ФЛАГ ENTRY) ---
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= 2) { // Проверка каждые 2 тика (10 раз в секунду) для мгновенного отклика
                tickCounter = 0;

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (player.hasPermissionLevel(3)) continue; // Игнорируем администраторов

                    String worldId = player.getWorld().getRegistryKey().getValue().toString();
                    BlockPos pos = player.getBlockPos();
                    Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, pos);

                    if (regionOpt.isPresent()) {
                        Region region = regionOpt.get();

                        // Используем метод из Region.java: проверяем, имеет ли игрок права
                        if (!region.isAllowed(player.getUuid())) {

                            // Если флаг entry равен false (вход запрещен), то !false превратится в true,
                            // и запустится процесс выталкивания нарушителя наружу.
                            if (!region.getFlags().getBoolean("entry")) {
                                player.sendMessage(Text.literal("§c[WG] Доступ в этот регион ограничен флагом entry!"), true);

                                // Берем точные координаты игрока из ПРЕДЫДУЩЕГО тика (до того как он зашел в приват)
                                double backX = player.prevX;
                                double backY = player.prevY;
                                double backZ = player.prevZ;

                                // Страховочный случай: если prev-координаты пустые (например, при первом заходе/спавне)
                                if (backX == 0 && backZ == 0) {
                                    backX = player.getX() - (player.getRotationVector().x * 1.5);
                                    backY = player.getY();
                                    backZ = player.getZ() - (player.getRotationVector().z * 1.5);
                                }

                                // Телепортируем нарушителя строго на его предыдущую валидную позицию вне региона
                                player.teleport(player.getServerWorld(), backX, backY, backZ, player.getYaw(), player.getPitch());
                            }
                        }
                    }
                }
            }
        });
    }
}