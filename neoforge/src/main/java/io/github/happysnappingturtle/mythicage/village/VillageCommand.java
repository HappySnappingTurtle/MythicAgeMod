package io.github.happysnappingturtle.mythicage.village;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Optional;

/**
 * 村庄运维命令（T10a 设计文档 §8），权限等级 2：
 * <pre>
 * /mythicage village info            执行者所在（64 格内最近）的村庄
 * /mythicage village list            全部已登记村庄
 * /mythicage village reset &lt;钟坐标&gt;  注销该钟的登记；下一次检查时按当前 villages.json 重新登记
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
                VillageService.title(v), VillageService.subtitle(v), v.vanillaType(), v.origin().id(),
                v.bell().dimension().location().toString(), b.getX(), b.getY(), b.getZ());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mythicage")
                .then(Commands.literal("village")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("info").executes(c -> {
                            Optional<VillageRegistry.Village> v = VillageService.villageAt(c.getSource().getLevel(),
                                    BlockPos.containing(c.getSource().getPosition()), VillageService.LEAVE_RANGE);
                            if (v.isEmpty()) {
                                c.getSource().sendFailure(Component.translatable("commands.mythic_age.village.none"));
                                return 0;
                            }
                            c.getSource().sendSuccess(() -> describe(v.get()), false);
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
                                })))));
    }
}
