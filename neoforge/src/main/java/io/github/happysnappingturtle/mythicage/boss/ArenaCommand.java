package io.github.happysnappingturtle.mythicage.boss;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 试炼竞技场的运维命令（T23），权限等级 2：
 * <ul>
 *   <li>{@code /mythicage arena list}：列出全部实例（编号、阶段、Boss 出生点、门）；</li>
 *   <li>{@code /mythicage arena tp <编号> <hall|gate|origin|region>}：传送到大厅入口安全点附近、门外、Boss 出生点、迷宫范围西南一带；</li>
 *   <li>{@code /mythicage arena reset <编号>}：立即复原。</li>
 * </ul>
 */
public final class ArenaCommand {
    private ArenaCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mythicage")
                .then(Commands.literal("arena").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("list").executes(ArenaCommand::list))
                        .then(Commands.literal("tp")
                                .then(Commands.argument("n", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("where", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("hall", "gate", "origin", "region"), b))
                                                .executes(ArenaCommand::tp))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("n", IntegerArgumentType.integer(1)).executes(ArenaCommand::reset)))));
    }

    private static List<TrialArena> arenas(CommandSourceStack src) {
        return new ArrayList<>(TrialArenas.get(src.getServer()).all());
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        List<TrialArena> all = arenas(ctx.getSource());
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("没有竞技场实例"), false);
        }
        for (int i = 0; i < all.size(); i++) {
            TrialArena a = all.get(i);
            int n = i + 1;
            BlockPos g = a.gate().isEmpty() ? a.bossOrigin() : a.gate().get(a.gate().size() / 2);
            ctx.getSource().sendSuccess(() -> Component.literal("#" + n + " " + a.kind() + " " + a.phase() + " 出生点 "
                    + a.bossOrigin().toShortString() + " 门 " + g.toShortString() + " 第 " + a.fightId() + " 场"), false);
        }
        return all.size();
    }

    private static TrialArena pick(CommandContext<CommandSourceStack> ctx) {
        List<TrialArena> all = arenas(ctx.getSource());
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (n > all.size()) {
            throw new IllegalArgumentException("没有第 " + n + " 个竞技场");
        }
        return all.get(n - 1);
    }

    private static int tp(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        TrialArena a = pick(ctx);
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getServer().getLevel(a.dimension());
        BlockPos to = switch (StringArgumentType.getString(ctx, "where")) {
            case "gate" -> a.gate().isEmpty() ? a.bossOrigin() : a.gate().get(2).south(3);
            case "origin" -> a.bossOrigin();
            case "region" -> new BlockPos(a.region().minX() + 6, a.bossOrigin().getY(), a.region().maxZ() - 20);
            default -> a.center().south(12);
        };
        p.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 180, 0);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        TrialArena a = pick(ctx);
        ServerLevel level = ctx.getSource().getServer().getLevel(a.dimension());
        a.reset(level);
        ctx.getSource().sendSuccess(() -> Component.literal("已复原 " + a.id()), true);
        return 1;
    }
}
