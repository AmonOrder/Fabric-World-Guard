package net.fabricworldguard.data;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricworldguard.data.FlagConfig;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlagSuggestions {
    public static final SuggestionProvider<ServerCommandSource> PROVIDER = (context, builder) -> {
        // Здесь мы собираем все доступные флаги
        Set<String> allFlags = new HashSet<>(List.of(
                "pvp", "explosions", "entry", "enderman-grief",
                "mob-protection", "hanging-protection"
        ));

        // Добавляем те флаги, которые мы загрузили из твоего json-конфига
        allFlags.addAll(FlagConfig.getAvailableFlags());

        return CommandSource.suggestMatching(allFlags, builder);
    };
}