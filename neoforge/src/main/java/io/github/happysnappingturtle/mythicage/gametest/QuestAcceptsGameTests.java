package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.quest.QuestView;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/** GA-7：服务端清单与发给客户端的负载、真实交付规则一致；沿用真实希腊口粮与物品组数据。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestAcceptsGameTests {
    private static final String BATCH = "quest_accepts";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private QuestAcceptsGameTests() {
    }

    private static void village(GameTestHelper h, String name, BiConsumer<VillageWorkGameTests.TV, FakePlayer> test) {
        VillageWorkGameTests.TV tv = GreekSoldierGameTests.village(h, Age.CLASSICAL, Map.of());
        FakePlayer player = QuestGameTests.player(h, name, 3, 6);
        try {
            var s = VillageWorkGameTests.state(h, tv);
            VillageData.civ("greek").orElseThrow().rations().portions().keySet()
                    .forEach(item -> s.stock.take(item, s.stock.count(item), 0));
            test.accept(tv, player);
        } finally {
            QuestGameTests.cleanupPlayers(player);
            GreekSoldierGameTests.cleanup(h, tv);
        }
        h.succeed();
    }

    private static QuestView.Row row(FakePlayer player, VillageWorkGameTests.TV tv, Quest q) {
        return QuestService.view(player, tv.id()).orElseThrow().rows().stream()
                .filter(r -> r.id().equals(q.id())).findFirst().orElseThrow();
    }

    private static QuestPayloads.View roundTrip(QuestPayloads.View payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            QuestPayloads.View.STREAM_CODEC.encode(buf, payload);
            QuestPayloads.View decoded = QuestPayloads.View.STREAM_CODEC.decode(buf);
            if (buf.readableBytes() != 0) {
                throw new AssertionError("委托负载解码后仍有剩余字节");
            }
            return decoded;
        } finally {
            buf.release();
        }
    }

    /** TC-GA7-01、20：发布口粮委托，服务端快照和客户端负载均含口粮表全部六项及折算。 */
    @GameTest(template = T, batch = BATCH)
    public static void accepts_food_snapshot(GameTestHelper h) {
        village(h, "ga7food", (tv, player) -> {
            QuestGameTests.scan(h, tv);
            Quest q = QuestGameTests.only(h, tv, "supply:food");
            var expected = VillageData.civ("greek").orElseThrow().rations().portions().entrySet().stream()
                    .map(e -> new QuestView.Accept(e.getKey(), e.getValue())).toList();
            h.assertTrue(expected.size() == 6, "真实希腊口粮表应有六项");
            h.assertTrue(row(player, tv, q).accepts().equals(expected), "服务端六项和折算应逐项一致");
            QuestView view = QuestService.view(player, tv.id()).orElseThrow();
            QuestView decoded = roundTrip(new QuestPayloads.View(view)).view();
            h.assertTrue(decoded.equals(view), "客户端快照不丢字段");
            h.assertTrue(decoded.rows().stream().filter(r -> r.id().equals(q.id())).findFirst().orElseThrow()
                    .accepts().equals(expected), "发给客户端的六项和折算应逐项一致");
        });
    }

    /** TC-GA7-02：任意原木备料委托列出组内全部成员，保持组内顺序，每件算 1。 */
    @GameTest(template = T, batch = BATCH)
    public static void accepts_log_group(GameTestHelper h) {
        village(h, "ga7logs", (tv, player) -> {
            var expected = VillageData.civ("greek").orElseThrow().buildCosts().items("#logs").stream()
                    .map(item -> new QuestView.Accept(item, 1)).toList();
            h.assertTrue(expected.size() > 1, "原木组必须有多种成员");
            var stock = VillageWorkGameTests.state(h, tv).stock;
            expected.forEach(a -> stock.take(a.item(), stock.count(a.item()), 0));
            QuestGameTests.project(h, tv, "#logs", 8, QuestGameTests.today(h) - 20);
            QuestGameTests.scan(h, tv);
            Quest q = QuestGameTests.only(h, tv, "supply:project:");
            h.assertTrue(row(player, tv, q).accepts().equals(expected), "原木组全部成员及顺序、折算一致");
        });
    }

    /** TC-GA7-03：单一物品备料委托只有这一项。 */
    @GameTest(template = T, batch = BATCH)
    public static void accepts_single_item(GameTestHelper h) {
        village(h, "ga7single", (tv, player) -> {
            QuestGameTests.project(h, tv, "minecraft:diamond", 3, QuestGameTests.today(h));
            QuestGameTests.scan(h, tv);
            Quest q = QuestGameTests.only(h, tv, "supply:project:");
            h.assertTrue(row(player, tv, q).accepts().equals(List.of(new QuestView.Accept("minecraft:diamond", 1))),
                    "单一物品只有一项，每件算 1");
        });
    }

    /** TC-GA7-04：真实委托网络编解码覆盖空清单、单项、多于六项以及非整数折算，不截短、不乱序。 */
    @GameTest(template = T, batch = BATCH)
    public static void accepts_payload_roundtrip(GameTestHelper h) {
        List<QuestView.Accept> accepts = List.of(new QuestView.Accept("mythic_age:greek_travel_meal", 1.5),
                new QuestView.Accept("minecraft:cooked_cod", 0.5), new QuestView.Accept("minecraft:bread", 1),
                new QuestView.Accept("minecraft:oak_log", 1), new QuestView.Accept("minecraft:spruce_log", 1),
                new QuestView.Accept("minecraft:birch_log", 1), new QuestView.Accept("mythic_age:olive_log", 1));
        List<QuestView.Row> rows = List.of(codecRow("empty", List.of()), codecRow("single", accepts.subList(0, 1)),
                codecRow("many", accepts));
        var payload = new QuestPayloads.View(new QuestView(UUID.randomUUID(), AffairsView.Line.of("title"), rows, 2, 3, 5, true));
        h.assertTrue(roundTrip(payload).equals(payload), "完整负载往返一致，包括 accepts 顺序与 0.5 / 1.5");
        h.succeed();
    }

    private static QuestView.Row codecRow(String id, List<QuestView.Accept> accepts) {
        return new QuestView.Row(id, "supply", AffairsView.Line.of("title", "arg"), 3, 24, 2, "public",
                List.of("emerald", "contribution"), "open", AffairsView.Line.of(""), true, "", true, 4, "source", accepts);
    }

    /** TC-GA7-21：2 面包 + 2 熟鳕鱼收四件，实际口粮与进度增加三份，客户端的「还差」减少三份。 */
    @GameTest(template = T, batch = BATCH)
    public static void accepts_food_delivery(GameTestHelper h) {
        village(h, "ga7deliver", (tv, player) -> {
            QuestGameTests.scan(h, tv);
            Quest q = QuestGameTests.only(h, tv, "supply:food");
            QuestView.Row before = row(player, tv, q);
            player.getInventory().add(new ItemStack(Items.BREAD, 2));
            player.getInventory().add(new ItemStack(Items.COOKED_COD, 2));
            var result = QuestGameTests.act(player, tv, q, QuestService.DELIVER);
            QuestView.Row after = row(player, tv, q);
            h.assertTrue(result.ok() && result.delivered() == 4, "应交付四件：" + result);
            h.assertTrue(after.done() - before.done() == 3, "进度增加三份");
            h.assertTrue((before.need() - before.done()) - (after.need() - after.done()) == 3, "还差减少三份");
            h.assertTrue(VillageData.civ("greek").orElseThrow().rations().total(VillageWorkGameTests.state(h, tv).stock) == 3,
                    "村库实际得到三份口粮");
            h.assertTrue(QuestGameTests.count(player, Items.BREAD) == 0 && QuestGameTests.count(player, Items.COOKED_COD) == 0,
                    "面包和熟鳕鱼已从背包扣除");
        });
    }

    /** TC-GA7-22 / D1：只有小麦时不收、不推进，返回对应「可交清单」提示的 missing 结果。 */
    @GameTest(template = T, batch = BATCH)
    public static void accepts_wheat_rejected(GameTestHelper h) {
        village(h, "ga7wheat", (tv, player) -> {
            QuestGameTests.scan(h, tv);
            Quest q = QuestGameTests.only(h, tv, "supply:food");
            QuestView.Row before = row(player, tv, q);
            int wheat = VillageWorkGameTests.state(h, tv).stock.count("minecraft:wheat");
            player.getInventory().add(new ItemStack(Items.WHEAT, 8));
            var result = QuestGameTests.act(player, tv, q, QuestService.DELIVER);
            QuestView.Row after = row(player, tv, q);
            h.assertTrue(result.result().equals("missing") && result.delivered() == 0, "应提示没有可交清单中的物品：" + result);
            h.assertTrue(QuestGameTests.count(player, Items.WHEAT) == 8, "不扣小麦");
            h.assertTrue(VillageWorkGameTests.state(h, tv).stock.count("minecraft:wheat") == wheat, "村库不收小麦");
            h.assertTrue(after.done() == before.done() && after.need() == before.need(), "进度与还差均不变");
            h.assertTrue(after.accepts().stream().noneMatch(a -> a.item().equals("minecraft:wheat")), "可交清单不含小麦");
        });
    }
}
