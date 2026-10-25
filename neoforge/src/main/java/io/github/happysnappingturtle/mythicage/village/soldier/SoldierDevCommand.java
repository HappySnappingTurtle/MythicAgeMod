package io.github.happysnappingturtle.mythicage.village.soldier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageSpace;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 士兵的开发命令（T20 设计文档 §16）：
 * <pre>
 * /mythicage dev village soldiers                 本村士兵：上限、班次、岗位、兵装、耐久、弹药、目标
 * /mythicage dev village soldier convert [职业]   最近的一名劳力立刻转岗（不看上限，按村库扣兵装）
 * /mythicage dev village soldier demote           最近的一名士兵降回劳力
 * /mythicage dev village soldier kit full|simple|none   最近的一名士兵换成这一档（不扣村库，拍图用）
 * /mythicage dev village soldier daystart         立刻做一次日开始的士兵处理（降回、换装、转岗 1 人）
 * </pre>
 */
public final class SoldierDevCommand {
    private SoldierDevCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var convert = Commands.literal("convert").executes(c -> withCtx(c, ctx -> convert(c, ctx, null)))
                .then(Commands.argument("profession", StringArgumentType.greedyString())
                        .executes(c -> withCtx(c, ctx -> convert(c, ctx, StringArgumentType.getString(c, "profession")))));
        var demote = Commands.literal("demote").executes(c -> withCtx(c, ctx -> nearest(c, ctx, ResidentEntity::isSoldier)
                .ifPresent(r -> {
                    Soldiers.demote(ctx, r);
                    ok(c, r.getName().getString() + " 降回 " + r.profession());
                })));
        var kit = Commands.literal("kit").then(Commands.argument("tier", StringArgumentType.word())
                .suggests((c, b) -> {
                    b.suggest("full");
                    b.suggest("simple");
                    b.suggest("none");
                    return b.buildFuture();
                })
                .executes(c -> withCtx(c, ctx -> nearest(c, ctx, ResidentEntity::isSoldier).ifPresent(r -> {
                    SoldierProfile.Tier t = SoldierProfile.Tier.byId(StringArgumentType.getString(c, "tier"));
                    ctx.data().soldier(r.profession()).ifPresent(p -> {
                        SoldierRecord rec = ctx.state().soldiers.get(r.getUUID());
                        if (rec != null) {
                            ctx.state().soldiers.put(r.getUUID(), rec.withTier(t, 0));
                        }
                        Soldiers.apply(r, p, t);
                        ok(c, r.getName().getString() + " → " + t.id());
                    });
                }))));
        var dayStart = Commands.literal("daystart").executes(c -> withCtx(c, ctx -> {
            Soldiers.dayStart(ctx);
            ok(c, "士兵日开始处理完：" + ctx.state().soldiers.size() + " 名士兵，上限 " + Soldiers.cap(ctx));
        }));
        d.register(Commands.literal("mythicage").then(Commands.literal("dev").requires(s -> s.hasPermission(2))
                .then(Commands.literal("village")
                        .then(Commands.literal("soldiers").executes(c -> withCtx(c, ctx -> list(c, ctx))))
                        .then(Commands.literal("soldier").then(convert).then(demote).then(kit).then(dayStart)))));
    }

    private static void list(CommandContext<CommandSourceStack> c, VillageContext ctx) {
        ok(c, "士兵上限 " + Soldiers.cap(ctx) + "（人口 " + ctx.state().roster.size() + "）· 警戒场次 "
                + Soldiers.alertSerial(ctx.village().id()) + " · 警戒中 " + ctx.rt().alarmed(ctx.now()));
        for (ResidentEntity r : Soldiers.soldiersOf(ctx)) {
            SoldierRecord rec = ctx.state().soldiers.get(r.getUUID());
            SoldierOrders o = r.soldierBrain().orders();
            ok(c, "  " + r.getName().getString() + " " + rec.profession() + " 兵装=" + rec.tier().id() + " 磨损=" + rec.wear()
                    + " 弹药=" + rec.ammo() + " 状态=" + (o == null ? "-" : o.state() + " 岗=" + o.post()) + " 目标="
                    + Optional.ofNullable(r.soldierBrain().target()).map(t -> t.getName().getString() + "(" + r.soldierBrain().decision() + ")")
                    .orElse("-") + " 撤退=" + r.soldierBrain().retreating() + " 举盾=" + r.soldierBrain().blocking()
                    + " 生命=" + (int) r.getHealth() + "/" + (int) r.getMaxHealth() + " 护甲=" + r.getArmorValue()
                    + " 位=" + r.blockPosition().toShortString());
        }
    }

    private static void convert(CommandContext<CommandSourceStack> c, VillageContext ctx, String profession) {
        String prof = profession != null ? profession : ctx.data().soldiers().order().stream().findFirst().orElse("");
        Optional<SoldierProfile> p = ctx.data().soldier(prof);
        if (p.isEmpty()) {
            c.getSource().sendFailure(Component.literal("不是兵种：" + prof));
            return;
        }
        String fallback = ctx.data().residents().fallback();
        nearest(c, ctx, r -> r.profession().equals(fallback)).ifPresent(r -> {
            VillageEconomy.State s = ctx.state();
            SoldierProfile.Tier t = s.stock.count(p.get().kits().item(SoldierProfile.Tier.FULL)) > 0 ? SoldierProfile.Tier.FULL
                    : SoldierProfile.Tier.SIMPLE;
            boolean done = Soldiers.enlist(ctx, r, prof, p.get(), t);
            if (done) {
                ok(c, r.getName().getString() + " → " + prof + "（" + t.id() + "）");
            } else {
                c.getSource().sendFailure(Component.literal("村库里没有 " + p.get().kits().item(t) + "，先 add 一套"));
            }
        });
    }

    private static Optional<ResidentEntity> nearest(CommandContext<CommandSourceStack> c, VillageContext ctx,
                                                    Predicate<ResidentEntity> filter) {
        Optional<ResidentEntity> r = ctx.residents().stream().filter(filter)
                .min(Comparator.comparingDouble(x -> x.distanceToSqr(c.getSource().getPosition())));
        if (r.isEmpty()) {
            c.getSource().sendFailure(Component.literal("附近没有合适的居民"));
        }
        return r;
    }

    private static int withCtx(CommandContext<CommandSourceStack> c, java.util.function.Consumer<VillageContext> run) {
        Optional<VillageRegistry.Village> v = VillageSpace.villageAt(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()));
        if (v.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("commands.mythic_age.village.none"));
            return 0;
        }
        return VillageScheduler.contextOf(c.getSource().getLevel(), v.get().id()).map(ctx -> {
            run.accept(ctx);
            VillageEconomy.get(c.getSource().getServer()).setDirty();
            return 1;
        }).orElse(0);
    }

    private static void ok(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
    }
}
