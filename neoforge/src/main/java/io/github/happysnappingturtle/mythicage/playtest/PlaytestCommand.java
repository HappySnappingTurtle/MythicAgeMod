package io.github.happysnappingturtle.mythicage.playtest;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.playtest.MilestoneClock;
import io.github.happysnappingturtle.mythicage.core.playtest.PlaytestNote;
import io.github.happysnappingturtle.mythicage.core.playtest.PlaytestStations;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

/**
 * 快速验证命令（GA-3 设计文档 §3），权限 2，正式 jar 可用：
 * <ul>
 *   <li>{@code /mythicage dev playtest}：列出 8 站</li>
 *   <li>{@code /mythicage dev playtest <站>}：进站</li>
 *   <li>{@code /mythicage dev playtest note <文字>}：记一条反馈</li>
 *   <li>{@code /mythicage dev playtest done}：结束当前站</li>
 *   <li>{@code /mythicage dev playtest restore}：恢复第一次进站前的神话数据</li>
 *   <li>{@code /mythicage dev playtest anchors reset}：丢掉固定的村庄、迷宫、神庙位置</li>
 * </ul>
 */
public final class PlaytestCommand {
    private static final Map<Age, String> AGE_NAMES = Map.of(Age.NONE, "未受祝福", Age.ARCHAIC, "远古", Age.CLASSICAL, "古典",
            Age.HEROIC, "英雄", Age.MYTHIC, "神话");
    private static final double REACH = 6;

    private PlaytestCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mythicage").then(Commands.literal("dev")
                .requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("playtest").executes(PlaytestCommand::list)
                        .then(Commands.literal("note").then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(PlaytestCommand::note)))
                        .then(Commands.literal("done").executes(PlaytestCommand::done))
                        .then(Commands.literal("restore").executes(PlaytestCommand::restore))
                        .then(Commands.literal("anchors").then(Commands.literal("reset").executes(PlaytestCommand::resetAnchors)))
                        .then(Commands.argument("station", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        PlaytestStations.ALL.stream().map(PlaytestStations.Station::id), b))
                                .executes(PlaytestCommand::enter)))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        Set<String> done = p == null ? Set.of() : PlaytestState.get(ctx.getSource().getServer()).done(p.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.playtest.list_header",
                PlaytestStations.totalMinutes()), false);
        for (PlaytestStations.Station s : PlaytestStations.ALL) {
            Component line = Component.translatable("commands.mythic_age.playtest.list_line", s.number(), s.id(),
                    Component.translatable(s.nameKey()), s.minutes(), Component.translatable(s.watchKey()));
            ctx.getSource().sendSuccess(() -> done.contains(s.id())
                    ? Component.literal("✔ ").withStyle(ChatFormatting.GREEN).append(line) : line, false);
        }
        return PlaytestStations.ALL.size();
    }

    private static int enter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "station");
        PlaytestStations.Station s = PlaytestStations.byId(id).orElse(null);
        if (s == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.playtest.unknown", id));
            return 0;
        }
        if (!PlaytestJobs.begin(p, s)) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.playtest.busy"));
            return 0;
        }
        return 1;
    }

    private static int note(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String text = StringArgumentType.getString(ctx, "text");
        if (text.isBlank()) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.playtest.note_empty"));
            return 0;
        }
        PlaytestLog.note(PlaytestNote.line(LocalTime.now(), text, state(p)));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.playtest.noted",
                PlaytestLog.file().toString()), false);
        return 1;
    }

    static PlaytestNote.State state(ServerPlayer p) {
        BlockPos pos = p.blockPosition();
        var civ = MythData.get(p).civ(PlaytestSetup.CIV);
        String held = p.getMainHandItem().isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
        return new PlaytestNote.State(pos.getX(), pos.getY(), pos.getZ(), p.level().dimension().location().getPath(),
                AGE_NAMES.get(civ.age()), civ.favor(), held, looking(p));
    }

    /** 看着的实体或方块 id；什么都没看着时为空串。 */
    static String looking(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(REACH));
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(p, eye, end, new AABB(eye, end).inflate(1),
                e -> !e.isSpectator() && e != p, REACH * REACH);
        if (eh != null) {
            Entity e = eh.getEntity();
            return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        }
        HitResult hit = p.pick(REACH, 0, false);
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            return BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(bh.getBlockPos()).getBlock()).toString();
        }
        return "";
    }

    private static int done(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        return PlaytestLog.done(p).map(e -> {
            PlaytestState.get(p.server).markDone(p.getUUID(), e.station());
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.playtest.station_done", e.station(),
                    MilestoneClock.format(e.seconds())), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.playtest.no_station"));
            return 0;
        });
    }

    private static int restore(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        if (PlaytestJobs.running(p.getUUID())) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.playtest.busy"));
            return 0;
        }
        if (!PlaytestSetup.restore(p, PlaytestState.get(p.server))) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.playtest.no_snapshot"));
            return 0;
        }
        PlaytestLog.interrupt(p);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.playtest.restored"), false);
        return 1;
    }

    private static int resetAnchors(CommandContext<CommandSourceStack> ctx) {
        PlaytestState.get(ctx.getSource().getServer()).resetAnchors();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.playtest.anchors_reset"), true);
        return 1;
    }
}
