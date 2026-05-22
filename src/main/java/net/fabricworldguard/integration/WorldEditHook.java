package net.fabricworldguard.integration;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.session.SessionManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockBox;

public class WorldEditHook {
    public static BlockBox getSelection(ServerPlayerEntity player) {
        try {
            // Используем UUID игрока для безопасного получения сессии напрямую через SessionManager
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.findByName(player.getName().getString());

            if (session == null || session.getSelectionWorld() == null) return null;

            com.sk89q.worldedit.regions.Region weRegion = session.getSelection(session.getSelectionWorld());
            if (weRegion == null) return null;

            // Извлекаем чистые координаты векторов через стандартные getX/Y/Z методы
            return new BlockBox(
                    weRegion.getMinimumPoint().getX(),
                    weRegion.getMinimumPoint().getY(),
                    weRegion.getMinimumPoint().getZ(),
                    weRegion.getMaximumPoint().getX(),
                    weRegion.getMaximumPoint().getY(),
                    weRegion.getMaximumPoint().getZ()
            );
        } catch (Exception e) {
            // Если выделение неполное или возникла ошибка приведения типов — возвращаем null
            return null;
        }
    }
}