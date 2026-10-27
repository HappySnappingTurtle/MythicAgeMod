package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.boss.ArenaHudPayload;
import io.github.happysnappingturtle.mythicage.content.LangKeys;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.FavorSourcePayload;
import io.github.happysnappingturtle.mythicage.player.MythDataCommand;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import io.github.happysnappingturtle.mythicage.power.TransientEntities;
import io.github.happysnappingturtle.mythicage.rite.AgeUnlockedPayload;
import io.github.happysnappingturtle.mythicage.rite.TrialService;
import io.github.happysnappingturtle.mythicage.rite.UnlockService;
import io.github.happysnappingturtle.mythicage.summon.SummonScreens;
import io.github.happysnappingturtle.mythicage.summon.SummonView;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * T24 阶段 ②：其余界面补齐与无障碍（单元测试用例文档 TC-T24-54～65）。神坛检视通用外壳的用例并入
 * {@code TempleGameTests.survey_payload_roundtrip}（TC-T24-50～53）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UiPolishGameTests {
    private static final String BATCH = "t24_ui_polish";

    private UiPolishGameTests() {
    }

    static ServerPlayer player(GameTestHelper h, String name) {
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
        BlockPos at = h.absolutePos(new BlockPos(1, 2, 1));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    /** TC-T24-54：神力的临时实体列在「临时效果」里：种类、个数、附近、剩余秒数；不占常驻槽位。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void summon_lists_temporary_effects(GameTestHelper h) {
        ServerPlayer p = player(h, "t24temp");
        ArmorStand a = EntityType.ARMOR_STAND.create(h.getLevel());
        ArmorStand b = EntityType.ARMOR_STAND.create(h.getLevel());
        BlockPos at = h.absolutePos(new BlockPos(1, 2, 1));
        a.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        b.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        h.getLevel().addFreshEntity(a);
        h.getLevel().addFreshEntity(b);
        TransientEntities.registerTransient(p, List.of(a, b), 200);
        SummonView v = SummonScreens.view(p);
        h.assertTrue(v.temps().size() == 1, "两个同种合成一行：" + v.temps());
        SummonView.Temp t = v.temps().get(0);
        h.assertTrue(t.entity().equals("minecraft:armor_stand") && t.count() == 2 && t.where() == SummonView.Where.NEAR,
                "种类、个数、附近：" + t);
        h.assertTrue(t.secondsLeft() > 0 && t.secondsLeft() <= 10, "剩余秒数：" + t.secondsLeft());
        h.assertTrue(v.occupied() == 0, "不占常驻槽位");
        a.discard();
        b.discard();
        h.succeed();
    }

    /** TC-T24-56、57、60：提示行的柱数、个人首杀；撞碎一根少一根；离开大厅清掉。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH, timeoutTicks = 300)
    public static void arena_hud_line(GameTestHelper h) {
        TrialArenaGameTests.Setup s = TrialArenaGameTests.setup(h, 41);
        ServerPlayer p = TrialArenaGameTests.inHall(h, s, "t24hud", 0, 6);
        TrialArenaGameTests.open(h, s, p);
        h.runAtTickTime(30, () -> {
            ArenaHudPayload hud = s.arena().hudForTest(p);
            h.assertTrue(hud.active() && hud.intact() == 8 && hud.total() == 8 && !hud.firstKill(), "8 / 8、首次挑战：" + hud);
            h.assertTrue(s.arena().hudShownForTest().contains(p.getUUID()), "推送过提示行");
            s.arena().breakPillar(h.getLevel(), 0, net.minecraft.core.Direction.NORTH);
            TrialService.recordKill(p, s.arena().spec().orElseThrow().trial());
            ArenaHudPayload after = s.arena().hudForTest(p);
            h.assertTrue(after.intact() == 7 && after.firstKill(), "撞碎一根 7 / 8、已首杀：" + after);
            p.moveTo(p.getX(), p.getY(), s.origin().getZ() + 60);
        });
        h.runAtTickTime(60, () -> {
            h.assertFalse(s.arena().hudShownForTest().contains(p.getUUID()), "离开大厅清掉提示行");
            TrialArenaGameTests.cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T24-58、59：复原原因与重新挑战提示都有三语文本。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void reset_reasons_have_text(GameTestHelper h) {
        for (String k : List.of("mythic_age.arena.reset.empty", "mythic_age.arena.reset.boss_missing",
                "mythic_age.arena.reset.spawn_failed", "mythic_age.arena.retry", "mythic_age.arena.left")) {
            h.assertTrue(LangKeys.all().containsKey(k), "缺语言键 " + k);
        }
        h.succeed();
    }

    /** TC-T24-61（服务端部分）：台账种类 → 来源语言键，且有三语文本。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void favor_source_key(GameTestHelper h) {
        String key = FavorSourcePayload.ledgerSource("greek_temple:minecraft:overworld:1,64,2");
        h.assertTrue(key.equals("mythic_age.favor.source.greek_temple"), key);
        h.assertTrue(LangKeys.all().containsKey(key), "来源有文本");
        h.succeed();
    }

    /** TC-T24-62：晋升界面「下一时代需要」带神庙等级的预告。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void next_age_lists_temple(GameTestHelper h) {
        ServerPlayer p = player(h, "t24next");
        AgeUnlockedPayload u = UnlockService.replay(p, Civilization.GREEK, Age.CLASSICAL);
        AgeUnlockedPayload.Next n = u.next().orElseThrow();
        h.assertTrue(n.age() == Age.HEROIC, "下一时代英雄");
        h.assertTrue(n.requirements().stream().anyMatch(c -> c.getContents() instanceof TranslatableContents t
                && t.getKey().equals("mythic_age.rite.preview_temple")), "有神庙等级预告：" + n.requirements());
        h.succeed();
    }

    /** TC-T24-63：委托的来源事件。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void quest_source_keys(GameTestHelper h) {
        h.assertTrue(QuestService.sourceKey("supply:project:3:minecraft:bricks").equals("mythic_age.quest.source.project"), "工程");
        h.assertTrue(QuestService.sourceKey("supply:food").equals("mythic_age.quest.source.food"), "口粮");
        h.assertTrue(QuestService.sourceKey("rescue:7").equals("mythic_age.quest.source.raid"), "敌袭");
        h.assertTrue(QuestService.sourceKey("unknown").isEmpty(), "认不出的不写");
        for (String k : List.of("project", "short", "food", "damage", "migrant", "raid")) {
            h.assertTrue(LangKeys.all().containsKey("mythic_age.quest.source." + k), "缺 " + k);
        }
        h.succeed();
    }

    /** TC-T24-64：命令反馈里的文明与时代是可翻译组件。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void command_names_translated(GameTestHelper h) {
        h.assertTrue(MythDataCommand.civName(Civilization.GREEK).getContents() instanceof TranslatableContents t
                && t.getKey().equals("mythic_age.civ.greek"), "文明");
        h.assertTrue(MythDataCommand.ageName(Age.HEROIC).getContents() instanceof TranslatableContents t
                && t.getKey().equals("mythic_age.age.heroic"), "时代");
        h.succeed();
    }

    /** TC-T24-65：竞技场与 Boss 的 7 个音效已注册、sounds.json 带字幕、三语有字幕文本。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void boss_sounds_have_subtitles(GameTestHelper h) throws Exception {
        JsonObject sounds;
        try (var in = UiPolishGameTests.class.getResourceAsStream("/assets/mythic_age/sounds.json")) {
            sounds = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (String id : List.of("arena.wall_warning", "arena.wall_move", "arena.pillar_break", "boss.minotaur.charge_windup",
                "boss.minotaur.roar", "boss.minotaur.throw", "boss.minotaur.stunned")) {
            h.assertTrue(PowerRegistries.sound(id) != null, "已注册 " + id);
            String sub = sounds.getAsJsonObject(id).get("subtitle").getAsString();
            h.assertTrue(LangKeys.all().containsKey(sub), "字幕文本 " + sub);
        }
        h.succeed();
    }
}
