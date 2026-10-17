package io.github.happysnappingturtle.mythicage.power;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.power.PowerSpec;
import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 神力的开发命令（T12 设计文档 §7），只在开发环境（非发布包）注册，权限等级 2：
 * <pre>
 * /mythicage dev power &lt;hit|area|place&gt;   登记开发神力，清掉自己的希腊神力冷却，给 16 张该神力的纸
 * </pre>
 * 开发神力与 GameTest 的测试神力同构：{@code dev_hit} 单体伤害 4、吟唱 20 刻；{@code dev_area} 选点半径 5 的敌方伤害 4、
 * 吟唱 30 刻；{@code dev_place} 放置一个 10 秒的临时菌光体、吟唱 10 刻。
 */
public final class PowerDevCommand {
    private static final String MATRIX = """
            "matrix":{"hostile":"affect","elite":"affect","boss":"affect","enemy_player":"pvp","neutral_animal":"affect"}""";
    private static final Map<String, String> SPECS = Map.of(
            "hit", """
                    {"schemaVersion":1,"id":"dev_hit","civilization":"greek","form":"scroll","castTicks":20,"cooldownTicks":100,
                     "range":24,"targetProfile":"HOSTILE_1",%s}""".formatted(MATRIX),
            "area", """
                    {"schemaVersion":1,"id":"dev_area","civilization":"greek","form":"scroll","castTicks":30,"cooldownTicks":100,
                     "range":24,"radius":5,"targetProfile":"HOSTILE_AREA","durationTicks":60,%s}""".formatted(MATRIX),
            "place", """
                    {"schemaVersion":1,"id":"dev_place","civilization":"greek","form":"placeable","castTicks":10,
                     "cooldownTicks":100,"targetProfile":"PLACEMENT","durationTicks":200}""");

    private PowerDevCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (FMLEnvironment.production) {
            return;
        }
        event.getDispatcher().register(Commands.literal("mythicage")
                .then(Commands.literal("dev")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("power")
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SPECS.keySet(), b))
                                        .executes(PowerDevCommand::give)))));
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String kind = StringArgumentType.getString(ctx, "kind");
        String json = SPECS.get(kind);
        if (json == null) {
            ctx.getSource().sendFailure(Component.literal("未知的开发神力：" + kind));
            return 0;
        }
        PowerSpec spec = PowerSpec.parse(json, new ArrayList<>());
        // 借用测试登记口：开发神力只活在内存里，重启即消失
        PowerSpecLoader.overrideForTest(spec);
        GodPowers.overrideForTest(effect(kind, spec.id()));
        PlayerMythData data = MythData.get(p);
        data.civ(Civilization.GREEK).setPowerCooldownUntil(0);
        data.powerCooldowns().clear();
        ItemStack s = new ItemStack(Items.PAPER, 16);
        s.set(PowerItems.GOD_POWER.get(), spec.id());
        s.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal(spec.id()));
        p.getInventory().add(s);
        ctx.getSource().sendSuccess(() -> Component.literal("已登记 " + spec.id()), false);
        return 1;
    }

    private static GodPower effect(String kind, String id) {
        return switch (kind) {
            case "hit" -> new GodPower() {
                @Override
                public String id() {
                    return id;
                }

                @Override
                public void execute(CastContext c) {
                    if (c.target() instanceof LivingEntity l) {
                        l.hurt(c.level().damageSources().magic(), 4);
                        PowerFx.emit(c, PowerFxPayload.Phase.HIT, List.of(l));
                    }
                }
            };
            case "area" -> new GodPower() {
                @Override
                public String id() {
                    return id;
                }

                @Override
                public void execute(CastContext c) {
                    Set<UUID> own = TargetFactsCollector.ownVillages(c.caster());
                    List<Entity> hit = new ArrayList<>();
                    for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class,
                            new AABB(c.origin(), c.origin()).inflate(c.spec().radius()),
                            e -> e.position().distanceTo(c.origin()) <= c.spec().radius())) {
                        if (!TargetClassifier.NONE.equals(TargetClassifier.reaction(c.spec(),
                                TargetFactsCollector.collect(c.caster(), e, own)))) {
                            e.hurt(c.level().damageSources().magic(), 4);
                            hit.add(e);
                        }
                    }
                    PowerFx.emit(c, PowerFxPayload.Phase.HIT, hit);
                }
            };
            default -> new GodPower() {
                @Override
                public String id() {
                    return id;
                }

                @Override
                public void execute(CastContext c) {
                    if (c.blockPos() != null && c.face() != null) {
                        TempBlocks.place(c.level(), c.blockPos().relative(c.face()), Blocks.SHROOMLIGHT.defaultBlockState(),
                                c.spec().durationTicks(), c.caster().getUUID(), false);
                    }
                }
            };
        };
    }
}
