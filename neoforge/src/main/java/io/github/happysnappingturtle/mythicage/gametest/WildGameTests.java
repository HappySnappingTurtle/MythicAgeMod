package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import io.github.happysnappingturtle.mythicage.core.wild.GrabRules;
import io.github.happysnappingturtle.mythicage.entity.centaur.WildCentaurEntity;
import io.github.happysnappingturtle.mythicage.entity.hamadryad.HamadryadEntity;
import io.github.happysnappingturtle.mythicage.entity.hamadryad.RootSpikeEntity;
import io.github.happysnappingturtle.mythicage.entity.harpy.HarpyEntity;
import io.github.happysnappingturtle.mythicage.entity.karkinos.KarkinosEntity;
import io.github.happysnappingturtle.mythicage.entity.stymphalian.BronzeFeatherEntity;
import io.github.happysnappingturtle.mythicage.entity.stymphalian.StymphalianBirdEntity;
import io.github.happysnappingturtle.mythicage.wild.WildSpawning;
import io.github.happysnappingturtle.mythicage.wild.greek.CentaurCampFeature;
import io.github.happysnappingturtle.mythicage.wild.greek.GreekWild;
import io.github.happysnappingturtle.mythicage.wild.greek.HarpyNestFeature;
import io.github.happysnappingturtle.mythicage.wild.greek.StymphalianNestFeature;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BellBlock;
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
 * T15 野外神话生物（TC-T15-10～16、20～27、30～38、40～46、50～58）。场地 17 × 36，地面在 y = 1。
 * 怪鸟用例要白天：批次 wild_bird 每个用例开头设成正午；夜晚用例单独一批（改的是整个世界的时间）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildGameTests {
    private static final String BATCH = "wild";
    private static final String BIRD = "wild_bird";
    private static final String BIRD_NIGHT = "wild_bird_night";
    private static final String HARPY = "wild_harpy";
    private static final String CRAB = "wild_crab";
    private static final String CRAB_DAY = "wild_crab_day";
    private static final String DRYAD = "wild_dryad";
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

    // ───────────── 阶段 ② 斯廷法利斯怪鸟 ─────────────

    /** 把世界时间设成一天中的 t 刻（不倒退日期）。 */
    private static void timeOfDay(GameTestHelper h, long t) {
        long now = h.getLevel().getDayTime();
        h.getLevel().setDayTime(now - Math.floorMod(now, 24000L) + t);
    }

    /** 一群怪鸟停在 at 附近的空中（第一只是领头鸟），巢在 nest（相对坐标）。 */
    private static List<StymphalianBirdEntity> flock(GameTestHelper h, int n, BlockPos at, BlockPos nest) {
        UUID g = UUID.randomUUID();
        List<StymphalianBirdEntity> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            StymphalianBirdEntity b = h.spawn(GreekWild.STYMPHALIAN_BIRD.get(), at.offset(i % 3 - 1, 0, i / 3 - 1));
            b.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(b.blockPosition()), MobSpawnType.STRUCTURE, null);
            b.joinFlock(g, h.absolutePos(nest), i == 0);
            out.add(b);
        }
        return out;
    }

    /** 不会动、不怕晒的靶子僵尸。 */
    private static Zombie dummy(GameTestHelper h, BlockPos at) {
        Zombie z = h.spawn(EntityType.ZOMBIE, at);
        z.setNoAi(true);
        z.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60));
        return z;
    }

    private static int feathers(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(BronzeFeatherEntity.class, h.getBounds().inflate(2)).size();
    }

    private static void clearBirds(List<StymphalianBirdEntity> birds) {
        birds.forEach(b -> b.discard());
    }

    /** TC-T15-20：6 只怪鸟、目标在下方——抖羽 16 刻里不射，之后恰好 6 根青铜羽；冷却里不会再来一轮。 */
    @GameTest(template = T, batch = BIRD, timeoutTicks = 60)
    public static void stymphalian_volley(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        Zombie target = dummy(h, new BlockPos(8, 2, 18));
        List<StymphalianBirdEntity> fl = flock(h, 6, new BlockPos(8, 10, 18), new BlockPos(8, 2, 30));
        int n = fl.get(0).startVolley(target);
        h.assertTrue(n == 6, "6 只都参加，实际 " + n);
        h.assertTrue(fl.get(1).startVolley(target) == 0, "不是领头鸟不能发令");
        h.runAfterDelay(StymphalianBirdEntity.RATTLE_TICKS - 4, () -> {
            h.assertTrue(feathers(h) == 0, "抖羽中不该射出，实际 " + feathers(h));
            h.assertTrue(fl.stream().allMatch(b -> b.state() == StymphalianBirdEntity.State.RATTLE), "全群在抖羽");
        });
        h.runAfterDelay(StymphalianBirdEntity.RATTLE_TICKS + 3, () -> {
            h.assertTrue(feathers(h) == 6, "一轮恰好 6 根，实际 " + feathers(h));
            h.assertTrue(fl.get(0).startVolley(target) == 0, "冷却中不再齐射");
            clearBirds(fl);
            h.succeed();
        });
    }

    /** TC-T15-21：正面举盾挡住青铜羽；没举盾的挨 3 点（扣掉僵尸自带护甲）。 */
    @GameTest(template = T, batch = BIRD, timeoutTicks = 80)
    public static void bronze_feather_blocked_by_shield(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        StymphalianBirdEntity shooter = h.spawn(GreekWild.STYMPHALIAN_BIRD.get(), new BlockPos(8, 8, 32));
        shooter.setNoAi(true);
        Zombie shielded = dummy(h, new BlockPos(4, 2, 10));
        Zombie bare = dummy(h, new BlockPos(12, 2, 10));
        for (Zombie z : List.of(shielded, bare)) {
            z.setYRot(0);
            z.setYHeadRot(0);
            z.yBodyRot = 0;
        }
        shielded.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        shielded.startUsingItem(InteractionHand.OFF_HAND);
        h.runAfterDelay(10, () -> {
            h.assertTrue(shielded.isBlocking(), "举盾满 5 刻");
            for (Zombie z : List.of(shielded, bare)) {
                // 从正前方（南边 4 格）水平射来
                BronzeFeatherEntity f = new BronzeFeatherEntity(h.getLevel(), shooter, 3);
                f.setPos(z.getX(), z.getY() + 1.2, z.getZ() + 4);
                f.shoot(0, 0, -1, (float) StymphalianBirdEntity.FEATHER_SPEED, 0);
                h.getLevel().addFreshEntity(f);
            }
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(feathers(h) == 0, "两根都已命中");
            h.assertTrue(shielded.getHealth() == shielded.getMaxHealth(), "举盾不受伤，实际 " + shielded.getHealth());
            h.assertTrue(bare.getHealth() < bare.getMaxHealth() - 2.5F, "没举盾挨了约 3 点，实际 " + bare.getHealth());
            shooter.discard();
            h.succeed();
        });
    }

    /** TC-T15-22：24 格内敲钟——鸟群受惊高飞、4 秒内不齐射，4 秒后恢复；25 格外的鸟群不受影响。 */
    @GameTest(template = T, batch = BIRD, timeoutTicks = 140)
    public static void stymphalian_startled_by_bell(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        h.setBlock(new BlockPos(8, 2, 2), Blocks.BELL);
        Zombie target = dummy(h, new BlockPos(2, 2, 18));
        List<StymphalianBirdEntity> near = flock(h, 3, new BlockPos(8, 4, 14), new BlockPos(8, 2, 14));
        List<StymphalianBirdEntity> far = flock(h, 3, new BlockPos(8, 4, 31), new BlockPos(8, 2, 31));
        h.runAfterDelay(2, () -> {
            BlockPos bell = h.absolutePos(new BlockPos(8, 2, 2));
            h.assertTrue(((BellBlock) Blocks.BELL).attemptToRing(null, h.getLevel(), bell, net.minecraft.core.Direction.NORTH), "钟响了");
            h.assertTrue(near.stream().allMatch(StymphalianBirdEntity::startled), "12 格内的鸟受惊");
            h.assertTrue(far.stream().noneMatch(StymphalianBirdEntity::startled), "29 格外的鸟不受影响");
            h.assertTrue(near.get(0).startVolley(target) == 0, "受惊时不能齐射");
            h.assertTrue(far.get(0).startVolley(target) == 3, "远处鸟群照常齐射");
        });
        h.runAfterDelay(30, () -> {
            double y = h.absolutePos(new BlockPos(8, 4, 14)).getY();
            h.assertTrue(near.stream().allMatch(b -> b.getY() > y + 3), "受惊的鸟往高处飞");
        });
        h.runAfterDelay(StymphalianBirdEntity.STARTLE_TICKS + 4, () -> {
            h.assertTrue(near.stream().noneMatch(StymphalianBirdEntity::startled), "4 秒后不再受惊");
            h.assertTrue(near.get(0).startVolley(target) == 3, "恢复齐射");
            clearBirds(near);
            clearBirds(far);
            h.succeed();
        });
    }

    /** TC-T15-23：青铜响板同钟；冷却 6 秒内再摇无效。 */
    @GameTest(template = T, batch = BIRD, timeoutTicks = 40)
    public static void krotala_startles_birds(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        List<StymphalianBirdEntity> near = flock(h, 3, new BlockPos(8, 4, 14), new BlockPos(8, 2, 14));
        List<StymphalianBirdEntity> far = flock(h, 3, new BlockPos(8, 4, 31), new BlockPos(8, 2, 31));
        FakePlayer p = player(h, "ma_wild_krotala", new BlockPos(8, 2, 4));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GreekWild.KROTALA.get()));
        h.runAfterDelay(2, () -> {
            InteractionResult first = p.getMainHandItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult();
            h.assertTrue(first.consumesAction(), "第一次摇响成功，实际 " + first);
            h.assertTrue(near.stream().allMatch(StymphalianBirdEntity::startled), "10 格内的鸟受惊");
            h.assertTrue(far.stream().noneMatch(StymphalianBirdEntity::startled), "27 格外的鸟不受影响");
            h.assertTrue(p.getCooldowns().isOnCooldown(GreekWild.KROTALA.get()), "进入冷却");
        });
        h.runAfterDelay(10, () -> {
            InteractionResult again = p.getMainHandItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult();
            h.assertTrue(again == InteractionResult.FAIL, "冷却中再摇无效，实际 " + again);
            h.assertTrue(far.stream().noneMatch(StymphalianBirdEntity::startled), "无效的那次不影响任何鸟");
            clearBirds(near);
            clearBirds(far);
            h.succeed();
        });
    }

    /** TC-T15-24：领头鸟被射落——其余成员放下目标、不再齐射，飞回巢（D4，白天也回）。 */
    @GameTest(template = T, batch = BIRD, timeoutTicks = 120)
    public static void stymphalian_leader_death_routs_flock(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        Zombie target = dummy(h, new BlockPos(8, 2, 6));
        BlockPos nest = new BlockPos(8, 2, 28);
        List<StymphalianBirdEntity> fl = flock(h, 5, new BlockPos(8, 8, 12), nest);
        FakePlayer p = player(h, "ma_wild_bird_leader", new BlockPos(8, 2, 4));
        fl.forEach(b -> b.setTarget(target));
        Vec3 home = Vec3.atBottomCenterOf(h.absolutePos(nest));
        double before = fl.get(1).position().distanceTo(home);
        fl.get(0).hurt(h.getLevel().damageSources().playerAttack(p), 1000);
        h.assertTrue(!fl.get(0).isAlive(), "领头鸟死了");
        for (StymphalianBirdEntity b : fl.subList(1, fl.size())) {
            h.assertTrue(b.routed() && b.getTarget() == null && !b.rattling(), "溃散：放下目标、不抖羽");
        }
        h.runAfterDelay(80, () -> {
            List<StymphalianBirdEntity> rest = fl.subList(1, fl.size());
            h.assertTrue(rest.stream().noneMatch(StymphalianBirdEntity::leader), "溃散期间不推新的领头鸟");
            h.assertTrue(rest.stream().allMatch(b -> b.getTarget() == null), "不再找目标");
            double after = fl.get(1).position().distanceTo(home);
            h.assertTrue(after < before - 5, "往巢飞：" + before + " → " + after);
            h.assertTrue(feathers(h) == 0, "没有射羽");
            clearBirds(fl);
            h.succeed();
        });
    }

    /** TC-T15-25：夜里回巢栖息、放下目标、不攻击。 */
    @GameTest(template = T, batch = BIRD_NIGHT, timeoutTicks = 400)
    public static void stymphalian_roosts_at_night(GameTestHelper h) {
        timeOfDay(h, 18000);
        floor(h);
        Zombie target = dummy(h, new BlockPos(8, 2, 6));
        BlockPos nest = new BlockPos(8, 2, 22);
        List<StymphalianBirdEntity> fl = flock(h, 4, new BlockPos(8, 6, 18), nest);
        fl.forEach(b -> b.setTarget(target));
        h.succeedWhen(() -> {
            h.assertTrue(fl.stream().allMatch(StymphalianBirdEntity::perched), "全都落在巢上："
                    + fl.stream().map(b -> String.format("%s%s@%.2f,%.2f,%.2f v=%.3f", b.perched() ? "P" : "F", b.leader() ? "L" : "",
                    b.getX() - h.absolutePos(nest).getX(), b.getY() - h.absolutePos(nest).getY(), b.getZ() - h.absolutePos(nest).getZ(),
                    b.getDeltaMovement().length())).toList());
            h.assertTrue(fl.stream().allMatch(b -> b.getTarget() == null && b.state() == StymphalianBirdEntity.State.PERCH), "放下目标");
            h.assertTrue(fl.stream().allMatch(b -> b.blockPosition().distManhattan(h.absolutePos(nest)) <= 3), "在巢的落脚处");
            h.assertTrue(feathers(h) == 0 && target.getHealth() == target.getMaxHealth(), "没有攻击");
            clearBirds(fl);
            timeOfDay(h, 6000);
        });
    }

    /** TC-T15-26：鸟巢生成——枯树干、树顶草巢、树下插着青铜羽；5–8 只怪鸟落在巢上，恰有 1 只领头鸟（1.15 倍）。 */
    @GameTest(template = T, batch = BIRD, timeoutTicks = 40)
    public static void stymphalian_nest_generates(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        BlockPos base = h.absolutePos(new BlockPos(8, 2, 18));
        BlockPos nest = StymphalianNestFeature.build(h.getLevel(), base, h.getLevel().getRandom());
        int n = StymphalianNestFeature.spawnFlock(h.getLevel(), nest, h.getLevel().getRandom());
        h.assertTrue(nest.equals(base.above(StymphalianNestFeature.TRUNK + 1)), "落脚处在草巢上一格");
        h.assertTrue(h.getLevel().getBlockState(base).is(Blocks.DARK_OAK_LOG)
                && h.getLevel().getBlockState(nest.below(2)).is(Blocks.DARK_OAK_LOG), "枯树干");
        h.assertTrue(h.getLevel().getBlockState(nest.below()).is(Blocks.HAY_BLOCK), "树顶草巢");
        int stuck = 0;
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-6, -2, -6), base.offset(6, 3, 6))) {
            if (h.getLevel().getBlockState(q).is(GreekWild.STUCK_BRONZE_FEATHER.get())) {
                stuck++;
            }
        }
        h.assertTrue(stuck >= 4 && stuck <= 6, "插地青铜羽 4–6 根，实际 " + stuck);
        List<StymphalianBirdEntity> bs = h.getLevel().getEntitiesOfClass(StymphalianBirdEntity.class, new AABB(nest).inflate(4));
        h.assertTrue(n >= 5 && n <= 8 && bs.size() == n, "5–8 只，实际生成 " + n + "、找到 " + bs.size());
        h.assertTrue(bs.stream().filter(StymphalianBirdEntity::leader).count() == 1, "恰有 1 只领头鸟");
        h.assertTrue(bs.stream().filter(StymphalianBirdEntity::leader).allMatch(b -> b.variantScale() == StymphalianBirdEntity.LEADER_SCALE),
                "领头鸟 1.15 倍");
        h.assertTrue(bs.stream().allMatch(b -> nest.equals(b.nest()) && b.groupId() != null && b.groupId().equals(bs.get(0).groupId())),
                "同一群、同一个巢");
        clearBirds(bs);
        h.succeed();
    }

    /** TC-T15-27：战利品表掉 1–2 根青铜羽毛；挖插地青铜羽得 1 根。 */
    @GameTest(template = T, batch = BIRD, timeoutTicks = 40)
    public static void stymphalian_loot(GameTestHelper h) {
        floor(h);
        StymphalianBirdEntity b = h.spawn(GreekWild.STYMPHALIAN_BIRD.get(), new BlockPos(8, 4, 18));
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "entities/stymphalian_bird")));
        for (int i = 0; i < 20; i++) {
            LootParams params = new LootParams.Builder(h.getLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, b)
                    .withParameter(LootContextParams.ORIGIN, b.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            int count = 0;
            for (ItemStack s : table.getRandomItems(params)) {
                h.assertTrue(s.is(GreekWild.BRONZE_FEATHER.get()), "只掉青铜羽毛，实际 " + s);
                count += s.getCount();
            }
            h.assertTrue(count >= 1 && count <= 2, "1–2 根，实际 " + count);
        }
        b.discard();
        BlockPos at = new BlockPos(4, 2, 4);
        h.setBlock(at, GreekWild.STUCK_BRONZE_FEATHER.get());
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(h.getBlockState(at), h.getLevel(), h.absolutePos(at), null);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(GreekWild.BRONZE_FEATHER.get()) && drops.get(0).getCount() == 1,
                "插地青铜羽掉 1 根青铜羽毛，实际 " + drops);
        h.succeed();
    }

    // ───────────── 阶段 ③ 哈耳庇厄 ─────────────

    /** 一只哈耳庇厄，巢在 nest（相对坐标，可为空）。 */
    private static HarpyEntity harpy(GameTestHelper h, BlockPos at, @javax.annotation.Nullable BlockPos nest) {
        HarpyEntity e = h.spawn(GreekWild.HARPY.get(), at);
        e.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.STRUCTURE, null);
        e.joinFlock(UUID.randomUUID(), nest == null ? null : h.absolutePos(nest));
        return e;
    }

    /** 不会动、不怕晒的尸壳靶子。 */
    private static net.minecraft.world.entity.monster.Husk husk(GameTestHelper h, BlockPos at) {
        net.minecraft.world.entity.monster.Husk z = h.spawn(EntityType.HUSK, at);
        z.setNoAi(true);
        return z;
    }

    /** TC-T15-30：开阔地、目标无盾——尖啸 16 刻里不动手，之后俯冲抓起，升高不超过 3 格，20 刻后松手并记上免疫。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 200)
    public static void harpy_grabs_on_open_ground(GameTestHelper h) {
        floor(h);
        var target = husk(h, new BlockPos(8, 2, 18));
        HarpyEntity hp = harpy(h, new BlockPos(8, 8, 12), null);
        double groundY = target.getY();
        double[] maxY = {groundY};
        long[] grabbedAt = {-1};
        h.onEachTick(() -> {
            maxY[0] = Math.max(maxY[0], target.getY());
            if (grabbedAt[0] < 0 && target.getVehicle() == hp) {
                grabbedAt[0] = h.getLevel().getGameTime();
            }
        });
        hp.beginAttack(target);
        h.startSequence()
                .thenIdle(HarpyEntity.SCREECH_TICKS - 2)
                .thenExecute(() -> {
                    h.assertTrue(hp.state() == HarpyEntity.State.SCREECH, "尖啸中，实际 " + hp.state());
                    h.assertTrue(!target.isPassenger(), "预警期间不抓");
                })
                .thenWaitUntil(() -> h.assertTrue(target.getVehicle() == hp, "抓起目标"))
                .thenExecute(() -> {
                    h.assertTrue(hp.state() == HarpyEntity.State.CARRY, "抓着飞");
                    h.assertTrue(target.getY() >= groundY - 0.3, "没被按进地里：" + groundY + " → " + target.getY());
                })
                .thenWaitUntil(() -> h.assertTrue(!target.isPassenger(), "松手"))
                .thenExecute(() -> {
                    long held = h.getLevel().getGameTime() - grabbedAt[0];
                    h.assertTrue(Math.abs(held - GrabRules.HOLD_TICKS) <= 2, "约 20 刻后松手，实际 " + held);
                    h.assertTrue(maxY[0] - groundY <= GrabRules.MAX_LIFT + 0.3, "升高不超过 3 格，实际 " + (maxY[0] - groundY));
                    h.assertTrue(maxY[0] - groundY >= 1.5, "确实被提起来了，实际 " + (maxY[0] - groundY));
                    h.assertTrue(HarpyEntity.immuneUntil(target) >= grabbedAt[0] + GrabRules.IMMUNE_TICKS - 1, "记上 8 秒免疫");
                    h.assertTrue(target.fallDistance >= GrabRules.RELEASE_FALL, "松手时记上已下落距离");
                    hp.discard();
                    target.discard();
                })
                .thenSucceed();
    }

    /** TC-T15-31：目标还在免疫期——不抓，改爪击（受伤、被击退）。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 160)
    public static void harpy_respects_grab_immunity(GameTestHelper h) {
        floor(h);
        var target = husk(h, new BlockPos(8, 2, 18));
        target.getPersistentData().putLong(HarpyEntity.IMMUNE_KEY, h.getLevel().getGameTime() + GrabRules.IMMUNE_TICKS);
        HarpyEntity hp = harpy(h, new BlockPos(8, 8, 12), null);
        boolean[] carried = {false};
        h.onEachTick(() -> carried[0] |= target.isPassenger());
        hp.beginAttack(target);
        h.succeedWhen(() -> {
            h.assertTrue(target.getHealth() < target.getMaxHealth(), "挨了爪击");
            h.assertTrue(!carried[0], "免疫期内从没被抓起");
            hp.discard();
            target.discard();
        });
    }

    /** TC-T15-32：目标正面举盾——抓举失败，哈耳庇厄坠地眩晕 40 刻。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 160)
    public static void harpy_blocked_by_shield(GameTestHelper h) {
        floor(h);
        Zombie target = dummy(h, new BlockPos(8, 2, 14));
        target.setYRot(0);
        target.setYHeadRot(0);
        target.yBodyRot = 0;
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        target.startUsingItem(InteractionHand.OFF_HAND);
        HarpyEntity hp = harpy(h, new BlockPos(8, 8, 22), null);
        h.runAfterDelay(6, () -> hp.beginAttack(target));
        long[] stunnedAt = {-1};
        h.onEachTick(() -> {
            if (stunnedAt[0] < 0 && hp.state() == HarpyEntity.State.STUNNED) {
                stunnedAt[0] = h.getLevel().getGameTime();
            }
            h.assertTrue(!target.isPassenger(), "举盾时不能被抓起");
        });
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(stunnedAt[0] >= 0, "坠地眩晕"))
                .thenExecute(() -> h.assertTrue(target.getHealth() == target.getMaxHealth(), "举盾没受伤"))
                .thenIdle(HarpyEntity.STUN_TICKS - 4)
                .thenExecute(() -> h.assertTrue(hp.state() == HarpyEntity.State.STUNNED, "眩晕持续约 40 刻"))
                .thenIdle(8)
                .thenExecute(() -> {
                    h.assertTrue(hp.state() != HarpyEntity.State.STUNNED, "40 刻后恢复");
                    hp.discard();
                    target.discard();
                })
                .thenSucceed();
    }

    /** TC-T15-33：俯冲途中被击中——同样坠地眩晕，不抓。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 100)
    public static void harpy_hit_during_dive(GameTestHelper h) {
        floor(h);
        var target = husk(h, new BlockPos(8, 2, 4));
        HarpyEntity hp = harpy(h, new BlockPos(8, 12, 30), null);
        FakePlayer p = player(h, "ma_wild_harpy_hit", new BlockPos(2, 2, 4));
        hp.beginAttack(target);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(hp.state() == HarpyEntity.State.DIVE, "进入俯冲"))
                .thenExecute(() -> hp.hurt(h.getLevel().damageSources().playerAttack(p), 1))
                .thenExecute(() -> {
                    h.assertTrue(hp.state() == HarpyEntity.State.STUNNED, "被击中后眩晕，实际 " + hp.state());
                    h.assertTrue(!target.isPassenger(), "没有抓到");
                    hp.discard();
                    target.discard();
                })
                .thenSucceed();
    }

    /** TC-T15-34：崖巢 24 格内——不抓举，改为爪击 + 击退。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 160)
    public static void harpy_swipes_near_nest(GameTestHelper h) {
        floor(h);
        var target = husk(h, new BlockPos(8, 2, 18));
        HarpyEntity hp = harpy(h, new BlockPos(8, 8, 12), new BlockPos(8, 2, 30));
        boolean[] carried = {false};
        h.onEachTick(() -> carried[0] |= target.isPassenger());
        hp.beginAttack(target);
        h.succeedWhen(() -> {
            h.assertTrue(target.getHealth() < target.getMaxHealth(), "挨了爪击");
            h.assertTrue(!carried[0], "巢边不抓举");
            hp.discard();
            target.discard();
        });
    }

    /** TC-T15-35：目标站在悬崖边（落差 7 格）——不抓举，改为爪击。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 160)
    public static void harpy_swipes_at_cliff_edge(GameTestHelper h) {
        floor(h);
        for (BlockPos q : BlockPos.betweenClosed(new BlockPos(2, 2, 15), new BlockPos(14, 8, 26))) {
            h.setBlock(q, Blocks.STONE);
        }
        var target = husk(h, new BlockPos(8, 9, 16));
        HarpyEntity hp = harpy(h, new BlockPos(8, 14, 22), null);
        boolean[] carried = {false};
        h.onEachTick(() -> carried[0] |= target.isPassenger());
        h.runAfterDelay(2, () -> {
            h.assertTrue(HarpyEntity.maxDropNear(h.getLevel(), target.blockPosition()) > GrabRules.CLIFF_DROP, "附近有 > 4 格落差");
            hp.beginAttack(target);
        });
        h.succeedWhen(() -> {
            h.assertTrue(target.getHealth() < target.getMaxHealth(), "挨了爪击");
            h.assertTrue(!carried[0], "悬崖边不抓举");
            hp.discard();
            target.discard();
        });
    }

    /** TC-T15-36：崖巢生成——粗土巢台、骨块、骷髅头、崖面方解石白痕；2–4 只同群同巢。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 40)
    public static void harpy_nest_generates(GameTestHelper h) {
        floor(h);
        for (BlockPos q : BlockPos.betweenClosed(new BlockPos(0, 2, 0), new BlockPos(16, 8, 14))) {
            h.setBlock(q, Blocks.STONE);
        }
        BlockPos base = h.absolutePos(new BlockPos(8, 9, 12));
        BlockPos nest = HarpyNestFeature.build(h.getLevel(), base, net.minecraft.core.Direction.SOUTH, h.getLevel().getRandom());
        int n = HarpyNestFeature.spawnFlock(h.getLevel(), nest, h.getLevel().getRandom());
        h.assertTrue(nest.equals(base), "落脚处就是巢台中心");
        h.assertTrue(h.getLevel().getBlockState(base.below()).is(Blocks.COARSE_DIRT), "粗土巢台");
        int calcite = 0;
        int bones = 0;
        int skulls = 0;
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-3, -8, -3), base.offset(3, 2, 4))) {
            var st = h.getLevel().getBlockState(q);
            calcite += st.is(Blocks.CALCITE) ? 1 : 0;
            bones += st.is(Blocks.BONE_BLOCK) ? 1 : 0;
            skulls += st.is(Blocks.SKELETON_SKULL) ? 1 : 0;
        }
        h.assertTrue(calcite >= 6, "三道白痕，每道至少 2 格，实际 " + calcite);
        h.assertTrue(bones == 1 && skulls == 1, "骨块与骷髅头各一，实际 " + bones + " / " + skulls);
        h.assertTrue(!h.getLevel().getBlockState(base.below()).is(Blocks.CALCITE), "巢台本身没被刷白");
        List<HarpyEntity> hs = h.getLevel().getEntitiesOfClass(HarpyEntity.class, new AABB(nest).inflate(4));
        h.assertTrue(n >= 2 && n <= 4 && hs.size() == n, "2–4 只，实际生成 " + n + "、找到 " + hs.size());
        h.assertTrue(hs.stream().allMatch(e -> nest.equals(e.nest()) && e.groupId() != null && e.groupId().equals(hs.get(0).groupId())),
                "同一群、同一个巢");
        hs.forEach(e -> e.discard());
        h.succeed();
    }

    /** TC-T15-37：战利品表每次 0–1 根风暴之羽，40 次里至少掉过一次。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 40)
    public static void harpy_loot(GameTestHelper h) {
        floor(h);
        HarpyEntity e = h.spawn(GreekWild.HARPY.get(), new BlockPos(8, 4, 18));
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "entities/harpy")));
        int dropped = 0;
        for (int i = 0; i < 40; i++) {
            LootParams params = new LootParams.Builder(h.getLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, e)
                    .withParameter(LootContextParams.ORIGIN, e.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            int count = 0;
            for (ItemStack st : table.getRandomItems(params)) {
                if (st.isEmpty()) {
                    continue; // 数量抽到 0 时战利品表给出空堆，实际不掉
                }
                h.assertTrue(st.is(GreekWild.STORM_FEATHER.get()), "只掉风暴之羽，实际 " + st);
                count += st.getCount();
            }
            h.assertTrue(count <= 1, "每次 0–1 根，实际 " + count);
            dropped += count;
        }
        e.discard();
        h.assertTrue(dropped > 0, "40 次里至少掉过一次");
        h.succeed();
    }

    /** TC-T15-38：没有巢也没有目标（命令召唤的）——在出生点附近盘旋，不会一直往上飞。 */
    @GameTest(template = T, batch = HARPY, timeoutTicks = 140)
    public static void harpy_without_nest_circles_in_place(GameTestHelper h) {
        floor(h);
        HarpyEntity e = h.spawn(GreekWild.HARPY.get(), new BlockPos(8, 4, 18));
        double y0 = e.getY();
        h.runAfterDelay(120, () -> {
            h.assertTrue(e.getY() - y0 <= 8.0, "盘旋高度不超过出生点上方 8 格，实际 +" + (e.getY() - y0));
            h.assertTrue(e.position().subtract(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 4, 18)))).horizontalDistance() <= 8,
                    "水平不离开出生点 8 格");
            e.discard();
            h.succeed();
        });
    }

    // ───────────── 阶段 ④ 卡尔基诺斯 ─────────────

    /** 一只卡尔基诺斯，面朝 +Z（领地中心 = 出生点）。 */
    private static KarkinosEntity crab(GameTestHelper h, BlockPos at) {
        KarkinosEntity k = h.spawn(GreekWild.KARKINOS.get(), at);
        k.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(k.blockPosition()), MobSpawnType.SPAWN_EGG, null);
        k.setYRot(0);
        k.yBodyRot = 0;
        k.yHeadRot = 0;
        return k;
    }

    private static net.minecraft.world.entity.animal.Chicken chicken(GameTestHelper h, BlockPos at) {
        var c = h.spawn(EntityType.CHICKEN, at);
        c.setNoAi(true);
        return c;
    }

    /** TC-T15-40：正面挨 10 点只掉 2 点，背后挨 10 点掉 10 点。 */
    @GameTest(template = T, batch = CRAB, timeoutTicks = 40)
    public static void karkinos_frontal_shell(GameTestHelper h) {
        timeOfDay(h, 18000);
        floor(h);
        KarkinosEntity k = crab(h, new BlockPos(8, 2, 18));
        Zombie front = dummy(h, new BlockPos(8, 2, 23));
        Zombie back = dummy(h, new BlockPos(8, 2, 13));
        float hp0 = k.getHealth();
        k.hurt(h.getLevel().damageSources().mobAttack(front), 10);
        float afterFront = k.getHealth();
        k.invulnerableTime = 0;
        k.hurt(h.getLevel().damageSources().mobAttack(back), 10);
        float afterBack = k.getHealth();
        h.assertTrue(Math.abs(hp0 - afterFront - 2.0F) < 0.01F, "正面 10 → 2，实际 " + (hp0 - afterFront));
        h.assertTrue(Math.abs(afterFront - afterBack - 10.0F) < 0.01F, "背后 10 → 10，实际 " + (afterFront - afterBack));
        k.discard();
        h.succeed();
    }

    /** TC-T15-41：掀翻——翻倒 60 刻，期间受伤 ×2（正面也是）、不夹击，之后恢复；别的玩家打的、或隔了几刻的不算。 */
    @GameTest(template = T, batch = CRAB, timeoutTicks = 120)
    public static void karkinos_flip(GameTestHelper h) {
        timeOfDay(h, 18000);
        floor(h);
        KarkinosEntity k = crab(h, new BlockPos(8, 2, 18));
        FakePlayer p = player(h, "ma_wild_crab_flip", new BlockPos(8, 2, 22));
        FakePlayer other = player(h, "ma_wild_crab_other", new BlockPos(9, 2, 22));
        Zombie front = dummy(h, new BlockPos(8, 2, 23));
        // 别的玩家的伤害不触发
        k.armFlip(p);
        k.hurt(h.getLevel().damageSources().playerAttack(other), 1);
        h.assertTrue(!k.flipped(), "另一名玩家打的不翻");
        k.invulnerableTime = 0;
        k.armFlip(p);
        k.hurt(h.getLevel().damageSources().playerAttack(p), 1);
        h.assertTrue(k.flipped(), "满足条件的这一下把它掀翻");
        k.invulnerableTime = 0;
        float before = k.getHealth();
        k.hurt(h.getLevel().damageSources().mobAttack(front), 5);
        h.assertTrue(Math.abs(before - k.getHealth() - 10.0F) < 0.01F, "翻倒时正面 5 → 10，实际 " + (before - k.getHealth()));
        var prey = chicken(h, new BlockPos(8, 2, 20));
        h.runAfterDelay(50, () -> {
            h.assertTrue(k.flipped(), "60 刻内仍翻倒");
            h.assertTrue(prey.isAlive() && prey.getHealth() == prey.getMaxHealth(), "翻倒时不夹击");
        });
        h.runAfterDelay(64, () -> {
            h.assertTrue(!k.flipped(), "60 刻后翻回来");
            k.discard();
            prey.discard();
            h.succeed();
        });
    }

    /** TC-T15-41b：掀翻预约只管紧接着的那一下（隔了几刻再打不翻）。 */
    @GameTest(template = T, batch = CRAB, timeoutTicks = 40)
    public static void karkinos_flip_window(GameTestHelper h) {
        timeOfDay(h, 18000);
        floor(h);
        KarkinosEntity k = crab(h, new BlockPos(8, 2, 18));
        FakePlayer p = player(h, "ma_wild_crab_late", new BlockPos(8, 2, 22));
        k.armFlip(p);
        h.runAfterDelay(4, () -> {
            k.hurt(h.getLevel().damageSources().playerAttack(p), 1);
            h.assertTrue(!k.flipped(), "隔 4 刻不翻");
            k.discard();
            h.succeed();
        });
    }

    /** TC-T15-42：领地 8 格内的猎物会被盯上，12 格外的不会；目标跑出 16 格就放弃，回领地中心。 */
    @GameTest(template = T, batch = CRAB, timeoutTicks = 200)
    public static void karkinos_territory(GameTestHelper h) {
        timeOfDay(h, 18000);
        floor(h);
        KarkinosEntity k = crab(h, new BlockPos(8, 2, 4));
        var far = chicken(h, new BlockPos(8, 2, 17));
        h.runAfterDelay(25, () -> h.assertTrue(k.getTarget() == null, "13 格外的不理，实际 " + k.getTarget()));
        h.runAfterDelay(26, () -> chicken(h, new BlockPos(8, 2, 10)));
        h.startSequence()
                .thenIdle(27)
                .thenWaitUntil(() -> h.assertTrue(k.getTarget() instanceof net.minecraft.world.entity.animal.Chicken c && c != far, "6 格内的被盯上"))
                .thenExecute(() -> {
                    var t = k.getTarget();
                    Vec3 v = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 22)));
                    t.teleportTo(v.x, v.y, v.z);
                })
                .thenWaitUntil(() -> h.assertTrue(k.getTarget() == null, "目标离领地中心 18 格：放弃"))
                .thenWaitUntil(() -> h.assertTrue(k.position().distanceTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 4)))) < 2.5,
                        "回到领地中心"))
                .thenExecute(() -> {
                    k.discard();
                    h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Chicken.class, h.getBounds().inflate(4)).forEach(e -> e.discard());
                })
                .thenSucceed();
    }

    /** TC-T15-44：张钳 14 刻里目标不受伤，之后夹击（鸡一下就死）。 */
    @GameTest(template = T, batch = CRAB, timeoutTicks = 120)
    public static void karkinos_pinch_windup(GameTestHelper h) {
        timeOfDay(h, 18000);
        floor(h);
        KarkinosEntity k = crab(h, new BlockPos(8, 2, 16));
        var prey = chicken(h, new BlockPos(8, 2, 18));
        long[] windupAt = {-1};
        h.onEachTick(() -> {
            if (windupAt[0] < 0 && k.state() == KarkinosEntity.State.WINDUP) {
                windupAt[0] = h.getLevel().getGameTime();
            }
        });
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(windupAt[0] >= 0, "开始张钳"))
                .thenIdle(KarkinosEntity.WINDUP_TICKS - 3)
                .thenExecute(() -> h.assertTrue(prey.isAlive(), "张钳期间没受伤"))
                .thenWaitUntil(() -> h.assertTrue(!prey.isAlive(), "夹击"))
                .thenExecute(() -> k.discard())
                .thenSucceed();
    }

    /** TC-T15-43：白天回领地中心埋进泥里（碰撞箱变矮）；受伤或被踩就钻出来。 */
    @GameTest(template = T, batch = CRAB_DAY, timeoutTicks = 200)
    public static void karkinos_buries_by_day(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        KarkinosEntity hit = crab(h, new BlockPos(4, 2, 10));
        KarkinosEntity stepped = crab(h, new BlockPos(12, 2, 24));
        Zombie z = dummy(h, new BlockPos(8, 2, 2));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(hit.buried() && stepped.buried(), "白天都埋起来"))
                .thenExecute(() -> {
                    h.assertTrue(hit.getBbHeight() < 0.4F, "埋着时碰撞箱变矮，实际 " + hit.getBbHeight());
                    hit.hurt(h.getLevel().damageSources().mobAttack(z), 1);
                    h.assertTrue(hit.state() == KarkinosEntity.State.EMERGE, "受伤钻出");
                    var husk = h.spawn(EntityType.HUSK, new BlockPos(12, 3, 24));
                    husk.setNoAi(false);
                })
                .thenWaitUntil(() -> h.assertTrue(stepped.state() != KarkinosEntity.State.BURIED, "被踩钻出"))
                .thenExecute(() -> {
                    hit.discard();
                    stepped.discard();
                    h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, h.getBounds().inflate(4)).forEach(e -> e.discard());
                    timeOfDay(h, 18000);
                })
                .thenSucceed();
    }

    /** TC-T15-45：自然刷新条件——钟 28 格内不刷；区域里已有 3 只不刷；区域空时可以；刷怪蛋不受限。单独一批（别的用例里的蟹会算进区域密度）。 */
    @GameTest(template = T, batch = "wild_crab_spawn", timeoutTicks = 40)
    public static void karkinos_natural_spawn_rule(GameTestHelper h) {
        floor(h);
        h.setBlock(new BlockPos(8, 2, 2), Blocks.BELL);
        var pred = io.github.happysnappingturtle.mythicage.wild.WildSpawning.<KarkinosEntity>natural("greek:karkinos");
        var level = h.getLevel();
        var prev = level.getDifficulty();
        BlockPos spot = h.absolutePos(new BlockPos(8, 2, 30));
        h.runAfterDelay(5, () -> {
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
            h.assertTrue(!pred.test(GreekWild.KARKINOS.get(), level, MobSpawnType.NATURAL, spot, level.getRandom()), "钟 28 格内不刷");
        });
        // 区域密度与「可以刷」用一张不排除村庄的测试卡：GameTest 世界里别的用例登记的村庄可能就在 64 格内（村庄排除见 TC-T15-10）
        WildCreatureCard real = io.github.happysnappingturtle.mythicage.wild.WildCreatures.card("greek:karkinos").orElseThrow();
        io.github.happysnappingturtle.mythicage.wild.WildCreatures.putForTest(new WildCreatureCard("greek", "test_karkinos",
                real.entity(), real.attitude(), real.spawn(), real.biomes(), real.weight(), real.minGroup(), real.maxGroup(),
                real.densityArea(), real.densityMax(), 0));
        var open = io.github.happysnappingturtle.mythicage.wild.WildSpawning.<KarkinosEntity>natural("greek:test_karkinos");
        h.runAfterDelay(10, () -> {
            h.assertTrue(open.test(GreekWild.KARKINOS.get(), level, MobSpawnType.NATURAL, spot, level.getRandom()), "区域空：可以");
            List<KarkinosEntity> three = new ArrayList<>();
            // 放在判定点同一格上：区域按 256 格网格划分，场地位置随机，分开放可能跨到相邻区域（偶发失败，2026-10-23 修）
            for (int i = 0; i < 3; i++) {
                three.add(crab(h, new BlockPos(8, 2, 30)));
            }
            h.assertTrue(!open.test(GreekWild.KARKINOS.get(), level, MobSpawnType.NATURAL, spot, level.getRandom()), "区域里已有 3 只：不刷");
            h.assertTrue(pred.test(GreekWild.KARKINOS.get(), level, MobSpawnType.SPAWN_EGG, spot, level.getRandom()), "刷怪蛋不受限");
            three.forEach(e -> e.discard());
            level.getServer().setDifficulty(prev, true);
            h.succeed();
        });
    }

    /** TC-T15-46：掉 1 块巨蟹甲壳。 */
    @GameTest(template = T, batch = CRAB, timeoutTicks = 40)
    public static void karkinos_loot(GameTestHelper h) {
        floor(h);
        KarkinosEntity e = h.spawn(GreekWild.KARKINOS.get(), new BlockPos(8, 2, 18));
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "entities/karkinos")));
        LootParams params = new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, e)
                .withParameter(LootContextParams.ORIGIN, e.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        List<ItemStack> drops = table.getRandomItems(params);
        e.discard();
        h.assertTrue(drops.size() == 1 && drops.get(0).is(GreekWild.CRAB_SHELL.get()) && drops.get(0).getCount() == 1,
                "掉 1 块巨蟹甲壳，实际 " + drops);
        h.succeed();
    }

    // ───────────── 阶段 ⑤ 树仙女 ─────────────

    /** 一棵 5 格高的本命橡木（树根在 root），绑定一只树仙女站在 at。 */
    private static HamadryadEntity dryad(GameTestHelper h, BlockPos root, BlockPos at) {
        for (int i = 0; i < 5; i++) {
            h.setBlock(root.above(i), GreekWild.HAMADRYAD_OAK_LOG.get());
        }
        HamadryadEntity d = h.spawn(GreekWild.HAMADRYAD.get(), at);
        d.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(d.blockPosition()), MobSpawnType.STRUCTURE, null);
        d.bindTree(h.absolutePos(root));
        return d;
    }

    private static void chop(GameTestHelper h, FakePlayer p, BlockPos rel) {
        h.setBlock(rel, Blocks.OAK_LOG);
        p.gameMode.destroyBlock(h.absolutePos(rel));
    }

    /** TC-T15-50：夜里走到树旁化树（碰撞像原木、高 2.9 格）；天亮现形。 */
    @GameTest(template = T, batch = "wild_dryad_night", timeoutTicks = 200)
    public static void hamadryad_night_tree_form(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        HamadryadEntity d = dryad(h, new BlockPos(8, 2, 10), new BlockPos(8, 2, 12));
        h.runAfterDelay(2, () -> timeOfDay(h, 18000));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(d.form() == HamadryadEntity.Form.TREE && d.act() == HamadryadEntity.Act.NONE, "夜里化树"))
                .thenExecute(() -> {
                    h.assertTrue(d.canBeCollidedWith() && !d.isPushable(), "化树时像原木：挡路、推不动");
                    h.assertTrue(Math.abs(d.getBbHeight() - 2.9F) < 0.01F, "高 2.9 格，实际 " + d.getBbHeight());
                    timeOfDay(h, 6000);
                })
                .thenWaitUntil(() -> h.assertTrue(d.form() == HamadryadEntity.Form.MAIDEN && d.act() == HamadryadEntity.Act.NONE, "天亮现形"))
                .thenExecute(() -> {
                    h.assertTrue(!d.canBeCollidedWith(), "少女形态不挡路");
                    d.discard();
                })
                .thenSucceed();
    }

    /** TC-T15-51：本命树 16 格内砍 3 块原木——前两次只警告，第 3 次现形并唤根刺（裂纹 16 刻后才出刺）。 */
    @GameTest(template = T, batch = DRYAD, timeoutTicks = 120)
    public static void hamadryad_warns_then_strikes(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        HamadryadEntity d = dryad(h, new BlockPos(8, 2, 6), new BlockPos(8, 2, 8));
        FakePlayer p = player(h, "ma_dryad_chop", new BlockPos(8, 2, 14));
        chop(h, p, new BlockPos(5, 2, 14));
        h.assertTrue(d.strikes(p) == 1 && !d.angry() && d.act() == HamadryadEntity.Act.WARN, "第 1 次：警告");
        chop(h, p, new BlockPos(6, 2, 14));
        h.assertTrue(d.strikes(p) == 2 && !d.angry(), "第 2 次：警告");
        chop(h, p, new BlockPos(7, 2, 14));
        h.assertTrue(d.angry() && d.getTarget() == p, "第 3 次：现形、记恨砍树者");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(!h.getLevel().getEntitiesOfClass(RootSpikeEntity.class, h.getBounds().inflate(2)).isEmpty(), "唤出根刺"))
                .thenExecute(() -> {
                    RootSpikeEntity s = h.getLevel().getEntitiesOfClass(RootSpikeEntity.class, h.getBounds().inflate(2)).get(0);
                    h.assertTrue(s.phase() == 0, "先是裂纹");
                    h.assertTrue(s.position().distanceTo(p.position()) < 1.5, "根刺在砍树者脚下");
                })
                .thenWaitUntil(() -> {
                    RootSpikeEntity s = h.getLevel().getEntitiesOfClass(RootSpikeEntity.class, h.getBounds().inflate(2)).get(0);
                    h.assertTrue(s.phase() == 1, "出刺");
                    h.assertTrue(s.age() == RootSpikeEntity.CRACK_TICKS, "裂纹满 16 刻才出刺，实际 " + s.age());
                })
                .thenExecute(() -> {
                    d.discard();
                    h.getLevel().getEntitiesOfClass(RootSpikeEntity.class, h.getBounds().inflate(2)).forEach(e -> e.discard());
                })
                .thenSucceed();
    }

    /** TC-T15-52：本命树 16 格外砍原木不计次数。 */
    @GameTest(template = T, batch = DRYAD, timeoutTicks = 40)
    public static void hamadryad_ignores_far_chopping(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        HamadryadEntity d = dryad(h, new BlockPos(8, 2, 2), new BlockPos(8, 2, 4));
        FakePlayer p = player(h, "ma_dryad_far", new BlockPos(8, 2, 30));
        chop(h, p, new BlockPos(8, 2, 21));
        h.assertTrue(d.strikes(p) == 0, "19 格外不计，实际 " + d.strikes(p));
        d.discard();
        h.succeed();
    }

    /** TC-T15-53：玩家砍掉树根那格本命橡木——她随之死去，砍树者挖掘疲劳 I 2400 刻。 */
    @GameTest(template = T, batch = DRYAD, timeoutTicks = 40)
    public static void hamadryad_dies_with_tree_player(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        HamadryadEntity d = dryad(h, new BlockPos(8, 2, 18), new BlockPos(8, 2, 21));
        FakePlayer p = player(h, "ma_dryad_fell", new BlockPos(8, 2, 16));
        p.gameMode.destroyBlock(h.absolutePos(new BlockPos(8, 2, 18)));
        d.checkTreeNow();
        h.assertTrue(!d.isAlive(), "树仙女随树死去");
        var eff = p.getEffect(MobEffects.DIG_SLOWDOWN);
        h.assertTrue(eff != null && eff.getAmplifier() == 0 && eff.getDuration() == HamadryadEntity.FATIGUE_TICKS,
                "挖掘疲劳 I 2400 刻，实际 " + eff);
        h.succeed();
    }

    /** TC-T15-54：本命橡木被非玩家原因毁掉——她死去，没人得挖掘疲劳。 */
    @GameTest(template = T, batch = DRYAD, timeoutTicks = 40)
    public static void hamadryad_dies_with_tree_explosion(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        HamadryadEntity d = dryad(h, new BlockPos(8, 2, 18), new BlockPos(8, 2, 21));
        FakePlayer p = player(h, "ma_dryad_near", new BlockPos(8, 2, 16));
        h.setBlock(new BlockPos(8, 2, 18), Blocks.AIR);
        h.runAfterDelay(12, () -> {
            h.assertTrue(!d.isAlive(), "10 刻一次的检查发现树没了，她死去");
            h.assertTrue(p.getEffect(MobEffects.DIG_SLOWDOWN) == null, "非玩家原因不给挖掘疲劳");
            h.succeed();
        });
    }

    /** TC-T15-55：对本命橡木用骨粉 3 次得宁芙之露 1 个；3 个 MC 日内再用不给、不耗骨粉。 */
    @GameTest(template = T, batch = DRYAD, timeoutTicks = 40)
    public static void hamadryad_gives_dew(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        HamadryadEntity d = dryad(h, new BlockPos(8, 2, 18), new BlockPos(8, 2, 21));
        FakePlayer p = player(h, "ma_dryad_dew", new BlockPos(8, 2, 16));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 6));
        BlockPos log = h.absolutePos(new BlockPos(8, 3, 18));
        Runnable use = () -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                p, InteractionHand.MAIN_HAND, log, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(log), net.minecraft.core.Direction.NORTH, log, false)));
        use.run();
        use.run();
        h.assertTrue(p.getInventory().countItem(GreekWild.NYMPH_DEW.get()) == 0, "两次还不给");
        use.run();
        h.assertTrue(p.getInventory().countItem(GreekWild.NYMPH_DEW.get()) == 1, "第 3 次给宁芙之露");
        h.assertTrue(p.getMainHandItem().getCount() == 3, "用掉 3 份骨粉");
        use.run();
        use.run();
        use.run();
        h.assertTrue(p.getInventory().countItem(GreekWild.NYMPH_DEW.get()) == 1, "冷却中不再给");
        h.assertTrue(p.getMainHandItem().getCount() == 3, "冷却中不耗骨粉");
        h.assertTrue(d.dewReadyAt() >= h.getLevel().getGameTime() + HamadryadEntity.DEW_COOLDOWN - 2, "3 个 MC 日的冷却");
        d.discard();
        h.succeed();
    }

    /** TC-T15-57：圣林生成——本命橡木树干、三棵橡树、还愿彩带、蜂巢；1–2 只绑定本命树。 */
    @GameTest(template = T, batch = DRYAD, timeoutTicks = 40)
    public static void hamadryad_grove_generates(GameTestHelper h) {
        timeOfDay(h, 6000);
        floor(h);
        BlockPos center = h.absolutePos(new BlockPos(8, 2, 18));
        BlockPos root = io.github.happysnappingturtle.mythicage.wild.greek.HamadryadGroveFeature.build(h.getLevel(), center, h.getLevel().getRandom());
        int n = io.github.happysnappingturtle.mythicage.wild.greek.HamadryadGroveFeature.spawn(h.getLevel(), root, h.getLevel().getRandom());
        h.assertTrue(h.getLevel().getBlockState(root).is(GreekWild.HAMADRYAD_OAK_LOG.get())
                && h.getLevel().getBlockState(root.above(5)).is(GreekWild.HAMADRYAD_OAK_LOG.get()), "本命橡木树干");
        int oak = 0;
        int ribbons = 0;
        int hives = 0;
        for (BlockPos q : BlockPos.betweenClosed(center.offset(-8, -1, -8), center.offset(8, 12, 8))) {
            var st = h.getLevel().getBlockState(q);
            oak += st.is(Blocks.OAK_LOG) ? 1 : 0;
            ribbons += st.is(GreekWild.VOTIVE_RIBBON.get()) ? 1 : 0;
            hives += st.is(Blocks.BEE_NEST) ? 1 : 0;
        }
        h.assertTrue(oak >= 15, "三棵普通橡树，原木 " + oak);
        h.assertTrue(ribbons >= 4, "还愿彩带至少 4 条，实际 " + ribbons);
        h.assertTrue(hives == 1, "一个蜂巢，实际 " + hives);
        List<HamadryadEntity> ds = h.getLevel().getEntitiesOfClass(HamadryadEntity.class, new AABB(root).inflate(6));
        h.assertTrue(n >= 1 && n <= 2 && ds.size() == n && ds.stream().allMatch(e -> root.equals(e.treeRoot())), "1–2 只绑定本命树");
        ds.forEach(e -> e.discard());
        h.succeed();
    }

    /** TC-T15-58：击杀只掉 1–2 根木棍。 */
    @GameTest(template = T, batch = DRYAD, timeoutTicks = 40)
    public static void hamadryad_loot(GameTestHelper h) {
        floor(h);
        HamadryadEntity e = h.spawn(GreekWild.HAMADRYAD.get(), new BlockPos(8, 2, 18));
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "entities/hamadryad")));
        for (int i = 0; i < 10; i++) {
            LootParams params = new LootParams.Builder(h.getLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, e)
                    .withParameter(LootContextParams.ORIGIN, e.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            List<ItemStack> drops = table.getRandomItems(params);
            h.assertTrue(drops.size() == 1 && drops.get(0).is(Items.STICK) && drops.get(0).getCount() >= 1 && drops.get(0).getCount() <= 2,
                    "1–2 根木棍，实际 " + drops);
        }
        e.discard();
        h.succeed();
    }
}
