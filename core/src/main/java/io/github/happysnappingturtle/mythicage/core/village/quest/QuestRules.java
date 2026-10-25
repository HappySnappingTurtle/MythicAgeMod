package io.github.happysnappingturtle.mythicage.core.village.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 任务的通用规则（T21 设计文档 §3.4、§4.2）：接取上限、开放上限、来源去重、发布条件。纯函数。
 */
public final class QuestRules {
    private QuestRules() {
    }

    /** 不能接的原因（语言键后缀）。 */
    public enum Refusal { CLOSED, ALREADY, TAKEN, FULL, EVENT }

    /** 这名玩家在这个村接着的、还没结束的任务数（事件任务不算，§3.4）。 */
    public static int activeFor(Collection<Quest> quests, UUID player) {
        int n = 0;
        for (Quest q : quests) {
            if (q.state.active() && q.scope != Quest.Scope.EVENT && q.accepted.contains(player)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 能不能接：任务在进行、没接过、个人任务没被别人接、名额没满（{@code max}，02 号 §6 每人每村 3 个）。
     * 事件任务不用接。
     */
    public static Optional<Refusal> canAccept(Quest q, UUID player, int active, int max) {
        if (q.scope == Quest.Scope.EVENT) {
            return Optional.of(Refusal.EVENT);
        }
        if (q.state != QuestState.OPEN) {
            return Optional.of(Refusal.CLOSED);
        }
        if (q.accepted.contains(player)) {
            return Optional.of(Refusal.ALREADY);
        }
        if (q.scope == Quest.Scope.PERSONAL && !q.accepted.isEmpty()) {
            return Optional.of(Refusal.TAKEN);
        }
        if (active >= max) {
            return Optional.of(Refusal.FULL);
        }
        return Optional.empty();
    }

    /** 同一来源有没有还没结束的任务（来源去重，§3.3）。 */
    public static boolean openSource(Collection<Quest> quests, String source) {
        for (Quest q : quests) {
            if (q.source.equals(source) && q.state.active()) {
                return true;
            }
        }
        return false;
    }

    /** 一个候选来源：来源键与排序权重（越大越先发，§3.4：缺料时间长、损坏多的先）。 */
    public record Candidate(String source, double priority) {
    }

    /**
     * 从候选里挑要发布的：去掉已有未结束任务的来源，按权重从大到小、同权重按来源键，最多补到每村开放上限 {@code maxOpen}。
     */
    public static List<Candidate> pick(Collection<Quest> quests, List<Candidate> candidates, int maxOpen) {
        long open = quests.stream().filter(q -> q.state.active() && q.scope != Quest.Scope.EVENT).count();
        int room = (int) Math.max(0, maxOpen - open);
        List<Candidate> out = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        candidates.stream()
                .filter(c -> !openSource(quests, c.source()))
                .sorted(Comparator.comparingDouble(Candidate::priority).reversed().thenComparing(Candidate::source))
                .forEach(c -> {
                    if (out.size() < room && seen.add(c.source())) {
                        out.add(c);
                    }
                });
        return out;
    }

    /**
     * 村里能产的料要备料多久没进展才发任务（§4.2）：从最近一次「缺口变小」那天起满 {@code graceDays} 个 MC 日。
     */
    public static boolean stockingDue(long lastProgressDay, long today, int graceDays) {
        return today - lastProgressDay >= graceDays;
    }

    /** 截止日：到了这一天（含）就过期。 */
    public static boolean expired(Quest q, long today) {
        return q.deadline >= 0 && today >= q.deadline;
    }

    /** 结束了多久可以从任务表里清掉（都结算了的、失败的）：保留 {@code keepDays} 个 MC 日供界面显示。 */
    public static boolean purge(Quest q, long today, int keepDays) {
        if (q.state == QuestState.FAILED || q.state == QuestState.SETTLED) {
            return q.endedDay >= 0 && today - q.endedDay >= keepDays;
        }
        return false;
    }
}
