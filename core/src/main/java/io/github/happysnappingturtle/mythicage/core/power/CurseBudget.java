package io.github.happysnappingturtle.mythicage.core.power;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 诅咒的目标挑选（T13 设计文档 §3.4；05 号 GP-GR-09）：按最大生命从低到高累加，遇到第一个会让累计超过预算的就停，
 * 不再继续看后面的（卡片新增规则）。精英、Boss 由调用方事先排除。
 */
public final class CurseBudget {
    public static final double BUDGET = 60;

    /** 一个候选：任意标识 + 最大生命。 */
    public record Candidate<T>(T id, double maxHealth) {
    }

    private CurseBudget() {
    }

    public static <T> List<T> select(List<Candidate<T>> candidates, double budget) {
        List<Candidate<T>> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble(Candidate::maxHealth));
        List<T> out = new ArrayList<>();
        double sum = 0;
        for (Candidate<T> c : sorted) {
            if (sum + c.maxHealth() > budget) {
                break;
            }
            sum += c.maxHealth();
            out.add(c.id());
        }
        return out;
    }
}
