package io.github.happysnappingturtle.mythicage.core.village.quest;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一个任务实例（T21 设计文档 §3.1）。可变：进度、贡献、接取与结算名单随事件更新；只由 neoforge 的任务服务改。
 *
 * <ul>
 *   <li>{@code source}：来源键，同一来源同时只有一个未结束的任务（§3.3）；</li>
 *   <li>{@code target}：类型相关（供给 = 物品或物品组，修复 = 建筑编号，护送 = 移民，救援 = 居民）；</li>
 *   <li>{@code need}：固定需求（职业缺料、修复、护送、救援）；在建项目与口粮这类「还差多少」随村库变化的，
 *       由来源当场算，{@code need} 只是发布时的值；</li>
 *   <li>{@code prices}：发布时记下的单价（绿宝石 / 件），之后热更报酬表不影响已有任务（12 号 §6 规则版本）；</li>
 *   <li>{@code owed}：每名玩家该得的绿宝石（小数累计，结算时向上取整）；{@code contrib}：每名玩家的贡献点。</li>
 * </ul>
 */
public final class Quest {
    public enum Scope {
        /** 谁都能接、一起完成（供给、修复）。 */
        PUBLIC,
        /** 一人接（护送）。 */
        PERSONAL,
        /** 事件任务：不用接、不占名额（救援）。 */
        EVENT;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Scope byId(String id) {
            for (Scope s : values()) {
                if (s.id().equals(id)) {
                    return s;
                }
            }
            return PUBLIC;
        }
    }

    public final int number;
    public final String type;
    public final String source;
    public final String target;
    public final Scope scope;
    public final long issued;
    public int need;
    public int done;
    public long deadline;
    public QuestState state = QuestState.OPEN;
    public String reason = "";
    /** 进入结束状态（COMPLETE / INVALID / EXPIRED / FAILED）的 MC 日。 */
    public long endedDay = -1;
    /** 缺口是被接了任务的玩家交付补上的（COMPLETE）；被捐赠、村里自产补上的是 INVALID「需求已满足」。 */
    public boolean filled;
    /** 进入暂停的 MC 日（恢复时截止日顺延这么多天）。 */
    public long pausedSince = -1;
    public final Map<String, Double> prices = new LinkedHashMap<>();
    public final Map<UUID, Double> owed = new LinkedHashMap<>();
    public final Map<UUID, Integer> contrib = new LinkedHashMap<>();
    public final Set<UUID> accepted = new LinkedHashSet<>();
    public final Set<UUID> settled = new LinkedHashSet<>();

    public Quest(int number, String type, String source, String target, Scope scope, long issued, int need, long deadline) {
        this.number = number;
        this.type = type;
        this.source = source;
        this.target = target;
        this.scope = scope;
        this.issued = issued;
        this.need = need;
        this.deadline = deadline;
    }

    public String id() {
        return "q" + number;
    }

    /**
     * 改状态；不合法的迁移返回 false 且不改。进入结束状态时记下日子；暂停与恢复时按暂停天数顺延截止日。
     */
    public boolean move(QuestState to, long today, String why) {
        if (!state.canMoveTo(to)) {
            return false;
        }
        if (to == QuestState.PAUSED) {
            pausedSince = today;
        } else if (state == QuestState.PAUSED && to == QuestState.OPEN) {
            deadline += Math.max(0, today - pausedSince);
            pausedSince = -1;
        }
        if (to.ended() || to == QuestState.FAILED) {
            endedDay = today;
        }
        state = to;
        if (why != null) {
            reason = why;
        }
        return true;
    }

    /** 这名玩家在这个任务里交过东西（或修过、护送过、救过）。 */
    public boolean contributed(UUID player) {
        return owed.getOrDefault(player, 0.0) > 0 || contrib.getOrDefault(player, 0) > 0;
    }

    /** 结束了、但这名玩家还没结算。 */
    public boolean owes(UUID player) {
        return state.ended() && contributed(player) && !settled.contains(player);
    }

    /** 结束了且所有贡献者都结算了。 */
    public boolean allSettled() {
        for (UUID p : participants()) {
            if (contributed(p) && !settled.contains(p)) {
                return false;
            }
        }
        return true;
    }

    public Set<UUID> participants() {
        Set<UUID> out = new LinkedHashSet<>(owed.keySet());
        out.addAll(contrib.keySet());
        return out;
    }

    /** 记一笔贡献：绿宝石（小数）与贡献点。 */
    public void credit(UUID player, double emeralds, int points) {
        if (emeralds > 0) {
            owed.merge(player, emeralds, Double::sum);
        }
        if (points > 0) {
            contrib.merge(player, points, Integer::sum);
        }
    }

    /** 结算给这名玩家的绿宝石数（向上取整；0 件不给）。 */
    public int payout(UUID player) {
        return SupplyRules.payout(owed.getOrDefault(player, 0.0));
    }
}
