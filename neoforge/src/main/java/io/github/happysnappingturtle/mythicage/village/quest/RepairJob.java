package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 村民自修（T21 设计文档 §6.4，D3）：修复任务到期或没人接的损坏格，村民从村库取 1 件同种方块放回原状。
 * 优先级同在建项目（{@link Priority#ACTIVE_BUILD}）：房子破了先补；村库里没有这种方块时由缺料供给发任务
 * （{@code supply:repair:<物品>}，{@link SupplyQuests}）。
 */
public final class RepairJob implements JobType {
    public static final String ID = "repair_cell";
    static final String ANIM = "build_hammer";
    static final int TICKS = 40;
    /** 每轮最多派几格（同一座建筑一次修几格，不把全村的人都叫去）。 */
    static final int PER_ROUND = 4;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Job> candidates(VillageContext ctx) {
        List<Job> out = new ArrayList<>();
        VillageQuests.Book book = QuestService.book(ctx.level().getServer(), ctx.village().id());
        for (VillageQuests.Damage d : book.damage.values()) {
            for (VillageQuests.Cell c : d.cells.values()) {
                if (!c.self) {
                    continue;
                }
                String item = item(c);
                if (item.isEmpty() || ctx.state().stock.count(item) <= 0) {
                    continue;
                }
                BlockPos p = BlockPos.of(c.pos);
                if (!ctx.loaded(p)) {
                    continue;
                }
                out.add(new Job(ctx.nextJobId(), ID, VillageContext.pos(p), "repair:" + c.pos, d.key, Job.walkActSettle(ANIM, TICKS),
                        Map.of(item, 1), Map.of(), Set.of(), Priority.ACTIVE_BUILD, 1, true, Job.DEFAULT_TIMEOUT));
                if (out.size() >= PER_ROUND) {
                    return out;
                }
            }
        }
        return out;
    }

    /** 放回这格要用的物品（方块对应的物品；没有物品的方块不自修）。 */
    static String item(VillageQuests.Cell c) {
        Item i = c.state.getBlock().asItem();
        return i == Items.AIR ? "" : BuiltInRegistries.ITEM.getKey(i).toString();
    }

    @Override
    public boolean settle(VillageContext ctx, ResidentEntity resident, Job job) {
        VillageQuests.Book book = QuestService.book(ctx.level().getServer(), ctx.village().id());
        VillageQuests.Damage d = book.damage.get(job.ref());
        Pos t = job.target();
        BlockPos p = new BlockPos(t.x(), t.y(), t.z());
        VillageQuests.Cell c = d == null ? null : d.cells.get(p.asLong());
        if (c == null || !ctx.loaded(p)) {
            return false;
        }
        boolean placed = BuildingDamage.restore(ctx.level(), p, c.state);
        // 放上了：修好；格子被别的方块占了（玩家放了东西）：不再管这一格（不拆玩家方块）
        d.cells.remove(p.asLong());
        VillageQuests.get(ctx.level().getServer()).setDirty();
        io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.invalidate(ctx.village().id());
        return placed;
    }
}
