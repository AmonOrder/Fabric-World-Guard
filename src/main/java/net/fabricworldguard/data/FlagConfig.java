package net.fabricworldguard.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FlagConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config/fabricworldguard/flags-config.json");

    // Карта: Имя флага -> Список ID блоков (например, "use-doors" -> ["minecraft:oak_door", ...])
    private static Map<String, List<String>> flagBlocks = new HashMap<>();

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                // Создаем дефолтный конфиг, если его нет
                createDefaultConfig();
            }
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                flagBlocks = GSON.fromJson(reader, new TypeToken<Map<String, List<String>>>(){}.getType());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        Map<String, List<String>> defaults = new HashMap<>();
        defaults.put("use-doors", Arrays.asList("minecraft:oak_door", "minecraft:iron_door"));
        defaults.put("use-chests", Arrays.asList("minecraft:chest", "minecraft:barrel"));

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(defaults, writer);
        }
        flagBlocks = defaults;
    }

    // Метод для проверки, является ли блок объектом определенного флага
    public static boolean isBlockInFlag(String flag, String blockId) {
        return flagBlocks.getOrDefault(flag, Collections.emptyList()).contains(blockId);
    }

    public static Set<String> getAvailableFlags() {
        return flagBlocks.keySet();
    }

    //Метод возврата названия всех флагов
    public static Set<String> getAllFlagNames() {
        return flagBlocks.keySet();
    }
}