package io.github.happysnappingturtle.mythicage.conduct;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 善恶值的管理命令（T18 §4.4），权限 2，用于测试与运维，不是赎罪接口：
 * <pre>
 * /mythicage conduct get &lt;玩家&gt;
 * /mythicage conduct set &lt;玩家&gt; &lt;值&gt;
 * /mythicage conduct add &lt;玩家&gt; &lt;变化&gt;     （走正常结算：截断、正向上限、记录）
 * /mythicage conduct history &lt;玩家&gt;
 * </pre>
 */
public final class ConductCommand {
    private ConductCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("mythicage").then(Commands.literal("conduct")
                .requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player()).executes(ConductCommand::get)))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", IntegerArgumentType.integer(-100, 100)).executes(ConductCommand::set))))
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("delta", IntegerArgumentType.integer(-200, 200)).executes(ConductCommand::add))))
                .then(Commands.literal("history").then(Commands.argument("player", EntityArgument.player())
                        .executes(ConductCommand::history)))));
    }

    private static int get(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.conduct.get", p.getDisplayName(),
                ConductService.score(p)), false);
        return ConductService.score(p);
    }

    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        int v = IntegerArgumentType.getInteger(c, "value");
        PlayerMythData d = MythData.get(p);
        int delta = v - d.conductScore();
        d.setConductScore(v);
        d.addConductEntry(new PlayerMythData.ConductEntry("admin", delta, p.level().getGameTime()));
        MythData.changed(p);
        c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.conduct.get", p.getDisplayName(), v), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        int actual = ConductService.change(p, "admin", IntegerArgumentType.getInteger(c, "delta"));
        c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.conduct.get", p.getDisplayName(),
                ConductService.score(p)), true);
        return actual;
    }

    private static int history(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        var recent = MythData.get(p).recentConduct();
        if (recent.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.conduct.none", p.getDisplayName()), false);
        }
        for (PlayerMythData.ConductEntry e : recent) {
            c.getSource().sendSuccess(() -> Component.literal((e.delta() > 0 ? "+" : "") + e.delta() + "  ")
                    .append(Component.translatable("mythic_age.conduct.reason." + e.reason()))
                    .append("  @" + e.gameTime()), false);
        }
        return recent.size();
    }
}
