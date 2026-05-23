package net.fabricworldguard.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricworldguard.data.GroupLimits;
import net.fabricworldguard.data.Region;
import net.fabricworldguard.data.RegionManager;
import net.fabricworldguard.integration.WorldEditHook;
import net.fabricworldguard.data.ModConfig;
import net.fabricworldguard.data.FlagConfig;
import net.fabricworldguard.data.FlagSuggestions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class RegionCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        var root = CommandManager.literal("rg");
        var fwg = CommandManager.literal("fwg");

        root.then(CommandManager.literal("claim").then(CommandManager.argument("name", StringArgumentType.word()).executes(RegionCommand::executeClaim)));
        root.then(CommandManager.literal("delete").then(CommandManager.argument("name", StringArgumentType.word()).executes(RegionCommand::executeDelete)));

        registerMemberCommand(root, "addmember", true);
        registerMemberCommand(root, "addowner", false);
        registerRemoveCommand(root, "removemember", true);
        registerRemoveCommand(root, "removeowner", false);

        root.then(CommandManager.literal("flag")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .then(CommandManager.argument("flag", StringArgumentType.word())
                                .suggests(FlagSuggestions.PROVIDER)
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(RegionCommand::executeFlag)))));

        root.then(CommandManager.literal("info")
                .executes(RegionCommand::executeInfo)
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .requires(s -> s.hasPermissionLevel(3))
                        .executes(ctx -> executeInfoWithName(ctx, StringArgumentType.getString(ctx, "name")))));

        root.then(CommandManager.literal("list").executes(ctx -> {
            String names = RegionManager.getRegions().values().stream().map(Region::getName).collect(Collectors.joining(", "));
            ctx.getSource().sendFeedback(() -> Text.literal("Регионы: " + names), false);
            return 1;
        }));

        root.then(CommandManager.literal("lists").requires(s -> s.hasPermissionLevel(3)).executes(RegionCommand::executeLists));

        fwg.then(CommandManager.literal("reload").requires(s -> s.hasPermissionLevel(3)).executes(ctx -> {
            RegionManager.load();
            ModConfig.load();
            FlagConfig.load();
            ctx.getSource().sendFeedback(() -> Text.literal("§aКонфиг и данные перезагружены!"), false);
            return 1;
        }));

        dispatcher.register(root);
        dispatcher.register(CommandManager.literal("region").redirect(root.build()));
        dispatcher.register(fwg);
    }

    private static int executeFlag(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return 0;

        String name = StringArgumentType.getString(ctx, "name").toLowerCase();
        String flag = StringArgumentType.getString(ctx, "flag");
        boolean value = BoolArgumentType.getBool(ctx, "value");

        Region reg = RegionManager.getRegions().get(name);
        if (reg == null) {
            ctx.getSource().sendError(Text.literal("§cРегион не найден!"));
            return 0;
        }

        if (!reg.getOwner().equals(p.getUuid()) && !p.hasPermissionLevel(3)) {
            ctx.getSource().sendError(Text.literal("§cНет прав!"));
            return 0;
        }

        reg.getFlags().set(flag, value);
        RegionManager.save();

        // Новая синхронизированная логика отображения статуса флага
        String status = value ? "§aРАЗРЕШЕНО" : "§cЗАПРЕЩЕНО";
        ctx.getSource().sendFeedback(() -> Text.literal("§a[WG] Флаг §e" + flag + "§a в регионе §e" + reg.getName() + "§a теперь " + status), false);
        return 1;
    }

    private static int executeClaim(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return 0;

        String name = StringArgumentType.getString(ctx, "name").toLowerCase();
        BlockBox box = WorldEditHook.getSelection(p);
        if (box == null) { p.sendMessage(Text.literal("§cВыделите область WorldEdit!"), true); return 0; }

        if (RegionManager.isColliding(box, p.getUuid())) {
            p.sendMessage(Text.literal("§cЭта область пересекается с другим регионом!"), true);
            return 0;
        }

        GroupLimits limits = ModConfig.getInstance().getLimitsForPlayer(p);
        long count = RegionManager.getRegions().values().stream().filter(r -> r.getOwner().equals(p.getUuid())).count();

        if (count >= limits.regionLimit) { p.sendMessage(Text.literal("§cЛимит регионов!"), true); return 0; }

        Region reg = new Region(name, p.getUuid(), box, p.getWorld().getRegistryKey().getValue().toString());
        RegionManager.getRegions().put(name, reg);
        RegionManager.save();
        p.sendMessage(Text.literal("§aРегион " + name + " создан!"));
        return 1;
    }

    private static int executeDelete(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return 0;

        String name = StringArgumentType.getString(ctx, "name").toLowerCase();
        Region reg = RegionManager.getRegions().get(name);
        if (reg == null) return 0;
        if (!reg.getOwner().equals(p.getUuid()) && !p.hasPermissionLevel(3)) return 0;

        RegionManager.getRegions().remove(name);
        RegionManager.save();
        ctx.getSource().sendFeedback(() -> Text.literal("§aРегион удален!"), false);
        return 1;
    }

    private static void registerMemberCommand(com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> rg, String cmd, boolean isMember) {
        rg.then(CommandManager.literal(cmd).then(CommandManager.argument("name", StringArgumentType.word())
                .then(CommandManager.argument("target", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    if (p == null) return 0;

                    String name = StringArgumentType.getString(ctx, "name").toLowerCase();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    Region reg = RegionManager.getRegions().get(name);
                    if (reg == null) return 0;

                    if (!p.hasPermissionLevel(3)) {
                        if (isMember ? !reg.isOwnerOrCoOwner(p.getUuid()) : !reg.getOwner().equals(p.getUuid())) {
                            p.sendMessage(Text.literal("§cУ вас нет прав на этот регион!"), true);
                            return 0;
                        }
                    }

                    Optional<GameProfile> profileOpt = Objects.requireNonNull(ctx.getSource().getServer().getUserCache()).findByName(targetName);
                    if (profileOpt.isEmpty()) {
                        p.sendMessage(Text.literal("§cИгрок не найден в базе!"), true);
                        return 0;
                    }
                    UUID targetUuid = profileOpt.get().getId();

                    if (isMember) reg.getMembers().add(targetUuid);
                    else reg.getOwners().add(targetUuid);

                    RegionManager.save();
                    p.sendMessage(Text.literal("§aИгрок " + targetName + " добавлен!"), false);
                    return 1;
                }))));
    }

    private static void registerRemoveCommand(com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> rg, String cmd, boolean isMember) {
        rg.then(CommandManager.literal(cmd).then(CommandManager.argument("name", StringArgumentType.word())
                .then(CommandManager.argument("target", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    if (p == null) return 0;

                    String name = StringArgumentType.getString(ctx, "name").toLowerCase();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    Region reg = RegionManager.getRegions().get(name);
                    if (reg == null) return 0;

                    if (!p.hasPermissionLevel(3)) {
                        if (isMember ? !reg.isOwnerOrCoOwner(p.getUuid()) : !reg.getOwner().equals(p.getUuid())) {
                            p.sendMessage(Text.literal("§cУ вас нет прав на этот регион!"), true);
                            return 0;
                        }
                    }

                    Optional<GameProfile> profileOpt = Objects.requireNonNull(ctx.getSource().getServer().getUserCache()).findByName(targetName);
                    if (profileOpt.isEmpty()) {
                        p.sendMessage(Text.literal("§cИгрок не найден в базе!"), true);
                        return 0;
                    }
                    UUID targetUuid = profileOpt.get().getId();

                    if (isMember) reg.getMembers().remove(targetUuid);
                    else reg.getOwners().remove(targetUuid);

                    RegionManager.save();
                    p.sendMessage(Text.literal("§aИгрок " + targetName + " удален!"), false);
                    return 1;
                }))));
    }

    private static String getNameByUuid(net.minecraft.server.MinecraftServer server, UUID uuid) {
        return Objects.requireNonNull(server.getUserCache()).getByUuid(uuid)
                .map(GameProfile::getName)
                .orElse(uuid.toString().substring(0, 8) + "...");
    }

    private static int executeInfo(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        String worldId = p.getWorld().getRegistryKey().getValue().toString();
        Optional<Region> reg = RegionManager.getRegionAt(worldId, p.getBlockPos());

        if (reg.isPresent()) {
            sendRegionInfo(ctx.getSource(), reg.get());
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal("§eСвободная территория"), false);
        }
        return 1;
    }

    private static int executeInfoWithName(CommandContext<ServerCommandSource> ctx, String name) {
        Region reg = RegionManager.getRegions().get(name.toLowerCase());
        if (reg != null) sendRegionInfo(ctx.getSource(), reg);
        return 1;
    }

    private static void sendRegionInfo(ServerCommandSource source, Region reg) {
        var server = source.getServer();
        String ownerName = getNameByUuid(server, reg.getOwner());
        String members = reg.getMembers().stream().map(u -> getNameByUuid(server, u)).collect(Collectors.joining(", "));
        String date = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(reg.getCreatedAt()),
                ZoneId.systemDefault()
        ).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        source.sendFeedback(() -> Text.literal("§e--- Регион: " + reg.getName() + " ---")
                .append("\n§7Владелец: §f" + ownerName)
                .append("\n§7Участники: §f" + (members.isEmpty() ? "нет" : members))
                .append("\n§7Дата: §f" + date)
                .append("\n§7Координаты: §f" + reg.getBox().getMinX() + "," + reg.getBox().getMinY() + "," + reg.getBox().getMinZ()
                        + " §7по §f" + reg.getBox().getMaxX() + "," + reg.getBox().getMaxY() + "," + reg.getBox().getMaxZ()), false);
    }

    private static int executeLists(CommandContext<ServerCommandSource> ctx) {
        var regions = RegionManager.getRegions().values();
        var server = ctx.getSource().getServer();
        var grouped = regions.stream().collect(Collectors.groupingBy(Region::getOwner));

        ctx.getSource().sendFeedback(() -> Text.literal("§6--- Список приватов ---"), false);
        grouped.forEach((ownerUuid, list) -> {
            String ownerName = getNameByUuid(server, ownerUuid);
            String names = list.stream().map(Region::getName).collect(Collectors.joining(", "));
            ctx.getSource().sendFeedback(() -> Text.literal("§e" + ownerName + ": §7" + names), false);
        });
        return 1;
    }
}