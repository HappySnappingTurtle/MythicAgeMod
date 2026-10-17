package io.github.happysnappingturtle.mythicage.rite;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.faith.FaithProfile;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxStep;
import io.github.happysnappingturtle.mythicage.core.rite.PromotionTable;
import io.github.happysnappingturtle.mythicage.core.rite.RiteKind;
import io.github.happysnappingturtle.mythicage.core.rite.RiteRules;
import io.github.happysnappingturtle.mythicage.core.rite.VillageStageTable;
import io.github.happysnappingturtle.mythicage.core.trade.InventoryPlan;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.village.Pantheon;
import io.github.happysnappingturtle.mythicage.faith.FaithProfiles;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianGesture;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKinds;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import io.github.happysnappingturtle.mythicage.village.PantheonLoader;
import io.github.happysnappingturtle.mythicage.village.VillagePopulation;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 晋升、立祀、改奉的服务端流程（T11a 设计文档 §6.1）。只在有村庄的祭司处举行（会话沿用 T9 的交易会话）。
 * 顺序同 T9：验证 → 预留供品 → 玩家事务一次提交（收据防重复）→ 扣供品、更新村庄 → 晋升后钩子与反馈。
 * 事务提交之后的步骤不会失败。
 */
public final class RiteService {
    public enum Reason { OK, DUPLICATE, NPC_GONE, PRICE_CHANGED, BLOCKED, NOT_ENOUGH_ITEMS }

    /** @param issues 不可举行的原因（BLOCKED 时） */
    public record Outcome(Reason reason, List<Component> issues) {
        static Outcome of(Reason r) {
            return new Outcome(r, List.of());
        }

        public boolean ok() {
            return reason == Reason.OK;
        }
    }

    /**
     * 一次评估：规则计划、修正后的代价、条件行、全部缺项。
     *
     * @param issues 结构性缺项 + 资源缺项 + 未满足的建筑条件（文本）
     */
    public record Evaluation(RiteRules.Plan plan, RiteRules.Cost cost, List<RitesView.Line> lines, List<Component> issues) {
        public boolean ok() {
            return issues.isEmpty();
        }
    }

    private RiteService() {
    }

    static RiteRules.Village village(VillageRegistry.Village v) {
        return new RiteRules.Village(v.id(), v.civ(), v.patron(), v.stage(), v.shrines());
    }

    // ───────────── 评估 ─────────────

    public static Evaluation evaluate(ServerPlayer player, Civilization civ, VillageRegistry.Village v, RiteKind kind,
                                      Age age, String god) {
        PlayerMythData.CivState s = MythData.get(player).civ(civ);
        long today = MythDays.today(player.server);
        PromotionTable table = RiteTables.promotion(civ);
        RiteRules.Plan plan = RiteRules.plan(s, civ, village(v), PantheonLoader.get(civ), table, kind, age, god, today);
        RiteRules.Cost cost = plan.target() == Age.NONE ? plan.cost()
                : RiteHooks.modify(player, civ, kind, plan.target(), plan.cost());
        List<Component> issues = new ArrayList<>();
        for (RiteRules.Issue i : plan.issues()) {
            issues.add(RiteText.issue(i));
        }
        List<RitesView.Line> lines = new ArrayList<>();
        boolean open = plan.target() != Age.NONE && table.entry(plan.target()).map(e -> !e.broken()).orElse(false);
        if (open) {
            InventoryPlan inv = TradeService.plan(player);
            Map<String, Integer> have = new HashMap<>();
            lines.add(new RitesView.Line(Component.translatable("mythic_age.rite.line.favor"), s.favor() >= cost.favor(),
                    Component.literal(s.favor() + " / " + cost.favor())));
            for (TradeOffer.ItemAmount a : cost.offerings()) {
                int n = inv.count(a.item());
                have.put(a.item(), n);
                lines.add(new RitesView.Line(TradeService.stack(a.item(), 1).getHoverName(), n >= a.count(),
                        Component.literal(n + " / " + a.count())));
            }
            if (cost.vouchers() > 0) {
                int n = s.vouchers(cost.voucherTier());
                lines.add(new RitesView.Line(Component.translatable("mythic_age.rite.line.voucher",
                        Component.translatable("mythic_age.trial." + cost.voucherTier().id())), n >= cost.vouchers(),
                        Component.literal(n + " / " + cost.vouchers())));
            }
            Optional<PromotionTable.AgeEntry> entry = table.entry(plan.target());
            List<RitesView.Line> requirementLines = new ArrayList<>();
            if (entry.isPresent() && !entry.get().broken()) {
                for (PromotionTable.RequirementSpec spec : entry.get().requirements()) {
                    Optional<PromotionRequirement> req = PromotionRequirements.get(spec.type());
                    if (req.isEmpty() || !plan.scopes().contains(req.get().scope())) {
                        continue;
                    }
                    PromotionRequirement.Line l = req.get().check(player, civ, plan.target(), v, spec.params());
                    requirementLines.add(new RitesView.Line(l.label(), l.met(), l.progress()));
                }
            }
            for (RiteRules.Issue i : RiteRules.resources(cost, s.favor(), have, s.vouchers(cost.voucherTier()))) {
                issues.add(RiteText.issue(i));
            }
            for (RitesView.Line l : requirementLines) {
                if (!l.met()) {
                    issues.add(Component.translatable("mythic_age.rite.issue.requirement", l.label(), l.progress()));
                }
            }
            lines.addAll(requirementLines);
        }
        return new Evaluation(plan, cost, lines, issues);
    }

    // ───────────── 界面 ─────────────

    public static RitesView view(ServerPlayer player, Civilization civ, VillageRegistry.Village v) {
        PlayerMythData.CivState s = MythData.get(player).civ(civ);
        List<RitesView.Option> options = new ArrayList<>();
        options.add(option(player, civ, v, RiteKind.PROMOTION, Age.NONE));
        options.add(option(player, civ, v, RiteKind.CONSECRATION, Age.NONE));
        boolean any = false;
        for (Age a : List.of(Age.CLASSICAL, Age.HEROIC, Age.MYTHIC)) {
            if (s.age().atLeast(a)) {
                options.add(option(player, civ, v, RiteKind.REDEDICATION, a));
                any = true;
            }
        }
        if (!any) {
            options.add(option(player, civ, v, RiteKind.REDEDICATION, Age.CLASSICAL));
        }
        List<String> shrines = new ArrayList<>(v.shrineGods());
        return new RitesView(v.patron(), v.stage(), shrines, options);
    }

    /** 根本性的阻碍：有它时整个仪式无从谈起，界面只显示这一条，不列候选、条件和变化。 */
    private static final java.util.Set<String> FATAL = java.util.Set.of("wrong_village", "not_blessed", "max_age",
            "need_classical", "village_max", "village_caught_up", "not_open", "data_error", "age_not_reached");

    private static RitesView.Option option(ServerPlayer player, Civilization civ, VillageRegistry.Village v, RiteKind kind, Age age) {
        Evaluation e = evaluate(player, civ, v, kind, age, "");
        RiteRules.Plan p = e.plan();
        String current = kind == RiteKind.REDEDICATION
                ? MythData.get(player).civ(civ).minorGods().getOrDefault(age, "") : "";
        Optional<RiteRules.Issue> fatal = p.issues().stream().filter(i -> FATAL.contains(i.key())).findFirst();
        if (fatal.isPresent()) {
            return new RitesView.Option(kind, p.target(), p.approved(), e.cost().favor(), List.of(), List.of(),
                    List.of(RiteText.issue(fatal.get())), List.of(), current);
        }
        List<Component> blocking = new ArrayList<>();
        for (RiteRules.Issue i : p.issues()) {
            if (!i.key().equals("choose_god")) {
                blocking.add(RiteText.issue(i));
            }
        }
        List<RitesView.Candidate> cands = new ArrayList<>();
        Pantheon pantheon = PantheonLoader.get(civ);
        List<TradeOffer> offers = TradeTableLoader.table(civ).offers();
        for (RiteRules.Candidate c : RiteRules.candidates(pantheon, v.patron(), p.target())) {
            cands.add(new RitesView.Candidate(c.id(), c.available(), items(offers, c.id(), TradeOffer.Category.GOD_POWER),
                    items(offers, c.id(), TradeOffer.Category.SUMMON), items(offers, c.id(), null)));
        }
        return new RitesView.Option(kind, p.target(), p.approved(), e.cost().favor(), cands, e.lines(), blocking,
                preview(player.serverLevel(), civ, v, p), current);
    }

    /** 某次神的商品图标：category 为空时取神力、召唤以外的类别。 */
    private static List<ItemStack> items(List<TradeOffer> offers, String god, TradeOffer.Category category) {
        List<ItemStack> out = new ArrayList<>();
        for (TradeOffer o : offers) {
            boolean power = o.category() == TradeOffer.Category.GOD_POWER || o.category() == TradeOffer.Category.SUMMON;
            if (o.minorGod().equals(god) && (category == null ? !power : o.category() == category)) {
                out.add(TradeService.stack(o.result().item(), o.result().count()));
            }
        }
        return out;
    }

    private static List<Component> preview(ServerLevel level, Civilization civ, VillageRegistry.Village v, RiteRules.Plan p) {
        List<Component> out = new ArrayList<>();
        if (p.target() == Age.NONE) {
            return out;
        }
        Component age = Component.translatable("mythic_age.age." + p.target().id());
        switch (p.kind()) {
            case PROMOTION -> out.add(Component.translatable("mythic_age.rite.preview.you_age", age));
            case REDEDICATION -> out.add(Component.translatable("mythic_age.rite.preview.rededicate", age));
            default -> {
            }
        }
        if (p.kind() != RiteKind.REDEDICATION) {
            if (p.target().atLeast(v.stage()) && p.target() != v.stage()) {
                VillageStageTable table = RiteTables.stages(civ);
                VillageStageTable.Stage st = table.stage(p.target());
                out.add(Component.translatable("mythic_age.rite.preview.stage", age));
                // 按本村现有成员算出前后的人口目标与卫兵上限（D9、D10；与 VillagePopulation 同一套公式）
                int members = VillagePopulation.members(level, v.bell());
                int base = v.baseline() >= 0 ? v.baseline() : members;
                int popBefore = table.populationTarget(base, v.stage());
                int popAfter = table.populationTarget(base, p.target());
                if (popAfter > popBefore) {
                    out.add(Component.translatable("mythic_age.rite.preview.population", popBefore, popAfter));
                }
                int guardsBefore = table.guardCap(members, v.stage());
                int guardsAfter = table.guardCap(members, p.target());
                if (guardsAfter > guardsBefore) {
                    out.add(Component.translatable("mythic_age.rite.preview.guards", guardsBefore, guardsAfter));
                }
                st.preview().forEach(k -> out.add(Component.translatable(k)));
            } else {
                out.add(Component.translatable("mythic_age.rite.preview.already",
                        Component.translatable("mythic_age.age." + v.stage().id())));
            }
        }
        out.add(Component.translatable("mythic_age.rite.preview.shrine"));
        return out;
    }

    // ───────────── 举行 ─────────────

    public static Outcome perform(ServerPlayer player, RiteKind kind, Age age, String god, long expectedFavor,
                                  String requestId) {
        Optional<TradeService.Session> session = TradeService.session(player);
        if (session.isEmpty() || session.get().venue() != TradeOffer.Venue.GUARDIAN
                || !TradeService.valid(player, session.get())) {
            TradeService.close(player);
            return Outcome.of(Reason.NPC_GONE);
        }
        Civilization civ = session.get().civ();
        Optional<VillageRegistry.Village> village = TradeService.village(player, session.get());
        if (village.isEmpty()) {
            return new Outcome(Reason.BLOCKED, List.of(Component.translatable("mythic_age.rite.issue.wrong_village")));
        }
        PlayerMythData data = MythData.get(player);
        String receipt = "rite:" + requestId;
        if (data.receipts().contains(ReceiptBook.Scope.REQUEST, receipt)) {
            return Outcome.of(Reason.DUPLICATE);
        }
        VillageRegistry.Village v = village.get();
        Evaluation e = evaluate(player, civ, v, kind, age, god);
        if (e.plan().target() != Age.NONE && e.cost().favor() != expectedFavor) {
            return Outcome.of(Reason.PRICE_CHANGED);
        }
        if (!e.ok()) {
            return new Outcome(Reason.BLOCKED, e.issues());
        }
        // 预留供品
        InventoryPlan inv = TradeService.plan(player);
        for (TradeOffer.ItemAmount a : e.cost().offerings()) {
            if (!inv.remove(a.item(), a.count())) {
                return Outcome.of(Reason.NOT_ENOUGH_ITEMS);
            }
        }
        // 提交
        RiteRules.Plan plan = e.plan();
        RiteRules.Cost cost = e.cost();
        RiteRules.Village rv = village(v);
        long today = MythDays.today(player.server);
        Age before = data.civ(civ).age();
        TxStep step = new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData d) {
                PlayerMythData.CivState s = d.civ(civ);
                if (s.age() != before) {
                    return Optional.of("时代已变化");
                }
                if (s.favor() < cost.favor() || (cost.vouchers() > 0 && s.vouchers(cost.voucherTier()) < cost.vouchers())) {
                    return Optional.of("恩宠或凭证不足");
                }
                return Optional.empty();
            }

            @Override
            public void apply(PlayerMythData d) {
                RiteRules.applyToPlayer(d.civ(civ), plan, cost, rv, today);
            }
        };
        Transactions.Result r = MythData.transact(player, new Transactions.Request(receipt, ReceiptBook.Scope.REQUEST,
                List.of(step)));
        if (r.status() == Transactions.Status.DUPLICATE) {
            return Outcome.of(Reason.DUPLICATE);
        }
        if (!r.committed()) {
            MythicAge.LOGGER.warn("仪式 {} 在预检通过后被事务拒绝：{}", kind.id(), r.reason());
            return Outcome.of(Reason.PRICE_CHANGED);
        }
        // 提交之后：扣供品、更新村庄（不会失败）
        for (TradeOffer.ItemAmount a : cost.offerings()) {
            TradeService.removeItems(player, a.item(), a.count());
        }
        RiteRules.VillageChange change = RiteRules.villageChange(plan, rv);
        VillageRegistry reg = VillageRegistry.get(player.server);
        VillageRegistry.Village updated = v.withRite(change.stage(), change.shrineAge(), change.shrineGod());
        reg.update(updated);
        updated = VillagePopulation.recordBaseline(player.serverLevel(), updated);
        if (updated.stage() != v.stage()) {
            announceStage(player, updated);
        }
        if (kind == RiteKind.PROMOTION) {
            afterPromotion(player, civ, plan.target(), god, updated);
        }
        if (player.level().getEntity(session.get().guardianEntityId()) instanceof GuardianEntity g) {
            g.playGesture(GuardianGesture.ANNOUNCE);
        }
        return Outcome.of(Reason.OK);
    }

    /** 晋升之后：钩子、刷新玩家神庙认定（按新时代重扫）、弹出成功界面。 */
    static void afterPromotion(ServerPlayer player, Civilization civ, Age age, String god, VillageRegistry.Village v) {
        io.github.happysnappingturtle.mythicage.advancement.MythEvents.fire(player, io.github.happysnappingturtle.mythicage.advancement.MythEvents.AGE_REACHED, civ, age.ordinal());
        RiteHooks.runAfterPromotion(player, civ, age, god, v);
        refreshShrines(player, civ);
        UnlockService.flush(player);
    }

    /** 时代变化后，玩家本文明的神庙立即按新时代刷新（T8a 的缓存原本只在重扫时更新）。 */
    public static void refreshShrines(ServerPlayer player, Civilization civ) {
        Age age = MythData.get(player).civ(civ).age();
        for (ShrineRegistry.Entry e : ShrineRegistry.get(player.server).ownedBy(player.getUUID(), civ)) {
            ServerLevel level = player.server.getLevel(e.pos().dimension());
            if (level != null && level.isLoaded(e.pos().pos()) && level.getBlockEntity(e.pos().pos()) instanceof ShrineBlockEntity be) {
                be.refreshOwnerAge(age);
                ShrineKinds.byId(e.kind()).ifPresent(k -> k.hooks().onBound(level, e.pos().pos(), Optional.empty()));
            }
        }
    }

    private static void announceStage(ServerPlayer actor, VillageRegistry.Village v) {
        ServerLevel level = actor.server.getLevel(v.bell().dimension());
        if (level == null) {
            return;
        }
        Component msg = Component.translatable("mythic_age.village.stage_up", VillageService.title(v),
                Component.translatable("mythic_age.age." + v.stage().id()));
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(v.bell().pos(), VillageService.LEAVE_RANGE)) {
                p.sendSystemMessage(msg);
            }
        }
    }

    // ───────────── 网络 ─────────────

    public static void handlePerform(RitePayloads.Perform p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        Outcome o = perform(player, p.kind(), p.age(), p.god(), p.expectedFavor(), p.requestId());
        if (o.reason() == Reason.NPC_GONE) {
            TradeService.send(player, new io.github.happysnappingturtle.mythicage.trade.TradePayloads.Close());
            return;
        }
        TradeService.session(player).ifPresent(s -> TradeService.send(player,
                new RitePayloads.Result(p.requestId(), o.reason(), o.issues(), TradeService.viewOf(player, s))));
    }
}
