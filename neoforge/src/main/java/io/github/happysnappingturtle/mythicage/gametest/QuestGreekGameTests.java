package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestCivRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.village.economy.ResidentDialogue;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * T21 阶段 ④：希腊的委托数据（单元测试用例文档 TC-T21-95～97）。用真实的希腊文明数据（{@code village_civ/greek.json} 的
 * {@code quests} 段）；三语对话键由 {@code check_content} 校验（TC-97），这里只看对话种类与键名。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestGreekGameTests {
    private static final String BATCH = "quest_greek";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final String TIN = "mythic_age:tin_ingot";
    private static final String FLATBREAD = "mythic_age:olive_flatbread";

    private QuestGreekGameTests() {
    }

    static net.minecraft.world.item.Item item(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id));
    }

    static String key(ResidentEntity r, GameTestHelper h) {
        return ((TranslatableContents) ResidentDialogue.line(h.getLevel().getServer(), r).getContents()).getKey();
    }

    /** 希腊数据读进来了：锡锭发委托（定量 3）、单价对照青铜匠收购价（3 锭 1 绿宝石）、救援不给绿宝石（D5）。 */
    @GameTest(template = T, batch = BATCH)
    public static void greek_quest_data(GameTestHelper h) {
        QuestCivRules r = VillageData.civ(GreekSoldierGameTests.GREEK).orElseThrow().quests();
        h.assertTrue(r.supplyItems().equals(Map.of(TIN, 3)), "锡锭定量 3：" + r.supplyItems());
        h.assertTrue(Math.ceil(3 * r.price(TIN) - 1.0E-9) == 1, "3 锭 1 绿宝石：" + r.price(TIN));
        h.assertTrue(Math.ceil(6 * r.price(FLATBREAD) - 1.0E-9) == 1, "6 张薄饼 1 绿宝石（同农人出售价）：" + r.price(FLATBREAD));
        h.assertTrue(r.rescueReward() == 0 && r.escortReward() == 3, "护送 3、救援 0");
        h.succeed();
    }

    /** TC-T21-95、96：希腊村青铜匠缺锡：发锡锭委托（需求 3），对话转「委托」；交 3 锭结算 1 绿宝石、贡献 3，对话不再是委托或缺料。 */
    @GameTest(template = T, batch = BATCH)
    public static void greek_tin_quest(GameTestHelper h) {
        VillageWorkGameTests.TV tv = GreekSoldierGameTests.village(h, Age.CLASSICAL, Map.of());
        try {
            ResidentEntity smith = QuestGameTests.spawn(h, tv, "greek:bronzesmith", 4, 10, false);
            QuestGameTests.spawn(h, tv, "greek:farmer", 2, 4, true);
            QuestGameTests.scan(h, tv);
            Quest q = QuestGameTests.only(h, tv, "supply:short:");
            h.assertTrue(q.target.equals(TIN) && q.need == 3, "锡锭 3：" + q.target + " " + q.need);
            h.assertTrue(ResidentDialogue.kind(h.getLevel().getServer(), smith) == ResidentDialogue.Kind.QUEST, "对话转委托");
            h.assertTrue(key(smith, h).equals("mythic_age.village.say.greek.bronzesmith.quest"), "青铜匠自己的一句：" + key(smith, h));
            FakePlayer a = QuestGameTests.player(h, "g96", 8, 12);
            a.getInventory().add(new ItemStack(item(TIN), 5));
            QuestGameTests.act(a, tv, q, QuestService.DELIVER);
            h.assertTrue(q.state == QuestState.SETTLED, "交 3 完成：" + q.state + "/" + q.reason);
            h.assertTrue(QuestGameTests.count(a, Items.EMERALD) == 1, "绿宝石 1：" + QuestGameTests.count(a, Items.EMERALD));
            h.assertTrue(QuestService.contribution(h.getLevel().getServer(), tv.id(), a.getUUID()) == 3, "贡献 3");
            ResidentDialogue.Kind k = ResidentDialogue.kind(h.getLevel().getServer(), smith);
            h.assertTrue(k != ResidentDialogue.Kind.QUEST && k != ResidentDialogue.Kind.SHORT, "有锡了：" + k);
            io.github.happysnappingturtle.mythicage.player.OnlinePlayers.forTest(a, false);
        } finally {
            GreekSoldierGameTests.cleanup(h, tv);
        }
        h.succeed();
    }

    /** 希腊村口粮不足：发口粮委托，农人说自己的「委托」；交 24 张橄榄薄饼补足底线 24，结算 4 绿宝石。 */
    @GameTest(template = T, batch = BATCH)
    public static void greek_food_quest(GameTestHelper h) {
        VillageWorkGameTests.TV tv = GreekSoldierGameTests.village(h, Age.CLASSICAL, Map.of());
        try {
            ResidentEntity farmer = QuestGameTests.spawn(h, tv, "greek:farmer", 2, 4, true);
            VillageEconomy.State s = VillageWorkGameTests.state(h, tv);
            VillageData.civ(GreekSoldierGameTests.GREEK).orElseThrow().rations().portions().keySet()
                    .forEach(item -> s.stock.take(item, s.stock.count(item), 0));
            QuestGameTests.scan(h, tv);
            Quest q = QuestGameTests.only(h, tv, "supply:food");
            h.assertTrue(q.need == 24, "补足底线 24 份：" + q.need);
            h.assertTrue(key(farmer, h).equals("mythic_age.village.say.greek.farmer.quest"), "农人自己的一句：" + key(farmer, h));
            FakePlayer a = QuestGameTests.player(h, "g97", 3, 6);
            a.getInventory().add(new ItemStack(item(FLATBREAD), 24));
            QuestGameTests.act(a, tv, q, QuestService.DELIVER);
            h.assertTrue(q.state == QuestState.SETTLED, "补足完成：" + q.state + "/" + q.reason);
            h.assertTrue(QuestGameTests.count(a, Items.EMERALD) == 4, "绿宝石 4：" + QuestGameTests.count(a, Items.EMERALD));
            io.github.happysnappingturtle.mythicage.player.OnlinePlayers.forTest(a, false);
        } finally {
            GreekSoldierGameTests.cleanup(h, tv);
        }
        h.succeed();
    }
}
