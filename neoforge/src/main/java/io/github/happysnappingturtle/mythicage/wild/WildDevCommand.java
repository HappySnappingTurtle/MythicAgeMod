package io.github.happysnappingturtle.mythicage.wild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.entity.harpy.HarpyEntity;
import io.github.happysnappingturtle.mythicage.wild.greek.CentaurCampFeature;
import io.github.happysnappingturtle.mythicage.wild.greek.HarpyNestFeature;
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
 * /mythicage dev wild harpy    哈耳庇厄崖巢（在附近 32 格内逐列找崖边）
 * /mythicage dev wild harpy_attack [目标]   让 32 格内最近的一只哈耳庇厄立刻对目标（默认是你）尖啸俯冲（跳过冷却）
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
                                .then(Commands.literal("nest").executes(WildDevCommand::nest))
                                .then(Commands.literal("harpy").executes(WildDevCommand::harpy))
                                .then(Commands.literal("harpy_attack").executes(c -> harpyAttack(c, c.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.entity())
                                                .executes(c -> harpyAttack(c, net.minecraft.commands.arguments.EntityArgument.getEntity(c, "target"))))))));
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

    private static int harpy(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
        HarpyNestFeature.Cliff c = HarpyNestFeature.findCliff(level, at, 32, 1);
        if (c == null) {
            ctx.getSource().sendFailure(Component.literal("32 格内没有落差 ≥ 6 格、3 × 3 平整的崖边"));
            return 0;
        }
        BlockPos nest = HarpyNestFeature.build(level, c.top(), c.drop(), level.getRandom());
        int n = HarpyNestFeature.spawnFlock(level, nest, level.getRandom());
        ctx.getSource().sendSuccess(() -> Component.literal("崖巢 " + nest.toShortString() + "（朝 " + c.drop().getName() + "）：哈耳庇厄 "
                + n + " 只"), false);
        return n;
    }

    private static int harpyAttack(CommandContext<CommandSourceStack> ctx, net.minecraft.world.entity.Entity target) {
        if (!(target instanceof net.minecraft.world.entity.LivingEntity t)) {
            ctx.getSource().sendFailure(Component.literal("目标必须是生物"));
            return 0;
        }
        var list = t.level().getEntitiesOfClass(HarpyEntity.class, t.getBoundingBox().inflate(32));
        var h = list.stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(t)));
        h.ifPresent(e -> e.beginAttack(t));
        ctx.getSource().sendSuccess(() -> Component.literal(h.isPresent() ? "哈耳庇厄开始尖啸俯冲" : "32 格内没有哈耳庇厄"), false);
        return h.isPresent() ? 1 : 0;
    }
}
