package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.conduct.ConductEvents;
import io.github.happysnappingturtle.mythicage.conduct.ConductService;
import io.github.happysnappingturtle.mythicage.conduct.Responsibility;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.mixin.ZombieVillagerAccessor;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.player.PlayerDataNbt;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import io.github.happysnappingturtle.mythicage.summon.greek.SummonPegasusEntity;
import io.github.happysnappingturtle.mythicage.village.VillageAnchor;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import io.github.happysnappingturtle.mythicage.village.VillageZones;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * T18 村庄锚点、空间范围与善恶值（TC-T18-20～64）。场地 17 × 16 × 36，地面在 y = 1。
 * 玩家用 FakePlayer（不在玩家列表里、不会受伤、不 tick）：登记为在线；回归直接调 tickOnline；自卫的「先动手」直接记录。
 * 改全局配置的用例单独成批，结束时恢复。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConductGameTests {
    private static final String BATCH = "conduct";
    private static final String CFG = "conduct_cfg";
    /** 原版村庄的兴趣点扫描半径 64 格，会把相邻用例的钟也算进来：用到原版村庄的用例各占一批。 */
    private static final String V1 = "conduct_vanilla_1";
    private static final String V2 = "conduct_vanilla_2";
    private static final String V3 = "conduct_vanilla_3";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private ConductGameTests() {
    }

    // ───────────── 搭建 ─────────────

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static FakePlayer player(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 34))));
        OnlinePlayers.forTest(p, true);
        return p;
    }

    private static int score(FakePlayer p) {
        return MythData.get(p).conductScore();
    }

    private static <T extends Mob> T still(T e) {
        e.setNoAi(true);
        return e;
    }

    /** 生成村庄：建筑盒 (2..6, 2..6)，预留地块 (2..6, 12..16)，锚点在 (4, 2, 4)（没有钟）。 */
    private record Generated(VillageRegistry.Village village) {
    }

    private static Generated generated(GameTestHelper h) {
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        Box built = new Box(o.getX() + 2, o.getY() + 2, o.getZ() + 2, o.getX() + 6, o.getY() + 6, o.getZ() + 6);
        Box plot = new Box(o.getX() + 2, o.getY() + 2, o.getZ() + 16, o.getX() + 6, o.getY() + 4, o.getZ() + 20);
        BlockPos center = h.absolutePos(new BlockPos(4, 2, 4));
        UUID id = UUID.randomUUID();
        VillageInfoPiece.Info info = new VillageInfoPiece.Info("greek", "zeus", "test", 1, "test",
                new Pos(center.getX(), center.getY(), center.getZ()), Map.of(), List.of(built),
                List.of(new VillagePlan.Plot("p1", "house", "classical", plot, "north")));
        VillageRegistry.Village v = new VillageRegistry.Village(id, GlobalPos.of(h.getLevel().dimension(), center), "greek", "zeus",
                VillageRegistry.Origin.GENERATED, null, "", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        VillagePlans.get(h.getLevel().getServer()).put(new VillagePlans.Plan(id, h.getLevel().dimension(), "test",
                h.getLevel().getRandom().nextLong(), info, true));
        return new Generated(v);
    }

    /** 原版村庄：钟在 (8, 2, 4)，堆肥桶（农夫工作站）在 (8, 2, 12)。 */
    private static VillageRegistry.Village vanilla(GameTestHelper h) {
        h.setBlock(new BlockPos(8, 2, 4), Blocks.BELL);
        h.setBlock(new BlockPos(8, 2, 12), Blocks.COMPOSTER);
        // 放钟时原版村庄可能已经自动登记（BellBlockMixin）；有就用它
        GlobalPos bell = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(8, 2, 4)));
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        VillageRegistry.Village v = reg.byBell(bell).orElseGet(() -> {
            VillageRegistry.Village nv = new VillageRegistry.Village(UUID.randomUUID(), bell, "greek", "zeus",
                    VillageRegistry.Origin.VANILLA, null, "plains", -1);
            reg.add(nv);
            return nv;
        });
        VillageZones.refresh(h.getLevel(), v);
        return v;
    }

    /** 同一个锚点上的全部村庄（GuardianSpawner 在实体加入时也会登记附近的钟，可能多出一条）。 */
    private static List<VillageRegistry.Village> atBell(GameTestHelper h, GlobalPos bell) {
        return VillageRegistry.get(h.getLevel().getServer()).all().stream().filter(x -> x.bell().equals(bell)).toList();
    }

    /**
     * 锚点 70 格内的全部原版村庄。各批次的 GameTest 会重用同一块场地，别的用例留下的村庄登记可能还在，
     * 它的兴趣点扫描（64 格）会把这里的钟、工作站也算进去。
     */
    private static List<VillageRegistry.Village> vanillaNear(GameTestHelper h, GlobalPos bell) {
        return VillageRegistry.get(h.getLevel().getServer()).in(bell.dimension()).stream()
                .filter(x -> x.anchor().kind() != VillageAnchor.Kind.CENTER && x.bell().pos().distManhattan(bell.pos()) <= 140
                        && Math.abs(x.bell().pos().getX() - bell.pos().getX()) <= 70
                        && Math.abs(x.bell().pos().getZ() - bell.pos().getZ()) <= 70)
                .toList();
    }

    private static void cleanup(GameTestHelper h, List<FakePlayer> players, VillageRegistry.Village... villages) {
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        for (VillageRegistry.Village v : villages) {
            VillagePlans.get(h.getLevel().getServer()).remove(v.id());
            for (VillageRegistry.Village x : vanillaNear(h, v.bell())) {
                VillageZones.unknownForTest(x.id(), false);
            }
            for (VillageRegistry.Village x : atBell(h, v.bell())) {
                reg.remove(x.bell());
            }
            VillageZones.unknownForTest(v.id(), false);
        }
        for (FakePlayer p : players) {
            OnlinePlayers.forTest(p, false);
        }
        for (Class<? extends LivingEntity> c : List.of(Villager.class, IronGolem.class, Wolf.class, Zombie.class,
                ZombieVillager.class, net.minecraft.world.entity.animal.Animal.class, SummonPegasusEntity.class)) {
            h.killAllEntitiesOfClass(c);
        }
    }

    private static VillageZones.Zone zone(GameTestHelper h, int x, int z) {
        return VillageZones.zoneAt(h.getLevel(), h.absolutePos(new BlockPos(x, 2, z))).zone();
    }

    private static void hit(LivingEntity e, DamageSource s, float amount) {
        e.invulnerableTime = 0;
        e.hurt(s, amount);
    }

    private static DamageSource melee(GameTestHelper h, FakePlayer p) {
        return h.getLevel().damageSources().playerAttack(p);
    }

    // ───────────── 3.1 锚点与范围 ─────────────

    /** TC-T18-20 / 21：生成村庄锚点没有钟不算消失；原版村庄钟被拆掉照旧标记。 */
    @GameTest(template = T, batch = V1)
    public static void tc20_anchor_kinds(GameTestHelper h) {
        floor(h);
        Generated g = generated(h);
        VillageRegistry.Village v = vanilla(h);
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        try {
            h.assertTrue(g.village().anchor().kind() == VillageAnchor.Kind.CENTER, "生成村庄是 CENTER 锚点");
            h.assertTrue(v.anchor().kind() == VillageAnchor.Kind.BELL, "原版村庄是 BELL 锚点");
            h.setBlock(new BlockPos(8, 2, 4), Blocks.AIR);
            VillageService.checkBells(h.getLevel(), h.absolutePos(new BlockPos(8, 2, 8)));
            h.assertTrue(reg.byId(g.village().id()).orElseThrow().missingSinceDay() < 0, "生成村庄没有钟也不算消失");
            h.assertTrue(reg.byId(v.id()).orElseThrow().missingSinceDay() >= 0, "原版村庄的钟拆了要标记消失");
        } finally {
            cleanup(h, List.of(), g.village(), v);
        }
        h.succeed();
    }

    /** TC-T18-22 / 25：生成村庄的建筑盒内、外扩 4 格边上、5 格外、预留地块、登记的资源区。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc22_generated_zones(GameTestHelper h) {
        Generated g = generated(h);
        try {
            h.assertTrue(zone(h, 4, 4) == VillageZones.Zone.BUILT, "建筑盒内");
            h.assertTrue(zone(h, 10, 4) == VillageZones.Zone.BUILT, "外扩 4 格的边上");
            h.assertTrue(zone(h, 12, 4) == VillageZones.Zone.OUTSIDE, "外扩 6 格");
            h.assertTrue(zone(h, 4, 18) == VillageZones.Zone.PLANNED, "预留地块");
            BlockPos o = h.absolutePos(BlockPos.ZERO);
            VillageZones.registerResource(g.village().id(), new Box(o.getX() + 12, 0, o.getZ() + 28, o.getX() + 15, 300, o.getZ() + 32));
            h.assertTrue(zone(h, 13, 30) == VillageZones.Zone.RESOURCE, "资源区");
            VillageZones.clearResources(g.village().id());
        } finally {
            cleanup(h, List.of(), g.village());
        }
        h.succeed();
    }

    /** TC-T18-23 / 24：原版村庄按兴趣点算已建范围，不按 48 格圆；边界未知时 known = false。 */
    @GameTest(template = T, batch = V2)
    public static void tc23_vanilla_zones(GameTestHelper h) {
        floor(h);
        VillageRegistry.Village v = vanilla(h);
        try {
            h.assertTrue(zone(h, 8, 4) == VillageZones.Zone.BUILT, "钟旁");
            h.assertTrue(zone(h, 11, 12) == VillageZones.Zone.BUILT, "工作站旁 3 格");
            h.assertTrue(zone(h, 8, 26) == VillageZones.Zone.OUTSIDE, "离所有兴趣点 14 格，仍在钟 48 格内");
            vanillaNear(h, v.bell()).forEach(x -> VillageZones.unknownForTest(x.id(), true));
            var hit = VillageZones.zoneAt(h.getLevel(), h.absolutePos(new BlockPos(8, 2, 4)));
            h.assertFalse(hit.known(), "边界未知，实际 " + hit.zone() + " " + hit.village().map(x -> x.id() + "/" + x.origin()).orElse("-")
                    + "，本村 " + v.id());
        } finally {
            cleanup(h, List.of(), v);
        }
        h.succeed();
    }

    // ───────────── 3.2 归因 ─────────────

    /** TC-T18-30～35：直接、箭、召唤单位、狼、神力、无主僵尸。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void tc30_attribution(GameTestHelper h) {
        floor(h);
        FakePlayer p = player(h, "cd_blame");
        try {
            var ds = h.getLevel().damageSources();
            Responsibility.Blame direct = Responsibility.of(ds.playerAttack(p));
            h.assertTrue(direct.player().orElseThrow().equals(p.getUUID()) && direct.via() == Responsibility.Via.DIRECT, "直接");
            Arrow arrow = new Arrow(h.getLevel(), p, new ItemStack(Items.ARROW), null);
            h.assertTrue(Responsibility.of(ds.arrow(arrow, p)).via() == Responsibility.Via.PROJECTILE, "箭");
            SummonService.Result r = SummonService.summon(p, SummonUnits.card("greek:pegasus").orElseThrow(),
                    Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 20))));
            h.assertTrue(r == SummonService.Result.OK, "召唤天马应成功，实际 " + r);
            SummonPegasusEntity peg = h.getLevel().getEntitiesOfClass(SummonPegasusEntity.class,
                    new AABB(h.absolutePos(new BlockPos(8, 2, 20))).inflate(10), e -> p.getUUID().equals(e.link().owner)).get(0);
            Responsibility.Blame summon = Responsibility.of(ds.mobAttack(peg));
            h.assertTrue(summon.via() == Responsibility.Via.SUMMON && summon.player().orElseThrow().equals(p.getUUID()), "召唤单位");
            Wolf wolf = still(h.spawn(EntityType.WOLF, 4, 2, 20));
            wolf.tame(p);
            h.assertTrue(Responsibility.of(ds.mobAttack(wolf)).via() == Responsibility.Via.PET, "狼");
            Responsibility.Blame power = Responsibility.of(ds.source(GreekPowers.DIVINE_BOLT, p));
            h.assertTrue(power.via() == Responsibility.Via.POWER && power.player().orElseThrow().equals(p.getUUID()), "神力");
            Zombie z = still(h.spawn(EntityType.ZOMBIE, 12, 2, 20));
            h.assertTrue(Responsibility.of(ds.mobAttack(z)).player().isEmpty(), "无主僵尸没有责任人");
        } finally {
            cleanup(h, List.of(p));
        }
        h.succeed();
    }

    // ───────────── 3.3 计分 ─────────────

    /** TC-T18-40 / 41：伤害 −2，同窗口杀死再 −10；被免疫、伤害为 0 不算。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc40_villager_escalation(GameTestHelper h) {
        floor(h);
        FakePlayer p = player(h, "cd_villager");
        try {
            Villager v = still(h.spawn(EntityType.VILLAGER, 8, 2, 20));
            Villager shielded = still(h.spawn(EntityType.VILLAGER, 4, 2, 20));
            shielded.setInvulnerable(true);
            hit(shielded, melee(h, p), 2);
            hit(v, melee(h, p), 0);
            h.assertTrue(score(p) == 0, "免疫、伤害为 0 不算，实际 " + score(p));
            hit(v, melee(h, p), 2);
            h.assertTrue(score(p) == -2, "伤害 −2，实际 " + score(p));
            hit(v, melee(h, p), 2);
            h.assertTrue(score(p) == -2, "同窗口再伤害不计");
            hit(v, melee(h, p), 1000);
            h.assertTrue(score(p) == -12, "致死合计 −12，实际 " + score(p));
            List<PlayerMythData.ConductEntry> recent = MythData.get(p).recentConduct();
            h.assertTrue(recent.size() == 2 && recent.get(0).reason().equals("harm_villager")
                    && recent.get(1).reason().equals("kill_villager") && recent.get(1).delta() == -10, "记录：" + recent);
        } finally {
            cleanup(h, List.of(p));
        }
        h.succeed();
    }

    /** TC-T18-43 / 49 / 50：村内铁傀儡 −2、村外 0；铁傀儡先动手时还手不算；自己先打村民引来的不豁免。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc43_defenders_and_self_defense(GameTestHelper h) {
        floor(h);
        Generated g = generated(h);
        FakePlayer p = player(h, "cd_golem");
        FakePlayer q = player(h, "cd_golem2");
        try {
            IronGolem inside = still(h.spawn(EntityType.IRON_GOLEM, 4, 2, 4));
            IronGolem outside = still(h.spawn(EntityType.IRON_GOLEM, 13, 2, 26));
            hit(outside, melee(h, p), 2);
            h.assertTrue(score(p) == 0, "村外铁傀儡不算");
            hit(inside, melee(h, p), 2);
            h.assertTrue(score(p) == -2, "村内铁傀儡 −2，实际 " + score(p));
            // 自卫：另一个铁傀儡先打 q，q 还手
            IronGolem angry = still(h.spawn(EntityType.IRON_GOLEM, 5, 2, 5));
            ConductEvents.noteAggression(angry, q, h.getLevel().getGameTime());
            hit(angry, melee(h, q), 2);
            h.assertTrue(score(q) == 0, "铁傀儡先动手，还手不算，实际 " + score(q));
            // 自造危险：q 先打村民，再有铁傀儡打 q，q 还手
            Villager v = still(h.spawn(EntityType.VILLAGER, 3, 2, 3));
            hit(v, melee(h, q), 2);
            IronGolem guard = still(h.spawn(EntityType.IRON_GOLEM, 6, 2, 3));
            ConductEvents.noteAggression(guard, q, h.getLevel().getGameTime());
            hit(guard, melee(h, q), 2);
            h.assertTrue(score(q) == -4, "自己先打村民引来的，不豁免：−2 −2，实际 " + score(q));
        } finally {
            cleanup(h, List.of(p, q), g.village());
        }
        h.succeed();
    }

    /** TC-T18-42：守护者伤害 −3，致死合计 −15。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc42_guardian(GameTestHelper h) {
        floor(h);
        FakePlayer p = player(h, "cd_guardian");
        try {
            var g = h.spawn(io.github.happysnappingturtle.mythicage.guardian.GuardianTypes.typeFor(
                    io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK).orElseThrow(), 8, 2, 20);
            g.setNoAi(true);
            hit(g, melee(h, p), 2);
            h.assertTrue(score(p) == -3, "伤害守护者 −3，实际 " + score(p));
            hit(g, melee(h, p), 1000);
            h.assertTrue(score(p) == -15, "合计 −15，实际 " + score(p));
        } finally {
            h.killAllEntitiesOfClass(io.github.happysnappingturtle.mythicage.guardian.GuardianEntity.class);
            cleanup(h, List.of(p));
        }
        h.succeed();
    }

    /** TC-T18-44 / 45 / 47：同一种动物村内 −1 / 合计 −4，村外 0；牛、鸡、狼同理；喀耳刻的猪在村内 0。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc44_animals_inside_outside(GameTestHelper h) {
        floor(h);
        Generated g = generated(h);
        FakePlayer p = player(h, "cd_animals");
        try {
            int expected = 0;
            for (EntityType<? extends Mob> type : List.of(EntityType.PIG, EntityType.COW, EntityType.CHICKEN, EntityType.WOLF)) {
                Mob in = still(h.spawn(type, 4, 2, 4));
                Mob out = still(h.spawn(type, 13, 2, 28));
                hit(out, melee(h, p), 1);
                hit(out, melee(h, p), 1000);
                h.assertTrue(score(p) == expected, type + " 村外 0，实际 " + score(p));
                hit(in, melee(h, p), 1);
                h.assertTrue(score(p) == expected - 1, type + " 村内伤害 −1，实际 " + score(p));
                hit(in, melee(h, p), 1000);
                expected -= 4;
                h.assertTrue(score(p) == expected, type + " 村内合计 −4，实际 " + score(p));
            }
            Mob cursed = still(h.spawn(GreekPowers.CURSED_PIG.get(), 4, 2, 4));
            hit(cursed, melee(h, p), 1000);
            h.assertTrue(score(p) == expected, "喀耳刻的猪不算动物");
        } finally {
            cleanup(h, List.of(p), g.village());
        }
        h.succeed();
    }

    /** TC-T18-46：村内受伤后到村外死亡只算 −1；村外受伤后进村死亡算 −4。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc46_cross_boundary(GameTestHelper h) {
        floor(h);
        Generated g = generated(h);
        FakePlayer p = player(h, "cd_cross");
        try {
            Mob a = still(h.spawn(EntityType.PIG, 4, 2, 4));
            hit(a, melee(h, p), 1);
            Vec3 out = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(13, 2, 28)));
            a.moveTo(out.x, out.y, out.z);
            hit(a, melee(h, p), 1000);
            h.assertTrue(score(p) == -1, "村内伤、村外死：−1，实际 " + score(p));
            Mob b = still(h.spawn(EntityType.PIG, 13, 2, 28));
            hit(b, melee(h, p), 1);
            Vec3 in = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 2, 4)));
            b.moveTo(in.x, in.y, in.z);
            hit(b, melee(h, p), 1000);
            h.assertTrue(score(p) == -5, "村外伤、村内死：再 −4，实际 " + score(p));
        } finally {
            cleanup(h, List.of(p), g.village());
        }
        h.succeed();
    }

    /** TC-T18-48：原版村庄边界未知时，村里的猪受伤不扣。 */
    @GameTest(template = T, batch = V3)
    public static void tc48_unknown_boundary(GameTestHelper h) {
        floor(h);
        VillageRegistry.Village v = vanilla(h);
        FakePlayer p = player(h, "cd_unknown");
        try {
            Mob pig = still(h.spawn(EntityType.PIG, 8, 2, 5));
            List<VillageRegistry.Village> near = vanillaNear(h, v.bell());
            near.forEach(x -> VillageZones.unknownForTest(x.id(), true));
            hit(pig, melee(h, p), 1);
            var hz = VillageZones.zoneAt(h.getLevel(), pig.blockPosition());
            h.assertTrue(score(p) == 0, "边界未知不扣，实际 " + score(p) + "；范围 " + hz.zone() + " known=" + hz.known() + " "
                    + hz.village().map(x -> x.id() + "/" + x.origin()).orElse("-") + "，本村 " + v.id());
            near.forEach(x -> VillageZones.unknownForTest(x.id(), false));
            VillageZones.refresh(h.getLevel(), v);
            Mob pig2 = still(h.spawn(EntityType.PIG, 8, 2, 6));
            hit(pig2, melee(h, p), 1);
            h.assertTrue(score(p) == -1, "边界算出来后照常 −1，实际 " + score(p));
        } finally {
            cleanup(h, List.of(p), v);
        }
        h.succeed();
    }

    /** TC-T18-51：PvP 不计。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc51_pvp(GameTestHelper h) {
        FakePlayer p = player(h, "cd_pvp");
        FakePlayer q = player(h, "cd_pvp2");
        try {
            h.assertTrue(ConductEvents.judge(h.getLevel(), q, melee(h, p), true) == 0 && score(p) == 0, "PvP 不计");
        } finally {
            cleanup(h, List.of(p, q));
        }
        h.succeed();
    }

    /** TC-T18-52：治愈僵尸村民 +6；同一个原村民再被感染、再治愈不再给。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void tc52_cure_once(GameTestHelper h) {
        floor(h);
        FakePlayer p = player(h, "cd_cure");
        Difficulty before = h.getLevel().getDifficulty();
        try {
            ZombieVillager zv = still(h.spawn(EntityType.ZOMBIE_VILLAGER, 8, 2, 20));
            ((ZombieVillagerAccessor) zv).mythicAge$setConversionStarter(p.getUUID());
            ((ZombieVillagerAccessor) zv).mythicAge$finishConversion(h.getLevel());
            h.assertTrue(score(p) == 6, "治愈 +6，实际 " + score(p));
            Villager cured = h.getLevel().getEntitiesOfClass(Villager.class, new AABB(h.absolutePos(new BlockPos(8, 2, 20))).inflate(3)).get(0);
            h.getLevel().getServer().setDifficulty(Difficulty.HARD, true);
            Zombie z = still(h.spawn(EntityType.ZOMBIE, 4, 2, 20));
            z.killedEntity(h.getLevel(), cured);
            ZombieVillager again = h.getLevel().getEntitiesOfClass(ZombieVillager.class,
                    new AABB(h.absolutePos(new BlockPos(8, 2, 20))).inflate(3)).get(0);
            ((ZombieVillagerAccessor) again).mythicAge$setConversionStarter(p.getUUID());
            ((ZombieVillagerAccessor) again).mythicAge$finishConversion(h.getLevel());
            h.assertTrue(score(p) == 6, "同一个原村民第二次治愈不给，实际 " + score(p));
        } finally {
            h.getLevel().getServer().setDifficulty(before, true);
            cleanup(h, List.of(p));
        }
        h.succeed();
    }

    /** TC-T18-53 / 54 / 55：正向每日上限 12；在线刻回归；截断到 −100。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc53_cap_regression_clamp(GameTestHelper h) {
        floor(h);
        FakePlayer p = player(h, "cd_cap");
        try {
            for (int i = 0; i < 3; i++) {
                ConductEvents.reward(p, "rescue_resident", 6);
            }
            h.assertTrue(score(p) == 12, "每日正向上限 12，实际 " + score(p));
            MythData.get(p).setConductScore(-10);
            ConductService.tickOnline(p, 24000);
            h.assertTrue(score(p) == -8, "在线 24000 刻回归 2，实际 " + score(p));
            ConductService.tickOnline(p, 23980);
            ConductService.tickOnline(p, 20);
            h.assertTrue(score(p) == -6, "分两次累计也算");
            MythData.get(p).setConductScore(1);
            ConductService.tickOnline(p, 24000);
            h.assertTrue(score(p) == 0, "不越过 0");
            MythData.get(p).setConductScore(-95);
            Villager v = still(h.spawn(EntityType.VILLAGER, 8, 2, 20));
            hit(v, melee(h, p), 1000);
            h.assertTrue(score(p) == -100, "截断到 −100，实际 " + score(p));
        } finally {
            cleanup(h, List.of(p));
        }
        h.succeed();
    }

    // ───────────── 3.4 配置与持久化 ─────────────

    private static String configText(boolean enabled, int harmVillager) {
        return "{\"schemaVersion\":1,\"enabled\":" + enabled + ",\"events\":{\"harmVillager\":" + harmVillager + "}}";
    }

    /** TC-T18-60 / 61：总开关关闭不计、回归也停；热更新 harmVillager = −5 后下一次按 −5。 */
    @GameTest(template = T, batch = CFG)
    public static void tc60_config(GameTestHelper h) {
        floor(h);
        FakePlayer p = player(h, "cd_cfg");
        var holder = ModConfigs.conduct();
        var original = holder.current();
        try {
            holder.reload(configText(false, -2));
            Villager v = still(h.spawn(EntityType.VILLAGER, 8, 2, 20));
            hit(v, melee(h, p), 2);
            MythData.get(p).setConductScore(-10);
            ConductService.tickOnline(p, 24000);
            h.assertTrue(score(p) == -10, "关闭时不计也不回归，实际 " + score(p));
            holder.reload(configText(true, -5));
            Villager v2 = still(h.spawn(EntityType.VILLAGER, 4, 2, 20));
            hit(v2, melee(h, p), 2);
            h.assertTrue(score(p) == -15, "热更新后按 −5，实际 " + score(p));
        } finally {
            // 配置文件没动过（只用文本热更新），按文件恢复
            holder.reloadFile(ModConfigs.file(io.github.happysnappingturtle.mythicage.core.config.conduct.ConductSchema.INSTANCE));
            h.assertTrue(holder.current().equals(original), "配置已恢复");
            cleanup(h, List.of(p));
        }
        h.succeed();
    }

    /** TC-T18-62 / 63：账本写成 NBT 再读回，同窗口内杀死只补 −10；格式 4 的旧档读作空。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc62_restart(GameTestHelper h) {
        floor(h);
        FakePlayer p = player(h, "cd_restart");
        try {
            Villager v = still(h.spawn(EntityType.VILLAGER, 8, 2, 20));
            hit(v, melee(h, p), 2);
            PlayerMythData back = PlayerDataNbt.read(PlayerDataNbt.write(MythData.get(p)));
            h.assertTrue(back.conduct().equals(MythData.get(p).conduct()), "善恶状态往返相等");
            p.setData(io.github.happysnappingturtle.mythicage.player.ModAttachments.PLAYER_DATA, back);
            hit(v, melee(h, p), 1000);
            h.assertTrue(score(p) == -12, "读回后只补 −10，实际 " + score(p));
            var old = PlayerDataNbt.write(new PlayerMythData());
            old.getCompound("conduct").remove("incidents");
            old.getCompound("conduct").remove("onlineTicks");
            old.putInt("format", 4);
            PlayerMythData o = PlayerDataNbt.read(old);
            h.assertTrue(o.conduct().incidents().view().isEmpty() && o.conduct().onlineTicks() == 0, "旧档读作空");
        } finally {
            cleanup(h, List.of(p));
        }
        h.succeed();
    }
}
