package io.github.happysnappingturtle.mythicage.core.config.runtime;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.config.ConfigIssue;
import io.github.happysnappingturtle.mythicage.core.config.ConfigSchema;
import io.github.happysnappingturtle.mythicage.core.config.ObjectReader;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.CatchUp;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Construction;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Favor;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.FavorMode;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.ForceLoad;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Idle;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Norse;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Offline;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.OfflineClock;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Population;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Preset;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.PublicMode;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.PublicResources;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Rested;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Snapshot;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Village;

import java.util.List;
import java.util.Map;

/**
 * {@code runtime.json} 的 schema 与 A/B/C/E/F 预设（T3 设计文档 §5.1）。
 */
public final class RuntimeConfigSchema implements ConfigSchema<RuntimeConfig> {
    public static final RuntimeConfigSchema INSTANCE = new RuntimeConfigSchema();

    /** tick 类字段的上限：保持在 int 范围内，避免下游换算溢出。 */
    private static final long MAX_TICKS = Integer.MAX_VALUE;
    /** 天数类字段的上限：超过一年没有实际意义。 */
    private static final double MAX_DAYS = 365;

    private static final Map<String, Preset> PRESETS = Map.of(
            "A", Preset.A, "B", Preset.B, "C", Preset.C, "E", Preset.E, "F", Preset.F);
    private static final Map<String, FavorMode> FAVOR_MODES = Map.of(
            "online_snapshot", FavorMode.ONLINE_SNAPSHOT,
            "capped_catchup", FavorMode.CAPPED_CATCHUP,
            "loaded_only", FavorMode.LOADED_ONLY);
    private static final Map<String, OfflineClock> CLOCKS = Map.of(
            "server_ticks", OfflineClock.SERVER_TICKS, "wall_clock", OfflineClock.WALL_CLOCK);
    private static final Map<String, PublicMode> PUBLIC_MODES = Map.of(
            "logical_when_not_real", PublicMode.LOGICAL_WHEN_NOT_REAL, "loaded_only", PublicMode.LOADED_ONLY);

    private static final RuntimeConfig PRESET_E = new RuntimeConfig(
            Preset.E,
            new Favor(FavorMode.ONLINE_SNAPSHOT,
                    new Idle(true, 12000),
                    new Snapshot(1.0, 120000, 600, 24000, 0.5),
                    new Offline(0.0, 1.0, OfflineClock.SERVER_TICKS),
                    new CatchUp(24000)),
            new Norse(new Rested(false, 2.0, 1.0, true)),
            new Village(new ForceLoad(false),
                    new PublicResources(PublicMode.LOGICAL_WHEN_NOT_REAL, 128, false, 72000, 1.0),
                    new Construction(false, 24000, 2.0),
                    new Population(false)));

    private RuntimeConfigSchema() {
    }

    /** 预设对应的完整配置（未经任何覆盖）。 */
    public static RuntimeConfig preset(Preset preset) {
        RuntimeConfig e = PRESET_E;
        Favor f = e.favor();
        Village v = e.village();
        return switch (preset) {
            case E -> e;
            case F -> new RuntimeConfig(Preset.F,
                    new Favor(f.mode(), f.idle(), f.snapshot(),
                            new Offline(0.5, f.offline().maxBufferDays(), f.offline().clock()), f.catchUp()),
                    new Norse(new Rested(true, 2.0, 1.0, true)), v);
            case C -> new RuntimeConfig(Preset.C,
                    new Favor(FavorMode.CAPPED_CATCHUP, f.idle(), f.snapshot(), f.offline(), f.catchUp()),
                    e.norse(),
                    new Village(v.forceLoad(),
                            withMaxCatchUp(v.publicResources(), 24000),
                            new Construction(false, 0, v.construction().bankSpendMultiplier()),
                            v.population()));
            case A -> loadedOnly(Preset.A, false);
            case B -> loadedOnly(Preset.B, true);
        };
    }

    private static RuntimeConfig loadedOnly(Preset preset, boolean forceLoad) {
        RuntimeConfig e = PRESET_E;
        Favor f = e.favor();
        Village v = e.village();
        PublicResources p = v.publicResources();
        return new RuntimeConfig(preset,
                new Favor(FavorMode.LOADED_ONLY, f.idle(), f.snapshot(), f.offline(), f.catchUp()),
                e.norse(),
                new Village(new ForceLoad(forceLoad),
                        new PublicResources(PublicMode.LOADED_ONLY, p.realRunRadius(), p.requireOwnerOnline(),
                                p.maxCatchUpTicks(), p.rateFactor()),
                        v.construction(), v.population()));
    }

    private static PublicResources withMaxCatchUp(PublicResources p, long maxCatchUpTicks) {
        return new PublicResources(p.mode(), p.realRunRadius(), p.requireOwnerOnline(), maxCatchUpTicks, p.rateFactor());
    }

    @Override
    public String name() {
        return "runtime";
    }

    @Override
    public int currentVersion() {
        return 1;
    }

    @Override
    public RuntimeConfig defaults() {
        return PRESET_E;
    }

    @Override
    public RuntimeConfig base(ObjectReader root, List<ConfigIssue> issues) {
        return preset(root.choice("preset", Preset.E, PRESETS));
    }

    @Override
    public RuntimeConfig read(ObjectReader root, RuntimeConfig base) {
        ObjectReader favor = root.object("favor");
        Favor bf = base.favor();
        ObjectReader idle = favor.object("idle");
        ObjectReader snap = favor.object("snapshot");
        ObjectReader off = favor.object("offline");
        ObjectReader catchUp = favor.object("catchUp");
        Favor f = new Favor(
                favor.choice("mode", bf.mode(), FAVOR_MODES),
                new Idle(idle.bool("enabled", bf.idle().enabled()),
                        idle.intIn("graceTicks", bf.idle().graceTicks(), 1200, 72000)),
                new Snapshot(snap.doubleIn("rateFactor", bf.snapshot().rateFactor(), 0, 1),
                        snap.longIn("maxTicksSinceVisit", bf.snapshot().maxTicksSinceVisit(), 0, MAX_TICKS),
                        snap.longIn("visitMinTicks", bf.snapshot().visitMinTicks(), 0, MAX_TICKS),
                        snap.longIn("verifyTicks", bf.snapshot().verifyTicks(), 0, MAX_TICKS),
                        snap.doubleIn("unverifiedFactor", bf.snapshot().unverifiedFactor(), 0, 1)),
                new Offline(off.doubleIn("rate", bf.offline().rate(), 0, 1),
                        off.doubleIn("maxBufferDays", bf.offline().maxBufferDays(), 0, MAX_DAYS),
                        off.choice("clock", bf.offline().clock(), CLOCKS)),
                new CatchUp(catchUp.longIn("maxSimulationTicks", bf.catchUp().maxSimulationTicks(), 0, MAX_TICKS)));

        ObjectReader rested = root.object("norse").object("rested");
        Rested br = base.norse().rested();
        Norse n = new Norse(new Rested(
                rested.bool("enabled", br.enabled()),
                rested.doubleIn("multiplier", br.multiplier(), 1, 10),
                rested.doubleIn("maxBufferDays", br.maxBufferDays(), 0, MAX_DAYS),
                rested.bool("bypassDailyCap", br.bypassDailyCap())));

        ObjectReader village = root.object("village");
        Village bv = base.village();
        ObjectReader pub = village.object("public");
        ObjectReader cons = village.object("construction");
        PublicResources bp = bv.publicResources();
        Village v = new Village(
                new ForceLoad(village.object("forceLoad").bool("enabled", bv.forceLoad().enabled())),
                new PublicResources(pub.choice("mode", bp.mode(), PUBLIC_MODES),
                        pub.intIn("realRunRadius", bp.realRunRadius(), 16, 512),
                        pub.bool("requireOwnerOnline", bp.requireOwnerOnline()),
                        pub.longIn("maxCatchUpTicks", bp.maxCatchUpTicks(), 0, MAX_TICKS),
                        pub.doubleIn("rateFactor", bp.rateFactor(), 0, 1)),
                new Construction(cons.lockedBool("simulateWhenUnloaded", false),
                        cons.longIn("bankMaxTicks", bv.construction().bankMaxTicks(), 0, MAX_TICKS),
                        cons.doubleIn("bankSpendMultiplier", bv.construction().bankSpendMultiplier(), 1, 10)),
                new Population(village.object("population").lockedBool("simulateWhenUnloaded", false)));

        return new RuntimeConfig(base.preset(), f, n, v);
    }

    @Override
    public JsonObject toJson(RuntimeConfig c) {
        JsonObject root = new JsonObject();
        root.addProperty("preset", c.preset().name());

        Favor f = c.favor();
        JsonObject favor = new JsonObject();
        favor.addProperty("mode", nameOf(FAVOR_MODES, f.mode()));
        JsonObject idle = new JsonObject();
        idle.addProperty("enabled", f.idle().enabled());
        idle.addProperty("graceTicks", f.idle().graceTicks());
        favor.add("idle", idle);
        JsonObject snap = new JsonObject();
        snap.addProperty("rateFactor", f.snapshot().rateFactor());
        snap.addProperty("maxTicksSinceVisit", f.snapshot().maxTicksSinceVisit());
        snap.addProperty("visitMinTicks", f.snapshot().visitMinTicks());
        snap.addProperty("verifyTicks", f.snapshot().verifyTicks());
        snap.addProperty("unverifiedFactor", f.snapshot().unverifiedFactor());
        favor.add("snapshot", snap);
        JsonObject off = new JsonObject();
        off.addProperty("rate", f.offline().rate());
        off.addProperty("maxBufferDays", f.offline().maxBufferDays());
        off.addProperty("clock", nameOf(CLOCKS, f.offline().clock()));
        favor.add("offline", off);
        JsonObject catchUp = new JsonObject();
        catchUp.addProperty("maxSimulationTicks", f.catchUp().maxSimulationTicks());
        favor.add("catchUp", catchUp);
        root.add("favor", favor);

        Rested r = c.norse().rested();
        JsonObject rested = new JsonObject();
        rested.addProperty("enabled", r.enabled());
        rested.addProperty("multiplier", r.multiplier());
        rested.addProperty("maxBufferDays", r.maxBufferDays());
        rested.addProperty("bypassDailyCap", r.bypassDailyCap());
        JsonObject norse = new JsonObject();
        norse.add("rested", rested);
        root.add("norse", norse);

        Village v = c.village();
        JsonObject village = new JsonObject();
        JsonObject forceLoad = new JsonObject();
        forceLoad.addProperty("enabled", v.forceLoad().enabled());
        village.add("forceLoad", forceLoad);
        PublicResources p = v.publicResources();
        JsonObject pub = new JsonObject();
        pub.addProperty("mode", nameOf(PUBLIC_MODES, p.mode()));
        pub.addProperty("realRunRadius", p.realRunRadius());
        pub.addProperty("requireOwnerOnline", p.requireOwnerOnline());
        pub.addProperty("maxCatchUpTicks", p.maxCatchUpTicks());
        pub.addProperty("rateFactor", p.rateFactor());
        village.add("public", pub);
        JsonObject cons = new JsonObject();
        cons.addProperty("simulateWhenUnloaded", v.construction().simulateWhenUnloaded());
        cons.addProperty("bankMaxTicks", v.construction().bankMaxTicks());
        cons.addProperty("bankSpendMultiplier", v.construction().bankSpendMultiplier());
        village.add("construction", cons);
        JsonObject pop = new JsonObject();
        pop.addProperty("simulateWhenUnloaded", v.population().simulateWhenUnloaded());
        village.add("population", pop);
        root.add("village", village);
        return root;
    }

    private static <E> String nameOf(Map<String, E> names, E value) {
        return names.entrySet().stream().filter(e -> e.getValue() == value).findFirst().orElseThrow().getKey();
    }
}
