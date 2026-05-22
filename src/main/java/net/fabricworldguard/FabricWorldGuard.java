package net.fabricworldguard;

import net.fabricworldguard.command.RegionCommand;
import net.fabricworldguard.data.FlagConfig;
import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.fabricworldguard.data.ModConfig;
import net.fabricworldguard.event.ProtectionEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        ProtectionEvents.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                RegionCommand.register(dispatcher));

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (player.hasPermissionLevel(3)) continue;

                    String worldId = player.getWorld().getRegistryKey().getValue().toString();
                    BlockPos pos = player.getBlockPos();
                    Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, pos);

                    if (regionOpt.isPresent()) {
                        Region region = regionOpt.get();
                        if (!region.getFlags().getBoolean("entry") && !region.isAllowed(player.getUuid())) {
                            BlockPos spawnPos = server.getOverworld().getSpawnPos();
                            player.teleport(server.getOverworld(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.getYaw(), player.getPitch());
                            player.sendMessage(Text.literal("§cВы были телепортированы на спавн, так как у вас нет доступа в этот регион!"), true);
                        }
                    }
                }
            }
        });
    }
}