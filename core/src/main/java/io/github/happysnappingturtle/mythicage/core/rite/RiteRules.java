package io.github.happysnappingturtle.mythicage.core.rite;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.village.Pantheon;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 晋升、立祀、改奉的纯规则（T11a 设计文档 §4.1）。分三步：
 * <ol>
 *   <li>{@link #plan}：目标时代、基础代价、要查哪些范围的建筑条件、结构性的缺项（村庄、候选、冷却……）；</li>
 *   <li>{@link #resources}：按（经修正后的）代价核对恩宠、供品、Boss 凭证；</li>
 *   <li>{@link #applyToPlayer} 与 {@link #villageChange}：提交后玩家与村庄的新状态。</li>
 * </ol>
 * 缺项全部列出（红线 6：逐条告诉玩家），不只第一条。
 */
public final class RiteRules {
    public static final int REDEDICATION_COOLDOWN_DAYS = 3;

    private RiteRules() {
    }

    /**
     * 举行地村庄。
     *
     * @param civ     文明 id；中立为空
     * @param patron  主神 id；可能为空
     * @param shrines 各时代立过祠的次神
     */
    public record Village(UUID id, String civ, String patron, Age stage, Map<Age, List<String>> shrines) {
        public boolean hasShrine(String god) {
            return shrines.values().stream().anyMatch(l -> l.contains(god));
        }
    }

    /** 一条缺项：语言键 {@code mythic_age.rite.issue.<key>} 与参数。 */
    public record Issue(String key, List<Object> args) {
        public static Issue of(String key, Object... args) {
            return new Issue(key, List.of(args));
        }
    }

    /**
     * @param favor       恩宠
     * @param offerings   供品
     * @param voucherTier 要消耗的 Boss 凭证档；NONE = 不要
     * @param vouchers    凭证张数（0 或 1）
     */
    public record Cost(long favor, List<TradeOffer.ItemAmount> offerings, Age voucherTier, int vouchers) {
        public static final Cost NONE = new Cost(0, List.of(), Age.NONE, 0);

        public Cost {
            offerings = List.copyOf(offerings);
        }
    }

    /**
     * @param target   晋升 / 立祀的目标时代；改奉为所改的时代；无法确定时为 NONE
     * @param approved 立祀时：该主神已认可玩家到目标级（半价）
     * @param scopes   要查的建筑条件范围
     * @param issues   结构性缺项（不含资源与建筑条件）
     */
    public record Plan(RiteKind kind, Age target, String god, boolean approved, Cost cost, Set<RequirementScope> scopes,
                       List<Issue> issues) {
        public Plan {
            scopes = Set.copyOf(scopes);
            issues = List.copyOf(issues);
        }
    }

    public record Candidate(String id, boolean available) {
    }

    public static Optional<Age> next(Age a) {
        return a.ordinal() + 1 < Age.values().length ? Optional.of(Age.values()[a.ordinal() + 1]) : Optional.empty();
    }

    /** 本村主神名下某时代的全部次神（含未开放的，界面灰显）。 */
    public static List<Candidate> candidates(Pantheon pantheon, String patron, Age age) {
        return pantheon.underPatron(patron, age).stream().map(g -> new Candidate(g.id(), g.available())).toList();
    }

    public static long half(long favor) {
        return (favor + 1) / 2;
    }

    /**
     * @param age  改奉时：要改的时代；晋升、立祀忽略（目标由玩家时代与村庄阶段决定）
     * @param god  所选次神；为空时缺项里有 choose_god（界面预览用）
     */
    public static Plan plan(PlayerMythData.CivState s, Civilization civ, Village v, Pantheon pantheon, PromotionTable table,
                            RiteKind kind, Age age, String god, long today) {
        List<Issue> issues = new ArrayList<>();
        if (!civ.id().equals(v.civ()) || v.patron().isEmpty()) {
            issues.add(Issue.of("wrong_village"));
            return new Plan(kind, Age.NONE, god, false, Cost.NONE, Set.of(), issues);
        }
        if (s.age() == Age.NONE) {
            issues.add(Issue.of("not_blessed"));
            return new Plan(kind, Age.NONE, god, false, Cost.NONE, Set.of(), issues);
        }
        Age target;
        Set<RequirementScope> scopes = EnumSet.noneOf(RequirementScope.class);
        boolean approved = false;
        switch (kind) {
            case PROMOTION -> {
                Optional<Age> n = next(s.age());
                if (n.isEmpty()) {
                    issues.add(Issue.of("max_age"));
                    return new Plan(kind, Age.NONE, god, false, Cost.NONE, scopes, issues);
                }
                target = n.get();
                scopes.add(RequirementScope.PLAYER);
                scopes.add(RequirementScope.VILLAGE);
            }
            case CONSECRATION -> {
                if (!s.age().atLeast(Age.CLASSICAL)) {
                    issues.add(Issue.of("need_classical"));
                    return new Plan(kind, Age.NONE, god, false, Cost.NONE, scopes, issues);
                }
                Optional<Age> n = next(v.stage() == Age.NONE ? Age.ARCHAIC : v.stage());
                if (n.isEmpty()) {
                    issues.add(Issue.of("village_max"));
                    return new Plan(kind, Age.NONE, god, false, Cost.NONE, scopes, issues);
                }
                target = n.get();
                if (!s.age().atLeast(target)) {
                    issues.add(Issue.of("village_caught_up", v.stage().id()));
                }
                approved = s.approval(v.patron()).atLeast(target);
                scopes.add(RequirementScope.VILLAGE);
            }
            default -> {
                target = age;
                if (!PromotionTable.promotable(age) || !s.age().atLeast(age)) {
                    issues.add(Issue.of("age_not_reached", age.id()));
                }
                long days = s.rededicatedDay() < 0 ? Long.MAX_VALUE : today - s.rededicatedDay();
                if (days < REDEDICATION_COOLDOWN_DAYS) {
                    issues.add(Issue.of("cooldown", REDEDICATION_COOLDOWN_DAYS - days));
                }
                if (!god.isEmpty() && god.equals(s.minorGods().get(age))) {
                    issues.add(Issue.of("same_god"));
                }
            }
        }
        Optional<PromotionTable.AgeEntry> entry = table.entry(target);
        Cost cost = Cost.NONE;
        if (entry.isEmpty()) {
            issues.add(Issue.of("not_open", target.id()));
        } else if (entry.get().broken()) {
            issues.add(Issue.of("data_error", target.id()));
        } else {
            PromotionTable.AgeEntry e = entry.get();
            cost = switch (kind) {
                case PROMOTION -> new Cost(e.favor(), e.offerings(), e.trial(), e.trial() == Age.NONE ? 0 : 1);
                case CONSECRATION -> approved ? new Cost(half(e.favor()), e.offerings(), Age.NONE, 0)
                        : new Cost(e.favor(), e.offerings(), e.trial(), e.trial() == Age.NONE ? 0 : 1);
                default -> new Cost(half(e.favor()), List.of(), Age.NONE, 0);
            };
        }
        List<Candidate> cands = candidates(pantheon, v.patron(), target);
        if (god.isEmpty()) {
            issues.add(Issue.of(cands.stream().anyMatch(Candidate::available) ? "choose_god" : "no_candidate"));
        } else {
            Optional<Candidate> c = cands.stream().filter(x -> x.id().equals(god)).findFirst();
            if (c.isEmpty()) {
                issues.add(Issue.of("not_candidate", god));
            } else if (!c.get().available()) {
                issues.add(Issue.of("god_unavailable", god));
            }
        }
        return new Plan(kind, target, god, approved, cost, scopes, issues);
    }

    /** 核对资源，返回全部缺项。itemsHave：物品 id → 背包里的数量。 */
    public static List<Issue> resources(Cost cost, long favorHave, Map<String, Integer> itemsHave, int vouchersHave) {
        List<Issue> out = new ArrayList<>();
        if (favorHave < cost.favor()) {
            out.add(Issue.of("favor", favorHave, cost.favor()));
        }
        for (TradeOffer.ItemAmount a : cost.offerings()) {
            int have = itemsHave.getOrDefault(a.item(), 0);
            if (have < a.count()) {
                out.add(Issue.of("offering", a.item(), have, a.count()));
            }
        }
        if (cost.vouchers() > 0 && vouchersHave < cost.vouchers()) {
            out.add(Issue.of("voucher", cost.voucherTier().id(), vouchersHave, cost.vouchers()));
        }
        return out;
    }

    /** 提交后玩家的新状态（恩宠、凭证、时代、次神、认可、冷却、仪式村庄、待弹成功界面）。 */
    public static void applyToPlayer(PlayerMythData.CivState s, Plan p, Cost cost, Village v, long today) {
        s.setFavor(s.favor() - cost.favor());
        if (cost.vouchers() > 0) {
            s.setVouchers(cost.voucherTier(), s.vouchers(cost.voucherTier()) - cost.vouchers());
        }
        switch (p.kind()) {
            case PROMOTION -> {
                s.setAge(p.target());
                s.minorGods().put(p.target(), p.god());
                s.approve(v.patron(), p.target());
                s.markRiteVillage(v.id(), p.target(), true);
                s.pendingUnlocks().add(p.target());
            }
            case CONSECRATION -> {
                s.approve(v.patron(), p.target());
                s.markRiteVillage(v.id(), p.target(), false);
            }
            case REDEDICATION -> {
                s.minorGods().put(p.target(), p.god());
                s.setRededicatedDay(today);
                s.markRiteVillage(v.id(), p.target(), false);
            }
        }
    }

    /** 提交后村庄的变化：新阶段、为哪位次神在哪个时代立祠。 */
    public record VillageChange(Age stage, Age shrineAge, String shrineGod) {
    }

    public static VillageChange villageChange(Plan p, Village v) {
        Age stage = switch (p.kind()) {
            case PROMOTION -> p.target().atLeast(v.stage()) ? p.target() : v.stage();
            case CONSECRATION -> p.target();
            case REDEDICATION -> v.stage();
        };
        return new VillageChange(stage, p.target(), p.god());
    }
}
