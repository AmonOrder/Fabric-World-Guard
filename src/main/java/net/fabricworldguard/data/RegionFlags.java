package net.fabricworldguard.data;

import java.util.HashMap;
import java.util.Map;

public class RegionFlags {
    private final Map<String, Boolean> flags = new HashMap<>();

    public RegionFlags() {
        // Теперь все флаги по умолчанию true (защита включена)
        flags.put("pvp", true);
        flags.put("explosions", true);
        flags.put("entry", true);
        flags.put("enderman-grief", true);
        flags.put("mob-protection", true);
        flags.put("hanging-protection", true);
        flags.put("use-doors", true);
        flags.put("use-chests", true);
    }

    public void set(String flag, boolean val) {
        // Просто кладем в мапу. Теперь любой флаг из JSON будет работать!
        flags.put(flag.toLowerCase(), val);
    }

    public boolean getBoolean(String flag) {
        // Если флаг был установлен, возвращаем его.
        // Если нет — возвращаем true (или false, по умолчанию для защиты)
        return flags.getOrDefault(flag.toLowerCase(), true);
    }
}

    /*public boolean pvp = false;
    public boolean explosions = false;
    public boolean entry = true;
    public boolean endermanGrief = false; // Единообразно!
    public boolean mobProtection = true;
    public boolean hangingProtection = true;
    public boolean useDoors = true;
    public boolean useChests = true;

    public void set(String flag, boolean val) {
        switch (flag.toLowerCase()) {
            case "pvp" -> pvp = val;
            case "explosions" -> explosions = val;
            case "entry" -> entry = val;
            case "enderman-grief" -> endermanGrief = val; // Используем человекочитаемые ключи
            case "mob-protection" -> mobProtection = val;
            case "hanging-protection" -> hangingProtection = val;
            case "use-doors" -> useDoors = val;
            case "use-chests" -> useChests = val;
            default -> // Можно добавить логгер или просто проигнорировать
                    System.out.println("Попытка установить неизвестный флаг: " + flag);
        }
    }

    public boolean getBoolean(String flag) {
        return switch (flag.toLowerCase()) {
            case "pvp" -> pvp;
            case "explosions" -> explosions;
            case "entry" -> entry;
            case "enderman-grief" -> endermanGrief;
            case "mob-protection" -> mobProtection;
            case "hanging-protection" -> hangingProtection;
            case "use-doors" -> useDoors;
            case "use-chests" -> useChests;
            default -> true; // Если флага нет, по умолчанию разрешаем (true)
        };
    }*/
