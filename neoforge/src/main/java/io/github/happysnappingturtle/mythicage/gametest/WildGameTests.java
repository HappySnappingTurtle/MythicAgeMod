package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import io.github.happysnappingturtle.mythicage.entity.centaur.WildCentaurEntity;
import io.github.happysnappingturtle.mythicage.wild.WildSpawning;
import io.github.happysnappingturtle.mythicage.wild.greek.CentaurCampFeature;
import io.github.happysnappingturtle.mythicage.wild.greek.GreekWild;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * T15 野外神话生物（TC-T15-10～16）。场地 17 × 36，地面在 y = 1。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildGameTests {
    private static final String BATCH = "wild";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private WildGameTests() {
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }
    }

    private static FakePlayer player(GameTestHelper h, String name, BlockPos at) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        Vec3 v = Vec3.atBottomCenterOf(h.absolutePos(at));
        p.moveTo(v.x, v.y, v.z, 0, 0);
        return p;
    }

    /** 一群半人马（第一只是首领）。 */
    private static List<WildCentaurEntity> group(GameTestHelper h, int n, BlockPos center) {
        UUID g = UUID.randomUUID();
        List<WildCentaurEntity> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            WildCentaurEntity c = h.spawn(GreekWild.CENTAUR.get(), center.offset(i * 2 - n, 0, 0));
            c.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(c.blockPosition()), MobSpawnType.STRUCTURE, null);
            c.joinGroup(g, h.absolutePos(center), i == 0);
            out.add(c);
        }
        return out;
    }

    /** TC-T15-10：敌对生物卡在村庄（原版钟）64 格内不生成，65 格外可以。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 40)
    public static void wild_spawn_avoids_villages(GameTestHelper h) {
        floor(h);
        h.setBlock(new BlockPos(8, 2, 2), Blocks.BELL);
        WildCreatureCard hostile = new WildCreatureCard("greek", "test_hostile", "minecraft:zombie",
                WildCreatureCard.Attitude.HOSTILE, WildCreatureCard.Spawn.NATURAL, List.of("minecraft:is_overworld"),
                5, 1, 1, 256, 1, 64);
        h.runAfterDelay(5, () -> {
            BlockPos bell = h.absolutePos(new BlockPos(8, 2, 2));
            h.assertTrue(!WildSpawning.villageAllows(h.getLevel(), bell.offset(0, 0, 30), hostile), "30 格：不生成");
            h.assertTrue(WildSpawning.villageAllows(h.getLevel(), bell.offset(0, 0, 66), hostile), "66 格：可以");
            WildCreatureCard neutral = new WildCreatureCard("greek", "test_neutral", "mythic_age:centaur",
                    WildCreatureCard.Attitude.NEUTRAL, WildCreatureCard.Spawn.STRUCTURE, List.of(), 0, 1, 1, 256, 1, 0);
            h.assertTrue(WildSpawning.villageAllows(h.getLevel(), bell.offset(0, 0, 3), neutral), "中立不排除");
            h.succeed();
        });
    }

    /** TC-T15-11：打群里的一只，全群以玩家为目标。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 40)
    public static void centaur_group_aggro(GameTestHelper h) {
        floor(h);
        List<WildCentaurEntity> g = group(h, 4, new BlockPos(8, 2, 18));
        FakePlayer p = player(h, "ma_wild_aggro", new BlockPos(8, 2, 4));
        g.get(2).hurt(h.getLevel().damageSources().playerAttack(p), 1);
        for (WildCentaurEntity c : g) {
            h.assertTrue(c.getTarget() == p && c.angryAt(p), "全群都以玩家为目标");
        }
        h.succeed();
    }

    /** TC-T15-12：没人招惹时不攻击玩家与村民；不让拴绳。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void centaur_neutral_by_default(GameTestHelper h) {
        floor(h);
        List<WildCentaurEntity> g = group(h, 3, new BlockPos(8, 2, 18));
        Villager v = h.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 10));
        player(h, "ma_wild_calm", new BlockPos(8, 2, 6));
        h.runAfterDelay(80, () -> {
            for (WildCentaurEntity c : g) {
                h.assertTrue(c.getTarget() == null, "不应有目标，实际 " + c.getTarget());
                h.assertTrue(!c.busy(), "不应拉弓或踢击");
            }
            h.assertTrue(v.isAlive() && v.getHealth() == v.getMaxHealth(), "村民没受伤");
            h.assertTrue(!g.get(0).canBeLeashed(), "野外半人马不让拴绳");
            h.succeed();
        });
    }

    /** TC-T15-13：首领被杀，其余成员放下仇恨并远离凶手。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 40)
    public static void centaur_leader_death_routs_group(GameTestHelper h) {
        floor(h);
        List<WildCentaurEntity> g = group(h, 3, new BlockPos(8, 2, 18));
        FakePlayer p = player(h, "ma_wild_leader", new BlockPos(8, 2, 4));
        g.get(1).hurt(h.getLevel().damageSources().playerAttack(p), 1);
        h.assertTrue(g.get(0).leader() && g.get(0).variantScale() == WildCentaurEntity.LEADER_SCALE, "首领 1.1 倍");
        g.get(0).hurt(h.getLevel().damageSources().playerAttack(p), 1000);
        for (WildCentaurEntity c : g.subList(1, g.size())) {
            h.assertTrue(c.getTarget() == null && !c.angryAt(p), "首领死后放下仇恨");
            h.assertTrue(c.fleeing(), "远离凶手");
        }
        h.succeed();
    }

    /** TC-T15-14：不主动打僵尸；僵尸打它，只有它自己反击。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void centaur_retaliates_against_mobs(GameTestHelper h) {
        floor(h);
        List<WildCentaurEntity> g = group(h, 2, new BlockPos(8, 2, 18));
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 6));
        z.setNoAi(true);
        z.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60));
        h.runAfterDelay(60, () -> {
            h.assertTrue(g.get(0).getTarget() == null && g.get(1).getTarget() == null, "不主动攻击僵尸");
            g.get(1).hurt(h.getLevel().damageSources().mobAttack(z), 1);
            h.assertTrue(g.get(1).getTarget() == z, "被打的那只反击");
            h.assertTrue(g.get(0).getTarget() == null, "别的成员不被卷进来");
            h.succeed();
        });
    }

    /** TC-T15-15：营地生成——营火点着、有箭靶，3–5 只半人马且恰有 1 只首领。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 40)
    public static void centaur_camp_generates(GameTestHelper h) {
        floor(h);
        BlockPos center = h.absolutePos(new BlockPos(8, 2, 18));
        CentaurCampFeature.build(h.getLevel(), center, h.getLevel().getRandom());
        int n = CentaurCampFeature.spawnGroup(h.getLevel(), center, h.getLevel().getRandom());
        h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.CAMPFIRE)
                && h.getLevel().getBlockState(center).getValue(CampfireBlock.LIT), "中央营火点着");
        int targets = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-7, -1, -7), center.offset(7, 3, 7))) {
            if (h.getLevel().getBlockState(p).is(Blocks.TARGET)) {
                targets++;
            }
        }
        h.assertTrue(targets == 3, "三个箭靶，实际 " + targets);
        List<WildCentaurEntity> cs = h.getLevel().getEntitiesOfClass(WildCentaurEntity.class, new AABB(center).inflate(10));
        h.assertTrue(n >= 3 && n <= 5 && cs.size() == n, "3–5 只，实际生成 " + n + "、找到 " + cs.size());
        h.assertTrue(cs.stream().filter(WildCentaurEntity::leader).count() == 1, "恰有 1 只首领");
        h.assertTrue(cs.stream().allMatch(c -> center.equals(c.camp()) && c.groupId() != null
                && c.groupId().equals(cs.get(0).groupId())), "同一群、同一营地");
        cs.forEach(c -> c.discard());
        h.succeed();
    }

    /** TC-T15-16：战利品表能掉半人马鬃弦与皮革。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 40)
    public static void centaur_loot(GameTestHelper h) {
        floor(h);
        WildCentaurEntity c = h.spawn(GreekWild.CENTAUR.get(), new BlockPos(8, 2, 18));
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "entities/centaur")));
        boolean sinew = false;
        boolean leather = false;
        for (int i = 0; i < 40; i++) {
            LootParams params = new LootParams.Builder(h.getLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, c)
                    .withParameter(LootContextParams.ORIGIN, c.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            for (ItemStack s : table.getRandomItems(params)) {
                sinew |= s.is(GreekWild.CENTAUR_SINEW.get());
                leather |= s.is(Items.LEATHER);
            }
        }
        c.discard();
        h.assertTrue(sinew && leather, "40 次里应掉过鬃弦与皮革：鬃弦 " + sinew + "、皮革 " + leather);
        h.succeed();
    }
}
