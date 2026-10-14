package io.github.happysnappingturtle.mythicage.core.config.conduct;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.config.ConfigSchema;
import io.github.happysnappingturtle.mythicage.core.config.ObjectReader;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductConfig.Events;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductConfig.Range;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductConfig.Regression;

/**
 * {@code conduct.json} 的 schema（T3 设计文档 §5.3）。
 */
public final class ConductSchema implements ConfigSchema<ConductConfig> {
    public static final ConductSchema INSTANCE = new ConductSchema();

    private static final int SCORE_LIMIT = 1000;
    private static final int EVENT_LIMIT = 100;

    private static final ConductConfig DEFAULTS = new ConductConfig(
            true,
            new Range(-100, 100),
            0,
            new Regression(2, 24000),
            12,
            200,
            new Events(-2, -12, -3, -15, -2, -10, -1, -4, 6, 3, 2),
            true,
            false);

    private ConductSchema() {
    }

    @Override
    public String name() {
        return "conduct";
    }

    @Override
    public int currentVersion() {
        return 1;
    }

    @Override
    public ConductConfig defaults() {
        return DEFAULTS;
    }

    @Override
    public ConductConfig read(ObjectReader root, ConductConfig base) {
        ObjectReader rangeReader = root.object("range");
        Range range = new Range(
                rangeReader.intIn("min", base.range().min(), -SCORE_LIMIT, SCORE_LIMIT),
                rangeReader.intIn("max", base.range().max(), -SCORE_LIMIT, SCORE_LIMIT));
        if (!(range.min() < 0 && 0 < range.max())) {
            root.warn("range", "必须满足 min < 0 < max，已整体使用默认值");
            range = base.range();
        }
        int initial = root.intIn("initial", base.initial(), -SCORE_LIMIT, SCORE_LIMIT);
        if (initial < range.min() || initial > range.max()) {
            root.warn("initial", "超出 range [" + range.min() + ", " + range.max() + "]，已使用默认值");
            initial = clampDefaultInto(base.initial(), range);
        }

        ObjectReader reg = root.object("regression");
        Regression regression = new Regression(
                reg.intIn("points", base.regression().points(), 0, EVENT_LIMIT),
                reg.longIn("intervalOnlineTicks", base.regression().intervalOnlineTicks(), 1, Integer.MAX_VALUE));

        ObjectReader ev = root.object("events");
        Events b = base.events();
        int harmVillager = negative(ev, "harmVillager", b.harmVillager());
        int killVillager = negative(ev, "killVillagerTotal", b.killVillagerTotal());
        int harmGuardian = negative(ev, "harmGuardian", b.harmGuardian());
        int killGuardian = negative(ev, "killGuardianTotal", b.killGuardianTotal());
        int harmDefender = negative(ev, "harmDefender", b.harmDefender());
        int killDefender = negative(ev, "killDefenderTotal", b.killDefenderTotal());
        int harmAnimal = negative(ev, "harmAnimalInVillage", b.harmAnimalInVillage());
        int killAnimal = negative(ev, "killAnimalInVillageTotal", b.killAnimalInVillageTotal());

        // 致死档不能罚得比受伤档轻；成对回退，避免只改一半造成新的不一致
        int[] villager = pair(ev, "Villager", harmVillager, killVillager, b.harmVillager(), b.killVillagerTotal());
        int[] guardian = pair(ev, "Guardian", harmGuardian, killGuardian, b.harmGuardian(), b.killGuardianTotal());
        int[] defender = pair(ev, "Defender", harmDefender, killDefender, b.harmDefender(), b.killDefenderTotal());
        int[] animal = pair(ev, "AnimalInVillage", harmAnimal, killAnimal, b.harmAnimalInVillage(),
                b.killAnimalInVillageTotal());

        Events events = new Events(villager[0], villager[1], guardian[0], guardian[1], defender[0], defender[1],
                animal[0], animal[1],
                ev.intIn("cureZombieVillager", b.cureZombieVillager(), 0, EVENT_LIMIT),
                ev.intIn("rescueResident", b.rescueResident(), 0, EVENT_LIMIT),
                ev.intIn("rescueEscortQuest", b.rescueEscortQuest(), 0, EVENT_LIMIT));

        return new ConductConfig(
                root.bool("enabled", base.enabled()),
                range,
                initial,
                regression,
                root.intIn("positiveDailyCap", base.positiveDailyCap(), 0, SCORE_LIMIT),
                root.intIn("incidentWindowTicks", base.incidentWindowTicks(), 1, 72000),
                events,
                root.bool("selfDefenseExempt", base.selfDefenseExempt()),
                root.bool("pvpCountsAsConduct", base.pvpCountsAsConduct()));
    }

    private static int negative(ObjectReader ev, String key, int fallback) {
        return ev.intIn(key, fallback, -EVENT_LIMIT, 0);
    }

    private static int[] pair(ObjectReader ev, String suffix, int harm, int killTotal, int harmDefault, int killDefault) {
        if (killTotal > harm) {
            ev.warn("kill" + suffix + "Total", "致死总罚分不能比 harm" + suffix + " 轻，两者都已使用默认值");
            return new int[]{harmDefault, killDefault};
        }
        return new int[]{harm, killTotal};
    }

    private static int clampDefaultInto(int value, Range range) {
        return Math.max(range.min(), Math.min(range.max(), value));
    }

    @Override
    public JsonObject toJson(ConductConfig c) {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", c.enabled());
        JsonObject range = new JsonObject();
        range.addProperty("min", c.range().min());
        range.addProperty("max", c.range().max());
        root.add("range", range);
        root.addProperty("initial", c.initial());
        JsonObject reg = new JsonObject();
        reg.addProperty("points", c.regression().points());
        reg.addProperty("intervalOnlineTicks", c.regression().intervalOnlineTicks());
        root.add("regression", reg);
        root.addProperty("positiveDailyCap", c.positiveDailyCap());
        root.addProperty("incidentWindowTicks", c.incidentWindowTicks());
        Events e = c.events();
        JsonObject ev = new JsonObject();
        ev.addProperty("harmVillager", e.harmVillager());
        ev.addProperty("killVillagerTotal", e.killVillagerTotal());
        ev.addProperty("harmGuardian", e.harmGuardian());
        ev.addProperty("killGuardianTotal", e.killGuardianTotal());
        ev.addProperty("harmDefender", e.harmDefender());
        ev.addProperty("killDefenderTotal", e.killDefenderTotal());
        ev.addProperty("harmAnimalInVillage", e.harmAnimalInVillage());
        ev.addProperty("killAnimalInVillageTotal", e.killAnimalInVillageTotal());
        ev.addProperty("cureZombieVillager", e.cureZombieVillager());
        ev.addProperty("rescueResident", e.rescueResident());
        ev.addProperty("rescueEscortQuest", e.rescueEscortQuest());
        root.add("events", ev);
        root.addProperty("selfDefenseExempt", c.selfDefenseExempt());
        root.addProperty("pvpCountsAsConduct", c.pvpCountsAsConduct());
        return root;
    }
}
