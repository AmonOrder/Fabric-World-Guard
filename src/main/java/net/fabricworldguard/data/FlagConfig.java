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
    private static final Map<String, List<String>> flagBlocks = new HashMap<>();

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                createDefaultConfig();
                return; // Выходим, так как createDefaultConfig уже сам заполнил flagBlocks
            }
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                // Читаем JSON во временную карту
                Map<String, List<String>> rawData = GSON.fromJson(reader, new TypeToken<Map<String, List<String>>>(){}.getType());

                if (rawData != null) {
                    flagBlocks.clear();

                    // Безопасно переносим данные в flagBlocks, переводя всё в нижний регистр
                    for (Map.Entry<String, List<String>> entry : rawData.entrySet()) {
                        String flagName = entry.getKey();
                        List<String> blocks = entry.getValue();

                        if (blocks != null) {
                            List<String> processedBlocks = new ArrayList<>();
                            for (String b : blocks) {
                                if (b != null) {
                                    processedBlocks.add(b.toLowerCase().trim());
                                }
                            }
                            flagBlocks.put(flagName, processedBlocks);
                        }
                    }
                    System.out.println("[FabricWorldGuard] Конфиг флагов успешно перезагружен!");
                }
            }
        } catch (Exception e) {
            System.err.println("[FabricWorldGuard] КРИТИЧЕСКАЯ ОШИБКА ПРИ ПЕРЕЗАГРУЗКЕ КОНФИГА! Проверь синтаксис JSON.");
            e.printStackTrace(); // Это выведет точную причину и строку ошибки в консоль сервера
        }
    }

    private static void createDefaultConfig() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        Map<String, List<String>> defaults = new LinkedHashMap<>(); // Использован LinkedHashMap для сохранения красивого порядка флагов
        defaults.put("use-doors", Arrays.asList("minecraft:oak_door", "minecraft:iron_door"));
        defaults.put("use-chests", Arrays.asList("minecraft:chest", "minecraft:barrel"));

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(defaults, writer);
        }

        // Загружаем дефолты тоже в нижнем регистре
        flagBlocks.clear();
        defaults.forEach((flag, blocks) -> {
            List<String> lowerCaseBlocks = new ArrayList<>();
            blocks.forEach(b -> lowerCaseBlocks.add(b.toLowerCase().trim()));
            flagBlocks.put(flag, lowerCaseBlocks);
        });
    }

    // Метод для проверки, является ли блок объектом определенного флага (Регистронезависимый)
    public static boolean isBlockInFlag(String flag, String blockId) {
        if (!flagBlocks.containsKey(flag)) return false;

        List<String> blocks = flagBlocks.get(flag);
        if (blocks == null) return false;

        // Приводим проверяемый ID к нижнему регистру перед поиском в списке
        return blocks.contains(blockId.toLowerCase().trim());
    }

    public static Set<String> getAvailableFlags() {
        return flagBlocks.keySet();
    }
}