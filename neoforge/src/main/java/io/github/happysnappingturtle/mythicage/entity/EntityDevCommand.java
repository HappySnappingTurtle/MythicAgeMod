package io.github.happysnappingturtle.mythicage.entity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.List;

/**
 * 实体动画的开发命令（T7 设计文档 §6.3），只在开发环境（非发布包）注册，权限等级 2：
 * <pre>
 * /mythicage dev anim &lt;实体&gt; &lt;状态&gt;        直接设置动画状态（auto|idle|walk|charge_windup|charge_run|charge_recover）
 * /mythicage dev charge &lt;实体&gt; &lt;距离&gt;      沿当前朝向冲锋一次：蓄力 → 冲刺（直线位移，无伤害）→ 收势
 * /mythicage dev variant &lt;实体&gt; &lt;变体&gt;     切换外观变体（none 为基础外观）
 * </pre>
 */
public final class EntityDevCommand {
    private static final SimpleCommandExceptionType NO_ENTITY = new SimpleCommandExceptionType(
            Component.translatableWithFallback("commands.mythic_age.dev.no_entity", "No supported entity selected"));
    private static final DynamicCommandExceptionType UNKNOWN_STATE = new DynamicCommandExceptionType(
            s -> Component.translatableWithFallback("commands.mythic_age.dev.unknown_state", "Unknown animation state: %s", s));

    private EntityDevCommand() {
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
                        .then(Commands.literal("anim")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("state", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(MythAnimState.values()).map(MythAnimState::id), b))
                                                .executes(EntityDevCommand::anim))))
                        .then(Commands.literal("charge")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(0, 64))
                                                .executes(EntityDevCommand::charge))))
                        .then(Commands.literal("variant")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("variant", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("none", "boss"), b))
                                                .executes(EntityDevCommand::variant))))));
    }

    private static List<MinotaurEntity> targets(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<MinotaurEntity> list = EntityArgument.getEntities(ctx, "targets").stream()
                .filter(e -> e instanceof MinotaurEntity)
                .map(e -> (MinotaurEntity) e)
                .toList();
        if (list.isEmpty()) {
            throw NO_ENTITY.create();
        }
        return list;
    }

    private static int anim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "state");
        MythAnimState state = MythAnimState.byId(id);
        if (state == null) {
            throw UNKNOWN_STATE.create(id);
        }
        List<MinotaurEntity> list = targets(ctx);
        for (MinotaurEntity e : list) {
            e.charge().cancel();
            e.setAnimState(state);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.dev.anim", list.size(), state.id()), false);
        return list.size();
    }

    private static int charge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        double distance = DoubleArgumentType.getDouble(ctx, "distance");
        List<MinotaurEntity> list = targets(ctx);
        list.forEach(e -> e.startCharge(distance));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.dev.charge", list.size(), distance), false);
        return list.size();
    }

    private static int variant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "variant");
        String v = "none".equals(name) ? "" : name;
        List<MinotaurEntity> list = targets(ctx);
        list.forEach(e -> e.setVariant(v));
        float scale = list.get(0).variantScale();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.dev.variant", list.size(), name, scale), false);
        return list.size();
    }
}
