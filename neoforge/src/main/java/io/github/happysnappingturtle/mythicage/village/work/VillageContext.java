package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.time.VillageCalendar;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.behavior.VillageBehavior;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * 一次调度时的村庄上下文：世界、村庄登记、规划、运行数据、文明数据、此刻的作息段与历法日期。
 *
 * @param timeOfDay 0–23999（0 = 日出）
 * @param segment   作息段（{@code work}、{@code sleep}…）
 * @param today     MC 日序号（{@code MythDays}）
 */
public record VillageContext(ServerLevel level, VillageRegistry.Village village, VillagePlans.Plan plan,
                             VillageEconomy.State state, CivVillageData data, VillageScheduler.Runtime rt,
                             VillageBehavior behavior, long now, int timeOfDay, String segment, long today,
                             VillageCalendar.Date date, List<ResidentEntity> residents) {
    public List<Pos> anchors(String name) {
        return plan == null ? List.of() : plan.anchors(name);
    }

    public static BlockPos block(Pos p) {
        return new BlockPos(p.x(), p.y(), p.z());
    }

    public static Pos pos(BlockPos b) {
        return new Pos(b.getX(), b.getY(), b.getZ());
    }

    public boolean loaded(BlockPos p) {
        return level.isLoaded(p);
    }

    public BlockPos center() {
        return village.bell().pos();
    }

    public long nextJobId() {
        return rt.nextJobId();
    }

    /** 历法偏移（含开发命令的调整）。 */
    public int offset() {
        return VillageData.offset(data, state);
    }

    public io.github.happysnappingturtle.mythicage.core.village.life.LifeData life() {
        return data.life();
    }

    public boolean raining() {
        return level.isRainingAt(center().above(2)) || level.isRaining();
    }
}
