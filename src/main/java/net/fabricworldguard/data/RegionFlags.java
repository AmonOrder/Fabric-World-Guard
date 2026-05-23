package net.fabricworldguard.data;

import java.util.HashMap;
import java.util.Map;

public class RegionFlags {
    private final Map<String, Boolean> flags = new HashMap<>();

    public RegionFlags() {
        // Теперь все флаги по умолчанию true (защита включена)
        flags.put("pvp", false);          // ПвП запрещено чужакам
        flags.put("explosions", false);   // Взрывы не ломают блоки
        flags.put("entry", true);         // Входить в регион можно
        flags.put("enderman-grief", false);
        flags.put("mob-protection", true);
        flags.put("hanging-protection", true);
        flags.put("use-doors", false);    // Двери закрыты для чужаков
        flags.put("use-chests", false);
    }

    public void set(String flag, boolean val) {
        // Просто кладем в мапу. Теперь любой флаг из JSON будет работать!
        flags.put(flag.toLowerCase(), val);
    }

    public boolean getBoolean(String flag) {
        // Если флаг был установлен, возвращаем его.
        return flags.getOrDefault(flag.toLowerCase(), false);
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
