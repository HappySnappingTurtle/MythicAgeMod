package io.github.happysnappingturtle.mythicage.village.quest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageSpace;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * NPC 任务的开发命令（T21 设计文档 §16）：
 * <pre>
 * /mythicage dev village quest list               本村任务：编号、类型、来源、状态、进度、截止、接取者、贡献
 * /mythicage dev village quest scan               立刻做一次日开始的任务扫描（发布、过期、清旧）
 * /mythicage dev village quest open               不用走到管事身边，直接打开委托页
 * /mythicage dev village quest act &lt;编号&gt; &lt;动作&gt;  不看距离地接取 / 交付 / 放弃（accept / deliver / abandon）
 * /mythicage dev village quest expire &lt;编号&gt;      让任务今天到期（实测自修用）
 * /mythicage dev village quest damage             本村的建筑损坏记录（每座最多列 12 格）
 * /mythicage dev village quest selfrepair         未修的损坏格立刻转给村民自修（不等 5 日）
 * </pre>
 */
public final class QuestDevCommand {
    private QuestDevCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var list = Commands.literal("list").executes(c -> withCtx(c, ctx -> {
            VillageQuests.Book b = QuestService.book(ctx.level().getServer(), ctx.village().id());
            ok(c, "任务 " + b.quests.size() + " 条 · 下一个编号 q" + b.next + " · 今日 " + ctx.today());
            for (Quest q : b.quests) {
                ok(c, "  " + q.id() + " " + q.type + " " + q.source + " " + q.state.id() + (q.reason.isEmpty() ? "" : "(" + q.reason + ")")
                        + " " + q.done + "/" + q.need + " 截止 " + q.deadline + " 接取 " + q.accepted.size() + " 贡献 " + q.contrib.size()
                        + " 已结算 " + q.settled.size() + " | " + AffairsView.toComponent(QuestService.title(ctx, q)).getString());
            }
        }));
        var scan = Commands.literal("scan").executes(c -> withCtx(c, ctx -> {
            QuestService.scan(ctx);
            ok(c, "已扫描：" + QuestService.book(ctx.level().getServer(), ctx.village().id()).quests.size() + " 条任务");
        }));
        var open = Commands.literal("open").executes(c -> withCtx(c, ctx -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p != null) {
                QuestService.open(p, ctx.village().id());
            }
        }));
        var act = Commands.literal("act").then(Commands.argument("quest", StringArgumentType.word())
                .then(Commands.argument("action", StringArgumentType.word()).executes(c -> withCtx(c, ctx -> {
                    ServerPlayer p = c.getSource().getPlayer();
                    if (p == null) {
                        return;
                    }
                    QuestService.Outcome o = QuestService.act(p, ctx.village().id(), StringArgumentType.getString(c, "quest"),
                            StringArgumentType.getString(c, "action"), UUID.randomUUID().toString(), false);
                    ok(c, "结果 " + o.result() + "，交付 " + o.delivered());
                }))));
        var damage = Commands.literal("damage").executes(c -> withCtx(c, ctx -> {
            VillageQuests.Book b = QuestService.book(ctx.level().getServer(), ctx.village().id());
            ok(c, "损坏 " + b.damage.size() + " 座 · 玩家方块 " + b.playerBlocks.size());
            b.damage.values().forEach(dm -> {
                ok(c, "  " + dm.key + " 未修 " + dm.cells.size() + " 超出 " + dm.overflow);
                dm.cells.values().stream().limit(12).forEach(x -> ok(c, "    " + BlockPos.of(x.pos).toShortString() + " " + x.block
                        + (x.cause == null ? " 无主" : " 责任 " + x.cause.toString().substring(0, 8)) + " 第 " + x.day + " 日" + (x.self ? " 自修" : "")));
            });
        }));
        var self = Commands.literal("selfrepair").executes(c -> withCtx(c, ctx -> {
            VillageQuests.Book b = QuestService.book(ctx.level().getServer(), ctx.village().id());
            int n = 0;
            for (VillageQuests.Damage dm : b.damage.values()) {
                for (VillageQuests.Cell x : dm.cells.values()) {
                    if (!x.self) {
                        x.self = true;
                        n++;
                    }
                }
            }
            ok(c, "转给村民自修 " + n + " 格");
        }));
        var expire = Commands.literal("expire").then(Commands.argument("quest", StringArgumentType.word()).executes(c -> withCtx(c, ctx -> {
            VillageQuests.Book b = QuestService.book(ctx.level().getServer(), ctx.village().id());
            b.byId(StringArgumentType.getString(c, "quest")).ifPresentOrElse(q -> {
                q.deadline = ctx.today();
                QuestService.refresh(ctx);
                ok(c, q.id() + " → " + q.state.id());
            }, () -> c.getSource().sendFailure(Component.literal("没有这个任务")));
        })));
        d.register(Commands.literal("mythicage").then(Commands.literal("dev").requires(s -> s.hasPermission(2))
                .then(Commands.literal("village").then(Commands.literal("quest").then(list).then(scan).then(open).then(act)
                        .then(expire).then(damage).then(self)))));
    }

    private static int withCtx(CommandContext<CommandSourceStack> c, Consumer<VillageContext> run) {
        Optional<VillageRegistry.Village> v = VillageSpace.villageAt(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()));
        if (v.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("commands.mythic_age.village.none"));
            return 0;
        }
        return VillageScheduler.contextOf(c.getSource().getLevel(), v.get().id()).map(ctx -> {
            run.accept(ctx);
            VillageQuests.get(c.getSource().getServer()).setDirty();
            return 1;
        }).orElse(0);
    }

    private static void ok(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
    }
}
