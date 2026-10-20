package io.github.happysnappingturtle.mythicage.rite;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.rite.RiteRules;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.MythDataCommand;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

/**
 * 仪式的运维命令（T11a 设计文档 §6.3），权限等级 2：
 * <ul>
 *   <li>{@code /mythicage rite promote <玩家> <文明> <次神>}：不检查代价直接晋升一级；玩家在本文明村庄里时同时推进该村、立祠</li>
 *   <li>{@code /mythicage village stage <钟坐标> <时代>}：设定村庄阶段</li>
 *   <li>{@code /mythicage trial grant <玩家> <文明> <档> [数量]}：发 Boss 凭证</li>
 * </ul>
 */
public final class RiteCommand {
    private RiteCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mythicage")
                .then(Commands.literal("rite").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("promote")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(MythDataCommand.civArgument()
                                                .then(Commands.argument("god", StringArgumentType.word())
                                                        .executes(RiteCommand::promote))))))
                .then(Commands.literal("village").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("stage")
                                .then(Commands.argument("bell", BlockPosArgument.blockPos())
                                        .then(ageArgument().executes(RiteCommand::stage)))))
                .then(Commands.literal("trial").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(MythDataCommand.civArgument()
                                                .then(ageArgument()
                                                        .executes(ctx -> grant(ctx, 1))
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                                .executes(ctx -> grant(ctx,
                                                                        IntegerArgumentType.getInteger(ctx, "count"))))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> ageArgument() {
        return Commands.argument("age", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                java.util.Arrays.stream(Age.values()).map(Age::id), b));
    }

    private static Age age(CommandContext<CommandSourceStack> ctx) {
        return Age.byId(StringArgumentType.getString(ctx, "age")).orElse(Age.NONE);
    }

    private static int promote(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Civilization civ = MythDataCommand.civ(ctx);
        String god = StringArgumentType.getString(ctx, "god");
        PlayerMythData.CivState s = MythData.get(player).civ(civ);
        Optional<Age> next = RiteRules.next(s.age());
        if (next.isEmpty() || s.age() == Age.NONE) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.rite.cannot", s.age().id()));
            return 0;
        }
        Age target = next.get();
        s.setAge(target);
        s.minorGods().put(target, god);
        s.pendingUnlocks().add(target);
        Optional<VillageRegistry.Village> v = io.github.happysnappingturtle.mythicage.village.VillageSpace.villageAt(player.serverLevel(),
                player.blockPosition()).filter(x -> x.civ().equals(civ.id()));
        v.ifPresent(village -> {
            s.approve(village.patron(), target);
            s.markRiteVillage(village.id(), target, true);
            VillageRegistry.get(player.server).update(village.withRite(target, target, god));
        });
        MythData.changed(player);
        RiteService.afterPromotion(player, civ, target, god, v.orElse(null));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.rite.promoted",
                player.getDisplayName(), civ.id(), target.id(), god), true);
        return 1;
    }

    private static int stage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos bell = BlockPosArgument.getLoadedBlockPos(ctx, "bell");
        VillageRegistry reg = VillageRegistry.get(ctx.getSource().getServer());
        Optional<VillageRegistry.Village> v = reg.byBell(GlobalPos.of(ctx.getSource().getLevel().dimension(), bell));
        if (v.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.village.unknown"));
            return 0;
        }
        Age age = age(ctx);
        reg.update(v.get().withStage(age));
        reg.byId(v.get().id()).ifPresent(x -> io.github.happysnappingturtle.mythicage.village.VillagePopulation
                .recordBaseline(ctx.getSource().getLevel(), x));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.village.stage_set",
                bell.toShortString(), age.id()), true);
        return 1;
    }

    private static int grant(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Civilization civ = MythDataCommand.civ(ctx);
        Age tier = age(ctx);
        if (tier != Age.CLASSICAL && tier != Age.HEROIC) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.trial.bad_tier"));
            return 0;
        }
        TrialService.grant(player, civ, tier, count);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.trial.granted",
                player.getDisplayName(), civ.id(), tier.id(), MythData.get(player).civ(civ).vouchers(tier)), true);
        return 1;
    }
}
