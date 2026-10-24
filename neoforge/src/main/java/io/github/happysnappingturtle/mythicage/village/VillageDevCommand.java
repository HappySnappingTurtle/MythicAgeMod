package io.github.happysnappingturtle.mythicage.village;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
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
 * </pre>
 */
public final class VillageDevCommand {
    private VillageDevCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    private static Optional<VillageRegistry.Village> here(CommandContext<CommandSourceStack> c) {
        Optional<VillageRegistry.Village> v = VillageSpace.villageAt(c.getSource().getLevel(),
                BlockPos.containing(c.getSource().getPosition()));
        if (v.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("commands.mythic_age.village.none"));
        }
        return v;
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
                                        + " 可更新=" + r.onGround() + " 步高=" + r.maxUpStep() + " 寻路=" + r.getNavigation().getClass().getSimpleName()), false);
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
                        }).orElse(0))))));
    }
}
