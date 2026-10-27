package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestCivRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.SupplyRules;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 缺料供给的四种来源（T21 设计文档 §4.2、§5、§6.4）：在建项目缺料、职业本职缺料、口粮不足、村民自修缺的方块。
 * 来源键：{@code supply:project:<项目>:<物品或组>}、{@code supply:short:<物品>}、{@code supply:food}、{@code supply:repair:<物品>}。
 */
final class SupplyQuests {
    static final String TYPE = "supply";
    static final String PROJECT = "supply:project:";
    static final String SHORT = "supply:short:";
    static final String FOOD = "supply:food";
    /** 村民自修缺的方块（T21 阶段 ②，§6.4）。 */
    static final String REPAIR = "supply:repair:";

    private SupplyQuests() {
    }

    // ───────────── 候选来源 ─────────────

    /**
     * 今天该发的来源（日开始调用）：项目缺的料里「本村没有来源」的立刻发，能产的备料满宽限期没进展才发；
     * 职业缺料只发文明登记过的；口粮跌破底线就发。顺手更新备料进展记录。
     */
    static List<QuestRules.Candidate> candidates(VillageContext ctx, VillageQuests.Book book, VillagesConfig.Quests cfg) {
        List<QuestRules.Candidate> out = new ArrayList<>();
        VillageEconomy.State s = ctx.state();
        CivVillageData d = ctx.data();
        long today = ctx.today();
        Set<String> live = new HashSet<>();
        Optional<BuildProject> project = s.activeProject().filter(p -> p.state == BuildProject.State.STOCKING);
        project.ifPresent(p -> p.missing().forEach((key, n) -> {
            String src = PROJECT + p.id + ":" + key;
            live.add(src);
            if (!producible(d, key)) {
                out.add(new QuestRules.Candidate(src, 100 + (today - p.startDay)));
                return;
            }
            VillageQuests.Track t = book.tracks.get(src);
            if (t == null || n < t.missing()) {
                t = new VillageQuests.Track(n, t == null ? p.startDay : today);
                book.tracks.put(src, t);
            }
            if (QuestRules.stockingDue(t.progressDay(), today, cfg.stockingGraceDays())) {
                out.add(new QuestRules.Candidate(src, today - t.progressDay()));
            }
        }));
        book.tracks.keySet().removeIf(k -> !live.contains(k));
        QuestCivRules rules = d.quests();
        Set<String> professions = new HashSet<>();
        s.roster.values().forEach(m -> professions.add(m.profession()));
        for (String prof : professions) {
            CivVillageData.Profession p = d.professions().get(prof);
            if (p == null || p.shortWhen().items().isEmpty() || !p.shortWhen().items().stream().allMatch(i -> s.stock.count(i) == 0)) {
                continue;
            }
            for (String item : p.shortWhen().items()) {
                if (rules.supplyItems().containsKey(item)) {
                    out.add(new QuestRules.Candidate(SHORT + item, 50));
                }
            }
        }
        if (d.rations().floor() > 0 && d.rations().belowFloor(s.stock)) {
            out.add(new QuestRules.Candidate(FOOD, 200));
        }
        // 自修缺的方块：村里产不了的立刻发；能产的等宽限期（从最早那格转自修算起）
        repairNeeds(book).forEach((item, n) -> {
            if (n > s.stock.count(item)) {
                long day = book.damage.values().stream().flatMap(x -> x.cells.values().stream())
                        .filter(c -> c.self && RepairJob.item(c).equals(item)).mapToLong(c -> c.day).min().orElse(today);
                if (!producible(d, item) || QuestRules.stockingDue(day, today, cfg.stockingGraceDays())) {
                    out.add(new QuestRules.Candidate(REPAIR + item, 120));
                }
            }
        });
        return out;
    }

    /** 自修要用的方块：物品 → 件数。 */
    static Map<String, Integer> repairNeeds(VillageQuests.Book book) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        for (VillageQuests.Damage dmg : book.damage.values()) {
            for (VillageQuests.Cell c : dmg.cells.values()) {
                String item = RepairJob.item(c);
                if (c.self && !item.isEmpty()) {
                    out.merge(item, 1, Integer::sum);
                }
            }
        }
        return out;
    }

    /** 本村有没有这种料的来源（规则在 {@link CivVillageData#producible}）。 */
    static boolean producible(CivVillageData d, String key) {
        return d.producible(key);
    }

    // ───────────── 发布 ─────────────

    /** 发布：定需求、记下单价（规则版本）。来源已不成立时返回 empty。 */
    static Optional<Quest> create(VillageContext ctx, int number, String source, VillagesConfig.Quests cfg) {
        QuestCivRules rules = ctx.data().quests();
        long today = ctx.today();
        String target;
        Quest q;
        if (source.startsWith(PROJECT)) {
            target = source.substring(source.indexOf(':', PROJECT.length()) + 1);
            q = new Quest(number, TYPE, source, target, Quest.Scope.PUBLIC, today, 0, today + cfg.supplyDays());
            q.prices.put(target, rules.price(target));
        } else if (source.startsWith(SHORT)) {
            target = source.substring(SHORT.length());
            q = new Quest(number, TYPE, source, target, Quest.Scope.PUBLIC, today, rules.supplyItems().getOrDefault(target, 1),
                    today + cfg.supplyDays());
            q.prices.put(target, rules.price(target));
        } else if (source.startsWith(REPAIR)) {
            target = source.substring(REPAIR.length());
            q = new Quest(number, TYPE, source, target, Quest.Scope.PUBLIC, today, 0, today + cfg.supplyDays());
            q.prices.put(target, rules.price(target));
        } else if (source.equals(FOOD)) {
            q = new Quest(number, TYPE, source, "", Quest.Scope.PUBLIC, today, 0, today + cfg.supplyDays());
            ctx.data().rations().portions().keySet().forEach(item -> q.prices.put(item, rules.price(item)));
        } else {
            return Optional.empty();
        }
        int rem = remaining(ctx, q);
        if (rem <= 0) {
            return Optional.empty();
        }
        q.need = q.done + rem;
        return Optional.of(q);
    }

    // ───────────── 进度 ─────────────

    /**
     * 还差多少（项目：物品件数；职业缺料：件数；口粮：份数，向上取整）。来源已经没了（项目建成 / 取消 / 换了）返回 −1。
     */
    static int remaining(VillageContext ctx, Quest q) {
        VillageEconomy.State s = ctx.state();
        if (q.source.startsWith(PROJECT)) {
            long id = projectId(q.source);
            Optional<BuildProject> p = s.activeProject();
            if (p.isEmpty() || p.get().id != id) {
                return -1;
            }
            return p.get().missing().getOrDefault(q.target, 0);
        }
        if (q.source.startsWith(SHORT)) {
            return Math.max(0, q.need - q.done);
        }
        if (q.source.startsWith(REPAIR)) {
            int need = repairNeeds(QuestService.book(ctx.level().getServer(), ctx.village().id())).getOrDefault(q.target, 0);
            return Math.max(0, need - s.stock.count(q.target));
        }
        if (q.source.equals(FOOD)) {
            double gap = ctx.data().rations().floor() - ctx.data().rations().total(s.stock);
            return gap <= 0 ? 0 : (int) Math.ceil(gap - 1.0E-9);
        }
        return -1;
    }

    /** 职业缺料任务：村里已经没有这个职业的人了（来源消失）。 */
    static boolean professionGone(VillageContext ctx, Quest q) {
        if (!q.source.startsWith(SHORT)) {
            return false;
        }
        for (VillageEconomy.Member m : ctx.state().roster.values()) {
            CivVillageData.Profession p = ctx.data().professions().get(m.profession());
            if (p != null && p.shortWhen().items().contains(q.target)) {
                return false;
            }
        }
        return true;
    }

    static long projectId(String source) {
        String rest = source.substring(PROJECT.length());
        return Long.parseLong(rest.substring(0, rest.indexOf(':')));
    }

    /** 能交哪些物品（物品组按组内成员；口粮按口粮表）。 */
    static List<String> items(VillageContext ctx, Quest q) {
        if (q.source.equals(FOOD)) {
            return new ArrayList<>(ctx.data().rations().portions().keySet());
        }
        return ctx.data().buildCosts().items(q.target);
    }

    /** 每件算几份进度（口粮按口粮值，其余 1）。 */
    static double unitValue(VillageContext ctx, Quest q, String item) {
        if (q.source.equals(FOOD)) {
            return ctx.data().rations().portions().getOrDefault(item, 0.0);
        }
        return 1;
    }

    /** 这件物品还能收几件。 */
    static int acceptable(VillageContext ctx, Quest q, String item, int remaining) {
        if (q.source.equals(FOOD)) {
            return SupplyRules.foodUnits(remaining, unitValue(ctx, q, item));
        }
        return remaining;
    }

    /** 单价：发布时记下的（物品，或物品组）。 */
    static double price(Quest q, String item) {
        Map<String, Double> p = q.prices;
        return p.containsKey(item) ? p.get(item) : p.getOrDefault(q.target, 0.0);
    }

    // ───────────── 文本 ─────────────

    /** 委托页与记录里的标题。 */
    static AffairsView.Line title(VillageContext ctx, Quest q) {
        if (q.source.startsWith(PROJECT)) {
            Optional<BuildProject> p = ctx.state().activeProject().filter(x -> x.id == projectId(q.source));
            String building = p.map(x -> {
                var spec = io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(x.building);
                return spec == null ? "@mythic_age.quest.building.unknown"
                        : io.github.happysnappingturtle.mythicage.village.build.Construction.nameArg(spec, x.god);
            }).orElse("@mythic_age.quest.building.unknown");
            return AffairsView.Line.of("mythic_age.quest.title.supply.project", building, "@" + QuestService.itemKey(q.target));
        }
        if (q.source.startsWith(SHORT)) {
            return AffairsView.Line.of("mythic_age.quest.title.supply.short", "@" + QuestService.itemKey(q.target));
        }
        if (q.source.startsWith(REPAIR)) {
            return AffairsView.Line.of("mythic_age.quest.title.supply.repair", "@" + QuestService.itemKey(q.target));
        }
        return AffairsView.Line.of("mythic_age.quest.title.supply.food");
    }
}
