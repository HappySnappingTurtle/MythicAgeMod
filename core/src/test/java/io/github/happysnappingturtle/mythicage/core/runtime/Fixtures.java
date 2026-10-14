package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Favor;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.FavorMode;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Offline;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig.Snapshot;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfigSchema;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.UUID;

/** T5 测试的公共构造：快照产率统一取 48/日（每 500 tick 产出 1 点）。 */
final class Fixtures {
    static final double RATE = 48;
    static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Fixtures() {
    }

    static RuntimeConfig preset(RuntimeConfig.Preset p) {
        return RuntimeConfigSchema.preset(p);
    }

    static Favor e() {
        return preset(RuntimeConfig.Preset.E).favor();
    }

    static Favor f() {
        return preset(RuntimeConfig.Preset.F).favor();
    }

    static Favor withMode(Favor f, FavorMode mode) {
        return new Favor(mode, f.idle(), f.snapshot(), f.offline(), f.catchUp());
    }

    static Favor withSnapshot(Favor f, Snapshot s) {
        return new Favor(f.mode(), f.idle(), s, f.offline(), f.catchUp());
    }

    static Favor withMaxTicksSinceVisit(Favor f, long ticks) {
        Snapshot s = f.snapshot();
        return withSnapshot(f, new Snapshot(s.rateFactor(), ticks, s.visitMinTicks(), s.verifyTicks(),
                s.unverifiedFactor()));
    }

    static Favor withRateFactor(Favor f, double factor) {
        Snapshot s = f.snapshot();
        return withSnapshot(f, new Snapshot(factor, s.maxTicksSinceVisit(), s.visitMinTicks(), s.verifyTicks(),
                s.unverifiedFactor()));
    }

    static Favor withOffline(Favor f, Offline o) {
        return new Favor(f.mode(), f.idle(), f.snapshot(), o, f.catchUp());
    }

    /** 已验证、快照 48/日、在 tick 0 建立的台账。 */
    static ProducerLedger verifiedLedger(String id) {
        ProducerLedger l = new ProducerLedger(new ProducerId(id), OWNER, Civilization.GREEK, RATE, 0);
        l.recordObservation(RATE);
        return l;
    }
}
