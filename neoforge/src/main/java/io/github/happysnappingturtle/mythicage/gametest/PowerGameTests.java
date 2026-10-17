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
import io.github.happysnappingturtle.mythicage.power.PowerAimPayload;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
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
 * {@code test_area}（以施法者为中心半径 4 的敌方，伤害 5）；阶段 ② ③ 另加大型、临时实体、产物、补播用的测试神力。
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

    /** 大型神力：以施法者为中心、持续上界 200 刻（护盾时长）。 */
    record Big(String id) implements GodPower {
        @Override
        public void execute(CastContext ctx) {
            EXECUTED.incrementAndGet();
        }
    }

    /** 产 10 只临时鸡（寿命 60 刻）。 */
    record Summon(String id) implements GodPower {
        @Override
        public void execute(CastContext ctx) {
            List<net.minecraft.world.entity.animal.Chicken> out = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                var c = EntityType.CHICKEN.create(ctx.level());
                c.moveTo(ctx.caster().getX() + (i % 5) - 2, ctx.caster().getY(), ctx.caster().getZ() + 3 + i / 5);
                c.setNoAi(true);
                ctx.level().addFreshEntity(c);
                out.add(c);
            }
            SUMMONED.add(out);
            io.github.happysnappingturtle.mythicage.power.TransientEntities.registerTransient(ctx.caster(), out, 60);
        }
    }

    /** 产一组 3 个盔甲架（产物）。 */
    record Product(String id) implements GodPower {
        @Override
        public void execute(CastContext ctx) {
            List<net.minecraft.world.entity.decoration.ArmorStand> out = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                var a = EntityType.ARMOR_STAND.create(ctx.level());
                a.moveTo(ctx.caster().getX() + i - 1, ctx.caster().getY(), ctx.caster().getZ() + 6);
                ctx.level().addFreshEntity(a);
                out.add(a);
            }
            PRODUCED.add(out);
            io.github.happysnappingturtle.mythicage.power.TransientEntities.registerProducts(ctx.caster(), id, out);
        }
    }

    static final List<List<net.minecraft.world.entity.animal.Chicken>> SUMMONED = new java.util.concurrent.CopyOnWriteArrayList<>();
    static final List<List<net.minecraft.world.entity.decoration.ArmorStand>> PRODUCED = new java.util.concurrent.CopyOnWriteArrayList<>();

    static PowerSpec big() {
        return PowerSpec.parse("""
                {"schemaVersion":1,"id":"test_big","civilization":"greek","form":"scroll","castTicks":0,"cooldownTicks":0,
                 "targetProfile":"WORLD_AREA","selfCentered":true,"large":true,"durationTicks":200}""", new ArrayList<>());
    }

    /** 非大型、以施法者为中心、持续上界 20 刻（补播用）。 */
    static PowerSpec linger() {
        return PowerSpec.parse("""
                {"schemaVersion":1,"id":"test_linger","civilization":"greek","form":"scroll","castTicks":0,"cooldownTicks":0,
                 "targetProfile":"WORLD_AREA","selfCentered":true,"durationTicks":20}""", new ArrayList<>());
    }

    @BeforeBatch(batch = BATCH)
    public static void before(ServerLevel level) {
        for (PowerSpec s : List.of(spec("test_hit", 10, "HOSTILE_1", false), spec("test_slow", 40, "HOSTILE_1", false),
                spec("test_area", 10, "HOSTILE_AREA", true), big(), spec("test_summon", 0, "PLACEMENT", true),
                spec("test_product", 0, "PLACEMENT", true), linger())) {
            PowerSpecLoader.overrideForTest(s.id().equals("test_summon") || s.id().equals("test_product")
                    ? new PowerSpec(s.id(), "", s.civ(), s.form(), 0, 0, s.range(), s.profile(), true, false, s.matrix(), 0, 0)
                    : s);
        }
        GodPowers.overrideForTest(new Hit("test_hit"));
        GodPowers.overrideForTest(new Hit("test_slow"));
        GodPowers.overrideForTest(new Area("test_area"));
        GodPowers.overrideForTest(new Big("test_big"));
        GodPowers.overrideForTest(new Summon("test_summon"));
        GodPowers.overrideForTest(new Product("test_product"));
        GodPowers.overrideForTest(new Big("test_linger"));
        io.github.happysnappingturtle.mythicage.power.PowerShields.clearForTest();
        io.github.happysnappingturtle.mythicage.power.TransientEntities.clearForTest();
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        PowerSpecLoader.overrideForTest(null);
        GodPowers.overrideForTest(null);
        PowerService.clearForTest();
        level.getServer().setPvpAllowed(true);
        io.github.happysnappingturtle.mythicage.power.PowerShields.clearForTest();
        io.github.happysnappingturtle.mythicage.power.TransientEntities.clearForTest();
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

    // ───────────── 共享约束（TC-T12-70～76） ─────────────

    private static void resetCooldowns(Vulnerable p) {
        data(p).civ(Civilization.GREEK).setPowerCooldownUntil(0);
        data(p).powerCooldowns().clear();
    }

    /** 瞄准 + 确认（吟唱 0 刻的神力下一刻就提交）。 */
    private static PowerReason cast(Vulnerable p) {
        PowerService.use(p, InteractionHand.MAIN_HAND);
        return PowerService.use(p, InteractionHand.MAIN_HAND);
    }

    private static Vulnerable at(GameTestHelper h, String name, String power, int x, int z) {
        Vulnerable p = caster(h, name, power, 5);
        BlockPos a = h.absolutePos(new BlockPos(x, 2, z));
        p.moveTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 0, 0);
        return p;
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void shield_blocks_large_powers(GameTestHelper h) {
        floor(h);
        Vulnerable a = at(h, "pw_big_a", "test_big", 8, 2);
        Vulnerable b = at(h, "pw_big_b", "test_big", 8, 22);
        h.assertTrue(cast(a) == PowerReason.OK, "第一个大型神力");
        h.runAfterDelay(3, () -> {
            h.assertTrue(PowerService.lastResultForTest(a).orElse(null) == PowerReason.COMMITTED, "已提交");
            PowerReason r = cast(b);
            h.assertTrue(r == PowerReason.SHIELDED, "20 格内被护盾拒绝：" + r);
            h.assertTrue(b.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 5, "不消耗");
            net.minecraft.world.phys.Vec3 far = a.position().add(40, 0, 0);
            h.assertTrue(io.github.happysnappingturtle.mythicage.power.PowerShields.remaining(h.getLevel(), far) == 0,
                    "40 格外不受护盾影响");
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 160)
    public static void transient_budget_and_expiry(GameTestHelper h) {
        floor(h);
        Vulnerable p = at(h, "pw_summon", "test_summon", 8, 4);
        SUMMONED.clear();
        cast(p);
        h.runAfterDelay(3, () -> {
            resetCooldowns(p);
            cast(p);
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(SUMMONED.size() == 2, "两次各 10 只：" + SUMMONED.size());
            List<net.minecraft.world.entity.animal.Chicken> first = SUMMONED.get(0);
            long removed = first.stream().filter(Entity::isRemoved).count();
            h.assertTrue(removed == 4 && first.subList(0, 4).stream().allMatch(Entity::isRemoved), "第一批最早的 4 只退场");
            h.assertTrue(io.github.happysnappingturtle.mythicage.power.TransientEntities.countForTest(p.getUUID()) == 16,
                    "账上 16 只");
        });
        h.runAfterDelay(90, () -> {
            h.assertTrue(SUMMONED.stream().flatMap(List::stream).allMatch(Entity::isRemoved), "60 刻后全部到期退场");
            h.assertTrue(io.github.happysnappingturtle.mythicage.power.TransientEntities.countForTest(p.getUUID()) == 0,
                    "账已清空");
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH)
    public static void expired_transient_not_loaded(GameTestHelper h) {
        var c = EntityType.CHICKEN.create(h.getLevel());
        BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
        c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putUUID("owner", UUID.randomUUID());
        t.putLong("expires", WorldClock.now(h.getLevel().getServer()) - 1);
        t.putString("kind", "transient");
        c.getPersistentData().put("mythic_age:power", t);
        h.getLevel().addFreshEntity(c);
        h.assertTrue(h.getLevel().getEntity(c.getUUID()) == null, "载入时已过期的临时实体不进入世界（TC-T12-73）");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 60)
    public static void product_group_replaced(GameTestHelper h) {
        floor(h);
        Vulnerable p = at(h, "pw_product", "test_product", 8, 4);
        PRODUCED.clear();
        cast(p);
        h.runAfterDelay(3, () -> {
            resetCooldowns(p);
            cast(p);
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(PRODUCED.size() == 2, "两组");
            h.assertTrue(PRODUCED.get(0).stream().allMatch(Entity::isRemoved), "旧组全部退场");
            h.assertTrue(PRODUCED.get(1).stream().noneMatch(Entity::isRemoved), "新组在");
            h.assertTrue(io.github.happysnappingturtle.mythicage.power.TransientEntities.productsForTest(p.getUUID(),
                    "test_product").size() == 3, "账上一组 3 个");
            PRODUCED.get(1).forEach(Entity::discard);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void temp_blocks_restore(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        UUID owner = UUID.randomUUID();
        BlockPos air = h.absolutePos(new BlockPos(2, 2, 2));
        BlockPos grass = h.absolutePos(new BlockPos(4, 1, 2));
        BlockPos chest = h.absolutePos(new BlockPos(6, 2, 2));
        BlockPos changed = h.absolutePos(new BlockPos(8, 2, 2));
        level.setBlock(grass, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        var glass = Blocks.GLASS.defaultBlockState();
        h.assertTrue(io.github.happysnappingturtle.mythicage.power.TempBlocks.place(level, air, glass, 40, owner, false)
                == io.github.happysnappingturtle.mythicage.power.TempBlocks.Result.PLACED, "空气上放");
        h.assertTrue(io.github.happysnappingturtle.mythicage.power.TempBlocks.place(level, grass, glass, 40, owner, false)
                == io.github.happysnappingturtle.mythicage.power.TempBlocks.Result.PLACED, "草方块上放");
        h.assertTrue(io.github.happysnappingturtle.mythicage.power.TempBlocks.place(level, chest, glass, 40, owner, false)
                == io.github.happysnappingturtle.mythicage.power.TempBlocks.Result.REFUSED_BLOCK_ENTITY, "箱子拒绝");
        io.github.happysnappingturtle.mythicage.power.TempBlocks.place(level, changed, glass, 40, owner, false);
        h.assertTrue(level.getBlockState(air).is(Blocks.GLASS) && level.getBlockState(grass).is(Blocks.GLASS), "已放下");
        // TC-T12-76：登记存档往返
        var data = io.github.happysnappingturtle.mythicage.power.TempBlocks.get(level.getServer());
        var back = io.github.happysnappingturtle.mythicage.power.TempBlocks.load(data.save(new net.minecraft.nbt.CompoundTag(),
                level.registryAccess()), level.registryAccess());
        h.assertTrue(back.entries().containsAll(data.entries()) && back.entries().size() == data.entries().size(),
                "登记往返一致");
        h.runAfterDelay(10, () -> level.setBlock(changed, Blocks.STONE.defaultBlockState(), 3));
        h.runAfterDelay(70, () -> {
            h.assertTrue(level.getBlockState(air).isAir(), "空气复原");
            h.assertTrue(level.getBlockState(grass).is(Blocks.GRASS_BLOCK), "草方块复原");
            h.assertTrue(level.getBlockState(chest).is(Blocks.CHEST), "箱子没动");
            h.assertTrue(level.getBlockState(changed).is(Blocks.STONE), "被改过的格不覆盖");
            h.succeed();
        });
    }

    // ───────────── 表现与瞄准预览（TC-T12-80～85） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void fx_payload_codec(GameTestHelper h) {
        List<Integer> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            many.add(i * 7);
        }
        PowerFxPayload fx = new PowerFxPayload("c1", "gp_gr_bolt", PowerFxPayload.Phase.CHANNEL_CANCEL, 42,
                ResourceLocation.withDefaultNamespace("overworld"), new Vec3(1.5, -2, 3.25), many, 1234L, -99L, 30,
                "INTERRUPTED_DAMAGE");
        h.assertTrue(fx.targets().size() == PowerFxPayload.MAX_TARGETS, "超过 16 个截断");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        PowerFxPayload.STREAM_CODEC.encode(buf, fx);
        h.assertTrue(PowerFxPayload.STREAM_CODEC.decode(buf).equals(fx), "表现包往返一致");
        for (PowerAimPayload a : List.of(PowerAimPayload.start("c2", spec("test_hit", 10, "HOSTILE_1", false)),
                PowerAimPayload.hover("c2", -1, false), PowerAimPayload.hover("c2", 77, true), PowerAimPayload.stop("c2"))) {
            FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
            PowerAimPayload.STREAM_CODEC.encode(b, a);
            h.assertTrue(PowerAimPayload.STREAM_CODEC.decode(b).equals(a), "瞄准包往返一致：" + a);
        }
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void fx_recipients_include_caster(GameTestHelper h) {
        Vulnerable p = caster(h, "pw_rcpt", "test_hit", 1);
        List<net.minecraft.server.level.ServerPlayer> to = PowerFx.recipients(h.getLevel(), p, p.position());
        h.assertTrue(to.contains(p), "施法者总在接收者里");
        h.assertTrue(to.stream().distinct().count() == to.size(), "不重复");
        h.succeed();
    }

    private static List<CustomPacketPayload> sent(Vulnerable p) {
        return PowerService.sessionForTest(p).map(s -> PowerFx.sentForTest(s.castId())).orElse(List.of());
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void aim_hover_and_channel_sequence(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_aim", "test_hit", 3);
        Mob zombie = mob(h, EntityType.ZOMBIE, 4, 9);
        Mob villager = mob(h, EntityType.VILLAGER, 12, 9);
        look(p, zombie);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        String castId = PowerService.sessionForTest(p).orElseThrow().castId();
        h.runAfterDelay(4, () -> {
            h.assertTrue(sent(p).getFirst() instanceof PowerAimPayload a && a.kind() == PowerAimPayload.Kind.START
                    && a.range() == 8, "先发 START：" + sent(p));
            h.assertTrue(sent(p).contains(PowerAimPayload.hover(castId, zombie.getId(), true)), "对准僵尸：合法");
            look(p, villager);
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(sent(p).contains(PowerAimPayload.hover(castId, villager.getId(), false)), "对准村民：不合法");
            look(p, zombie);
            h.assertTrue(PowerService.use(p, InteractionHand.MAIN_HAND) == PowerReason.OK, "确认");
            List<CustomPacketPayload> l = PowerFx.sentForTest(castId);
            h.assertTrue(l.get(l.size() - 2).equals(PowerAimPayload.stop(castId)), "确认后 STOP：" + l);
            h.assertTrue(l.getLast() instanceof PowerFxPayload f && f.phase() == PowerFxPayload.Phase.CHANNEL_START
                    && f.duration() == 10 && f.targets().equals(List.of(zombie.getId())) && f.caster() == p.getId(),
                    "CHANNEL_START 带吟唱刻数与目标：" + l.getLast());
        });
        h.runAfterDelay(30, () -> {
            List<CustomPacketPayload> l = PowerFx.sentForTest(castId);
            h.assertTrue(l.getLast() instanceof PowerFxPayload f && f.phase() == PowerFxPayload.Phase.COMMIT
                    && f.duration() == 0, "提交后 COMMIT：" + l.getLast());
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void aim_timeout_sends_stop(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_aimto", "test_hit", 3);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        String castId = PowerService.sessionForTest(p).orElseThrow().castId();
        h.succeedWhen(() -> {
            h.assertTrue(PowerService.lastResultForTest(p).orElse(null) == PowerReason.AIM_TIMEOUT, "等待瞄准超时");
            List<CustomPacketPayload> l = PowerFx.sentForTest(castId);
            h.assertTrue(!l.isEmpty() && l.getLast().equals(PowerAimPayload.stop(castId)), "超时也发 STOP：" + l);
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void channel_cancel_carries_reason(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_fxcancel", "test_slow", 3);
        Mob zombie = mob(h, EntityType.ZOMBIE, 8, 9);
        look(p, zombie);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        String castId = PowerService.sessionForTest(p).orElseThrow().castId();
        PowerService.use(p, InteractionHand.MAIN_HAND);
        h.runAfterDelay(5, () -> p.hurt(h.getLevel().damageSources().fellOutOfWorld(), 1));
        h.runAfterDelay(10, () -> {
            List<CustomPacketPayload> l = PowerFx.sentForTest(castId);
            h.assertTrue(l.getLast() instanceof PowerFxPayload f && f.phase() == PowerFxPayload.Phase.CHANNEL_CANCEL
                    && f.cancelReason().equals("INTERRUPTED_DAMAGE"), "打断发 CHANNEL_CANCEL：" + l);
            h.assertTrue(l.stream().noneMatch(x -> x instanceof PowerFxPayload f && f.phase() == PowerFxPayload.Phase.COMMIT),
                    "没有 COMMIT");
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void commit_replayed_while_live(GameTestHelper h) {
        floor(h);
        Vulnerable p = caster(h, "pw_replay", "test_linger", 3);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        String castId = PowerService.sessionForTest(p).orElseThrow().castId();
        PowerService.use(p, InteractionHand.MAIN_HAND);
        ChunkPos chunk = new ChunkPos(p.blockPosition());
        h.runAfterDelay(3, () -> {
            h.assertTrue(PowerService.lastResultForTest(p).orElse(null) == PowerReason.COMMITTED, "已提交");
            h.assertTrue(PowerFx.replay(p, h.getLevel(), chunk).contains(castId), "持续期内补发");
            h.assertTrue(PowerFx.sentForTest(castId).getLast() instanceof PowerFxPayload f
                    && f.phase() == PowerFxPayload.Phase.COMMIT && f.duration() > 0 && f.duration() < 20, "补发的是剩余时长");
            h.assertTrue(!PowerFx.replay(p, h.getLevel(), new ChunkPos(chunk.x + 3, chunk.z)).contains(castId),
                    "别的区块不补");
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(!PowerFx.replay(p, h.getLevel(), chunk).contains(castId), "过期后不补发");
            h.succeed();
        });
    }

    // ───────────── terrainEdits（TC-T12-77，单独一批：要临时改全局配置） ─────────────

    private static final String TERRAIN = "power_terrain";
    private static String savedWorldRules;

    @BeforeBatch(batch = TERRAIN)
    public static void beforeTerrain(ServerLevel level) {
        var holder = io.github.happysnappingturtle.mythicage.config.ModConfigs.worldRules();
        com.google.gson.JsonObject j = holder.schema().toJson(holder.current());
        j.addProperty("schemaVersion", holder.schema().currentVersion());
        savedWorldRules = j.toString();
        j.getAsJsonObject("godPowers").addProperty("terrainEdits", true);
        holder.reload(j.toString());
    }

    @AfterBatch(batch = TERRAIN)
    public static void afterTerrain(ServerLevel level) {
        io.github.happysnappingturtle.mythicage.config.ModConfigs.worldRules().reload(savedWorldRules);
    }

    @GameTest(template = T, batch = TERRAIN, timeoutTicks = 80)
    public static void terrain_edits_permanent_branch(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos perm = h.absolutePos(new BlockPos(2, 2, 2));
        BlockPos temp = h.absolutePos(new BlockPos(4, 2, 2));
        var glass = Blocks.GLASS.defaultBlockState();
        h.assertTrue(io.github.happysnappingturtle.mythicage.power.TempBlocks.place(level, perm, glass, 20, UUID.randomUUID(), true)
                == io.github.happysnappingturtle.mythicage.power.TempBlocks.Result.PERMANENT, "永久分支：直接放");
        h.assertTrue(io.github.happysnappingturtle.mythicage.power.TempBlocks.place(level, temp, glass, 20, UUID.randomUUID(), false)
                == io.github.happysnappingturtle.mythicage.power.TempBlocks.Result.PLACED, "没声明永久分支的仍是临时");
        h.runAfterDelay(40, () -> {
            h.assertTrue(level.getBlockState(perm).is(Blocks.GLASS), "永久方块不复原");
            h.assertTrue(level.getBlockState(temp).isAir(), "临时方块照常复原");
            h.succeed();
        });
    }
}
