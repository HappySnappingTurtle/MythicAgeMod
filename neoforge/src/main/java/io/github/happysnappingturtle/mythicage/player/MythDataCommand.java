package io.github.happysnappingturtle.mythicage.player;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxSteps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.UUID;

/**
 * 运维与测试命令（T4 设计文档 §6），权限等级 2：
 * {@code /mythicage favor get|add|set}、{@code /mythicage age set}。
 */
public final class MythDataCommand {
    private static final DynamicCommandExceptionType UNKNOWN_CIV = new DynamicCommandExceptionType(
            id -> Component.translatableWithFallback("commands.mythic_age.unknown_civ", "Unknown civilization: %s", id));
    private static final DynamicCommandExceptionType UNKNOWN_AGE = new DynamicCommandExceptionType(
            id -> Component.translatableWithFallback("commands.mythic_age.unknown_age", "Unknown age: %s", id));

    private MythDataCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mythicage")
                .then(Commands.literal("favor")
                        .requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> get(ctx, null))
                                        .then(civArgument().executes(ctx -> get(ctx, civ(ctx))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(civArgument()
                                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                        .executes(MythDataCommand::add)))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(civArgument()
                                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                        .executes(MythDataCommand::set))))))
                .then(Commands.literal("age")
                        .requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(civArgument()
                                                .then(Commands.argument("age", StringArgumentType.word())
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                                Arrays.stream(Age.values()).map(Age::id), b))
                                                        .executes(MythDataCommand::setAge)))))));
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> civArgument() {
        return Commands.argument("civ", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(Civilization.values()).map(Civilization::id), b));
    }

    public static Civilization civ(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "civ");
        return Civilization.byId(id).orElseThrow(() -> UNKNOWN_CIV.create(id));
    }

    private static int get(CommandContext<CommandSourceStack> ctx, Civilization only) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerMythData data = MythData.get(player);
        for (Civilization c : Civilization.values()) {
            if (only != null && c != only) {
                continue;
            }
            PlayerMythData.CivState s = data.civ(c);
            long cap = MythData.cap(player, c);
            ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.favor.get",
                    "%s %s: %s/%s (%s)", player.getDisplayName(), civName(c), s.favor(), cap, ageName(s.age())), false);
        }
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Civilization c = civ(ctx);
        long amount = LongArgumentType.getLong(ctx, "amount");
        long bonus = CapBonusProvider.Holder.get().capBonus(player, c);
        Transactions.Result result = MythData.transact(player, Transactions.Request.of(
                "cmd:" + UUID.randomUUID(), TxSteps.addFavor(c, amount, bonus)));
        long now = MythData.get(player).civ(c).favor();
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.favor.add",
                "%s %s +%s → %s (%s)", player.getDisplayName(), civName(c), amount, now, result.status().name()), true);
        return result.committed() ? 1 : 0;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Civilization c = civ(ctx);
        long amount = LongArgumentType.getLong(ctx, "amount");
        // 运维命令：直接设定，不受上限约束
        MythData.get(player).civ(c).setFavor(amount);
        MythData.changed(player);
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.favor.set",
                "%s %s = %s", player.getDisplayName(), civName(c), amount), true);
        return 1;
    }

    private static int setAge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Civilization c = civ(ctx);
        String id = StringArgumentType.getString(ctx, "age");
        Age age = Age.byId(id).orElseThrow(() -> UNKNOWN_AGE.create(id));
        PlayerMythData.CivState s = MythData.get(player).civ(c);
        // 升级时补上成功界面（跨级时按逐级合并为一个窗口，T11a §6.2）；降级不弹
        for (int i = Math.max(s.age().ordinal() + 1, Age.CLASSICAL.ordinal()); i <= age.ordinal(); i++) {
            s.pendingUnlocks().add(Age.values()[i]);
        }
        s.setAge(age);
        MythData.changed(player);
        io.github.happysnappingturtle.mythicage.rite.RiteService.refreshShrines(player, c);
        io.github.happysnappingturtle.mythicage.relic.RelicService.reconcile(player);
        io.github.happysnappingturtle.mythicage.rite.UnlockService.flush(player);
        io.github.happysnappingturtle.mythicage.advancement.MythEvents.fire(player, io.github.happysnappingturtle.mythicage.advancement.MythEvents.AGE_REACHED, c, age.ordinal());
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.age.set",
                "%s %s → %s", player.getDisplayName(), civName(c), ageName(age)), true);
        return 1;
    }

    /** 命令反馈里的文明名（T4 §136 留到 T24：不显示 id）。 */
    public static net.minecraft.network.chat.Component civName(io.github.happysnappingturtle.mythicage.core.player.Civilization c) {
        return net.minecraft.network.chat.Component.translatable("mythic_age.civ." + c.id());
    }

    public static net.minecraft.network.chat.Component ageName(io.github.happysnappingturtle.mythicage.core.player.Age a) {
        return net.minecraft.network.chat.Component.translatable("mythic_age.age." + a.id());
    }
}
