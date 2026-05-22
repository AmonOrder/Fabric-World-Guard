package net.fabricworldguard.event;

import net.fabricworldguard.data.FlagConfig;
import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.block.Block;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;


import java.util.Optional;


public class ProtectionEvents {

    public static void init() {
        // 1. Защита от ломания блоков
        // Блокировка ломания блоков — используем флаг "build"
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            // Добавляем "build" в качестве четвертого аргумента
            if (hasAccess(player, world, pos, "build")) return ActionResult.PASS;
            player.sendMessage(Text.literal("§cУ вас нет доступа к этому региону!"), true);
            return ActionResult.FAIL;
        });

        // 2. Защита от использования блоков (сундуки, двери, люки)
        // Блокировка использования — можно добавить логику выбора флага ("use-doors" или "use-chests")
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
            String blockId = Registries.BLOCK.getId(block).toString();

            Optional<Region> regionOpt = RegionManager.getRegionAt(world.getRegistryKey().getValue().toString(), hitResult.getBlockPos());

            // Проверяем, относится ли этот блок к какой-либо группе флагов
            if (regionOpt.isPresent()) {
                Region reg = regionOpt.get();

                // 1. Сначала проверяем динамические флаги
                // Перебираем все возможные категории из нашего конфига
                for (String flagName : FlagConfig.getAllFlagNames()) {
                    if (FlagConfig.isBlockInFlag(flagName, blockId)) {
                        // Если блок защищен этим флагом И защита включена (true)
                        if (reg.getFlags().getBoolean(flagName) && !reg.isAllowed(player.getUuid())) {
                            player.sendMessage(Text.literal("§cЭтот объект защищен в регионе!"), true);
                            return ActionResult.FAIL;
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });

        // 3. Защита от взаимодействия с сущностями (рамки, стойки)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof ItemFrameEntity || entity instanceof ArmorStandEntity) {
                // Теперь используем hasAccess!
                if (hasAccess(player, world, entity.getBlockPos(), "hanging-protection")) return ActionResult.PASS;
                player.sendMessage(Text.literal("§cЭтот объект защищен в регионе!"), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // 4. Защита мирных мобов (PassiveEntity)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof PassiveEntity) {
                // Теперь используем hasAccess!
                if (hasAccess(player, world, entity.getBlockPos(), "mob-protection")) return ActionResult.PASS;
                player.sendMessage(Text.literal("§cМирные существа защищены!"), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // 5. Защита мирных существ от урона извне (взрывы, огонь)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PassiveEntity) {
                Optional<Region> regionOpt = RegionManager.getRegionAt(entity.getWorld().getRegistryKey().getValue().toString(), entity.getBlockPos());
                // ВАЖНО: раньше у тебя было !mobProtection (запрет при выключенном),
                // теперь при включенной защите (true) мы должны возвращать false (запретить урон).
                // Поэтому логика такая: если регион есть И защита включена -> возвращаем false (отмена урона)
                return regionOpt.isEmpty() || !regionOpt.get().getFlags().getBoolean("mob-protection");
            }
            return true;
        });

        // 6. Защита сущностей от взрывов (через ExplosionEvents)

    }

    private static boolean hasAccess(PlayerEntity player, World world, BlockPos pos, String flag) {
        if (player.hasPermissionLevel(3)) return true; // Админ всегда имеет доступ

        Optional<Region> regionOpt = RegionManager.getRegionAt(world.getRegistryKey().getValue().toString(), pos);
        if (regionOpt.isEmpty()) return true; // Нет региона — разрешаем

        Region reg = regionOpt.get();
        if (reg.isAllowed(player.getUuid())) return true; // Владелец/участник — разрешаем

        // Если защита включена (true), возвращаем false (запрет),
        // поэтому используем отрицание !
        return !reg.getFlags().getBoolean(flag);
    }
}