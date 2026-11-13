package io.github.happysnappingturtle.mythicage.playtest;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekVillageTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.build.VillageProjects;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

/**
 * 快速验证的村庄神庙（GA-13 §2.8，取代 GA-3 的「在村边空地搭玩家神庙」）：神庙随卫城原地扩建，
 * 这里只负责把站点村子的神庙准备到要的等级，或者开工下一级让玩家看着它盖起来。
 * <ul>
 *   <li>等级不够：开工下一级（{@link #startUpgrade}），之后每刻 {@link #advance} 直接逐格落下（不等工人），直到够为止；</li>
 *   <li>{@link #startUpgrade}：「神庙扩建」站：村子推进到下一级的时代、备齐材料、开工，施工速度 ×{@link #UPGRADE_SPEED}。</li>
 * </ul>
 */
public final class PlaytestTemple {
    /** 「神庙扩建」站的施工倍速：一座中神庙几分钟内盖完。 */
    public static final double UPGRADE_SPEED = 8;
    /** {@link #advance} 一刻最多落几格（实际还受全服每刻配额限制）。 */
    private static final int MAX_CELLS_PER_TICK = 256;
    /** 开工前补进村库的口粮（第一种口粮物品的数量）。 */
    private static final int RATIONS = 64;

    private PlaytestTemple() {
    }

    public static int level(ServerLevel level, UUID village) {
        return VillageTemples.of(level.getServer(), village).map(VillageTemple::level).orElse(-1);
    }

    /** 正在扩建下一级卫城（进行中的项目就是下一级的建筑）。 */
    public static boolean upgrading(ServerLevel level, UUID village) {
        TempleRules rules = TempleService.rules();
        int next = level(level, village) + 1;
        if (next <= 0 || next > rules.maxLevel()) {
            return false;
        }
        Optional<VillageContext> ctx = VillageScheduler.contextOf(level, village);
        return ctx.flatMap(c -> c.state().activeProject())
                .map(p -> io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(p.building))
                .map(sp -> GreekVillageTemple.levelOf(rules, sp) == next).orElse(false);
    }

    /**
     * 开工下一级卫城：村子推进到那一级的时代，原来进行中的项目取消，材料全部放进村库，施工倍速设为 speed。
     * 已是最高级、或候选里没有这一项（原位放不下）时为空。
     */
    public static Optional<BuildProject> startUpgrade(ServerLevel level, UUID village, double speed) {
        TempleRules rules = TempleService.rules();
        int next = level(level, village) + 1;
        if (next <= 0 || next > rules.maxLevel()) {
            return Optional.empty();
        }
        TempleRules.Tier tier = rules.tier(next);
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        Optional<VillageRegistry.Village> v = reg.byId(village);
        if (v.isEmpty()) {
            return Optional.empty();
        }
        if (!v.get().stage().atLeast(tier.age())) {
            reg.update(v.get().withStage(tier.age()));
        }
        Optional<VillageContext> ctx = VillageScheduler.contextOf(level, village);
        if (ctx.isEmpty()) {
            return Optional.empty();
        }
        ctx.get().state().activeProject().ifPresent(p -> Construction.cancel(ctx.get(), p));
        // 口粮不够时村子不开大工程（food_short）：快速验证是开发工具，补够口粮
        ctx.get().data().rations().portions().keySet().stream().findFirst()
                .ifPresent(item -> ctx.get().state().stock.putUnchecked(item, RATIONS));
        Optional<VillageProjects.View> view = VillageProjects.candidates(ctx.get()).stream()
                .filter(x -> GreekVillageTemple.levelOf(rules, x.spec()) == next && x.candidate().available()).findFirst();
        if (view.isEmpty()) {
            MythicAge.LOGGER.info("快速验证：村子 {} 开不了 {}（候选里没有或原位放不下）：{}", village, tier.building(),
                    VillageProjects.candidates(ctx.get()).stream().map(x -> x.candidate().key() + "=" + x.candidate().reason()).toList());
            return Optional.empty();
        }
        Optional<BuildProject> p = Construction.start(ctx.get(), view.get().candidate().pick());
        if (p.isEmpty()) {
            return Optional.empty();
        }
        var costs = ctx.get().data().buildCosts();
        p.get().bill().forEach((k, n) -> ctx.get().state().stock.putUnchecked(costs.representative(k), n));
        Construction.speed(village, speed);
        Construction.tick(ctx.get());
        MythicAge.LOGGER.info("快速验证：村子 {} 开工 {}（{} 段，施工 ×{}）", village, p.get().building, p.get().segments, speed);
        return p;
    }

    /**
     * 推进一刻进行中的项目：同 {@code VillageBuildGameTests.step} 按段顺序逐格落下（不等工人），先拆后放；
     * 一刻能落多少受全服每刻配额限制。返回项目是否已落成（没有进行中的项目也算）。
     */
    public static boolean advance(ServerLevel level, UUID village) {
        Optional<VillageContext> ctx = VillageScheduler.contextOf(level, village);
        if (ctx.isEmpty()) {
            return false;
        }
        Optional<BuildProject> project = ctx.get().state().activeProject();
        if (project.isEmpty()) {
            return true;
        }
        BuildProject p = project.get();
        Construction.tick(ctx.get());
        for (int guard = 0; guard < MAX_CELLS_PER_TICK && p.active() && p.state == BuildProject.State.BUILDING; guard++) {
            Construction.Result r = null;
            for (int i = 0; i < p.segments; i++) {
                if (p.placed.get(i)) {
                    continue;
                }
                r = Construction.drop(ctx.get(), p, i);
                if (r == Construction.Result.PLACED || r == Construction.Result.BLOCKED || r == Construction.Result.SHORT
                        || r == Construction.Result.WAIT) {
                    break;
                }
            }
            if (r != Construction.Result.PLACED) {
                break;
            }
        }
        return !p.active();
    }
}
