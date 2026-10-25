package io.github.happysnappingturtle.mythicage.core.village.soldier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.config.ConfigIssue;
import io.github.happysnappingturtle.mythicage.core.config.ObjectReader;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesSchema;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.CommonVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T20 阶段 ①：士兵框架的纯逻辑（单元测试用例文档 TC-T20-01～39m）。 */
class SoldierTest {
    private static final double RATIO = 0.25;

    static SoldierProfile profile(String json) {
        return SoldierProfile.parse(JsonParser.parseString(json).getAsJsonObject(), "test", new ArrayList<>());
    }

    /** 希腊卫兵的数值（01 号 SO-GR-SP-01）。 */
    static final String HOPLITE = """
            {"kind": "melee", "health": 28, "armor": 6, "damage": 5,
             "timeline": {"windup": 12, "lock": 4, "active": 3, "recover": 9, "interval": 32},
             "melee": {"reach": 2.8, "radius": 0.25, "height": 1.15, "halfHeight": 0.5},
             "shield": {"arc": 120, "reduction": 0.6, "maxHold": 40, "gap": 15, "raise": 6, "lower": 8},
             "kits": {"full": {"item": "x:kit"}, "simple": {"item": "x:kit_simple", "armor": -2, "damage": -1},
                      "broken": "x:kit_broken"},
             "durability": 60, "attribution": {"favor": 0, "conduct": 0, "firstKill": 0}}""";

    // ───────────── 2.1 上限与配额 ─────────────

    @Test
    void tc01CapByPopulation() {
        int[] pop = {3, 4, 8, 16, 24, 40};
        int[] want = {0, 1, 2, 4, 6, 6};
        for (int i = 0; i < pop.length; i++) {
            assertEquals(want[i], SoldierRoster.cap(pop[i], 0, 6, RATIO), "人口 " + pop[i]);
        }
    }

    @Test
    void tc02StageBonus() {
        assertEquals(7, SoldierRoster.cap(24, 1, 6, RATIO));
        assertEquals(8, SoldierRoster.cap(24, 2, 6, RATIO));
        assertEquals(9, SoldierRoster.cap(24, 3, 6, RATIO));
    }

    @Test
    void tc03HardCapZeroAndTwelve() {
        assertEquals(0, SoldierRoster.cap(40, 3, 0, RATIO), "硬上限 0 = 关闭，阶段加成也不加");
        assertEquals(12, SoldierRoster.cap(60, 0, 12, RATIO));
    }

    @Test
    void tc04ConfigOutOfRange() {
        List<ConfigIssue> issues = new ArrayList<>();
        VillagesConfig c = VillagesSchema.INSTANCE.read(ObjectReader.root(JsonParser.parseString(
                "{\"soldiers\": {\"hardCap\": 13, \"ratio\": 0.6}}").getAsJsonObject(), issues), VillagesSchema.INSTANCE.defaults());
        assertEquals(6, c.soldiers().hardCap(), "越界用默认值");
        assertEquals(0.25, c.soldiers().ratio());
        assertEquals(2, issues.size(), issues::toString);
        VillagesConfig d = VillagesSchema.INSTANCE.read(ObjectReader.root(JsonParser.parseString(
                "{\"soldiers\": {\"ratio\": -1}}").getAsJsonObject(), new ArrayList<>()), VillagesSchema.INSTANCE.defaults());
        assertEquals(0.25, d.soldiers().ratio());
        assertTrue(VillagesSchema.INSTANCE.defaults().soldiers().enabled());
    }

    @Test
    void tc05RangedQuota() {
        int[] cap = {0, 1, 2, 3, 6, 9};
        int[] want = {0, 0, 1, 1, 3, 4};
        for (int i = 0; i < cap.length; i++) {
            assertEquals(want[i], SoldierRoster.rangedQuota(cap[i]), "上限 " + cap[i]);
        }
    }

    static final SoldierRoster.Branch MELEE = new SoldierRoster.Branch("g:hoplite", false, true);

    @Test
    void tc06RangedClosedMeleeFillsAll() {
        List<SoldierRoster.Branch> order = List.of(MELEE, new SoldierRoster.Branch("g:jav", true, false));
        Map<String, Integer> have = new LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            Optional<String> n = SoldierRoster.next(order, have, 3);
            assertEquals(Optional.of("g:hoplite"), n);
            have.merge(n.get(), 1, Integer::sum);
        }
        assertEquals(Optional.empty(), SoldierRoster.next(order, have, 3), "满了");
    }

    @Test
    void tc07NextIsRangedWhenMeleeFull() {
        List<SoldierRoster.Branch> order = List.of(MELEE, new SoldierRoster.Branch("g:jav", true, true));
        assertEquals(Optional.of("g:jav"), SoldierRoster.next(order, Map.of("g:hoplite", 2), 3));
        assertEquals(Optional.of("g:hoplite"), SoldierRoster.next(order, Map.of("g:hoplite", 1), 3));
        assertEquals(Optional.of("g:hoplite"), SoldierRoster.next(order, Map.of(), 1), "上限 1 一定是近战");
    }

    // ───────────── 2.2 转岗与降回 ─────────────

    static final String LAB = "g:laborer";

    static SoldierRoster.Candidate c(String id, String prof, boolean keeper, double d) {
        return new SoldierRoster.Candidate(uuid(id), prof, keeper, false, d);
    }

    static UUID uuid(String s) {
        return UUID.nameUUIDFromBytes(s.getBytes());
    }

    @Test
    void tc08KeeperNeverPicked() {
        List<SoldierRoster.Candidate> r = List.of(c("k", LAB, true, 1), c("a", LAB, false, 9), c("b", LAB, false, 5),
                c("c", LAB, false, 7));
        assertEquals(Optional.of(uuid("b")), SoldierRoster.pickConvert(r, LAB, 2));
    }

    @Test
    void tc09KeepMinLaborers() {
        List<SoldierRoster.Candidate> r = List.of(c("k", LAB, true, 1), c("a", LAB, false, 9));
        assertEquals(Optional.empty(), SoldierRoster.pickConvert(r, LAB, 2));
    }

    @Test
    void tc10SpecialistsNeverConverted() {
        List<SoldierRoster.Candidate> r = List.of(c("p", "g:potter", false, 1), c("f", "g:farmer", false, 2),
                c("f2", "g:farmer", false, 3));
        assertEquals(Optional.empty(), SoldierRoster.pickConvert(r, LAB, 0));
    }

    @Test
    void tc11TieBrokenByUuid() {
        SoldierRoster.Candidate a = c("a", LAB, false, 5);
        SoldierRoster.Candidate b = c("b", LAB, false, 5);
        UUID want = a.id().compareTo(b.id()) < 0 ? a.id() : b.id();
        assertEquals(Optional.of(want), SoldierRoster.pickConvert(List.of(c("k", LAB, true, 0), b, a), LAB, 1));
    }

    @Test
    void tc12KitChoice() {
        assertEquals(SoldierProfile.Tier.NONE, SoldierRoster.choose(0, 0), "没有兵装不转岗");
        assertEquals(SoldierProfile.Tier.SIMPLE, SoldierRoster.choose(0, 1));
        assertEquals(SoldierProfile.Tier.FULL, SoldierRoster.choose(1, 1));
    }

    @Test
    void tc13MigrantsNotPicked() {
        List<SoldierRoster.Candidate> r = List.of(c("k", LAB, true, 0), c("a", LAB, false, 9), c("b", LAB, false, 8),
                new SoldierRoster.Candidate(uuid("m"), LAB, false, true, 1));
        assertEquals(Optional.of(uuid("b")), SoldierRoster.pickConvert(r, LAB, 1));
    }

    @Test
    void tc13bMaleOnly() {
        List<SoldierRoster.Candidate> r = List.of(c("k", LAB, true, 0), c("a", LAB, false, 9),
                new SoldierRoster.Candidate(uuid("f"), LAB, false, false, 1, true));
        assertEquals(Optional.of(uuid("a")), SoldierRoster.pickConvert(r, LAB, 1), "女性居民不转岗");
        List<SoldierRoster.Candidate> onlyWomen = List.of(c("k", LAB, true, 0),
                new SoldierRoster.Candidate(uuid("f"), LAB, false, false, 1, true));
        assertEquals(Optional.empty(), SoldierRoster.pickConvert(onlyWomen, LAB, 1), "只剩女性劳力：没有人选");
    }

    @Test
    void tc14DemoteNewest() {
        List<SoldierRoster.Enlisted> s = List.of(new SoldierRoster.Enlisted(uuid("a"), "g:h", 1),
                new SoldierRoster.Enlisted(uuid("b"), "g:h", 5), new SoldierRoster.Enlisted(uuid("c"), "g:h", 3));
        assertEquals(List.of(uuid("b")), SoldierRoster.pickDemote(s, 2, 1));
        assertEquals(List.of(uuid("b"), uuid("c"), uuid("a")), SoldierRoster.pickDemote(s, 0, 0), "关闭时全部降回");
    }

    @Test
    void tc15SeedSoldierKept() {
        List<SoldierRoster.Enlisted> s = List.of(new SoldierRoster.Enlisted(uuid("a"), "g:h", 0));
        assertEquals(List.of(), SoldierRoster.pickDemote(s, 0, 1));
    }

    @Test
    void tc16Upgrade() {
        assertTrue(SoldierRoster.upgrade(SoldierProfile.Tier.SIMPLE, 1, 0));
        assertFalse(SoldierRoster.upgrade(SoldierProfile.Tier.SIMPLE, 0, 3));
        assertFalse(SoldierRoster.upgrade(SoldierProfile.Tier.FULL, 5, 5));
        assertTrue(SoldierRoster.upgrade(SoldierProfile.Tier.NONE, 0, 1));
        assertEquals(KitRules.Returned.SAME, KitRules.onReturn(SoldierProfile.Tier.SIMPLE, 10, 60), "换装退回简易");
    }

    // ───────────── 2.3 兵装 ─────────────

    @Test
    void tc17Durability() {
        assertFalse(KitRules.exhausted(59, 60));
        assertTrue(KitRules.exhausted(60, 60));
    }

    @Test
    void tc18ReturnByWear() {
        assertEquals(KitRules.Returned.SAME, KitRules.onReturn(SoldierProfile.Tier.FULL, 29, 60));
        assertEquals(KitRules.Returned.BROKEN, KitRules.onReturn(SoldierProfile.Tier.FULL, 30, 60));
    }

    @Test
    void tc19SimpleScrapped() {
        assertEquals(KitRules.Returned.SCRAP, KitRules.onReturn(SoldierProfile.Tier.SIMPLE, 30, 60));
        assertEquals(KitRules.Returned.NOTHING, KitRules.onReturn(SoldierProfile.Tier.NONE, 0, 60));
    }

    @Test
    void tc20RepairCost() {
        Map<String, Integer> full = new LinkedHashMap<>();
        full.put("bronze", 3);
        full.put("planks", 2);
        full.put("leather", 1);
        full.put("wool", 1);
        assertEquals(Map.of("bronze", 2, "planks", 1, "leather", 1, "wool", 1), KitRules.repairCost(full));
    }

    @Test
    void tc21Death() {
        assertEquals(KitRules.Returned.BROKEN, KitRules.onDeath(SoldierProfile.Tier.FULL));
        assertEquals(KitRules.Returned.SCRAP, KitRules.onDeath(SoldierProfile.Tier.SIMPLE));
    }

    // ───────────── 2.4 班次与岗位 ─────────────

    static final SoldierCivRules.Shifts S = SoldierCivRules.Shifts.DEFAULT;

    @Test
    void tc22SingleSoldierDayOnly() {
        assertEquals(DutyRules.Shift.DAY, DutyRules.shift(0, 1));
        assertEquals(DutyRules.State.ON_DUTY, DutyRules.state(DutyRules.Shift.DAY, 1000, S));
        assertEquals(DutyRules.State.OFF_DUTY, DutyRules.state(DutyRules.Shift.DAY, 18000, S));
    }

    @Test
    void tc23HalfOnNight() {
        int night = 0;
        for (int i = 0; i < 4; i++) {
            if (DutyRules.state(DutyRules.shift(i, 4), 18000, S) == DutyRules.State.ON_DUTY) {
                night++;
            }
        }
        assertEquals(2, night);
        int night3 = 0;
        for (int i = 0; i < 3; i++) {
            if (DutyRules.shift(i, 3) == DutyRules.Shift.NIGHT) {
                night3++;
            }
        }
        assertEquals(2, night3, "3 名时夜班 2 名（至少一半）");
    }

    @Test
    void tc24NightShiftSleepsMorning() {
        assertEquals(DutyRules.State.SHIFT_SLEEP, DutyRules.state(DutyRules.Shift.NIGHT, 3000, S));
        assertEquals(DutyRules.State.OFF_DUTY, DutyRules.state(DutyRules.Shift.NIGHT, 9000, S));
    }

    @Test
    void tc25ShiftBoundaries() {
        assertEquals(DutyRules.State.OFF_DUTY, DutyRules.state(DutyRules.Shift.DAY, 12000, S));
        assertEquals(DutyRules.State.ON_DUTY, DutyRules.state(DutyRules.Shift.NIGHT, 12000, S));
        assertEquals(DutyRules.State.ON_DUTY, DutyRules.state(DutyRules.Shift.DAY, 23000, S));
        assertEquals(DutyRules.State.SHIFT_SLEEP, DutyRules.state(DutyRules.Shift.NIGHT, 23000, S));
        SoldierCivRules.Shifts allDay = new SoldierCivRules.Shifts(0, 0, 0, 0, 0, 0);
        assertEquals(DutyRules.State.ON_DUTY, DutyRules.state(DutyRules.Shift.DAY, 13000, allDay), "from == to 是全天");
    }

    static final Pos E = new Pos(0, 0, 0), R = new Pos(10, 0, 0), C = new Pos(5, 0, 5);

    @Test
    void tc26PostsInOrder() {
        List<DutyRules.Post> p = DutyRules.posts(List.of("entrance", "ritual", "center"),
                Map.of("entrance", List.of(E), "ritual", List.of(R), "center", List.of(C)), null);
        assertEquals(List.of("entrance", "ritual", "center"), p.stream().map(DutyRules.Post::kind).toList());
        List<DutyRules.Post> q = DutyRules.posts(List.of("entrance", "ritual", "center"),
                Map.of("entrance", List.of(E), "center", List.of(C)), null);
        assertEquals(List.of("entrance", "center"), q.stream().map(DutyRules.Post::kind).toList(), "没有 ritual 就跳过");
    }

    @Test
    void tc27ExtrasPatrol() {
        List<DutyRules.Post> p = DutyRules.posts(List.of("a"), Map.of("a", List.of(E, R, C)), null);
        List<UUID> on = List.of(uuid("1"), uuid("2"), uuid("3"), uuid("4"), uuid("5"));
        Map<UUID, DutyRules.Assignment> a = DutyRules.assign(on, p);
        assertEquals(3, a.values().stream().filter(x -> !x.patrol()).count());
        assertEquals(2, a.values().stream().filter(DutyRules.Assignment::patrol).count());
        DutyRules.Assignment patrol = a.get(uuid("4"));
        assertEquals(0, patrol.target(0, 3));
        assertEquals(1, patrol.target(DutyRules.PATROL_LEG, 3));
        assertEquals(1, a.get(uuid("5")).target(0, 3), "两名巡逻者错开");
    }

    @Test
    void tc28FallbackBell() {
        Pos bell = new Pos(1, 2, 3);
        List<DutyRules.Post> p = DutyRules.posts(List.of("entrance"), Map.of(), bell);
        assertEquals(List.of(new DutyRules.Post("bell", bell)), p);
    }

    // ───────────── 2.5 敌我与拴绳 ─────────────

    static SoldierTargetRules.Decision d(SoldierTargetRules.Facts f) {
        return SoldierTargetRules.decide(f);
    }

    static final SoldierTargetRules.Facts HOSTILE = SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.HOSTILE);

    @Test
    void tc29HostileLeash() {
        assertEquals(SoldierTargetRules.Decision.ENGAGE, d(HOSTILE));
        assertTrue(SoldierTargetRules.canEngage(10, true));
        assertFalse(SoldierTargetRules.canEngage(17, true));
        assertFalse(SoldierTargetRules.canEngage(5, false), "村外");
    }

    @Test
    void tc30Chase() {
        assertTrue(SoldierTargetRules.keepChasing(23.9, true));
        assertFalse(SoldierTargetRules.keepChasing(24.1, true));
    }

    @Test
    void tc31AnimalsNever() {
        SoldierTargetRules.Facts a = SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.ANIMAL);
        assertEquals(SoldierTargetRules.Decision.IGNORE, d(a.attacking(true, true)));
        assertEquals(SoldierTargetRules.Decision.ESCORT, d(a.harmed(0).attacking(true, true)), "咬了居民也只护送，不打");
        assertEquals(SoldierTargetRules.Decision.IGNORE, d(a.harmed(101)));
    }

    @Test
    void tc32NeutralMemory() {
        SoldierTargetRules.Facts n = SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.NEUTRAL);
        assertEquals(SoldierTargetRules.Decision.ENGAGE, d(n.harmed(100)));
        assertEquals(SoldierTargetRules.Decision.IGNORE, d(n.harmed(101)));
        assertEquals(SoldierTargetRules.Decision.IGNORE, d(n));
    }

    @Test
    void tc33Players() {
        SoldierTargetRules.Facts p = SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.PLAYER);
        assertEquals(SoldierTargetRules.Decision.IGNORE, d(p.harmed(0).player(false, true, true)), "同队");
        assertEquals(SoldierTargetRules.Decision.ESCORT, d(p.harmed(0).player(false, false, false)), "PvP 关");
        assertEquals(SoldierTargetRules.Decision.ENGAGE, d(p.harmed(29 * 20).player(false, false, true)));
        assertEquals(SoldierTargetRules.Decision.IGNORE, d(p.harmed(31 * 20).player(false, false, true)));
        // 出村由拴绳处理
        assertFalse(SoldierTargetRules.keepChasing(3, false));
    }

    @Test
    void tc34CreativeIgnored() {
        SoldierTargetRules.Facts p = SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.PLAYER);
        assertEquals(SoldierTargetRules.Decision.IGNORE, d(p.harmed(0).player(true, false, true)));
    }

    @Test
    void tc35OwnedUnits() {
        SoldierTargetRules.Facts o = SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.OWNED);
        assertEquals(SoldierTargetRules.Decision.ESCORT, d(o.owned(false, true)));
        assertEquals(SoldierTargetRules.Decision.ENGAGE, d(o.owned(true, false)));
        assertEquals(SoldierTargetRules.Decision.IGNORE, d(o));
    }

    @Test
    void tc36EliteBoss() {
        SoldierTargetRules.Facts b = SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.BOSS);
        assertEquals(SoldierTargetRules.Decision.ESCORT, d(b));
        assertEquals(SoldierTargetRules.Decision.ESCORT, d(b.attacking(true, false)), "打我但不在触及内");
        assertEquals(SoldierTargetRules.Decision.ENGAGE, d(b.attacking(true, true)));
        assertEquals(SoldierTargetRules.Decision.ESCORT, d(SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.ELITE)));
    }

    @Test
    void tc37VillageFolk() {
        assertEquals(SoldierTargetRules.Decision.IGNORE,
                d(SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.VILLAGE_FOLK).harmed(0).attacking(true, true)));
    }

    // ───────────── 2.6 攻击几何 ─────────────

    @Test
    void tc38Timeline() {
        SoldierProfile.Timeline tl = profile(HOPLITE).timeline();
        assertEquals(AttackTimeline.Phase.WINDUP, AttackTimeline.phase(tl, 11));
        assertEquals(AttackTimeline.Phase.ACTIVE, AttackTimeline.phase(tl, 12));
        assertEquals(AttackTimeline.Phase.ACTIVE, AttackTimeline.phase(tl, 14));
        assertEquals(AttackTimeline.Phase.RECOVER, AttackTimeline.phase(tl, 15));
        assertEquals(AttackTimeline.Phase.COOLDOWN, AttackTimeline.phase(tl, 31));
        assertEquals(AttackTimeline.Phase.READY, AttackTimeline.phase(tl, 32));
        assertFalse(AttackTimeline.locked(tl, 7));
        assertTrue(AttackTimeline.locked(tl, 8));
        assertTrue(AttackTimeline.locked(tl, 11));
        assertTrue(AttackTimeline.locked(tl, 14));
        assertFalse(AttackTimeline.locked(tl, 15));
    }

    /** 宽 0.6、高 1.95 的目标，前沿离士兵 {@code near} 格，侧偏 {@code side}（士兵朝 +Z）。 */
    static ThrustGeometry.Box box(double near, double side) {
        return new ThrustGeometry.Box(side - 0.3, 0, near, side + 0.3, 1.95, near + 0.6);
    }

    static final SoldierProfile.Melee M = new SoldierProfile.Melee(2.8, 0.25, 1.15, 0.5);

    @Test
    void tc39aCapsule() {
        assertTrue(ThrustGeometry.hit(0, 0, 0, 0, M, Double.MAX_VALUE, box(1.5, 0)).isPresent());
        assertTrue(ThrustGeometry.hit(0, 0, 0, 0, M, Double.MAX_VALUE, box(2.8, 0)).isPresent(), "前沿正好在线段末端");
        // 胶囊两端是圆头：线段末端 2.8 再加半径 0.25
        assertTrue(ThrustGeometry.hit(0, 0, 0, 0, M, Double.MAX_VALUE, box(3.04, 0)).isPresent());
        assertFalse(ThrustGeometry.hit(0, 0, 0, 0, M, Double.MAX_VALUE, box(3.06, 0)).isPresent());
        assertFalse(ThrustGeometry.hit(0, 0, 0, 0, M, Double.MAX_VALUE,
                new ThrustGeometry.Box(-0.3, 0, -0.5, 0.3, 1.95, 0.2)).isPresent(), "整个在起点之后");
        assertTrue(ThrustGeometry.hit(0, 0, 0, 0, M, Double.MAX_VALUE, box(1.5, 0.2)).isPresent());
        assertFalse(ThrustGeometry.hit(0, 0, 0, 0, M, Double.MAX_VALUE, box(1.5, 0.6)).isPresent(), "侧偏 0.6：边缘离线 0.3 > 半径");
        assertTrue(ThrustGeometry.hit(0, 0, 0, 90, M, Double.MAX_VALUE,
                new ThrustGeometry.Box(-2.1, 0, -0.3, -1.5, 1.95, 0.3)).isPresent(), "朝 −X");
        assertFalse(ThrustGeometry.hit(0, 0, 0, 0, M, Double.MAX_VALUE,
                new ThrustGeometry.Box(-0.3, 1.8, 1.5, 0.3, 3, 2.1)).isPresent(), "高过胶囊");
    }

    @Test
    void tc39bBlockedByWall() {
        assertFalse(ThrustGeometry.hit(0, 0, 0, 0, M, 1.2, box(2.0, 0)).isPresent());
        assertTrue(ThrustGeometry.hit(0, 0, 0, 0, M, 1.2, box(1.0, 0)).isPresent());
    }

    @Test
    void tc39cAllyBlocks() {
        Object resident = "resident";
        Object zombie = "zombie";
        assertInstanceOf(ThrustGeometry.Withhold.class, ThrustGeometry.resolve(List.of(
                new ThrustGeometry.Hit(zombie, 2.4, true), new ThrustGeometry.Hit(resident, 1.2, false))));
        ThrustGeometry.Outcome o = ThrustGeometry.resolve(List.of(new ThrustGeometry.Hit(zombie, 1.2, true),
                new ThrustGeometry.Hit(resident, 2.4, false)));
        assertEquals(new ThrustGeometry.Strike(zombie), o);
        assertInstanceOf(ThrustGeometry.Miss.class, ThrustGeometry.resolve(List.of(new ThrustGeometry.Hit(resident, 1, false))));
    }

    @Test
    void tc39dShieldArc() {
        SoldierProfile.Shield s = profile(HOPLITE).shield();
        assertEquals(0.4, ShieldRules.multiplier(s, true, 0, 0, 5), 1e-9, "正前");
        double a59 = Math.toRadians(59);
        assertEquals(0.4, ShieldRules.multiplier(s, true, 0, -Math.sin(a59) * 5, Math.cos(a59) * 5), 1e-9);
        double a61 = Math.toRadians(61);
        assertEquals(1.0, ShieldRules.multiplier(s, true, 0, -Math.sin(a61) * 5, Math.cos(a61) * 5), 1e-9);
        assertEquals(1.0, ShieldRules.multiplier(s, true, 0, 0, -5), 1e-9, "正后");
        assertEquals(1.0, ShieldRules.multiplier(s, false, 0, 0, 5), 1e-9, "没举盾");
    }

    @Test
    void tc39eShieldHoldAndGap() {
        SoldierProfile.Shield s = profile(HOPLITE).shield();
        assertFalse(ShieldRules.mustLower(s, 39, 0));
        assertTrue(ShieldRules.mustLower(s, 40, 0));
        for (int t = 41; t <= 55; t++) {
            assertFalse(ShieldRules.canRaise(s, t, 40), "第 " + t + " 刻");
        }
        assertTrue(ShieldRules.canRaise(s, 56, 40));
        assertFalse(ShieldRules.effective(s, 5, 0));
        assertTrue(ShieldRules.effective(s, 6, 0));
    }

    @Test
    void tc39fInterceptStand() {
        double[] a = InterceptRules.stand(0, 0, 6, 0);
        assertEquals(1.5, a[0], 1e-9);
        assertEquals(0, a[1], 1e-9);
        double[] b = InterceptRules.stand(0, 0, 2, 0);
        assertEquals(1.0, b[0], 1e-9);
    }

    @Test
    void tc39gProjectileCover() {
        SoldierProfile.Shield s = profile(HOPLITE).shield();
        // 卫兵在 (0, 0) 朝 +Z（偏航 0），被护的居民在卫兵身后 (0, -2.5)
        double a50 = Math.toRadians(50);
        assertTrue(InterceptRules.covers(s, true, 0, 0, 0, -Math.sin(a50) * 8, Math.cos(a50) * 8, 0, -2.5));
        assertFalse(InterceptRules.covers(s, true, 0, 0, 0, 0, -8, 0, -2.5), "射手在背后");
        assertFalse(InterceptRules.covers(s, true, 0, 0, 0, 0, 8, 0, -3.5), "离居民 3.5 格");
        assertFalse(InterceptRules.covers(s, false, 0, 0, 0, 0, 8, 0, -2.5), "没举盾");
    }

    @Test
    void tc39nBallistics() {
        double[] v = Ballistics.launch(10, 0, 0, 1.1, 0.05).orElseThrow();
        assertEquals(1.1, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1e-9, "初速不变");
        assertTrue(v[1] > 0 && v[1] < v[0], "低抛（仰角 < 45°）");
        assertEquals(0, Ballistics.heightAt(v, 10, 0.05), 1.2, "落在 10 格外的同一高度附近（离散步长误差）");
        assertTrue(Ballistics.launch(40, 0, 0, 1.1, 0.05).isEmpty(), "超出最大射程");
        double[] flat = Ballistics.launch(3, 4, 0, 1.0, 0).orElseThrow();
        assertEquals(0.6, flat[0], 1e-9);
        assertEquals(0.8, flat[1], 1e-9);
    }

    // ───────────── 2.7 数据解析 ─────────────

    @Test
    void tc39hParseProfile() {
        SoldierProfile p = profile(HOPLITE);
        assertEquals(SoldierProfile.Kind.MELEE, p.kind());
        assertEquals(28, p.health());
        assertEquals(6, p.armor(SoldierProfile.Tier.FULL));
        assertEquals(4, p.armor(SoldierProfile.Tier.SIMPLE));
        assertEquals(4, p.damage(SoldierProfile.Tier.SIMPLE));
        assertEquals(60, p.durability());
        assertEquals("x:kit_broken", p.kits().broken());
        assertEquals(2.8, p.melee().reach());
        assertTrue(p.hasShield());
    }

    @Test
    void tc39iAttributionIsHardRule() {
        List<String> warnings = new ArrayList<>();
        JsonObject o = JsonParser.parseString(HOPLITE).getAsJsonObject();
        o.getAsJsonObject("attribution").addProperty("favor", 1);
        SoldierProfile.parse(o, "test", warnings);
        assertEquals(1, warnings.size(), warnings::toString);
        assertTrue(warnings.get(0).contains("attribution.favor"));
    }

    @Test
    void tc39jMissingFields() {
        JsonObject noMelee = JsonParser.parseString(HOPLITE).getAsJsonObject();
        noMelee.remove("melee");
        assertTrue(assertThrows(SoldierProfile.SoldierDataException.class, () -> SoldierProfile.parse(noMelee, "h", new ArrayList<>()))
                .getMessage().contains("h.melee"));
        JsonObject ranged = JsonParser.parseString(HOPLITE).getAsJsonObject();
        ranged.addProperty("kind", "ranged");
        ranged.add("ranged", new JsonObject());
        assertTrue(assertThrows(SoldierProfile.SoldierDataException.class, () -> SoldierProfile.parse(ranged, "j", new ArrayList<>()))
                .getMessage().contains("j.ammo"));
        JsonObject emptyKit = JsonParser.parseString(HOPLITE).getAsJsonObject();
        emptyKit.getAsJsonObject("kits").getAsJsonObject("full").addProperty("item", "");
        assertTrue(assertThrows(SoldierProfile.SoldierDataException.class, () -> SoldierProfile.parse(emptyKit, "k", new ArrayList<>()))
                .getMessage().contains("k.kits.full.item"));
    }

    @Test
    void tc39kCivRulesAndProfession() {
        CivVillageData d = CivVillageData.parse("g", JsonParser.parseString("""
                {"residents": {"initial": {"g:farmer": 2, "g:hoplite": 1}, "fallback": "g:laborer"},
                 "soldiers": {"order": ["g:hoplite", "g:jav"], "posts": ["entrance", "ritual"], "minLaborers": 2,
                              "seedKit": {"g:hoplite": "simple"}}}""").getAsJsonObject());
        assertEquals(List.of("g:hoplite", "g:jav"), d.soldiers().order());
        assertEquals(SoldierProfile.Tier.SIMPLE, d.soldiers().seedKit().get("g:hoplite"));
        assertEquals(List.of("g:farmer", "g:farmer", "g:hoplite", "g:laborer"), d.initialProfessions(4));
        JsonObject prof = JsonParser.parseString("{\"jobs\": [\"haul\"], \"soldier\": " + HOPLITE + "}").getAsJsonObject();
        CivVillageData.Profession p = CivVillageData.parseProfession("g:hoplite", prof);
        assertTrue(p.isSoldier());
        assertNull(CivVillageData.parseProfession("g:potter", JsonParser.parseString("{\"jobs\": []}").getAsJsonObject()).soldier());
        assertEquals(SoldierCivRules.NONE, CivVillageData.parse("x", new JsonObject()).soldiers());
    }

    // ───────────── 共用基础采集（D5） ─────────────

    static final JsonObject COMMON = JsonParser.parseString("""
            {"woodland": {"radius": 48, "max": 24, "professions": ["@fallback"]},
             "points": {"stone": {"outputs": {"minecraft:cobblestone": 2}, "professions": ["@fallback"]},
                        "gravel": {"outputs": {"minecraft:flint": 1}, "professions": ["@fallback"]}},
             "naturalPoints": {"stone": {"block": "minecraft:stone", "outcrop": true},
                               "gravel": {"block": "minecraft:gravel", "outcrop": true}}}""").getAsJsonObject();

    @Test
    void tc39lCommonMerge() {
        CivVillageData a = CivVillageData.parse("a", CommonVillageData.merge(COMMON, new JsonObject(), "a"));
        assertTrue(a.points().containsKey("stone") && a.points().containsKey("gravel"));
        assertEquals(48, a.woodland().radius());
        assertEquals(2, a.naturalPoints().size());
        CivVillageData b = CivVillageData.parse("b", CommonVillageData.merge(COMMON,
                JsonParser.parseString("{\"woodland\": {\"radius\": 32}}").getAsJsonObject(), "b"));
        assertEquals(32, b.woodland().radius());
        assertEquals(24, b.woodland().max(), "没写的字段沿用共用");
        CivVillageData c = CivVillageData.parse("c", CommonVillageData.merge(COMMON,
                JsonParser.parseString("{\"points\": {\"gravel\": null}, \"naturalPoints\": {\"gravel\": null}, \"woodland\": null}")
                        .getAsJsonObject(), "c"));
        assertFalse(c.points().containsKey("gravel"));
        assertFalse(c.naturalPoints().containsKey("gravel"));
        assertNull(c.woodland());
        assertTrue(c.points().containsKey("stone"));
        CivVillageData e = CivVillageData.parse("e", CommonVillageData.merge(COMMON,
                JsonParser.parseString("{\"naturalPoints\": {\"stone\": null, \"gravel\": null}}").getAsJsonObject(), "e"));
        assertTrue(e.naturalPoints().isEmpty(), "全关掉也能解析");
    }

    @Test
    void tc39mFallbackPlaceholder() {
        CivVillageData g = CivVillageData.parse("greek", CommonVillageData.merge(COMMON,
                JsonParser.parseString("{\"residents\": {\"fallback\": \"greek:laborer\"}}").getAsJsonObject(), "greek"));
        assertEquals(java.util.Set.of("greek:laborer"), g.points().get("stone").professions());
        assertEquals(java.util.Set.of("greek:laborer"), g.woodland().professions());
        CivVillageData t = CivVillageData.parse("test", CommonVillageData.merge(COMMON,
                JsonParser.parseString("{\"residents\": {\"fallback\": \"test:worker\"}}").getAsJsonObject(), "test"));
        assertEquals(java.util.Set.of("test:worker"), t.points().get("gravel").professions());
        CivVillageData n = CivVillageData.parse("norse", CommonVillageData.merge(COMMON, new JsonObject(), "norse"));
        assertEquals(java.util.Set.of("norse:laborer"), n.points().get("stone").professions(), "默认 <文明>:laborer");
    }
}
