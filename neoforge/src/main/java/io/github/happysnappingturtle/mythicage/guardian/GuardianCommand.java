package io.github.happysnappingturtle.mythicage.guardian;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.Optional;

/**
 * 守护者的运维命令（T9 设计文档 §7），权限等级 2：
 * <pre>
 * /mythicage guardian spawn [文明]   在执行者脚下生成一位该文明的祭司，居所即此处；不写文明时取所在村庄的文明
 * /mythicage guardian list      列出注册表中的全部祭司
 * /mythicage guardian gesture &lt;实体&gt; &lt;greet|announce|point|none&gt;   播放一次手势（审阅截图用）
 * </pre>
 */
public final class GuardianCommand {
    private GuardianCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mythicage")
                .then(Commands.literal("guardian")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(c -> {
                                    // 不写文明：取执行者所在村庄的文明
                                    Optional<Civilization> civ = io.github.happysnappingturtle.mythicage.village.VillageSpace.villageAt(c.getSource().getLevel(),
                                                    BlockPos.containing(c.getSource().getPosition()))
                                            .flatMap(v -> Civilization.byId(v.civ()));
                                    if (civ.isEmpty()) {
                                        c.getSource().sendFailure(Component.translatable("commands.mythic_age.guardian.need_civ"));
                                        return 0;
                                    }
                                    return spawn(c.getSource(), civ.get());
                                })
                                .then(Commands.argument("civ", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Civilization.values()).map(Civilization::id), b))
                                        .executes(c -> {
                                            String id = StringArgumentType.getString(c, "civ");
                                            Optional<Civilization> civ = Civilization.byId(id);
                                            if (civ.isEmpty()) {
                                                c.getSource().sendFailure(Component.translatable(
                                                        "commands.mythic_age.guardian.unknown_civ", id));
                                                return 0;
                                            }
                                            return spawn(c.getSource(), civ.get());
                                        })))
                        .then(Commands.literal("gesture")
                                .then(Commands.argument("targets", net.minecraft.commands.arguments.EntityArgument.entities())
                                        .then(Commands.argument("gesture", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                        Arrays.stream(GuardianGesture.values()).map(GuardianGesture::id), b))
                                                .executes(c -> {
                                                    String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "gesture");
                                                    GuardianGesture g = Arrays.stream(GuardianGesture.values())
                                                            .filter(x -> x.id().equals(id)).findFirst().orElse(GuardianGesture.NONE);
                                                    int n = 0;
                                                    for (net.minecraft.world.entity.Entity e : net.minecraft.commands.arguments.EntityArgument.getEntities(c, "targets")) {
                                                        if (e instanceof GuardianEntity gg) {
                                                            gg.playGesture(g);
                                                            n++;
                                                        }
                                                    }
                                                    return n;
                                                }))))
                        .then(Commands.literal("list").executes(c -> {
                            var all = GuardianRegistry.get(c.getSource().getServer()).all();
                            c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.guardian.count",
                                    all.size()), false);
                            for (GuardianRegistry.Entry e : all) {
                                BlockPos h = e.home().pos();
                                c.getSource().sendSuccess(() -> Component.translatable(
                                        e.alive() ? "commands.mythic_age.guardian.alive" : "commands.mythic_age.guardian.dead",
                                        e.guardianId().toString().substring(0, 8),
                                        Component.translatable("mythic_age.civ." + e.civ().id()),
                                        e.home().dimension().location().toString(),
                                        h.getX(), h.getY(), h.getZ(), e.deathDay()), false);
                            }
                            return all.size();
                        }))));
    }

    private static int spawn(CommandSourceStack src, Civilization civ) {
        if (!GuardianSpawner.hasGuardian(civ)) {
            src.sendFailure(Component.translatable("commands.mythic_age.guardian.no_guardian",
                    Component.translatable("mythic_age.civ." + civ.id())));
            return 0;
        }
        ServerLevel level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        GuardianEntity g = GuardianSpawner.spawnNew(level, at, Optional.empty(), civ);
        if (g == null) {
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("commands.mythic_age.guardian.spawned", g.getDisplayName(),
                at.getX(), at.getY(), at.getZ()), true);
        return 1;
    }
}
