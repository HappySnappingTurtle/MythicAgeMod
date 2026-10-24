package io.github.happysnappingturtle.mythicage.core.village.work;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工作单（T19 设计文档 §5.2，V1 §4.2）。由需求生成器或行为模块给出，调度器分派，居民实体执行。
 *
 * @param id          村内唯一（同一轮规划里）
 * @param type        类型（{@code gather_point}、{@code craft}…，可注册新类型）
 * @param target      目标坐标（走到这里干活）
 * @param targetKey   目标的稳定键（失败计数、防止两人抢同一目标），如 {@code point:clay_0}
 * @param ref         类型自己的引用（配方 id、资源点 id……）
 * @param steps       步骤序列
 * @param inputs      开始时托管的材料；不够就不开单
 * @param outputs     完成时一次性入库的产物（执行器可按真实状态改写）
 * @param professions 能做的职业；空表示任何空闲居民
 * @param priority    优先级
 * @param weight      同优先级内的权重（历法期调整），大的先排
 * @param outdoor     户外工作（雨天暂停）
 * @param assignee    只能由这名居民做（回家、睡觉）；为 null 时不限
 */
public record Job(long id, String type, Pos target, String targetKey, String ref, List<JobStep> steps,
                  Map<String, Integer> inputs, Map<String, Integer> outputs, Set<String> professions,
                  Priority priority, double weight, boolean outdoor, int timeoutTicks, java.util.UUID assignee) {
    public static final int DEFAULT_TIMEOUT = 1200;

    public Job(long id, String type, Pos target, String targetKey, String ref, List<JobStep> steps,
               Map<String, Integer> inputs, Map<String, Integer> outputs, Set<String> professions, Priority priority,
               double weight, boolean outdoor, int timeoutTicks) {
        this(id, type, target, targetKey, ref, steps, inputs, outputs, professions, priority, weight, outdoor, timeoutTicks,
                null);
    }

    public Job {
        steps = List.copyOf(steps);
        inputs = Map.copyOf(inputs);
        outputs = Map.copyOf(outputs);
        professions = Set.copyOf(professions);
    }

    public boolean allows(String profession) {
        return professions.isEmpty() || professions.contains(profession);
    }

    public boolean allows(java.util.UUID resident, String profession) {
        return (assignee == null || assignee.equals(resident)) && allows(profession);
    }

    public boolean needsWalk() {
        return steps.stream().anyMatch(s -> s.kind() == JobStep.Kind.MOVE);
    }

    public String escrowId() {
        return io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock.JOB_PREFIX + id;
    }

    public Job withPriority(Priority p) {
        return new Job(id, type, target, targetKey, ref, steps, inputs, outputs, professions, p, weight, outdoor, timeoutTicks, assignee);
    }

    public Job withWeight(double w) {
        return new Job(id, type, target, targetKey, ref, steps, inputs, outputs, professions, priority, w, outdoor, timeoutTicks, assignee);
    }

    public Job withId(long newId) {
        return new Job(newId, type, target, targetKey, ref, steps, inputs, outputs, professions, priority, weight, outdoor,
                timeoutTicks, assignee);
    }

    /** 常用的「走到 → 动作 → 结算」三步。 */
    public static List<JobStep> walkActSettle(String anim, int ticks) {
        return List.of(JobStep.move(), JobStep.act(anim, ticks), JobStep.settle());
    }
}
