package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxSteps;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.PlayerDataNbt;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * T4 玩家数据的集成测试（TC-T4-40 至 TC-T4-46）。
 * <p>
 * 使用 NeoForge 的 FakePlayer，而不是原版的 makeMockServerPlayerInLevel：后者的网络连接没有完成模组频道协商，
 * 玩家加入时 Curios 和本 mod 的同步包都会被拒绝（实测）。FakePlayer 的连接会忽略所有发包。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerDataGameTests {
    private static final String BATCH = "player_data";

    private PlayerDataGameTests() {
    }

    private static FakePlayer fakePlayer(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }

    /** TC-T4-40：填满所有字段的数据写成 NBT 再读回，完全相等。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void nbt_round_trip(GameTestHelper helper) {
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.GREEK).setAge(Age.HEROIC);
        d.civ(Civilization.GREEK).setFavor(175);
        d.civ(Civilization.NORSE).setAge(Age.ARCHAIC);
        d.civ(Civilization.NORSE).setFavor(12);
        d.firstKills().add("minotaur");
        d.discoveries().add("knossos");
        d.blueprints().add("harpe");
        d.recordForge("harpe");
        d.recordForge("harpe");
        d.setConductScore(-7);
        d.addConductEntry(new PlayerMythData.ConductEntry("harm_villager", -2, 1234));
        d.receipts().record(ReceiptBook.Scope.UNIQUE, "first-kill:minotaur");
        d.receipts().record(ReceiptBook.Scope.REQUEST, "req-b");
        d.receipts().record(ReceiptBook.Scope.REQUEST, "req-a");
        d.setDataVersion(42);
        PlayerMythData back = PlayerDataNbt.read(PlayerDataNbt.write(d));
        helper.assertTrue(d.equals(back), "往返后应完全相等：" + d + " vs " + back);
        helper.assertTrue(List.of("req-b", "req-a").equals(back.receipts().list(ReceiptBook.Scope.REQUEST)),
                "收据顺序应保持");
        helper.succeed();
    }

    /** TC-T4-41、TC-T4-42：未知文明、非法时代、负数或缺失的恩宠。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void nbt_tolerates_bad_entries(GameTestHelper helper) {
        CompoundTag civs = new CompoundTag();
        CompoundTag unknown = new CompoundTag();
        unknown.putLong("favor", 99);
        unknown.putString("age", "mythic");
        civs.put("atlantis2", unknown);
        CompoundTag greek = new CompoundTag();
        greek.putLong("favor", -5);
        greek.putString("age", "godlike");
        civs.put("greek", greek);
        CompoundTag egypt = new CompoundTag();
        egypt.putString("age", "classical");
        civs.put("egyptian", egypt);
        CompoundTag root = new CompoundTag();
        root.putInt("format", 1);
        root.put("civs", civs);

        PlayerMythData d = PlayerDataNbt.read(root);
        helper.assertTrue(d.civ(Civilization.GREEK).favor() == 0, "负数恩宠应按 0 读取");
        helper.assertTrue(d.civ(Civilization.GREEK).age() == Age.NONE, "非法时代应按未接触处理");
        helper.assertTrue(d.civ(Civilization.EGYPTIAN).favor() == 0, "缺失的恩宠应为 0");
        helper.assertTrue(d.civ(Civilization.EGYPTIAN).age() == Age.CLASSICAL, "其余字段应正常读取");
        helper.succeed();
    }

    /**
     * TC-T4-43：死亡后数据保留。重生时原版调用新玩家的 {@code restoreFrom(旧玩家, false)}，
     * NeoForge 在其中按 copyOnDeath 复制附件；这里直接走这条路径。
     */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void data_survives_death(GameTestHelper helper) {
        FakePlayer dead = fakePlayer(helper, "ma_dead");
        MythData.get(dead).civ(Civilization.GREEK).setAge(Age.ARCHAIC);
        MythData.transact(dead, Transactions.Request.of("death-test", TxSteps.addFavor(Civilization.GREEK, 50, 0)));
        FakePlayer reborn = fakePlayer(helper, "ma_reborn");
        reborn.restoreFrom(dead, false);
        PlayerMythData after = MythData.get(reborn);
        helper.assertTrue(after.civ(Civilization.GREEK).favor() == 50, "重生后恩宠应保留，实际 " + after);
        helper.assertTrue(after.civ(Civilization.GREEK).age() == Age.ARCHAIC, "重生后时代应保留");
        helper.assertTrue(after.receipts().contains(ReceiptBook.Scope.REQUEST, "death-test"), "重生后收据应保留");
        helper.succeed();
    }

    /** TC-T4-44、TC-T4-46：命令 add 走上限规则；set 不受上限约束。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void favor_commands(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper, "ma_cmd");
        var source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        var commands = helper.getLevel().getServer().getCommands();
        commands.performPrefixedCommand(source, "mythicage age set @s greek archaic");
        commands.performPrefixedCommand(source, "mythicage favor add @s greek 30");
        helper.assertTrue(MythData.get(player).civ(Civilization.GREEK).favor() == 30,
                "add 后应为 30，实际 " + MythData.get(player));
        helper.assertTrue(MythData.isDirty(player.getUUID()), "修改后应标记为需要同步");
        commands.performPrefixedCommand(source, "mythicage favor add @s greek 500");
        helper.assertTrue(MythData.get(player).civ(Civilization.GREEK).favor() == 100, "add 应截到上限 100");
        commands.performPrefixedCommand(source, "mythicage favor set @s greek 260");
        helper.assertTrue(MythData.get(player).civ(Civilization.GREEK).favor() == 260, "set 不受上限约束");
        helper.succeed();
    }

    /** TC-T4-45：同一 tick 内提交 3 次事务，只同步一次。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void one_sync_per_tick(GameTestHelper helper) {
        ServerPlayer player = fakePlayer(helper, "ma_sync");
        MythData.get(player).civ(Civilization.NORSE).setAge(Age.ARCHAIC);
        int before = MythData.syncCount(player.getUUID());
        for (int i = 0; i < 3; i++) {
            MythData.transact(player, Transactions.Request.of("sync-" + i, TxSteps.addFavor(Civilization.NORSE, 1, 0)));
        }
        helper.runAfterDelay(2, () -> {
            int synced = MythData.syncCount(player.getUUID()) - before;
            helper.assertTrue(synced == 1, "同一 tick 的 3 次修改应只同步 1 次，实际 " + synced);
            helper.assertTrue(MythData.get(player).civ(Civilization.NORSE).favor() == 3, "3 次事务都应生效");
            helper.succeed();
        });
    }
}
