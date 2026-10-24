package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 一种工作单类型（T19 设计文档 §5.1）：生成候选、派单前确认托管、结算。新文明需要新类型时注册一个，不改调度器。
 */
public interface JobType {
    String id();

    /** 这一轮的候选工作单（只看登记的资源与真实状态，不扫描世界）。 */
    List<Job> candidates(VillageContext ctx);

    /** 派单前要托管的材料（默认是 {@code job.inputs()}）；empty 表示此刻开不了单。 */
    default Optional<Map<String, Integer>> escrow(VillageContext ctx, Job job) {
        return Optional.of(job.inputs());
    }

    /**
     * 结算（工作单最后一步）：按真实状态改世界、入库。返回 false 表示目标已失效（托管退回、目标记失败）。
     * 成功时调度器随后消耗本单的托管。
     */
    boolean settle(VillageContext ctx, ResidentEntity resident, Job job);

    Map<String, JobType> REGISTRY = new LinkedHashMap<>();

    static void register(JobType t) {
        REGISTRY.put(t.id(), t);
    }
}
