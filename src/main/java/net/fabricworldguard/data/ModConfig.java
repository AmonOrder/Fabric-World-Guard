package net.fabricworldguard.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricworldguard.FabricWorldGuard;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fabricworldguard/config.json");

    // Инструкция для админа
    @SuppressWarnings("unused")
    public String help = "Чтобы добавить группу, используйте формат: \"имя\": { \"regionLimit\": 5, \"blockLimit\": 100000 }";

    public int maxRegionsPerPlayer = 3;
    public int maxBlocksPerRegion = 50000;
    public int bufferZone = 10;

    // Добавляем саму мапу групп, иначе код не скомпилируется
    public Map<String, GroupLimits> groupLimits = new HashMap<>();

    private static ModConfig instance = new ModConfig();

    public static ModConfig getInstance() { return instance; }

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                instance = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                FabricWorldGuard.LOGGER.error("Ошибка загрузки конфига: ", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        File parent = FILE.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            FabricWorldGuard.LOGGER.error("Не удалось создать директорию конфига!");
            return;
        }
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            FabricWorldGuard.LOGGER.error("Ошибка сохранения конфига: ", e);
        }
    }

    public GroupLimits getLimitsForPlayer(ServerPlayerEntity p) {
        // Проверяем группы из конфига
        for (String groupName : groupLimits.keySet()) {
            if (p.hasPermissionLevel(getRequiredPermission(groupName))) {
                return groupLimits.get(groupName);
            }
        }
        // Возвращаем дефолтные значения, если группа не найдена
        return new GroupLimits(maxRegionsPerPlayer, maxBlocksPerRegion);
    }

    private int getRequiredPermission(String group) {
        // Логика уровней: можно сделать через switch-case для удобства
        return group.equals("admin") ? 4 : 2;
    }
}