package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;
import java.util.UUID;

/**
 * 居民说的一句话（V3-希腊-居民 §6.3、T19 §1.8 I6），按优先级：警戒中 &gt; （节庆，阶段 ⑤）&gt; 本职缺料 &gt; 村里在建项目 &gt;
 * 正在做的事（手上有工作单时）/ 职业闲话（闲着时）。
 * 文本都在语言文件里：{@code mythic_age.village.say.<文明>.<职业>.chat.1–6} / {@code .short}，通用的
 * {@code mythic_age.village.say.alarm}、{@code .project}、{@code .project_building}。
 */
public final class ResidentDialogue {
    public static final int CHATTER = 6;

    public enum Kind { ALARM, SHORT, PROJECT, CHATTER }

    private ResidentDialogue() {
    }

    static String profKey(ResidentEntity r) {
        return "mythic_age.village.say." + r.profession().replace(':', '.');
    }

    /** 选哪一类（GameTest 看）。 */
    public static Kind kind(MinecraftServer server, ResidentEntity r) {
        Optional<UUID> v = r.village();
        if (v.isEmpty()) {
            return Kind.CHATTER;
        }
        if (VillageScheduler.runtime(v.get()).alarmed(WorldClock.now(server))) {
            return Kind.ALARM;
        }
        VillageEconomy.State s = VillageEconomy.get(server).state(v.get());
        if (isShort(r, s)) {
            return Kind.SHORT;
        }
        if (s.activeProject().isPresent() && VillageBuildings.all().containsKey(s.activeProject().get().building)) {
            return Kind.PROJECT;
        }
        return Kind.CHATTER;
    }

    static boolean isShort(ResidentEntity r, VillageEconomy.State s) {
        Optional<CivVillageData> data = VillageData.civ(r.civ());
        CivVillageData.Profession p = data.map(d -> d.professions().get(r.profession())).orElse(null);
        if (p == null) {
            return false;
        }
        CivVillageData.ShortWhen w = p.shortWhen();
        if (!w.point().isEmpty() && s.points.values().stream().noneMatch(x -> x.kind().equals(w.point()) && x.remaining() > 0)
                && s.points.values().stream().anyMatch(x -> x.kind().equals(w.point()))) {
            return true;
        }
        if (!w.items().isEmpty() && w.items().stream().allMatch(i -> s.stock.count(i) == 0)) {
            return true;
        }
        return w.food() && data.get().rations().belowFloor(s.stock);
    }

    public static Component line(MinecraftServer server, ResidentEntity r) {
        return switch (kind(server, r)) {
            case ALARM -> Component.translatable("mythic_age.village.say.alarm");
            case SHORT -> Component.translatable(profKey(r) + ".short");
            case PROJECT -> {
                VillageEconomy.State s = VillageEconomy.get(server).state(r.village().orElseThrow());
                BuildProject p = s.activeProject().orElseThrow();
                BuildingSpec spec = VillageBuildings.all().get(p.building);
                int missing = p.missing().values().stream().mapToInt(Integer::intValue).sum();
                yield missing > 0 ? Component.translatable("mythic_age.village.say.project", Construction.name(spec, p.god), missing)
                        : Component.translatable("mythic_age.village.say.project_building", Construction.name(spec, p.god));
            }
            // 手上有活就说正在做的事；闲着才说职业闲话
            case CHATTER -> r.resting() ? Component.translatable("mythic_age.village.activity.sleep")
                    : r.job().isPresent() ? Component.translatable("mythic_age.village.activity." + r.job().get().type())
                    : Component.translatable(profKey(r) + ".chat." + (1 + r.getRandom().nextInt(CHATTER)));
        };
    }
}
