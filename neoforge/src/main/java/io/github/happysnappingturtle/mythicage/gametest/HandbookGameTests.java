package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.bestiary.Bestiary;
import io.github.happysnappingturtle.mythicage.bestiary.BestiaryLoader;
import io.github.happysnappingturtle.mythicage.content.greek.GreekBestiary;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules;
import io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules.Tier;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.handbook.Handbook;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.UUID;

/**
 * T24 阶段 ③：《神话手札》发放与图鉴点亮（单元测试用例文档 TC-T24-70～83）。场地 17 × 16 × 36：玩家在 z = 4，
 * 生物在 z = 24（20 格外）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HandbookGameTests {
    private static final String BATCH = "t24_handbook";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private HandbookGameTests() {
    }

    static FakePlayer player(GameTestHelper h, String name) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 1, z, Blocks.STONE);
            }
        }
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 4))));
        p.getInventory().clearContent();
        OnlinePlayers.forTest(p, true);
        return p;
    }

    static <T extends Mob> T still(GameTestHelper h, EntityType<T> type, int x, int z) {
        T m = h.spawn(type, new BlockPos(x, 2, z));
        m.setNoAi(true);
        return m;
    }

    static Set<String> seen(FakePlayer p) {
        return MythData.get(p).discoveries();
    }

    static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id));
    }

    // ───────────── 手札发放 ─────────────

    /** TC-T24-70、71：第一次给一本手札并记收据；再给不重复。 */
    @GameTest(template = T, batch = BATCH)
    public static void handbook_given_once(GameTestHelper h) {
        FakePlayer p = player(h, "t24book");
        h.assertTrue(Handbook.giveOnce(p), "第一次给");
        h.assertTrue(MythData.get(p).receipts().contains(ReceiptBook.Scope.UNIQUE, Handbook.RECEIPT), "记收据");
        ItemStack book = p.getInventory().items.stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        h.assertTrue(BuiltInRegistries.ITEM.getKey(book.getItem()).toString().equals("patchouli:guide_book"), "是 Patchouli 书：" + book);
        h.assertTrue(book.getComponents().toString().contains("mythic_age:handbook"), "是本模组的手札：" + book.getComponents());
        h.assertFalse(Handbook.giveOnce(p), "第二次不给");
        h.assertTrue(p.getInventory().countItem(book.getItem()) == 1, "仍只有一本");
        OnlinePlayers.forTest(p, false);
        h.succeed();
    }

    /** TC-T24-73：背包满时掉在脚下，仍记收据。 */
    @GameTest(template = T, batch = BATCH)
    public static void handbook_dropped_when_full(GameTestHelper h) {
        FakePlayer p = player(h, "t24bookfull");
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            p.getInventory().items.set(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        h.assertTrue(Handbook.giveOnce(p), "照样给");
        h.assertTrue(MythData.get(p).receipts().contains(ReceiptBook.Scope.UNIQUE, Handbook.RECEIPT), "记收据");
        boolean dropped = !h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(3),
                e -> BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString().equals("patchouli:guide_book")).isEmpty();
        h.assertTrue(dropped, "掉在脚下");
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(3)).forEach(ItemEntity::discard);
        OnlinePlayers.forTest(p, false);
        h.succeed();
    }

    /** TC-T24-74：书 + 羽毛合成手札。 */
    @GameTest(template = T, batch = BATCH)
    public static void handbook_recipe(GameTestHelper h) {
        var r = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "handbook"));
        h.assertTrue(r.isPresent(), "有配方");
        ItemStack out = r.get().value().getResultItem(h.getLevel().registryAccess());
        h.assertTrue(BuiltInRegistries.ITEM.getKey(out.getItem()).toString().equals("patchouli:guide_book")
                && out.getComponents().toString().contains("mythic_age:handbook"), "产出是手札：" + out);
        h.succeed();
    }

    // ───────────── 图鉴点亮 ─────────────

    /** 点亮表与 GreekBestiary.ENTRIES 一致；召唤版、野外版、Boss 版共用一个条目（TC-T24-82）。 */
    @GameTest(template = T, batch = BATCH)
    public static void bestiary_table_matches(GameTestHelper h) {
        h.assertTrue(BestiaryLoader.table().entries().equals(GreekBestiary.ENTRIES), "条目一致：" + BestiaryLoader.table().entries());
        h.assertTrue(BestiaryLoader.table().entryOf("mythic_age:summon_minotaur").orElse("").equals("greek/minotaur")
                && BestiaryLoader.table().entryOf("mythic_age:minotaur_boss").orElse("").equals("greek/minotaur"), "弥诺陶洛斯共用条目");
        h.succeed();
    }

    /** TC-T24-75、76：20 格外看得见的怪鸟点亮目击；隔一堵墙的不点亮。 */
    @GameTest(template = T, batch = BATCH)
    public static void bestiary_sight_needs_line_of_sight(GameTestHelper h) {
        FakePlayer p = player(h, "t24sight");
        Mob hidden = still(h, (EntityType<Mob>) type("mythic_age:harpy"), 3, 24);
        for (int x = 0; x <= 6; x++) {
            for (int y = 2; y <= 6; y++) {
                h.setBlock(x, y, 22, Blocks.STONE);
            }
        }
        // 墙只挡左边：玩家看得见右边的怪鸟、看不见墙后的哈耳庇厄
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 4))));
        Mob bird = still(h, (EntityType<Mob>) type("mythic_age:stymphalian_bird"), 13, 24);
        Bestiary.sight(p);
        h.assertTrue(BestiaryRules.tier(seen(p), "greek/stymphalian_bird") == Tier.SIGHTED, "看得见的点亮目击");
        h.assertTrue(BestiaryRules.tier(seen(p), "greek/harpy") == Tier.NONE, "墙后的不点亮");
        hidden.discard();
        bird.discard();
        OnlinePlayers.forTest(p, false);
        h.succeed();
    }

    /** TC-T24-77、81：打一下点亮交战（顺带补上目击）；已到更高级再目击不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void bestiary_engage_on_hit(GameTestHelper h) {
        FakePlayer p = player(h, "t24engage");
        Mob harpy = still(h, (EntityType<Mob>) type("mythic_age:harpy"), 8, 8);
        harpy.hurt(h.getLevel().damageSources().playerAttack(p), 1.0F);
        h.assertTrue(BestiaryRules.tier(seen(p), "greek/harpy") == Tier.ENGAGED, "交战：" + BestiaryRules.tier(seen(p), "greek/harpy"));
        h.assertTrue(seen(p).contains("bestiary:greek/harpy:sighted"), "目击一并补上");
        h.assertFalse(Bestiary.record(p, harpy, Tier.SIGHTED), "再目击不算新级别");
        harpy.discard();
        OnlinePlayers.forTest(p, false);
        h.succeed();
    }

    /** TC-T24-78：玩家的狼杀死怪鸟，点亮到击败（按伤害归属）。 */
    @GameTest(template = T, batch = BATCH)
    public static void bestiary_defeat_by_pet(GameTestHelper h) {
        FakePlayer p = player(h, "t24pet");
        Wolf wolf = still(h, EntityType.WOLF, 6, 8);
        wolf.tame(p);
        Mob bird = still(h, (EntityType<Mob>) type("mythic_age:stymphalian_bird"), 8, 8);
        bird.hurt(h.getLevel().damageSources().mobAttack(wolf), 1000.0F);
        h.assertTrue(BestiaryRules.tier(seen(p), "greek/stymphalian_bird") == Tier.DEFEATED,
                "击败：" + BestiaryRules.tier(seen(p), "greek/stymphalian_bird"));
        wolf.discard();
        OnlinePlayers.forTest(p, false);
        h.succeed();
    }

    /** TC-T24-79：召唤出天马就点亮到驯服。 */
    @GameTest(template = T, batch = BATCH)
    public static void bestiary_pegasus_on_summon(GameTestHelper h) {
        FakePlayer p = player(h, "t24peg");
        SummonService.Result r = SummonService.summon(p, SummonUnits.card("greek:pegasus").orElseThrow(),
                Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 20))));
        h.assertTrue(r == SummonService.Result.OK, "召唤成功：" + r);
        h.assertTrue(BestiaryRules.tier(seen(p), "greek/pegasus") == Tier.DEFEATED, "天马点亮到驯服");
        h.getLevel().getEntitiesOfClass(Mob.class, new AABB(h.absolutePos(new BlockPos(8, 2, 20))).inflate(6)).forEach(Mob::discard);
        OnlinePlayers.forTest(p, false);
        h.succeed();
    }
}
