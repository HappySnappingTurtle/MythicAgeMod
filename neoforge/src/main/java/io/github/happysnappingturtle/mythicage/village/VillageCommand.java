package io.github.happysnappingturtle.mythicage.village;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectQueue;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.build.VillageProjects;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Optional;

/**
 * 村庄运维命令（T10a 设计文档 §8），权限等级 2：
 * <pre>
 * /mythicage village info            执行者所在（64 格内最近）的村庄，及其人口（T11a）
 * /mythicage village list            全部已登记村庄
 * /mythicage village reset &lt;钟坐标&gt;  注销该钟的登记；下一次检查时按当前 villages.json 重新登记
 * /mythicage village projects        所在村庄的候选项目、当前项目、「下一个」位（GA-11）
 * /mythicage village nominate &lt;序号&gt; 点名候选列表里的第几项为下一个项目；{@code nominate clear} 清空
 * </pre>
 */
public final class VillageCommand {
    private VillageCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static Component describe(VillageRegistry.Village v) {
        BlockPos b = v.bell().pos();
        return Component.translatable("commands.mythic_age.village.entry",
                VillageService.title(v), VillageService.subtitle(v), v.vanillaType().isEmpty() ? "-" : v.vanillaType(), v.origin().id(),
                v.bell().dimension().location().toString(), b.getX(), b.getY(), b.getZ());
    }

    /** 阶段、成员 / 目标、空床、卫兵上限、上次吸纳移民的日子（T11a 阶段 ③）。 */
    static Component population(net.minecraft.server.level.ServerLevel level, VillageRegistry.Village v) {
        int members = VillagePopulation.members(level, v.bell());
        return Component.translatable("commands.mythic_age.village.population",
                Component.translatable("mythic_age.age." + v.stage().id()), members, VillagePopulation.target(v, members),
                v.baseline(), VillagePopulation.freeBeds(level, v),
                VillagePopulation.guardCap(level, v, v.stage()), v.lastMigrationDay());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mythicage")
                .then(Commands.literal("village")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("info").executes(c -> {
                            Optional<VillageRegistry.Village> v = VillageSpace.villageAt(c.getSource().getLevel(),
                                    BlockPos.containing(c.getSource().getPosition()));
                            if (v.isEmpty()) {
                                c.getSource().sendFailure(Component.translatable("commands.mythic_age.village.none"));
                                return 0;
                            }
                            c.getSource().sendSuccess(() -> describe(v.get()), false);
                            c.getSource().sendSuccess(() -> population(c.getSource().getLevel(), v.get()), false);
                            return 1;
                        }))
                        .then(Commands.literal("list").executes(c -> {
                            List<VillageRegistry.Village> all = VillageRegistry.get(c.getSource().getServer()).all();
                            c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.village.count", all.size()), false);
                            for (VillageRegistry.Village v : all) {
                                c.getSource().sendSuccess(() -> describe(v), false);
                            }
                            return all.size();
                        }))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("bell", BlockPosArgument.blockPos()).executes(c -> {
                                    BlockPos b = BlockPosArgument.getLoadedBlockPos(c, "bell");
                                    boolean removed = VillageRegistry.get(c.getSource().getServer())
                                            .remove(GlobalPos.of(c.getSource().getLevel().dimension(), b));
                                    VillageService.forget(GlobalPos.of(c.getSource().getLevel().dimension(), b));
                                    c.getSource().sendSuccess(() -> Component.translatable(removed
                                            ? "commands.mythic_age.village.reset" : "commands.mythic_age.village.none"), true);
                                    return removed ? 1 : 0;
                                })))
                        .then(Commands.literal("projects").executes(c -> projects(c.getSource())))
                        .then(Commands.literal("nominate")
                                .then(Commands.literal("clear").executes(c -> clear(c.getSource())))
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(c -> nominate(c.getSource(), IntegerArgumentType.getInteger(c, "index")))))));
    }

    private static Optional<VillageContext> here(CommandSourceStack src) {
        Optional<VillageRegistry.Village> v = VillageSpace.villageAt(src.getLevel(), BlockPos.containing(src.getPosition()));
        Optional<VillageContext> ctx = v.flatMap(x -> VillageScheduler.contextOf(src.getLevel(), x.id()));
        if (ctx.isEmpty()) {
            src.sendFailure(Component.translatable("commands.mythic_age.village.none"));
        }
        return ctx;
    }

    static int projects(CommandSourceStack src) {
        Optional<VillageContext> ctx = here(src);
        if (ctx.isEmpty()) {
            return 0;
        }
        VillageEconomy.State s = ctx.get().state();
        s.activeProject().ifPresentOrElse(p -> {
            BuildingSpec spec = VillageBuildings.all().get(p.building);
            Component name = spec == null ? Component.literal(p.building) : Construction.name(spec, p.god);
            src.sendSuccess(() -> Component.translatable("commands.mythic_age.village.projects.current", name,
                    Component.translatable("mythic_age.village.project.state." + p.state.name().toLowerCase(java.util.Locale.ROOT))), false);
        }, () -> src.sendSuccess(() -> Component.translatable("commands.mythic_age.village.projects.current_none"), false));
        if (s.nextProject != null) {
            ProjectQueue.Next n = s.nextProject;
            src.sendSuccess(() -> Component.translatable("commands.mythic_age.village.projects.next", VillageProjects.name(n),
                    n.playerName()), false);
        }
        List<VillageProjects.View> list = VillageProjects.candidates(ctx.get());
        for (int i = 0; i < list.size(); i++) {
            VillageProjects.View view = list.get(i);
            ProjectPicker.Candidate c = view.candidate();
            int n = i + 1;
            int items = view.materials().values().stream().mapToInt(Integer::intValue).sum();
            Component state = c.available()
                    ? Component.translatable("commands.mythic_age.village.projects.rank", c.rank() + 1)
                    : Component.translatable("mythic_age.village.project.reason." + c.reason());
            Component name = Construction.name(view.spec(), c.pick().god());
            src.sendSuccess(() -> Component.translatable("commands.mythic_age.village.projects.entry", n,
                    Component.translatable("mythic_age.village.project.kind." + c.kind().name().toLowerCase(java.util.Locale.ROOT)),
                    name, state, view.materials().size(), items), false);
        }
        if (src.getEntity() instanceof ServerPlayer player) {
            int have = VillageProjects.contribution(player, ctx.get());
            int need = VillageProjects.threshold(player, ctx.get());
            src.sendSuccess(() -> Component.translatable("commands.mythic_age.village.projects.contribution", have, need), false);
        }
        return list.size();
    }

    static int nominate(CommandSourceStack src, int index) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        Optional<VillageContext> ctx = here(src);
        if (ctx.isEmpty()) {
            return 0;
        }
        List<VillageProjects.View> list = VillageProjects.candidates(ctx.get());
        if (index > list.size()) {
            src.sendFailure(Component.translatable("commands.mythic_age.village.nominate.index", list.size()));
            return 0;
        }
        return report(src, VillageProjects.nominate(player, ctx.get(), list.get(index - 1).candidate().key()));
    }

    static int clear(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        Optional<VillageContext> ctx = here(src);
        return ctx.isEmpty() ? 0 : report(src, VillageProjects.clear(player, ctx.get()));
    }

    static int report(CommandSourceStack src, VillageProjects.Outcome o) {
        Component msg = io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads.message(o);
        if (o.result() == VillageProjects.Result.OK || o.result() == VillageProjects.Result.CLEARED) {
            src.sendSuccess(() -> msg, true);
        } else {
            src.sendFailure(msg);
        }
        return o.result() == VillageProjects.Result.OK || o.result() == VillageProjects.Result.CLEARED ? 1 : 0;
    }
}
