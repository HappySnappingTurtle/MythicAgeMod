package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import io.github.happysnappingturtle.mythicage.core.runtime.SettlementEngine;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.runtime.PlayerActivity;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeDriver;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers;
import io.github.happysnappingturtle.mythicage.runtime.TestProducers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * T5 运行时钟的集成测试（TC-T5-40 至 TC-T5-46）。主人用 FakePlayer，活跃状态通过 {@link PlayerActivity} 设定；
 * 测试产出点默认不真实运行，因此走快照。每个用例用自己的 id 和主人，与驱动器的后台结算互不干扰。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RuntimeGameTests {
    private static final String BATCH = "runtime";
    /** 热更配置会影响全服，单独成批，避免与其他用例同时运行。 */
    private static final String CONFIG_BATCH = "runtime_config";
    private static final double RATE = 48;

    private RuntimeGameTests() {
    }

    private static FakePlayer owner(ServerLevel level, String name) {
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), name));
        MythData.get(p).civ(Civilization.GREEK).setAge(Age.ARCHAIC);
        PlayerActivity.markActive(p);
        return p;
    }

    /** 已验证、快照 48/日的测试产出点，lastSettledTick = now − ago。 */
    private static ProducerLedger ledger(MinecraftServer server, String name, FakePlayer owner, long ago) {
        long now = WorldClock.now(server);
        ProducerLedger l = new ProducerLedger(new ProducerId(TestProducers.TYPE + ":gt-" + name + "-" + UUID.randomUUID()),
                owner.getUUID(), Civilization.GREEK, RATE, now - ago);
        l.recordObservation(RATE);
        return RuntimeLedgers.get(server).register(l);
    }

    private static long greek(FakePlayer p) {
        return MythData.get(p).civ(Civilization.GREEK).favor();
    }

    private static void cleanup(MinecraftServer server, FakePlayer p, ProducerLedger l) {
        RuntimeLedgers.get(server).remove(l.id());
        PlayerActivity.forget(p.getUUID());
    }

    /** TC-T5-40：/time set 与直接设置 dayTime 都不改变 gameTime。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void time_set_keeps_game_time(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        long before = WorldClock.now(server);
        CommandSourceStack op = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        server.getCommands().performPrefixedCommand(op, "time set night");
        server.overworld().setDayTime(server.overworld().getDayTime() + 10000);
        helper.assertTrue(WorldClock.now(server) == before, "gameTime 不应因 /time set 或 setDayTime 改变");
        helper.succeed();
    }

    /** TC-T5-41：驱动器实际运行——活跃主人、未真实运行的 48/日产出点，推进 500 tick 后恩宠 +1。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH, timeoutTicks = 700)
    public static void driver_credits_snapshot(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        FakePlayer p = owner(helper.getLevel(), "rt_snapshot");
        ProducerLedger l = ledger(server, "snapshot", p, 0);
        helper.runAfterDelay(520, () -> {
            PlayerActivity.markActive(p);
            helper.assertTrue(greek(p) == 1, "推进 500 tick 后应恰好入账 1 点，实际 " + greek(p));
            helper.assertTrue(MythData.isDirty(p.getUUID()) || MythData.syncCount(p.getUUID()) > 0, "入账后应同步");
            cleanup(server, p, l);
            helper.succeed();
        });
    }

    /** TC-T5-42：主人最近一次输入已超过宽限期 → 不入账。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void idle_owner_gets_nothing(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        FakePlayer p = owner(helper.getLevel(), "rt_idle");
        long now = WorldClock.now(server);
        int grace = ModConfigs.runtime().current().favor().idle().graceTicks();
        PlayerActivity.setLastActiveTick(p, now - grace - 1);
        ProducerLedger l = ledger(server, "idle", p, 24000);
        SettlementEngine.Settlement s = RuntimeDriver.settle(server, RuntimeLedgers.get(server), l, now,
                ModConfigs.runtime().current().favor());
        helper.assertTrue(s.mode() == SettlementEngine.Mode.NONE, "挂机应为 NONE，实际 " + s.mode());
        helper.assertTrue(greek(p) == 0, "挂机不应入账");
        helper.assertTrue(l.lastSettledTick() == now, "这段时间作废，lastSettledTick 仍推进");
        cleanup(server, p, l);
        helper.succeed();
    }

    /** TC-T5-43：主人在另一维度（活跃）仍按快照入账。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void owner_in_other_dimension(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "需要下界维度");
        FakePlayer p = owner(nether, "rt_nether");
        ProducerLedger l = ledger(server, "nether", p, 24000);
        SettlementEngine.Settlement s = RuntimeDriver.settle(server, RuntimeLedgers.get(server), l,
                WorldClock.now(server), ModConfigs.runtime().current().favor());
        helper.assertTrue(s.mode() == SettlementEngine.Mode.SNAPSHOT, "应为 SNAPSHOT，实际 " + s.mode());
        helper.assertTrue(greek(p) == 48, "应入账 48，实际 " + greek(p));
        cleanup(server, p, l);
        helper.succeed();
    }

    /** TC-T5-44：台账写入 SavedData 再读回，全部字段一致；坏条目被跳过。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void ledgers_round_trip(GameTestHelper helper) {
        ProducerLedger l = new ProducerLedger(new ProducerId("shrine:minecraft:overworld:1,2,3"),
                UUID.randomUUID(), Civilization.NORSE, 60, 1000);
        l.recordObservation(40);
        l.setLastSettledTick(5000);
        l.setLastVisitTick(4000);
        l.setCarry(0.25);
        l.setOfflineBuffer(12.5);
        l.setNearbySinceTick(3000);
        l.setRealStreak(2000, 7.5);
        l.setAwayActiveTicks(600);
        l.invalidate();
        RuntimeLedgers data = new RuntimeLedgers();
        data.register(l);
        CompoundTag tag = data.save(new CompoundTag(), helper.getLevel().registryAccess());
        CompoundTag broken = new CompoundTag();
        broken.putString("id", "test:broken");
        broken.putString("civ", "atlantis2");
        tag.getList("ledgers", 10).add(broken);
        RuntimeLedgers back = RuntimeLedgers.load(tag, helper.getLevel().registryAccess());
        helper.assertTrue(back.all().size() == 1, "坏条目应被跳过");
        ProducerLedger r = back.get(l.id()).orElseThrow();
        helper.assertTrue(l.equals(r), "往返后应完全相等：" + l + " vs " + r);
        helper.succeed();
    }

    /** TC-T5-46：失效后不再入账；删除后驱动器不再处理。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void invalidated_ledger_stops(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RuntimeLedgers ledgers = RuntimeLedgers.get(server);
        FakePlayer p = owner(helper.getLevel(), "rt_invalid");
        ProducerLedger l = ledger(server, "invalid", p, 24000);
        ledgers.invalidate(l.id());
        RuntimeDriver.settle(server, ledgers, l, WorldClock.now(server), ModConfigs.runtime().current().favor());
        helper.assertTrue(greek(p) == 0, "失效后不应入账");
        ledgers.remove(l.id());
        helper.assertTrue(ledgers.get(l.id()).isEmpty(), "删除后应查不到");
        PlayerActivity.forget(p.getUUID());
        helper.succeed();
    }

    /** TC-T5-45：热更 runtime.json 前按旧配置结算到当前时刻。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = CONFIG_BATCH)
    public static void reload_settles_with_old_config(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ConfigHolder<RuntimeConfig> holder = ModConfigs.runtime();
        RuntimeConfig original = holder.current();
        helper.assertTrue(original.favor().snapshot().rateFactor() == 1.0, "前提：rateFactor 为 1.0");
        FakePlayer p = owner(helper.getLevel(), "rt_reload");
        ProducerLedger l = ledger(server, "reload", p, 24000);
        long now = WorldClock.now(server);
        try {
            var outcome = holder.reload("{\"schemaVersion\":1,\"preset\":\"E\",\"favor\":{\"snapshot\":{\"rateFactor\":0.5}}}");
            helper.assertTrue(outcome.status() == ConfigHolder.Status.APPLIED, "热更应生效：" + outcome);
            helper.assertTrue(l.lastSettledTick() == now, "钩子应把台账结算到当前 tick");
            helper.assertTrue(greek(p) == 48, "旧区间应按 rateFactor 1.0 结算为 48，实际 " + greek(p));
            helper.assertTrue(holder.current().favor().snapshot().rateFactor() == 0.5, "新配置应生效");
        } finally {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), original));
            cleanup(server, p, l);
        }
        helper.assertTrue(holder.current().equals(original), "测试后应恢复原配置");
        helper.succeed();
    }

    /** 测试产出点真实运行时走 REAL，按产出点上报值入账。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void real_running_uses_reported_yield(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        FakePlayer p = owner(helper.getLevel(), "rt_real");
        ProducerLedger l = ledger(server, "real", p, 12000);
        TestProducers.setRealRunning(l.id(), true);
        SettlementEngine.Settlement s = RuntimeDriver.settle(server, RuntimeLedgers.get(server), l,
                WorldClock.now(server), ModConfigs.runtime().current().favor());
        helper.assertTrue(s.mode() == SettlementEngine.Mode.REAL, "应为 REAL，实际 " + s.mode());
        helper.assertTrue(greek(p) == 24, "应入账 24，实际 " + greek(p));
        cleanup(server, p, l);
        helper.succeed();
    }
}
