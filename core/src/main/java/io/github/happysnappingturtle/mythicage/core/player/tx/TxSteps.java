package io.github.happysnappingturtle.mythicage.core.player.tx;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.FavorRules;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;

import java.util.Optional;

/** 内置事务步骤。 */
public final class TxSteps {
    private TxSteps() {
    }

    /** 扣除恩宠；余额不足时拒绝。 */
    public static TxStep spendFavor(Civilization civ, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("扣除的恩宠必须为正数：" + amount);
        }
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                long have = data.civ(civ).favor();
                return have >= amount ? Optional.empty()
                        : Optional.of(civ.id() + " 恩宠不足：需要 " + amount + "，现有 " + have);
            }

            @Override
            public void apply(PlayerMythData data) {
                data.civ(civ).setFavor(FavorRules.spend(data.civ(civ).favor(), amount).orElseThrow());
            }
        };
    }

    /** 增加恩宠，受上限约束（已达上限时不变，不算失败）。 */
    public static TxStep addFavor(Civilization civ, long amount, long capBonus) {
        if (amount <= 0) {
            throw new IllegalArgumentException("增加的恩宠必须为正数：" + amount);
        }
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                return Optional.empty();
            }

            @Override
            public void apply(PlayerMythData data) {
                PlayerMythData.CivState s = data.civ(civ);
                s.setFavor(FavorRules.add(s.favor(), amount, FavorRules.cap(s.age(), capBonus)));
            }
        };
    }

    /** 要求某文明的时代不低于 minAge。 */
    public static TxStep requireAge(Civilization civ, Age minAge) {
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                Age have = data.civ(civ).age();
                return have.atLeast(minAge) ? Optional.empty()
                        : Optional.of(civ.id() + " 时代不足：需要 " + minAge.id() + "，现为 " + have.id());
            }

            @Override
            public void apply(PlayerMythData data) {
            }
        };
    }

    /** 要求某文明的时代恰好是 age（初见只在 NONE 时成立，T9）。 */
    public static TxStep requireAgeExactly(Civilization civ, Age age) {
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                Age have = data.civ(civ).age();
                return have == age ? Optional.empty()
                        : Optional.of(civ.id() + " 时代应为 " + age.id() + "，现为 " + have.id());
            }

            @Override
            public void apply(PlayerMythData data) {
            }
        };
    }

    /** 每日限购记一次（T9）；limit 为 0 表示不限，只记账。今日已达上限时拒绝。 */
    public static TxStep recordDaily(String key, long day, int limit) {
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                int have = data.tradeDaily().count(key, day);
                return limit <= 0 || have < limit ? Optional.empty()
                        : Optional.of(key + " 今日已达限购 " + limit);
            }

            @Override
            public void apply(PlayerMythData data) {
                data.tradeDaily().record(key, day);
            }
        };
    }

    /** 结识村庄主神（T10a）：记入该文明的已结识主神集合；已结识时不变。 */
    public static TxStep knowPatron(Civilization civ, String patron) {
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                return Optional.empty();
            }

            @Override
            public void apply(PlayerMythData data) {
                data.civ(civ).knownPatrons().add(patron);
            }
        };
    }

    /** 设定某文明的时代（晋升、首次交谈进入远古）。 */
    public static TxStep setAge(Civilization civ, Age age) {
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                return Optional.empty();
            }

            @Override
            public void apply(PlayerMythData data) {
                data.civ(civ).setAge(age);
            }
        };
    }

    public static TxStep recordFirstKill(String bossId) {
        return addTo(bossId, Kind.FIRST_KILL);
    }

    public static TxStep unlockBlueprint(String artifactId) {
        return addTo(artifactId, Kind.BLUEPRINT);
    }

    public static TxStep discover(String id) {
        return addTo(id, Kind.DISCOVERY);
    }

    private enum Kind { FIRST_KILL, BLUEPRINT, DISCOVERY }

    private static TxStep addTo(String id, Kind kind) {
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                return Optional.empty();
            }

            @Override
            public void apply(PlayerMythData data) {
                switch (kind) {
                    case FIRST_KILL -> data.firstKills().add(id);
                    case BLUEPRINT -> data.blueprints().add(id);
                    case DISCOVERY -> data.discoveries().add(id);
                }
            }
        };
    }
}
