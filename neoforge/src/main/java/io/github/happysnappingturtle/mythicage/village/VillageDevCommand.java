package io.github.happysnappingturtle.mythicage.village;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.build.SiteMarkers;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

/**
 * 村庄经济命令（T19 设计文档 §15）：
 * <pre>
 * /mythicage village stock                    所在村庄的库存、托管、容量、名册人数（权限 2）
 * /mythicage village affairs                  打开所在村庄的村务界面（权限 2）
 * /mythicage dev village add &lt;物品&gt; &lt;数量&gt;   往村库里加（开发环境）
 * /mythicage dev village run &lt;true|false&gt;     强制真实运行 / 未运行（开发环境；用来测离线补算）
 * /mythicage dev village process              立刻调度一次
 * /mythicage dev village talk                          对最近的居民右键（说话、打开交易或村务界面），不用对准
 * /mythicage dev village nextday                       MC 日序号 +1（不改世界时钟，重启后还原；看第二天的祈祷、移民）
 * /mythicage dev village migrate                       不等移民日，直接来一名在途移民（T19 阶段 ④）
 * /mythicage dev village project status|pick|cancel        施工项目（T19 阶段 ③）
 * /mythicage dev village project start &lt;建筑&gt; [地块]     开指定建筑的项目
 * /mythicage dev village project drop &lt;段数&gt;             不等居民，直接落段（拍图用）
 * /mythicage dev village calendar &lt;第几日&gt;              把本村历法调到一年中的第几日（看节庆、神谕日；T19 阶段 ⑤）
 * /mythicage dev village life                          历法、节庆段、神谕所、当日计数、祭司差事
 * /mythicage dev foundation give                       发一块城邦奠基石（T19 阶段 ⑥；正式获得要神话时代 + 许德拉之鳞，T25）
 * /mythicage dev foundation place &lt;坐标&gt;             以执行者的身份在某处放一块（等同手放：检查、记建立者、打开选主神界面）
 * </pre>
 */
public final class VillageDevCommand {
    private VillageDevCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    /** 逐格位图里一共有几格（状态命令用）。 */
    private static int count(java.util.Map<Integer, java.util.BitSet> cells) {
        return cells.values().stream().mapToInt(java.util.BitSet::cardinality).sum();
    }

    private static Optional<VillageRegistry.Village> here(CommandContext<CommandSourceStack> c) {
        Optional<VillageRegistry.Village> v = VillageSpace.villageAt(c.getSource().getLevel(),
                BlockPos.containing(c.getSource().getPosition()));
        if (v.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("commands.mythic_age.village.none"));
        }
        return v;
    }

    private static int withCtx(CommandContext<CommandSourceStack> c, java.util.function.Consumer<VillageContext> run) {
        return here(c).flatMap(v -> VillageScheduler.contextOf(c.getSource().getLevel(), v.id())).map(ctx -> {
            run.accept(ctx);
            return 1;
        }).orElse(0);
    }

    /** 开一个指定建筑的项目：替换类找可替换的旧建筑；否则用给定的地块，或第一块放得下的空地块。 */
    private static void start(CommandContext<CommandSourceStack> c, VillageContext ctx, String building, String plot) {
        var spec = io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(building);
        if (spec == null) {
            c.getSource().sendFailure(Component.literal("没有建筑 " + building));
            return;
        }
        String replaces = "";
        String plotId = plot == null ? "" : plot;
        if (!spec.replace().isEmpty()) {
            replaces = Construction.replaceableForCommand(ctx, spec).orElse("");
        } else if (plotId.isEmpty()) {
            plotId = Construction.firstPlotFor(ctx, spec).orElse("");
        }
        var p = Construction.start(ctx, new io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker.Pick(building,
                plotId, spec.god().equals("*") ? "apollo" : spec.god(), replaces));
        c.getSource().sendSuccess(() -> Component.literal(p.map(x -> "开工：" + x.building + " @ " + x.origin + " " + x.turn
                + " 段 " + x.segments + " 账单 " + x.bill()).orElse("开不了工")), false);
    }

    static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext build) {
        d.register(Commands.literal("mythicage").then(Commands.literal("village").requires(s -> s.hasPermission(2))
                .then(Commands.literal("stock").executes(c -> here(c).map(v -> {
                    VillageEconomy.State s = VillageEconomy.get(c.getSource().getServer()).state(v.id());
                    c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.village.stock",
                            s.stock.total(), s.stock.capacity(), s.roster.size(), s.points.size(),
                            s.stock.items().toString(), s.stock.escrows().size()), false);
                    VillageScheduler.Runtime rt = VillageScheduler.runtime(v.id());
                    c.getSource().sendSuccess(() -> Component.literal("完成/失败/作废：" + rt.counts()), false);
                    c.getSource().sendSuccess(() -> Component.literal("调度：" + rt.lastStats() + " · 已加载 "
                            + VillageScheduler.loaded(v.id()).size() + " · 进行中 " + rt.activeJobs().stream()
                            .map(j -> j.type() + "@" + j.target().x() + "," + j.target().y() + "," + j.target().z()).toList()), false);
                    for (io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity r : VillageScheduler.loaded(v.id())) {
                        c.getSource().sendSuccess(() -> Component.literal("  " + r.getName().getString() + " " + r.profession()
                                + " 单=" + r.job().map(j -> j.type()).orElse("-") + " 步=" + r.step() + " 导航完=" + r.getNavigation().isDone()
                                + " 位=" + r.blockPosition().toShortString() + " 动作=" + r.anim() + " 路径=" + r.job().map(j -> {
                                    var path = r.getNavigation().createPath(new BlockPos(j.target().x(), j.target().y(), j.target().z()), 1);
                                    return path == null ? "null" : (path.canReach() + "/" + path.getNodeCount() + "→" + path.getTarget().toShortString()
                                            + " 终=" + (path.getEndNode() == null ? "-" : path.getEndNode().asBlockPos().toShortString()));
                                }).orElse("-") + " 到中心=" + java.util.Optional.ofNullable(r.getNavigation().createPath(v.bell().pos(), 1))
                                        .map(pp -> pp.canReach() + "/" + pp.getNodeCount()).orElse("null")
                                        + " 可更新=" + r.onGround() + " 步高=" + r.maxUpStep() + " 寻路=" + r.getNavigation().getClass().getSimpleName()
                                + (r.job().filter(j -> j.type().equals(io.github.happysnappingturtle.mythicage.village.build.BuildJob.ID)).isPresent()
                                        ? " 施工=" + io.github.happysnappingturtle.mythicage.village.build.BuildJob.diagnostic(r) : "")), false);
                    }
                    return 1;
                }).orElse(0)))
                .then(Commands.literal("affairs").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return here(c).map(v -> {
                        VillageAffairs.open(p, v.id());
                        return 1;
                    }).orElse(0);
                }))));
        if (FMLEnvironment.production) {
            return;
        }
        io.github.happysnappingturtle.mythicage.village.soldier.SoldierDevCommand.register(d);
        io.github.happysnappingturtle.mythicage.village.quest.QuestDevCommand.register(d);
        // 奠基石（T19 阶段 ⑥，D9：阶段 A 只能用命令发放）
        d.register(Commands.literal("mythicage").then(Commands.literal("dev").requires(s -> s.hasPermission(2))
                .then(Commands.literal("foundation").then(Commands.literal("give").executes(c -> {
                    boolean ok = FoundationService.give(c.getSource().getPlayerOrException(),
                            io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK);
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "给了一块城邦奠基石" : "背包满了或这个文明没有奠基石"), false);
                    return ok ? 1 : 0;
                })).then(Commands.literal("place").then(Commands.argument("pos",
                        net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).executes(c -> {
                    var player = c.getSource().getPlayerOrException();
                    BlockPos at = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(c, "pos");
                    var civ = io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;
                    var err = FoundationService.placementError(player, c.getSource().getLevel(), at, civ);
                    if (err.isPresent()) {
                        c.getSource().sendFailure(err.get());
                        return 0;
                    }
                    c.getSource().getLevel().setBlockAndUpdate(at,
                            io.github.happysnappingturtle.mythicage.content.greek.village.GreekFoundation.HEARTH_STONE.get().defaultBlockState());
                    if (c.getSource().getLevel().getBlockEntity(at)
                            instanceof io.github.happysnappingturtle.mythicage.village.found.FoundationStoneBlockEntity be) {
                        FoundationService.onPlaced(c.getSource().getLevel(), at, player, be, civ);
                    }
                    return 1;
                }))))));
        d.register(Commands.literal("mythicage").then(Commands.literal("dev").requires(s -> s.hasPermission(2))
                .then(Commands.literal("village")
                        .then(Commands.literal("add").then(Commands.argument("item", ItemArgument.item(build))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100000)).executes(c -> here(c).map(v -> {
                                    String id = BuiltInRegistries.ITEM.getKey(ItemArgument.getItem(c, "item").getItem()).toString();
                                    int got = VillageEconomy.get(c.getSource().getServer()).state(v.id()).stock
                                            .deposit(id, IntegerArgumentType.getInteger(c, "count"));
                                    c.getSource().sendSuccess(() -> Component.literal(id + " +" + got), false);
                                    return 1;
                                }).orElse(0)))))
                        // 从村库取走（不进背包，直接销毁）：村库满了腾地方、测试缺料时用
                        .then(Commands.literal("take").then(Commands.argument("item", ItemArgument.item(build))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100000)).executes(c -> here(c).map(v -> {
                                    String id = BuiltInRegistries.ITEM.getKey(ItemArgument.getItem(c, "item").getItem()).toString();
                                    int got = VillageEconomy.get(c.getSource().getServer()).state(v.id()).stock
                                            .take(id, IntegerArgumentType.getInteger(c, "count"), 0);
                                    c.getSource().sendSuccess(() -> Component.literal(id + " -" + got), false);
                                    return 1;
                                }).orElse(0)))))
                        .then(Commands.literal("run").then(Commands.argument("running", BoolArgumentType.bool()).executes(c -> here(c).map(v -> {
                            boolean r = BoolArgumentType.getBool(c, "running");
                            VillageScheduler.forceRunningForTest(v.id(), r);
                            c.getSource().sendSuccess(() -> Component.literal("running = " + r), false);
                            return 1;
                        }).orElse(0))))
                        .then(Commands.literal("process").executes(c -> here(c).map(v -> {
                            boolean r = VillageScheduler.processNowForTest(c.getSource().getLevel(), v.id());
                            c.getSource().sendSuccess(() -> Component.literal("processed, running = " + r), false);
                            return 1;
                        }).orElse(0)))
                        .then(Commands.literal("talk").executes(c -> {
                            // 不用对准：对最近的居民「右键」（说一句话，有交易表的打开交易界面）
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            return p.serverLevel().getEntitiesOfClass(io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity.class,
                                            p.getBoundingBox().inflate(32)).stream()
                                    .min(java.util.Comparator.comparingDouble(r -> r.distanceToSqr(p)))
                                    .map(r -> {
                                        VillageScheduler.onInteract(p, r);
                                        return 1;
                                    }).orElse(0);
                        }))
                        .then(Commands.literal("nextday").executes(c -> {
                            io.github.happysnappingturtle.mythicage.guardian.MythDays.advanceForTest(1);
                            c.getSource().sendSuccess(() -> Component.literal("MC 日 → "
                                    + io.github.happysnappingturtle.mythicage.guardian.MythDays.today(c.getSource().getServer())), false);
                            return 1;
                        }))
                        .then(Commands.literal("calendar").then(Commands.argument("day", IntegerArgumentType.integer(1, 366))
                                .executes(c -> withCtx(c, ctx -> {
                                    int want = IntegerArgumentType.getInteger(c, "day");
                                    ctx.state().calendarShift += want - ctx.date().dayOfYear();
                                    VillageEconomy.get(c.getSource().getServer()).setDirty();
                                    var dt = ctx.data().calendar().date(ctx.today(), io.github.happysnappingturtle.mythicage.village.work.VillageData
                                            .offset(ctx.data(), ctx.state()));
                                    c.getSource().sendSuccess(() -> Component.literal("历法 → 第 " + dt.dayOfYear() + " 日 " + dt.period()
                                            + " " + dt.special()), false);
                                }))))
                        .then(Commands.literal("life").executes(c -> withCtx(c, ctx -> {
                            String fest = io.github.happysnappingturtle.mythicage.village.life.VillageLife.festivalToday(ctx)
                                    .map(f -> f.id()).orElse("无");
                            String phase = io.github.happysnappingturtle.mythicage.village.life.VillageLife.festivalSoon(ctx, 600)
                                    .map(a -> a.festival().id() + "/" + a.phase().kind() + (a.eve() ? "（前夜）" : "")).orElse("无");
                            String oracle = io.github.happysnappingturtle.mythicage.village.life.OracleService.status(c.getSource().getServer(),
                                    ctx.village()).map(o -> o.state() + " 下一个第 " + o.nextDay() + " 日").orElse("没有神谕所");
                            String priest = io.github.happysnappingturtle.mythicage.village.life.VillageLife.priest(ctx)
                                    .map(g -> g.errand() == null ? "在居所" : "差事 " + g.errand().tag()).orElse("不在");
                            c.getSource().sendSuccess(() -> Component.literal("钟 " + ctx.center().toShortString() + " · 祭坛 "
                                    + io.github.happysnappingturtle.mythicage.village.life.VillageLife.altar(ctx).toShortString()
                                    + " · 广场 " + io.github.happysnappingturtle.mythicage.village.life.VillageLife.point(ctx, "center")
                                    .map(BlockPos::toShortString).orElse("-") + " · ritual " + ctx.anchors("ritual") + " · 第 " + ctx.date().dayOfYear() + " 日 " + ctx.date().period()
                                    + " " + ctx.date().special() + " · 刻 " + ctx.timeOfDay() + " · 阶段 " + ctx.village().stage().id()
                                    + "\n节日 " + fest + " · 段 " + phase + " · 口粮 ×"
                                    + io.github.happysnappingturtle.mythicage.village.life.VillageLife.rations(ctx)
                                    + "\n神谕 " + oracle + " · 祭司 " + priest + "\n当日计数 " + ctx.state().daily), false);
                        })))
                        .then(Commands.literal("migrate").executes(c -> withCtx(c, ctx -> {
                            var m = io.github.happysnappingturtle.mythicage.village.work.ResidentMigration.forceForCommand(ctx);
                            c.getSource().sendSuccess(() -> Component.literal(m.map(r -> "在途移民 " + r.getName().getString()
                                    + " " + r.profession() + " @ " + r.blockPosition().toShortString() + " → 床 " + r.home().toShortString())
                                    .orElse("来不了（没空床或找不到落脚地）")), false);
                        })))
                        .then(Commands.literal("build").then(Commands.literal("speed")
                                .then(Commands.argument("multiplier", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(.01, 1000))
                                        .executes(c -> withCtx(c, ctx -> {
                                            double speed = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "multiplier");
                                            Construction.speed(ctx.village().id(), speed);
                                            c.getSource().sendSuccess(() -> Component.literal("施工速度 ×" + speed), false);
                                        })))))
                        .then(Commands.literal("project")
                                .then(Commands.literal("status").executes(c -> withCtx(c, ctx -> {
                                    String msg = ctx.state().activeProject().map(p -> p.building + " " + p.state + " @ " + p.origin
                                            + " 段 " + p.placed.cardinality() + "/" + p.segments
                                            + " 格 已放 " + count(p.placedCells) + " 被挡 " + count(p.blockedCells) + " 缺料 " + count(p.missingCells)
                                            + (p.blockedReason.isEmpty() ? "" : "（" + p.blockedReason + "）")
                                            + " 施工牌 " + SiteMarkers.sign(ctx, p).map(BlockPos::toShortString).orElse("无")
                                            + " 缺 " + p.missing() + " 账单 " + p.bill()
                                            + (p.blockedAt == null ? "" : " 停工 " + p.blockedAt)).orElse("没有进行中的项目");
                                    c.getSource().sendSuccess(() -> Component.literal(msg + " · 已建 " + ctx.state().constructed.size()
                                            + " · 储备 " + ctx.state().constructionBank), false);
                                })))
                                .then(Commands.literal("pick").executes(c -> withCtx(c, ctx -> {
                                    var p = Construction.daily(ctx);
                                    c.getSource().sendSuccess(() -> Component.literal(p.map(x -> "开工：" + x.building + " @ " + x.origin)
                                            .orElse("没有可选的项目")), false);
                                })))
                                .then(Commands.literal("start").then(Commands.argument("building", StringArgumentType.string())
                                        .executes(c -> withCtx(c, ctx -> start(c, ctx, StringArgumentType.getString(c, "building"), null)))
                                        .then(Commands.argument("plot", StringArgumentType.string()).executes(c -> withCtx(c, ctx ->
                                                start(c, ctx, StringArgumentType.getString(c, "building"), StringArgumentType.getString(c, "plot")))))))
                                .then(Commands.literal("drop").then(Commands.argument("count", IntegerArgumentType.integer(1, 10000))
                                        .executes(c -> withCtx(c, ctx -> ctx.state().activeProject().ifPresent(p -> {
                                            int n = IntegerArgumentType.getInteger(c, "count");
                                            int done = 0;
                                            Construction.Result last = null;
                                            for (int i = 0; i < p.segments && done < n && p.active(); i++) {
                                                if (p.placed.get(i)) {
                                                    continue;
                                                }
                                                if (p.state == io.github.happysnappingturtle.mythicage.core.village.build.BuildProject.State.STOCKING) {
                                                    Construction.tick(ctx);
                                                }
                                                last = Construction.drop(ctx, p, i);
                                                if (last != Construction.Result.PLACED) {
                                                    break;
                                                }
                                                done++;
                                            }
                                            int fd = done;
                                            Construction.Result fl = last;
                                            c.getSource().sendSuccess(() -> Component.literal("落了 " + fd + " 段，最后 " + fl), false);
                                        }))))))
                                .then(Commands.literal("cancel").executes(c -> withCtx(c, ctx -> ctx.state().activeProject().ifPresent(p -> {
                                    var back = Construction.cancel(ctx, p);
                                    c.getSource().sendSuccess(() -> Component.literal("取消，退回 " + back), false);
                                })))))));
    }
}
