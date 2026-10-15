package io.github.happysnappingturtle.mythicage.guardian;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

/**
 * 守护者的运维命令（T9 设计文档 §7），权限等级 2：
 * <pre>
 * /mythicage guardian spawn     在执行者脚下生成一位祭司，居所即此处
 * /mythicage guardian list      列出注册表中的全部祭司
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
                        .then(Commands.literal("spawn").executes(c -> {
                            ServerLevel level = c.getSource().getLevel();
                            BlockPos at = BlockPos.containing(c.getSource().getPosition());
                            GreekGuardianEntity g = GuardianSpawner.spawnNew(level, at, Optional.empty());
                            if (g == null) {
                                return 0;
                            }
                            c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.guardian.spawned",
                                    at.getX(), at.getY(), at.getZ()), true);
                            return 1;
                        }))
                        .then(Commands.literal("list").executes(c -> {
                            var all = GuardianRegistry.get(c.getSource().getServer()).all();
                            c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.guardian.count",
                                    all.size()), false);
                            for (GuardianRegistry.Entry e : all) {
                                BlockPos h = e.home().pos();
                                c.getSource().sendSuccess(() -> Component.translatable(
                                        e.alive() ? "commands.mythic_age.guardian.alive" : "commands.mythic_age.guardian.dead",
                                        e.guardianId().toString().substring(0, 8), e.home().dimension().location().toString(),
                                        h.getX(), h.getY(), h.getZ(), e.deathDay()), false);
                            }
                            return all.size();
                        }))));
    }
}
