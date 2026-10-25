package io.github.happysnappingturtle.mythicage.village.life;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.life.LifeData;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianGesture;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 祭司的日常差事（T19 设计文档 §1.10 J4，V3-希腊-居民 §2.1、§5.2、§5.6、§5.7）：数据 {@code life.priest} 里每条差事
 * 到点去一次——宙斯村清早听橡、上午到各祭坛奠酒（祭坛升一缕烟）、神谕日到神谕所坐诊。每天每条一次（记在村庄当日计数里）。
 * 祭司正在做别的差事（节庆游行）时等下一次调度。纯表现。
 */
final class PriestDuties {
    /** 一条差事的时段长度（没有 {@code until} 时）：到点后这么久内还没去就算了。 */
    static final int WINDOW = 3000;

    private PriestDuties() {
    }

    static void tick(VillageContext ctx) {
        List<LifeData.PriestDuty> duties = ctx.life().priest();
        if (duties.isEmpty()) {
            return;
        }
        int tod = ctx.timeOfDay();
        for (LifeData.PriestDuty d : duties) {
            int end = d.until() > 0 ? d.until() : d.at() + WINDOW;
            if (tod < d.at() || tod >= end || !VillageLife.applies(ctx, d.patron(), d.special(), d.minAge())) {
                continue;
            }
            String key = "priest:" + d.id();
            if (ctx.state().dailyCount(ctx.today(), key) > 0) {
                continue;
            }
            List<Pos> anchors = ctx.anchors(d.anchor());
            Optional<GuardianEntity> g = VillageLife.priest(ctx);
            if (anchors.isEmpty() || g.isEmpty() || g.get().errand() != null) {
                continue;
            }
            Optional<GuardianGesture> gesture = GuardianGesture.byId(d.gesture());
            if (gesture.isEmpty()) {
                continue;
            }
            List<BlockPos> stops = stops(ctx, anchors, d.all(), g.get().blockPosition());
            long until = d.until() > 0 ? ctx.level().getGameTime() + (d.until() - tod) : 0;
            g.get().startErrand(new GuardianEntity.Errand(d.id(), stops, gesture.get(), d.ticks(), until,
                    d.smoke() ? (pr, at) -> VillageLife.smoke(ctx.level(), at) : null));
            ctx.state().addDaily(ctx.today(), key, 1);
            return;
        }
    }

    /** 目的地：{@code all} 时从祭司所在处起每次去最近的下一处；否则只去最近的一处。都换成旁边能站的地方。 */
    static List<BlockPos> stops(VillageContext ctx, List<Pos> anchors, boolean all, BlockPos from) {
        List<BlockPos> left = new ArrayList<>();
        anchors.forEach(p -> left.add(VillageContext.block(p)));
        List<BlockPos> out = new ArrayList<>();
        BlockPos at = from;
        while (!left.isEmpty()) {
            BlockPos here = at;
            BlockPos next = left.stream().min(java.util.Comparator.comparingDouble(p -> p.distSqr(here))).orElseThrow();
            left.remove(next);
            out.add(next);
            at = next;
            if (!all) {
                break;
            }
        }
        return out;
    }
}
