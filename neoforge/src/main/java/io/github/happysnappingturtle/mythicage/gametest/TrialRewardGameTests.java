package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.boss.TrialArena;
import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import io.github.happysnappingturtle.mythicage.boss.TrialRewards;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.GreekKnossos;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.boss.ArenaPhase;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics;
import io.github.happysnappingturtle.mythicage.structure.RuinVaultBlockEntity;
import io.github.happysnappingturtle.mythicage.structure.RuinVaults;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.Setup;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.boss;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.cleanup;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.inHall;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.open;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.setup;

/**
 * T23 阶段 ③：结算与重试（单元测试用例文档 TC-T23-80～93）。击杀用参战者的攻击伤害源一次打死（参战者记「造成过伤害」）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrialRewardGameTests {
    private static final String BATCH = "trial_reward";
    private static final String T = GameTestTemplates.EMPTY_3X3;
    static final String TRIAL = "mythic_age:minotaur";

    private TrialRewardGameTests() {
    }

    static void kill(GameTestHelper h, Setup s, ServerPlayer by) {
        boss(h, s).hurt(h.getLevel().damageSources().playerAttack(by), 100000);
    }

    static PlayerMythData data(ServerPlayer p) {
        return MythData.get(p);
    }

    static int kills(ServerPlayer p) {
        return data(p).trialKills().getOrDefault(TRIAL, 0);
    }

    static int vouchers(ServerPlayer p) {
        return data(p).civ(Civilization.GREEK).vouchers(Age.CLASSICAL);
    }

    static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    static RuinVaultBlockEntity vault(GameTestHelper h, Setup s) {
        BlockPos v = LabyrinthGameTests.one(s.placed(), "vault");
        if (h.getLevel().getBlockEntity(v) instanceof RuinVaultBlockEntity be) {
            return be;
        }
        throw new GameTestAssertException("宝库方块不在 " + v);
    }

    /** TC-T23-80、85、86、91：一人打赢——记录、凭证、角、牛角、图纸、经验；宝库门开、走得到；有首杀记录能开宝库拿阿喀琉斯之甲。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void single_victory_settles(GameTestHelper h) {
        Setup s = setup(h, 51);
        ServerPlayer p = inHall(h, s, "gt_win", 0, 8);
        open(h, s, p);
        h.runAtTickTime(10, () -> kill(h, s, p));
        h.runAtTickTime(20, () -> {
            h.assertTrue(s.arena().phase() == ArenaPhase.VICTORY, "胜利");
            h.assertTrue(kills(p) == 1 && data(p).firstKills().contains(TRIAL), "击杀 1、首杀记录");
            h.assertTrue(vouchers(p) == 1, "古典试炼凭证 +1（英雄晋升用）：" + vouchers(p));
            h.assertTrue(count(p, GreekKnossos.MINOTAUR_HORN.get()) == 1, "弥诺陶洛斯之角 1");
            h.assertTrue(count(p, GreekKnossos.BULL_HORN.get()) == 2, "牛角 2");
            h.assertTrue(data(p).blueprints().contains("greek:harpe"), "哈耳珀图纸记录");
            h.assertTrue(p.totalExperience >= 100, "经验 100：" + p.totalExperience);
            h.assertTrue(LabyrinthGameTests.reachable(h.getLevel(), s.safe(), LabyrinthGameTests.one(s.placed(), "vault")),
                    "宝库门开了，走得到宝库");
            h.assertTrue(RuinVaults.open(p, vault(h, s)) == RuinVaults.Outcome.GIVEN, "有首杀记录：开宝库");
            h.assertTrue(count(p, GreekRelics.ARMOR_OF_ACHILLES.get()) == 1, "阿喀琉斯之甲");
            h.assertTrue(RuinVaults.open(p, vault(h, s)) == RuinVaults.Outcome.ALREADY, "每人只开一次");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-81、82、86：A 打过、B 在场满 30 秒 → 都记；C 在场 10 秒没打 → 不记，也开不了宝库（不写收据）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 900)
    public static void participants_by_damage_or_presence(GameTestHelper h) {
        Setup s = setup(h, 52);
        ServerPlayer a = inHall(h, s, "gt_pa", -3, 8);
        ServerPlayer b = inHall(h, s, "gt_pb", 3, 8);
        ServerPlayer[] c = new ServerPlayer[1];
        open(h, s, a);
        h.runAtTickTime(600, () -> c[0] = inHall(h, s, "gt_pc", 0, 10));
        h.runAtTickTime(640, () -> kill(h, s, a));
        h.runAtTickTime(650, () -> {
            h.assertTrue(kills(a) == 1, "A 打过：记");
            h.assertTrue(kills(b) == 1, "B 在场满 30 秒：记");
            h.assertTrue(kills(c[0]) == 0 && count(c[0], GreekKnossos.BULL_HORN.get()) == 0, "C 只在场 2 秒：不记");
            h.assertTrue(RuinVaults.open(c[0], vault(h, s)) == RuinVaults.Outcome.LOCKED, "C 开不了宝库");
            h.assertFalse(data(c[0]).receipts().contains(io.github.happysnappingturtle.mythicage.core.player.ReceiptBook.Scope.UNIQUE,
                    io.github.happysnappingturtle.mythicage.core.structure.VaultRules.receipt(h.getLevel().dimension().location().toString(),
                            vault(h, s).getBlockPos().getX(), vault(h, s).getBlockPos().getY(), vault(h, s).getBlockPos().getZ())),
                    "没写收据（以后打赢还能开）");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-84、87：死亡事件再来一次不重发；复原后再打一场：记录与凭证 +1、角再发、图纸不重复、宝库不能再开。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void repeat_fight_and_double_death(GameTestHelper h) {
        Setup s = setup(h, 53);
        ServerLevel level = h.getLevel();
        ServerPlayer p = inHall(h, s, "gt_rep", 0, 8);
        open(h, s, p);
        Mob[] first = new Mob[1];
        h.runAtTickTime(10, () -> {
            first[0] = boss(h, s);
            kill(h, s, p);
        });
        h.runAtTickTime(20, () -> {
            s.arena().onBossDeath(level, first[0]);
            h.assertTrue(TrialRewards.settle(level, s.arena()) == 0, "同一场再结算：不发");
            h.assertTrue(kills(p) == 1 && count(p, GreekKnossos.BULL_HORN.get()) == 2, "没有重发");
            h.assertTrue(RuinVaults.open(p, vault(h, s)) == RuinVaults.Outcome.GIVEN, "开宝库");
            s.arena().reset(level);
            h.assertTrue(s.arena().phase() == ArenaPhase.IDLE && s.arena().vaultOpen(), "复原后 IDLE，宝库门保持开着");
            open(h, s, p);
        });
        h.runAtTickTime(40, () -> kill(h, s, p));
        h.runAtTickTime(50, () -> {
            h.assertTrue(s.arena().fightId() == 2, "第 2 场");
            h.assertTrue(kills(p) == 2 && vouchers(p) == 2, "记录与凭证 +1");
            h.assertTrue(count(p, GreekKnossos.MINOTAUR_HORN.get()) == 2 && count(p, GreekKnossos.BULL_HORN.get()) == 4, "角再发");
            h.assertTrue(data(p).blueprints().stream().filter("greek:harpe"::equals).count() == 1, "图纸只一份");
            h.assertTrue(RuinVaults.open(p, vault(h, s)) == RuinVaults.Outcome.ALREADY, "宝库不能再开");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-88：结算时不在线的参战者：记进待发表，上线补发一次。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void offline_participant_gets_rewards_on_login(GameTestHelper h) {
        Setup s = setup(h, 54);
        ServerPlayer p = inHall(h, s, "gt_on", 0, 8);
        UUID away = UUID.randomUUID();
        open(h, s, p);
        h.runAtTickTime(10, () -> {
            s.arena().participants().damage(away, true);
            kill(h, s, p);
        });
        h.runAtTickTime(20, () -> {
            h.assertTrue(TrialRewards.get(h.getLevel().getServer()).pendingFor(away) == 1, "离线：待发 1 场");
            FakePlayer back = FakePlayerFactory.get(h.getLevel(), new GameProfile(away, "gt_away"));
            back.setGameMode(GameType.SURVIVAL);
            h.assertTrue(TrialRewards.deliver(back) == 1, "上线补发");
            h.assertTrue(kills(back) == 1 && count(back, GreekKnossos.BULL_HORN.get()) == 2, "补发了记录与物品");
            h.assertTrue(TrialRewards.deliver(back) == 0, "只补一次");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-89：战斗中存档往返（模拟重启）、Boss 还在 → 续战：阶段、参战者、窗口计数都在。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void restart_resumes_fight(GameTestHelper h) {
        Setup s = setup(h, 55);
        ServerLevel level = h.getLevel();
        ServerPlayer p = inHall(h, s, "gt_rs", 0, 8);
        open(h, s, p);
        h.runAtTickTime(10, () -> {
            boss(h, s).hurt(level.damageSources().playerAttack(p), 5);
            s.arena().windows().restore(3, 100);
            TrialArenas.get(level.getServer()).roundTripForTest(level.registryAccess(), s.arena().id());
        });
        h.runAtTickTime(20, () -> {
            TrialArena a = TrialArenas.get(level.getServer()).byId(s.arena().id()).orElseThrow();
            h.assertTrue(a != s.arena(), "读出了新实例");
            h.assertTrue(a.phase() == ArenaPhase.FIGHT_P1 && a.fightId() == 1, "阶段与场次");
            h.assertTrue(a.participants().legal(p.getUUID()), "参战者还在");
            h.assertTrue(a.windows().count() == 3, "窗口计数还在");
            h.assertTrue(a.boss(level).isPresent(), "Boss 还在");
            kill(h, s, p);
        });
        h.runAtTickTime(30, () -> {
            TrialArena a = TrialArenas.get(level.getServer()).byId(s.arena().id()).orElseThrow();
            h.assertTrue(a.phase() == ArenaPhase.VICTORY && kills(p) == 1, "续战后打赢照常结算");
            a.boss(level).ifPresent(Mob::discard);
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-90：Boss 实体丢失 600 刻 → 复原，不发奖励。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 900)
    public static void missing_boss_resets_without_reward(GameTestHelper h) {
        Setup s = setup(h, 56);
        ServerPlayer p = inHall(h, s, "gt_miss", 0, 8);
        open(h, s, p);
        h.runAtTickTime(10, () -> boss(h, s).discard());
        h.runAtTickTime(500, () -> h.assertTrue(s.arena().phase() == ArenaPhase.FIGHT_P1, "600 刻内不复原"));
        h.runAtTickTime(640, () -> {
            h.assertTrue(s.arena().phase() == ArenaPhase.IDLE, "复原：" + s.arena().phase());
            h.assertTrue(kills(p) == 0 && count(p, GreekKnossos.BULL_HORN.get()) == 0, "不发奖励");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-93：信物右键只提示，不重复写记录。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void token_only_reports(GameTestHelper h) {
        Setup s = setup(h, 57);
        ServerPlayer p = inHall(h, s, "gt_tok", 0, 8);
        open(h, s, p);
        h.runAtTickTime(10, () -> kill(h, s, p));
        h.runAtTickTime(20, () -> {
            ItemStack horn = new ItemStack(GreekKnossos.MINOTAUR_HORN.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, horn);
            horn.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(kills(p) == 1 && vouchers(p) == 1, "右键不写记录");
            cleanup(h, s);
            h.succeed();
        });
    }
}
