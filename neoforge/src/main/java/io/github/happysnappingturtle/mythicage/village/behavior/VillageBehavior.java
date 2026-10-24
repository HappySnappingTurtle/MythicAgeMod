package io.github.happysnappingturtle.mythicage.village.behavior;

import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.Scheduler;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文明的行为模块（T19 设计文档 §5.4，V1 §6.3）：挂在调度器上的扩展点，不改调度器本身。
 * 各文明一个实现（希腊 {@code GreekVillageBehavior}），在 {@code village_civ/<civ>.json} 的 {@code behavior} 里按 id 启用。
 */
public interface VillageBehavior {
    VillageBehavior NONE = () -> "";

    String id();

    /** 额外的工作单（祈祷、奠酒、打水……）。 */
    default List<Job> jobs(VillageContext ctx) {
        return List.of();
    }

    /** 改写或删掉（返回 empty）一份工作单。 */
    default Optional<Job> modify(VillageContext ctx, Job job) {
        return Optional.of(job);
    }

    /** 每个 MC 日开始时（历法切换、节庆布置）。 */
    default void onDayStart(VillageContext ctx) {
    }

    /** 某居民此刻能否被派这份工作单。 */
    default boolean available(VillageContext ctx, Scheduler.ResidentView resident, Job job) {
        return true;
    }

    // ───────────── 注册表 ─────────────

    Map<String, VillageBehavior> REGISTRY = new HashMap<>();

    static void register(VillageBehavior b) {
        REGISTRY.put(b.id(), b);
    }

    static VillageBehavior byId(String id) {
        return id == null || id.isEmpty() ? NONE : REGISTRY.getOrDefault(id, NONE);
    }
}
