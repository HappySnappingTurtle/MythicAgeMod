package io.github.happysnappingturtle.mythicage.wild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.wild.greek.CentaurCampFeature;
import io.github.happysnappingturtle.mythicage.wild.greek.StymphalianNestFeature;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 野外生物小结构的开发命令（T15），只在开发环境注册，权限等级 2。在执行位置脚下直接搭建并放入生物，
 * 跳过地形与区域密度检查（{@code /place feature} 会走 {@link WildSpawning#areaSiteAllows}，多数位置会被拒绝）：
 * <pre>
 * /mythicage dev wild camp     半人马营地（营火在脚下）
 * /mythicage dev wild nest     斯廷法利斯鸟巢（树干从脚下的水底 / 地面长起）
 * </pre>
 */
public final class WildDevCommand {
    private WildDevCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!FMLEnvironment.production) {
            register(event.getDispatcher());
        }
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mythicage")
                .then(Commands.literal("dev")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("wild")
                                .then(Commands.literal("camp").executes(WildDevCommand::camp))
                                .then(Commands.literal("nest").executes(WildDevCommand::nest)))));
    }

    private static int camp(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        CentaurCampFeature.build(level, at, level.getRandom());
        int n = CentaurCampFeature.spawnGroup(level, at, level.getRandom());
        ctx.getSource().sendSuccess(() -> Component.literal("营地 " + at.toShortString() + "：半人马 " + n + " 只"), false);
        return n;
    }

    private static int nest(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos feet = BlockPos.containing(ctx.getSource().getPosition());
        BlockPos at = new BlockPos(feet.getX(), level.getHeight(Heightmap.Types.OCEAN_FLOOR, feet.getX(), feet.getZ()), feet.getZ());
        BlockPos nest = StymphalianNestFeature.build(level, at, level.getRandom());
        int n = StymphalianNestFeature.spawnFlock(level, nest, level.getRandom());
        ctx.getSource().sendSuccess(() -> Component.literal("鸟巢 " + nest.toShortString() + "：怪鸟 " + n + " 只"), false);
        return n;
    }
}
