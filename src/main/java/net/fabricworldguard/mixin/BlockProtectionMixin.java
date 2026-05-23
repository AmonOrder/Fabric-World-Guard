package net.fabricworldguard.mixin;

import net.fabricworldguard.data.FlagConfig;
import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerPlayerInteractionManager.class)
public class BlockProtectionMixin {

    // Связываем поле player из оригинального класса с нашим миксином через Shadow
    @Final
    @Shadow
    protected ServerPlayerEntity player;

    // --- БЛОКИРОВКА ЛОМАНИЯ БЛОКОВ (ЛКМ) ---
    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void onTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Теперь мы можем использовать теневой 'player' напрямую без кастов!
        if (player.hasPermissionLevel(3)) return;

        World world = player.getWorld();
        String worldId = world.getRegistryKey().getValue().toString();

        Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, pos);
        if (regionOpt.isPresent() && !regionOpt.get().isAllowed(player.getUuid())) {
            player.sendMessage(Text.literal("§c[WG] Вы не можете ломать блоки в чужом регионе!"), true);
            cir.setReturnValue(false);
        }
    }

    // --- УСТАНОВКА И ОТКРЫТИЕ СУНДУКОВ (ПКМ) ---
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (player.hasPermissionLevel(3)) return;

        if (world.isClient) return;

        BlockPos clickedPos = hitResult.getBlockPos();
        String worldId = world.getRegistryKey().getValue().toString();

        Optional<Region> regionOpt = RegionManager.getRegionAt(worldId, clickedPos);

        // Если привата нет или игрок в доступе — разрешаем всё
        if (regionOpt.isEmpty() || regionOpt.get().isAllowed(player.getUuid())) {
            return;
        }

        Region region = regionOpt.get();

        // 1. Проверка строительства (Установка blocks)
        if (stack.getItem() instanceof BlockItem) {
            BlockPos targetPos = clickedPos.offset(hitResult.getSide());
            Optional<Region> targetRegionOpt = RegionManager.getRegionAt(worldId, targetPos);

            if (targetRegionOpt.isPresent() && !targetRegionOpt.get().isAllowed(player.getUuid())) {
                player.sendMessage(Text.literal("§c[WG] Вы не можете строить в чужом регионе!"), true);
                cir.setReturnValue(ActionResult.FAIL);
                return;
            }
        }

        // 2. Бронебойное получение ID блока (Защита от некорректных ID)
        Block block = world.getBlockState(clickedPos).getBlock();
        String blockId = block.getRegistryEntry().registryKey().getValue().toString().toLowerCase().trim();

        // Проверяем, зарегистрирован ли этот блок вообще в файле конфигурации флагов
        boolean isFunctionalBlock = false;
        for (String flagName : FlagConfig.getAvailableFlags()) {
            if (FlagConfig.isBlockInFlag(flagName, blockId)) {
                isFunctionalBlock = true;

                // Если владелец региона включил этот флаг (true) — РАЗРЕШАЕМ клик!
                // Просто выходим из миксина (return), позволяя оригинальному коду игры открывать блоки
                if (region.getFlags().getBoolean(flagName)) {
                    return;
                }
            }
        }

        // Если это функциональный блок, но флаг в регионе стоит в false
        if (isFunctionalBlock) {
            player.sendMessage(Text.literal("§c[WG] Использование этого блока запрещено флагами региона!"), true);
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        // Базовая защита привата для всех остальных обычных блоков (верстаки, печки, если их нет в json, или обычные камни)
        player.sendMessage(Text.literal("§c[WG] Взаимодействие с этим блоком запрещено в этом регионе!"), true);
        cir.setReturnValue(ActionResult.FAIL);
    }
}