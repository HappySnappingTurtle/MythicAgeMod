package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.power.CastSession;
import io.github.happysnappingturtle.mythicage.core.power.CooldownRules;
import io.github.happysnappingturtle.mythicage.core.power.PowerReason;
import io.github.happysnappingturtle.mythicage.core.power.PowerSpec;
import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.core.power.TargetFacts;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.GodPowers;
import io.github.happysnappingturtle.mythicage.power.PowerItems;
import io.github.happysnappingturtle.mythicage.power.PowerService;
import io.github.happysnappingturtle.mythicage.power.PowerSpecLoader;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T12 阶段 ①：施放全流程（TC-T12-60～67）。测试神力只在本批次登记：
 * {@code test_hit}（单体、伤害 5、吟唱 10 刻、长冷却 100 刻、距离 8）、{@code test_slow}（同上但吟唱 40 刻）、
 * {@code test_area}（以施法者为中心半径 4 的敌方，伤害 5）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowerGameTests {
    private static final String BATCH = "power";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    static final AtomicInteger EXECUTED = new AtomicInteger();

    private PowerGameTests() {
    }

    static PowerSpec spec(String id, int cast, String profile, boolean self) {
        return PowerSpec.parse("""
                {"schemaVersion":1,"id":"%s","civilization":"greek","form":"scroll","castTicks":%d,"cooldownTicks":100,
                 "range":8,"targetProfile":"%s","selfCentered":%s,
                 "matrix":{"hostile":"affect","elite":"affect","boss":"affect","enemy_player":"pvp","neutral_animal":"affect"}}"""
                .formatted(id, cast, profile, self), new ArrayList<>());
    }

    record Hit(String id) implements GodPower {
        @Override
        public void execute(CastContext ctx) {
            EXECUTED.incrementAndGet();
            if (ctx.target() instanceof LivingEntity l) {
                l.hurt(ctx.level().damageSources().magic(), 5);
            }
        }
    }

    record Area(String id) implements GodPower {
        @Override
        public void execute(CastContext ctx) {
            EXECUTED.incrementAndGet();
            Set<UUID> own = TargetFactsCollector.ownVillages(ctx.caster());
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class,
                    ctx.caster().getBoundingBox().inflate(4))) {
                TargetFacts f = TargetFactsCollector.collect(ctx.caster(), e, own);
                if (!TargetClassifier.NONE.equals(TargetClassifier.reaction(ctx.spec(), f))) {
                    e.hurt(ctx.level().damageSources().magic(), 5);
                }
            }
        }
    }

    @BeforeBatch(batch = BATCH)
    public static void before(ServerLevel level) {
        for (PowerSpec s : List.of(spec("test_hit", 10, "HOSTILE_1", false), spec("test_slow", 40, "HOSTILE_1", false),
                spec("test_area", 10, "HOSTILE_AREA", true))) {
            PowerSpecLoader.overrideForTest(s);
        }
        GodPowers.overrideForTest(new Hit("test_hit"));
        GodPowers.overrideForTest(new Hit("test_slow"));
        GodPowers.overrideForTest(new Area("test_area"));
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        PowerSpecLoader.overrideForTest(null);
        GodPowers.overrideForTest(null);
        PowerService.clearForTest();
        level.getServer().setPvpAllowed(true);
    }

    // ───────────── 搭建 ─────────────

    /** 会受伤、也能伤害其他玩家的假玩家（NeoForge 的 FakePlayer 对一切伤害免疫）。 */
    static final class Vulnerable extends FakePlayer {
        Vulnerable(ServerLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public boolean isInvulnerableTo(DamageSource source) {
            return false;
        }

        @Override
        public boolean canHarmPlayer(Player other) {
            return true;
        }
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    private static Vulnerable caster(GameTestHelper h, String name, String power, int count) {
        Vulnerable p = new Vulnerable(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        BlockPos at = h.absolutePos(new BlockPos(8, 2, 4));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        p.getInventory().clearContent();
        p.getInventory().selected = 0;
        ItemStack s = new ItemStack(Items.PAPER, count);
        s.set(PowerItems.GOD_POWER.get(), power);
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        MythData.get(p).civ(Civilization.GREEK).setFavor(40);
        return p;
    }

    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, int x, int z) {
        T m = h.spawn(type, new BlockPos(x, 2, z));
        m.setNoAi(true);
        return m;
    }

    private static void look(Player p, Entity e) {
        p.lookAt(EntityAnchorArgument.Anchor.EYES, e.getEyePosition());
    }

    private static PlayerMythData data(Player p) {
        return MythData.get((net.minecraft.server.level.ServerPlayer) p);
    }

    // ───────────── 全流程（TC-T12-60） ─────────────

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void cast_full_flow(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_flow", "test_hit", 3);
        Mob zombie = mob(h, EntityType.ZOMBIE, 8, 9);
        look(p, zombie);
        float hp = zombie.getHealth();
        h.assertTrue(PowerService.use(p, InteractionHand.MAIN_HAND) == PowerReason.OK, "进入瞄准");
        h.assertTrue(PowerService.sessionForTest(p).map(CastSession::state).orElse(null) == CastSession.State.AIMING, "瞄准中");
        h.assertTrue(PowerService.use(p, InteractionHand.MAIN_HAND) == PowerReason.OK, "确认目标");
        h.assertTrue(PowerService.sessionForTest(p).map(CastSession::state).orElse(null) == CastSession.State.CHANNELING,
                "吟唱中");
        h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 3, "吟唱中不消耗");
        long start = WorldClock.now(h.getLevel().getServer());
        h.succeedWhen(() -> {
            h.assertTrue(PowerService.lastResultForTest(p).orElse(null) == PowerReason.COMMITTED, "等待提交");
            h.assertTrue(zombie.getHealth() == hp - 5, "僵尸受 5 点：" + zombie.getHealth());
            h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2, "扣 1 件");
            h.assertTrue(CooldownRules.remaining(data(p), Civilization.GREEK, "test_hit", start + 20) > 0, "已记冷却");
            h.assertTrue(data(p).civ(Civilization.GREEK).favor() == 40, "恩宠不变");
        });
    }

    // ───────────── 打断与拒绝（TC-T12-61～64） ─────────────

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void interrupted_by_damage(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_hurt", "test_slow", 3);
        Mob zombie = mob(h, EntityType.ZOMBIE, 8, 9);
        look(p, zombie);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        // 新建的服务端玩家有 60 刻出生保护，普通伤害被挡；用能穿透无敌的伤害来源
        h.runAfterDelay(5, () -> p.hurt(h.getLevel().damageSources().fellOutOfWorld(), 1));
        h.runAfterDelay(50, () -> {
            h.assertTrue(PowerService.lastResultForTest(p).orElse(null) == PowerReason.INTERRUPTED_DAMAGE,
                    "受伤打断：" + PowerService.lastResultForTest(p));
            h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 3, "不消耗");
            h.assertTrue(CooldownRules.remaining(data(p), Civilization.GREEK, "test_slow",
                    WorldClock.now(h.getLevel().getServer())) == 0, "无冷却");
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void interrupted_by_switch(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_switch", "test_slow", 3);
        Mob zombie = mob(h, EntityType.ZOMBIE, 8, 9);
        look(p, zombie);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        h.runAfterDelay(5, () -> p.getInventory().selected = 1);
        h.runAfterDelay(50, () -> {
            h.assertTrue(PowerService.lastResultForTest(p).orElse(null) == PowerReason.INTERRUPTED_SWITCH, "切换打断");
            h.assertTrue(p.getInventory().getItem(0).getCount() == 3, "不消耗");
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH)
    public static void friendly_targets_refused(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_friend", "test_hit", 3);
        Mob villager = mob(h, EntityType.VILLAGER, 8, 9);
        Wolf wolf = mob(h, EntityType.WOLF, 4, 9);
        wolf.tame(p);
        Mob golem = mob(h, EntityType.IRON_GOLEM, 12, 9);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        for (Entity e : List.of(villager, wolf, golem)) {
            look(p, e);
            PowerReason r = PowerService.use(p, InteractionHand.MAIN_HAND);
            h.assertTrue(r == PowerReason.INVALID_TARGET, e.getType() + " 应拒绝锁定：" + r);
            h.assertTrue(PowerService.sessionForTest(p).map(CastSession::state).orElse(null) == CastSession.State.AIMING,
                    "仍在瞄准");
        }
        h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 3, "不消耗");
        p.setShiftKeyDown(true);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        h.assertTrue(PowerService.sessionForTest(p).isEmpty(), "潜行右键取消瞄准");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void target_leaves_range(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_range", "test_slow", 3);
        Mob zombie = mob(h, EntityType.ZOMBIE, 8, 9);
        look(p, zombie);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        h.runAfterDelay(5, () -> {
            BlockPos far = h.absolutePos(new BlockPos(8, 2, 30));
            zombie.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        });
        h.runAfterDelay(50, () -> {
            h.assertTrue(PowerService.lastResultForTest(p).orElse(null) == PowerReason.TARGET_LOST,
                    "目标超距：" + PowerService.lastResultForTest(p));
            h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 3, "不消耗");
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void cooldown_refuses(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_cd", "test_hit", 3);
        long now = WorldClock.now(h.getLevel().getServer());
        CooldownRules.apply(data(p), Civilization.GREEK, "other_greek_power", now, 600, 600);
        h.assertTrue(PowerService.use(p, InteractionHand.MAIN_HAND) == PowerReason.ON_COOLDOWN, "同文明短冷却中");
        h.assertTrue(PowerService.sessionForTest(p).isEmpty(), "不进入瞄准");
        h.succeed();
    }

    // ───────────── 范围与 PvP（TC-T12-66、67） ─────────────

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void area_hits_only_enemies(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_area", "test_area", 1);
        Mob zombie = mob(h, EntityType.ZOMBIE, 10, 4);
        Mob villager = mob(h, EntityType.VILLAGER, 6, 4);
        Wolf wolf = mob(h, EntityType.WOLF, 8, 6);
        wolf.tame(p);
        Mob golem = mob(h, EntityType.IRON_GOLEM, 8, 2);
        float zh = zombie.getHealth();
        float vh = villager.getHealth();
        float wh = wolf.getHealth();
        float gh = golem.getHealth();
        PowerService.use(p, InteractionHand.MAIN_HAND);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        h.succeedWhen(() -> {
            h.assertTrue(PowerService.lastResultForTest(p).orElse(null) == PowerReason.COMMITTED, "等待提交");
            h.assertTrue(zombie.getHealth() < zh, "僵尸受伤");
            h.assertTrue(villager.getHealth() == vh && wolf.getHealth() == wh && golem.getHealth() == gh,
                    "村民、自己的狼、铁傀儡不受伤");
        });
    }

    /** 另一名玩家不加进世界（假玩家加进世界会被当成在线玩家），直接检查分类与反应键。 */
    @GameTest(template = T, batch = BATCH)
    public static void pvp_switch(GameTestHelper h) {
        Vulnerable p = caster(h, "pw_pvp", "test_hit", 3);
        Vulnerable other = new Vulnerable(h.getLevel(), new GameProfile(UUID.randomUUID(), "pw_victim"));
        PowerSpec spec = PowerSpecLoader.get("test_hit").orElseThrow();
        h.getLevel().getServer().setPvpAllowed(false);
        String off = TargetClassifier.reaction(spec, TargetFactsCollector.collect(p, other, Set.of()));
        h.getLevel().getServer().setPvpAllowed(true);
        String on = TargetClassifier.reaction(spec, TargetFactsCollector.collect(p, other, Set.of()));
        h.assertTrue("none".equals(off) && "pvp".equals(on), "PvP 关 none、开 pvp：" + off + " / " + on);
        h.succeed();
    }

    /** TC-T12-23：冷却字段存档往返；格式 2 的旧档缺这些字段 → 0。 */
    @GameTest(template = T, batch = BATCH)
    public static void cooldowns_persist(GameTestHelper h) {
        PlayerMythData d = new PlayerMythData();
        CooldownRules.apply(d, Civilization.GREEK, "test_hit", 1000, 600, 1200);
        net.minecraft.nbt.CompoundTag tag = io.github.happysnappingturtle.mythicage.player.PlayerDataNbt.write(d);
        PlayerMythData back = io.github.happysnappingturtle.mythicage.player.PlayerDataNbt.read(tag);
        h.assertTrue(back.equals(d) && back.civ(Civilization.GREEK).powerCooldownUntil() == 1600
                && back.powerCooldowns().get("test_hit") == 2200, "往返一致");
        tag.remove("powerCooldowns");
        tag.getCompound("civs").getCompound("greek").remove("powerCooldownUntil");
        tag.putInt("format", 2);
        PlayerMythData old = io.github.happysnappingturtle.mythicage.player.PlayerDataNbt.read(tag);
        h.assertTrue(old.civ(Civilization.GREEK).powerCooldownUntil() == 0 && old.powerCooldowns().isEmpty(), "旧档缺省 0");
        h.succeed();
    }
}
