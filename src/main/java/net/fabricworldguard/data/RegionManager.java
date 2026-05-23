package net.fabricworldguard.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricworldguard.FabricWorldGuard;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockBox;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class RegionManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fabricworldguard/regions.json");
    private static final Map<String, Region> regions = new HashMap<>();

    // Загрузка регионов
    public static void load() {
        File parent = FILE.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            FabricWorldGuard.LOGGER.error("Не удалось создать директорию: {}", parent.getAbsolutePath());
        }
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                Type type = new TypeToken<HashMap<String, Region>>(){}.getType();
                Map<String, Region> map = GSON.fromJson(reader, type);
                if (map != null) {
                    regions.clear();
                    regions.putAll(map);
                }
            } catch (IOException e) {
                FabricWorldGuard.LOGGER.error("Ошибка при загрузке регионов: ", e);
            }
        }
    }

    // Сохранение регионов
    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(regions, writer);
        } catch (IOException e) {
            FabricWorldGuard.LOGGER.error("Ошибка при сохранении регионов: ", e);
        }
    }

    // Исправленный метод проверки коллизий
    public static boolean isColliding(BlockBox newBox, UUID player) {
        return regions.values().stream().anyMatch(r -> {
            BlockBox b = r.getBox();

            // Используем методы getMinX(), getMinY() и т.д.
            boolean intersects = Math.max(b.getMinX(), newBox.getMinX()) <= Math.min(b.getMaxX(), newBox.getMaxX()) &&
                    Math.max(b.getMinY(), newBox.getMinY()) <= Math.min(b.getMaxY(), newBox.getMaxY()) &&
                    Math.max(b.getMinZ(), newBox.getMinZ()) <= Math.min(b.getMaxZ(), newBox.getMaxZ());

            if (!r.isAllowed(player)) {
                int buffer = ModConfig.getInstance().bufferZone;
                boolean tooClose = (newBox.getMinX() <= b.getMaxX() + buffer && newBox.getMaxX() >= b.getMinX() - buffer) &&
                        (newBox.getMinY() <= b.getMaxY() + buffer && newBox.getMaxY() >= b.getMinY() - buffer) &&
                        (newBox.getMinZ() <= b.getMaxZ() + buffer && newBox.getMaxZ() >= b.getMinZ() - buffer);

                return intersects || tooClose;
            }

            return intersects;
        });
    }

    // Исправленный метод поиска региона
    public static Optional<Region> getRegionAt(String worldId, BlockPos pos) {
        return regions.values().stream().filter(r -> {
            BlockBox b = r.getBox();
            return r.getWorldId().equals(worldId) &&
                    pos.getX() >= b.getMinX() && pos.getX() <= b.getMaxX() &&
                    pos.getY() >= b.getMinY() && pos.getY() <= b.getMaxY() &&
                    pos.getZ() >= b.getMinZ() && pos.getZ() <= b.getMaxZ();
        }).findFirst();
    }

    public static Map<String, Region> getRegions() {
        return regions;
    }

    // Полностью замени метод isActionDenied в конце RegionManager.java на этот:
    public static boolean isActionDenied(String worldId, BlockPos pos, java.util.UUID playerUuid, String flagName) {
        Optional<Region> regionOpt = getRegionAt(worldId, pos);
        if (regionOpt.isEmpty()) {
            return false; // Региона нет — действие разрешено
        }

        Region region = regionOpt.get();
        // Если игрок — владелец или участник, ему можно абсолютно всё, флаги на него не влияют
        if (region.isAllowed(playerUuid)) {
            return false;
        }

        // Если флаг равен false (выключен), значит действие для чужака ЗАПРЕЩЕНО (возвращаем true)
        return !region.getFlags().getBoolean(flagName);
    }
}