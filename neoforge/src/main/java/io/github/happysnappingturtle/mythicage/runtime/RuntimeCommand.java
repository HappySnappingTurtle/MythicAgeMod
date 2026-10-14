package io.github.happysnappingturtle.mythicage.runtime;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import io.github.happysnappingturtle.mythicage.player.MythDataCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.OptionalLong;

/**
 * 运行时钟的开发命令（T5 设计文档 §6），权限等级 2：
 * <pre>
 * /mythicage runtime ledger add &lt;玩家&gt; &lt;文明&gt; &lt;日产率&gt;   创建测试产出点
 * /mythicage runtime ledger list
 * /mythicage runtime ledger real &lt;true|false&gt; &lt;id&gt;       设定测试产出点是否真实运行
 * /mythicage runtime ledger invalidate|remove &lt;id&gt;
 * /mythicage runtime settle                              立即结算全部台账
 * /mythicage runtime status &lt;玩家&gt;                       活跃状态
 * </pre>
 */
public final class RuntimeCommand {
    private static final DynamicCommandExceptionType UNKNOWN_LEDGER = new DynamicCommandExceptionType(
            id -> Component.translatableWithFallback("commands.mythic_age.runtime.unknown", "Unknown ledger: %s", id));

    private RuntimeCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mythicage")
                .then(Commands.literal("runtime")
                        .requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("ledger")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(MythDataCommand.civArgument()
                                                        .then(Commands.argument("rate", DoubleArgumentType.doubleArg(0, 100000))
                                                                .executes(RuntimeCommand::add)))))
                                .then(Commands.literal("list").executes(RuntimeCommand::list))
                                .then(Commands.literal("real")
                                        .then(Commands.argument("real", BoolArgumentType.bool())
                                                .then(idArgument().executes(RuntimeCommand::real))))
                                .then(Commands.literal("invalidate")
                                        .then(idArgument().executes(ctx -> simple(ctx, true))))
                                .then(Commands.literal("remove")
                                        .then(idArgument().executes(ctx -> simple(ctx, false)))))
                        .then(Commands.literal("settle").executes(RuntimeCommand::settle))
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(RuntimeCommand::status)))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> idArgument() {
        return Commands.argument("id", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                        RuntimeLedgers.get(c.getSource().getServer()).ids().stream().map(ProducerId::value), b));
    }

    private static ProducerLedger ledger(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        return RuntimeLedgers.get(ctx.getSource().getServer()).get(new ProducerId(id))
                .orElseThrow(() -> UNKNOWN_LEDGER.create(id));
    }

    private static int add(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Civilization civ = MythDataCommand.civ(ctx);
        double rate = DoubleArgumentType.getDouble(ctx, "rate");
        MinecraftServer server = ctx.getSource().getServer();
        RuntimeLedgers ledgers = RuntimeLedgers.get(server);
        int n = 1;
        while (ledgers.get(new ProducerId(TestProducers.TYPE + ":" + n)).isPresent()) {
            n++;
        }
        ProducerLedger l = ledgers.register(new ProducerLedger(new ProducerId(TestProducers.TYPE + ":" + n),
                player.getUUID(), civ, rate, WorldClock.now(server)));
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.runtime.added",
                "Ledger %s created", l.id().value()), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        RuntimeLedgers ledgers = RuntimeLedgers.get(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.runtime.count",
                "%s ledger(s), now=%s", ledgers.all().size(), WorldClock.now(ctx.getSource().getServer())), false);
        for (ProducerLedger l : ledgers.all()) {
            ctx.getSource().sendSuccess(() -> Component.literal(l.toString()), false);
        }
        return ledgers.all().size();
    }

    private static int real(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ProducerLedger l = ledger(ctx);
        boolean real = BoolArgumentType.getBool(ctx, "real");
        TestProducers.setRealRunning(l.id(), real);
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.runtime.real",
                "%s real running = %s", l.id().value(), String.valueOf(real)), true);
        return 1;
    }

    private static int simple(CommandContext<CommandSourceStack> ctx, boolean invalidate) throws CommandSyntaxException {
        ProducerLedger l = ledger(ctx);
        RuntimeLedgers ledgers = RuntimeLedgers.get(ctx.getSource().getServer());
        if (invalidate) {
            ledgers.invalidate(l.id());
        } else {
            ledgers.remove(l.id());
        }
        String key = invalidate ? "invalidated" : "removed";
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.runtime." + key,
                "%s " + key, l.id().value()), true);
        return 1;
    }

    private static int settle(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        RuntimeDriver.settleAll(server, ModConfigs.runtime().current().favor());
        int count = RuntimeLedgers.get(server).all().size();
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.runtime.settled",
                "Settled %s ledger(s)", count), true);
        return count;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        long now = WorldClock.now(server);
        RuntimeConfig.Idle idle = ModConfigs.runtime().current().favor().idle();
        boolean active = PlayerActivity.isActive(server, player.getUUID(), now, idle);
        OptionalLong last = PlayerActivity.lastActiveTick(player.getUUID());
        String idleFor = last.isPresent() ? String.valueOf(now - last.getAsLong()) : "-";
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.runtime.status",
                "%s: active=%s, idle %s/%s ticks, now=%s, ledgers=%s", player.getDisplayName(), String.valueOf(active), idleFor,
                idle.enabled() ? idle.graceTicks() : "∞", now,
                RuntimeLedgers.get(server).byOwner(player.getUUID()).size()), false);
        return active ? 1 : 0;
    }
}
