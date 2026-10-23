package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxSteps;
import io.github.happysnappingturtle.mythicage.core.relic.DamageRules;
import io.github.happysnappingturtle.mythicage.core.relic.EffectGroups;
import io.github.happysnappingturtle.mythicage.core.relic.RelicBinding;
import io.github.happysnappingturtle.mythicage.core.relic.RelicCard;
import io.github.happysnappingturtle.mythicage.core.relic.RelicState;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.PlayerDataNbt;
import io.github.happysnappingturtle.mythicage.relic.RelicCards;
import io.github.happysnappingturtle.mythicage.relic.RelicEffects;
import io.github.happysnappingturtle.mythicage.relic.RelicItems;
import io.github.happysnappingturtle.mythicage.relic.RelicService;
import io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import io.github.happysnappingturtle.mythicage.summon.greek.SummonPegasusEntity;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.List;
import java.util.UUID;

/**
 * T16 圣物框架与希腊圣物 ×3（TC-T16-20～74）。
 * 玩家用 FakePlayer（mock 玩家的连接会让 Curios 同步包被拒，见 PlayerDataGameTests）：它不在玩家列表里、不会受伤、不 tick，
 * 所以 Curios 槽变化后手动对账（与 CurioChangeEvent 走同一入口），玩家的减伤直接查 {@link RelicEffects#meleeReductions}。
 * 供奉的范围效果会影响 48 格内别的用例，按圣物分成单独的批次。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RelicGameTests {
    private static final String BATCH = "relic";
    private static final String ACHILLES = "relic_achilles";
    private static final String PYGMALION = "relic_pygmalion";
    private static final String T = GameTestTemplates.EMPTY_17X16X20;
    private static final BlockPos ALTAR = new BlockPos(8, 2, 8);
    private static final BlockPos ALTAR_B = new BlockPos(3, 2, 3);
    private static final String RHEIA = "greek:rheia_crown";
    private static final String STATUE = "greek:pygmalion_statue";

    private RelicGameTests() {
    }

    // ───────────── 搭建 ─────────────

    private static FakePlayer player(GameTestHelper h, String name, Age greek) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(h.absoluteVec(new Vec3(8.5, 2, 6.5)));
        MythData.get(p).civ(Civilization.GREEK).setAge(greek);
        RelicService.onlineForTest(p, true);
        return p;
    }

    private static ICurioStacksHandler slots(FakePlayer p) {
        return CuriosApi.getCuriosInventory(p).flatMap(i -> i.getStacksHandler(RelicItems.SLOT))
                .orElseThrow(() -> new GameTestAssertException("玩家没有圣物槽 " + RelicItems.SLOT));
    }

    /** 让 Curios 按槽位修饰立即调整槽数（真实玩家由 Curios 在下一刻处理）。 */
    private static int resized(FakePlayer p) {
        p.tickCount++;
        slots(p).update();
        return slots(p).getSlots();
    }

    private static void wear(FakePlayer p, int slot, ItemStack s) {
        slots(p).getStacks().setStackInSlot(slot, s);
        RelicService.reconcile(p);
    }

    private static ItemStack unwear(FakePlayer p, int slot) {
        ItemStack s = slots(p).getStacks().getStackInSlot(slot).copy();
        slots(p).getStacks().setStackInSlot(slot, ItemStack.EMPTY);
        RelicService.reconcile(p);
        return s;
    }

    private static void age(FakePlayer p, Age age) {
        MythData.get(p).civ(Civilization.GREEK).setAge(age);
        RelicService.reconcile(p);
    }

    private static BlockPos altar(GameTestHelper h, BlockPos rel, FakePlayer owner) {
        h.setBlock(rel.below(), Blocks.STONE);
        h.setBlock(rel, GreekTemple.GREEK_ALTAR.get().defaultBlockState().setValue(GreekAltarBlock.FACING, Direction.SOUTH));
        BlockPos abs = h.absolutePos(rel);
        ShrineService.bindForTest(h.getLevel(), abs, owner.getUUID(), owner.getGameProfile().getName(),
                MythData.get(owner).civ(Civilization.GREEK).age(), Direction.SOUTH);
        return abs;
    }

    private static ShrineBlockEntity shrine(GameTestHelper h, BlockPos abs) {
        if (h.getLevel().getBlockEntity(abs) instanceof ShrineBlockEntity be) {
            return be;
        }
        throw new GameTestAssertException("没有神庙方块实体 " + abs);
    }

    /** 手持 s 潜行右键神坛；返回供奉后手里是否空了（= 放进去了）。 */
    private static boolean offer(GameTestHelper h, FakePlayer p, BlockPos abs, ItemStack s) {
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        RelicService.offer(p, h.getLevel(), abs, InteractionHand.MAIN_HAND);
        boolean in = p.getMainHandItem().isEmpty();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return in;
    }

    private static ItemStack rheia() {
        return new ItemStack(GreekRelics.RHEIA_CROWN.get());
    }

    private static ItemStack armor() {
        return new ItemStack(GreekRelics.ARMOR_OF_ACHILLES.get());
    }

    private static ItemStack statue() {
        return new ItemStack(GreekRelics.PYGMALION_STATUE.get());
    }

    private static RelicState state(FakePlayer p, String id) {
        return MythData.get(p).relics().state(id);
    }

    private static long cap(FakePlayer p) {
        return MythData.cap(p, Civilization.GREEK);
    }

    private static void cleanup(GameTestHelper h, List<FakePlayer> players, BlockPos... altars) {
        for (BlockPos a : altars) {
            h.getLevel().removeBlock(a, false);
        }
        h.killAllEntitiesOfClass(ItemEntity.class);
        for (FakePlayer p : players) {
            RelicService.onlineForTest(p, false);
        }
    }

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        h.assertTrue(Math.abs(expected - actual) < 1e-3, what + "：应为 " + expected + "，实际 " + actual);
    }

    // ───────────── 3.1 槽位与唯一性 ─────────────

    /** TC-T16-20：新玩家的圣物槽 1 格。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc20_base_slot(GameTestHelper h) {
        FakePlayer p = player(h, "rl_base", Age.ARCHAIC);
        RelicService.reconcile(p);
        h.assertTrue(resized(p) == 1, "基础 1 格，实际 " + slots(p).getSlots());
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-21：英雄 2 格、神话 3 格；反复对账修饰只有两条。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc21_slots_grow(GameTestHelper h) {
        FakePlayer p = player(h, "rl_grow", Age.CLASSICAL);
        age(p, Age.HEROIC);
        h.assertTrue(resized(p) == 2, "英雄 2 格，实际 " + slots(p).getSlots());
        age(p, Age.MYTHIC);
        RelicService.reconcile(p);
        RelicService.reconcile(p);
        h.assertTrue(resized(p) == 3, "神话 3 格，实际 " + slots(p).getSlots());
        long ours = slots(p).getModifiers().keySet().stream().filter(k -> k.getPath().startsWith("relic_slots/")).count();
        h.assertTrue(ours == 2, "槽位修饰应恰好两条，实际 " + ours);
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-22：两格里放同一件圣物的两个物品，第二件不能戴。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc22_duplicate_rejected(GameTestHelper h) {
        FakePlayer p = player(h, "rl_dup", Age.HEROIC);
        RelicService.reconcile(p);
        resized(p);
        wear(p, 0, rheia());
        h.assertFalse(RelicService.canWear(p, 1, rheia()), "第二件同名圣物不能戴");
        h.assertTrue(RelicService.canWear(p, 1, statue()), "别的圣物可以戴");
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-23：瑞亚之冠已供奉，再戴另一件被拒；绑定表不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc23_offered_blocks_wear(GameTestHelper h) {
        FakePlayer p = player(h, "rl_block", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        h.assertTrue(offer(h, p, a, rheia()), "应能供奉");
        RelicBinding before = MythData.get(p).relics().get(RHEIA).orElseThrow();
        h.assertFalse(RelicService.canWear(p, 0, rheia()), "已供奉的圣物不能再戴");
        h.assertTrue(before.equals(MythData.get(p).relics().get(RHEIA).orElseThrow()), "绑定表不变");
        cleanup(h, List.of(p), a);
        h.succeed();
    }

    /** TC-T16-24：槽里塞进一件已被供奉占用的同名圣物 → 对账退回背包，供奉照常。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc24_eject_conflict(GameTestHelper h) {
        FakePlayer p = player(h, "rl_eject", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        offer(h, p, a, rheia());
        wear(p, 0, rheia());
        h.assertTrue(slots(p).getStacks().getStackInSlot(0).isEmpty(), "冲突的圣物应被退出圣物槽");
        h.assertTrue(p.getInventory().countItem(GreekRelics.RHEIA_CROWN.get()) == 1, "退回背包");
        h.assertTrue(state(p, RHEIA) == RelicState.OFFERED, "供奉照常");
        close(h, 0.15, RelicService.prayerR(p.getUUID()), "R");
        cleanup(h, List.of(p), a);
        h.succeed();
    }

    /** TC-T16-25：背包里的圣物没有效果。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc25_inventory_no_effect(GameTestHelper h) {
        FakePlayer p = player(h, "rl_inv", Age.ARCHAIC);
        p.getInventory().add(rheia());
        p.getInventory().add(statue());
        p.getInventory().add(armor());
        RelicService.reconcile(p);
        h.assertTrue(cap(p) == 100, "上限仍是 100，实际 " + cap(p));
        close(h, 20, p.getMaxHealth(), "生命上限");
        h.assertTrue(RelicEffects.meleeReductions(p, h.getLevel().damageSources().mobAttack(h.spawn(EntityType.ZOMBIE, 2, 2, 2))).isEmpty(),
                "没有减伤");
        h.killAllEntitiesOfClass(Zombie.class);
        cleanup(h, List.of(p));
        h.succeed();
    }

    // ───────────── 3.2 供奉位 ─────────────

    /** TC-T16-30：远古 1 位，多座神坛共用；晋升古典后可放第二件。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc30_pool_shared(GameTestHelper h) {
        FakePlayer p = player(h, "rl_pool", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        BlockPos b = altar(h, ALTAR_B, p);
        h.assertTrue(offer(h, p, a, rheia()), "神坛 A 供奉瑞亚之冠");
        h.assertFalse(offer(h, p, b, statue()), "远古 1 / 1，神坛 B 被拒");
        age(p, Age.CLASSICAL);
        h.assertTrue(offer(h, p, b, statue()), "古典 2 位，可以放");
        h.assertTrue(RelicService.offerUsage(p, Civilization.GREEK)[0] == 2, "用量 2");
        cleanup(h, List.of(p), a, b);
        h.succeed();
    }

    /** TC-T16-31：别人不能在我的神坛上供奉。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc31_not_owner(GameTestHelper h) {
        FakePlayer owner = player(h, "rl_own", Age.ARCHAIC);
        FakePlayer other = player(h, "rl_other", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, owner);
        h.assertFalse(offer(h, other, a, rheia()), "别人供奉被拒");
        h.assertTrue(shrine(h, a).relics().isEmpty() && state(other, RHEIA) == RelicState.INACTIVE, "架上与绑定表都不变");
        cleanup(h, List.of(owner, other), a);
        h.succeed();
    }

    /** TC-T16-32：别的文明的圣物不能供奉在希腊神坛。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc32_wrong_civ(GameTestHelper h) {
        FakePlayer p = player(h, "rl_civ", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        RelicCard egyptian = new RelicCard("egyptian:test_relic", "mythic_age:rheia_crown", Civilization.EGYPTIAN, List.of(), List.of());
        h.assertTrue(RelicService.offerError(p, shrine(h, a), egyptian, rheia()).isPresent(), "埃及圣物被拒");
        h.assertTrue(RelicService.offerError(p, shrine(h, a), RelicCards.byId(RHEIA).orElseThrow(), rheia()).isEmpty(),
                "希腊圣物可以");
        cleanup(h, List.of(p), a);
        h.succeed();
    }

    /** TC-T16-33：取出后回到背包，绑定删除，效果撤销。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc33_retrieve(GameTestHelper h) {
        FakePlayer p = player(h, "rl_take", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        offer(h, p, a, rheia());
        close(h, 0.15, RelicService.prayerR(p.getUUID()), "供奉后 R");
        h.assertTrue(RelicService.retrieve(p, h.getLevel(), a, 0), "取出成功");
        h.assertTrue(p.getInventory().countItem(GreekRelics.RHEIA_CROWN.get()) == 1, "回到背包");
        h.assertTrue(state(p, RHEIA) == RelicState.INACTIVE, "绑定删除");
        close(h, 0, RelicService.prayerR(p.getUUID()), "取出后 R");
        cleanup(h, List.of(p), a);
        h.succeed();
    }

    /** TC-T16-34：主人在线时拆神坛，圣物掉在原地、绑定删除。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc34_break_online(GameTestHelper h) {
        FakePlayer p = player(h, "rl_break", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        offer(h, p, a, rheia());
        h.getLevel().removeBlock(a, false);
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(a).inflate(2),
                e -> e.getItem().is(GreekRelics.RHEIA_CROWN.get()));
        h.assertTrue(drops.size() == 1, "圣物掉在原地，实际 " + drops.size());
        h.assertTrue(state(p, RHEIA) == RelicState.INACTIVE, "绑定删除");
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-35：主人不在线时神坛被拆，上线对账时删除绑定。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc35_break_offline(GameTestHelper h) {
        FakePlayer p = player(h, "rl_offline", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        offer(h, p, a, rheia());
        RelicService.onlineForTest(p, false);
        h.getLevel().removeBlock(a, false);
        h.assertTrue(state(p, RHEIA) == RelicState.OFFERED, "不在线：绑定先留着");
        RelicService.onlineForTest(p, true);
        RelicService.reconcile(p);
        h.assertTrue(state(p, RHEIA) == RelicState.INACTIVE, "上线对账后删除");
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-36：神坛区块未加载时对账，OFFERED 绑定保留。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc36_unloaded_kept(GameTestHelper h) {
        FakePlayer p = player(h, "rl_unloaded", Age.ARCHAIC);
        BlockPos far = new BlockPos(2_000_000, 64, 2_000_000);
        GlobalPos g = GlobalPos.of(h.getLevel().dimension(), far);
        ShrineRegistry reg = ShrineRegistry.get(h.getLevel().getServer());
        reg.add(new ShrineRegistry.Entry(g, p.getUUID(), "rl_unloaded", Direction.SOUTH, Civilization.GREEK, "greek:altar"));
        try {
            MythData.get(p).relics().bind(RHEIA, new RelicBinding(RelicState.OFFERED, UUID.randomUUID(), Civilization.GREEK,
                    RelicBinding.shrineWhere(h.getLevel().dimension().location().toString(), far.getX(), far.getY(), far.getZ())));
            h.assertFalse(h.getLevel().isLoaded(far), "远处区块不应已加载");
            RelicService.reconcile(p);
            h.assertTrue(state(p, RHEIA) == RelicState.OFFERED, "区块未加载：保留");
        } finally {
            reg.remove(g);
        }
        RelicService.reconcile(p);
        h.assertTrue(state(p, RHEIA) == RelicState.INACTIVE, "登记注销后删除");
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-37：神话时代供奉 3 件后降到古典：3 件保持，第 4 件被拒（D7）。 */
    @GameTest(template = T, batch = PYGMALION)
    public static void tc37_age_drop_keeps(GameTestHelper h) {
        FakePlayer p = player(h, "rl_drop", Age.MYTHIC);
        BlockPos a = altar(h, ALTAR, p);
        h.assertTrue(offer(h, p, a, rheia()) && offer(h, p, a, armor()) && offer(h, p, a, statue()), "神话时代可放 3 件");
        age(p, Age.CLASSICAL);
        h.assertTrue(RelicService.offerUsage(p, Civilization.GREEK)[0] == 3, "3 件仍是供奉状态");
        close(h, 0.15, RelicService.prayerR(p.getUUID()), "R 仍生效");
        BlockPos b = altar(h, ALTAR_B, p);
        h.assertTrue(RelicService.offerError(p, shrine(h, b), RelicCards.byId(RHEIA).orElseThrow(), rheia()).isPresent(),
                "第 4 件被拒");
        cleanup(h, List.of(p), a, b);
        h.succeed();
    }

    // ───────────── 3.3 瑞亚之冠 ─────────────

    /** TC-T16-40：远古时代佩戴，希腊上限 100 → 150；埃及不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc40_rheia_cap(GameTestHelper h) {
        FakePlayer p = player(h, "rl_cap", Age.ARCHAIC);
        MythData.get(p).civ(Civilization.EGYPTIAN).setAge(Age.ARCHAIC);
        wear(p, 0, rheia());
        h.assertTrue(cap(p) == 150, "希腊上限 150，实际 " + cap(p));
        h.assertTrue(MythData.cap(p, Civilization.EGYPTIAN) == 100, "埃及上限不变");
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-41：佩戴中晋升古典，上限 200（现算）。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc41_rheia_promotion(GameTestHelper h) {
        FakePlayer p = player(h, "rl_promo", Age.ARCHAIC);
        wear(p, 0, rheia());
        age(p, Age.CLASSICAL);
        h.assertTrue(cap(p) == 200, "古典上限 200，实际 " + cap(p));
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-42：神话时代 330（上限 350）卸下：上限 300，恩宠保留 330，再获得不增加。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc42_rheia_excess_kept(GameTestHelper h) {
        FakePlayer p = player(h, "rl_excess", Age.MYTHIC);
        wear(p, 0, rheia());
        MythData.get(p).civ(Civilization.GREEK).setFavor(330);
        ItemStack back = unwear(p, 0);
        h.assertTrue(cap(p) == 300, "上限 300，实际 " + cap(p));
        MythData.transact(p, Transactions.Request.internal(
                TxSteps.addFavor(Civilization.GREEK, 10, RelicService.capBonus(p, Civilization.GREEK))));
        h.assertTrue(MythData.get(p).civ(Civilization.GREEK).favor() == 330, "保留 330 不增长，实际 "
                + MythData.get(p).civ(Civilization.GREEK).favor());
        h.assertTrue(back.is(GreekRelics.RHEIA_CROWN.get()), "物品没有被吞掉");
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-43 / 44：供奉在神坛 → R = 0.15；主人不在线 → 0。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc43_rheia_offered_r(GameTestHelper h) {
        FakePlayer p = player(h, "rl_r", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        offer(h, p, a, rheia());
        close(h, 0.15, RelicService.prayerR(p.getUUID()), "R");
        close(h, 0, RelicService.prayerR(UUID.randomUUID()), "不在线的主人");
        RelicService.onlineForTest(p, false);
        close(h, 0, RelicService.prayerR(p.getUUID()), "主人下线");
        cleanup(h, List.of(p), a);
        h.succeed();
    }

    // ───────────── 3.4 阿喀琉斯之甲 ─────────────

    /** TC-T16-50 / 51 / 55：佩戴：近战 10 → 9.2；箭、爆炸、荆棘不减；对账 5 次不叠加。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc50_achilles_worn(GameTestHelper h) {
        FakePlayer p = player(h, "rl_achilles", Age.ARCHAIC);
        Zombie z = h.spawn(EntityType.ZOMBIE, 2, 2, 2);
        z.setNoAi(true);
        wear(p, 0, armor());
        for (int i = 0; i < 5; i++) {
            RelicService.reconcile(p);
        }
        var ds = h.getLevel().damageSources();
        close(h, 9.2, DamageRules.reduce(10, RelicEffects.meleeReductions(p, ds.mobAttack(z)), 0), "近战");
        Arrow arrow = new Arrow(h.getLevel(), z.getX(), z.getY() + 1, z.getZ(), new ItemStack(Items.ARROW), null);
        DamageSource[] others = {ds.arrow(arrow, z), ds.explosion(z, z), ds.thorns(z)};
        for (DamageSource s : others) {
            h.assertTrue(RelicEffects.meleeReductions(p, s).isEmpty(), "不是近战：" + s.getMsgId());
        }
        h.killAllEntitiesOfClass(Zombie.class);
        cleanup(h, List.of(p));
        h.succeed();
    }

    private static float hit(LivingEntity e, DamageSource s) {
        e.setHealth(e.getMaxHealth());
        float before = e.getHealth();
        e.invulnerableTime = 0;
        e.hurt(s, 10);
        return before - e.getHealth();
    }

    /** TC-T16-52 / 53 / 54：供奉：范围内的村民、铁傀儡、主人的狼与召唤单位 −15%；主人本人、别人的狼、49 格外不减；两座神坛不叠加。 */
    @GameTest(template = T, batch = ACHILLES, timeoutTicks = 100)
    public static void tc52_achilles_offered(GameTestHelper h) {
        FakePlayer owner = player(h, "rl_ach_owner", Age.ARCHAIC);
        FakePlayer other = player(h, "rl_ach_other", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, owner);
        BlockPos b = altar(h, ALTAR_B, other);
        h.assertTrue(offer(h, owner, a, armor()) && offer(h, other, b, armor()), "两座神坛都供奉了甲");
        Zombie z = h.spawn(EntityType.ZOMBIE, 1, 2, 15);
        z.setNoAi(true);
        DamageSource melee = h.getLevel().damageSources().mobAttack(z);
        Villager v = h.spawn(EntityType.VILLAGER, 10, 2, 10);
        v.setNoAi(true);
        IronGolem golem = h.spawn(EntityType.IRON_GOLEM, 12, 2, 4);
        golem.setNoAi(true);
        Wolf mine = h.spawn(EntityType.WOLF, 4, 2, 12);
        mine.tame(owner);
        mine.setNoAi(true);
        Villager far = EntityType.VILLAGER.create(h.getLevel());
        Vec3 c = Vec3.atCenterOf(a);
        far.moveTo(c.x, c.y + 49, c.z);
        far.setNoAi(true);
        h.getLevel().addFreshEntity(far);
        SummonService.Result r = SummonService.summon(owner, SummonUnits.card("greek:pegasus").orElseThrow(),
                Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(13, 2, 13))));
        h.assertTrue(r == SummonService.Result.OK, "召唤天马应成功，实际 " + r);
        SummonPegasusEntity peg = h.getLevel().getEntitiesOfClass(SummonPegasusEntity.class, new AABB(a).inflate(20),
                e -> owner.getUUID().equals(e.link().owner)).get(0);
        try {
            close(h, 8.5, hit(v, melee), "范围内村民（两座神坛也只算一次）");
            close(h, 8.5, hit(golem, melee), "铁傀儡");
            close(h, 8.5, hit(mine, melee), "主人的狼");
            close(h, 8.5, hit(peg, melee), "主人的召唤单位");
            close(h, 10, hit(far, melee), "49 格外的村民");
            h.assertTrue(RelicEffects.meleeReductions(owner, melee).isEmpty(), "主人本人不吃供奉减伤");
            // 另一名玩家的狼：两座神坛都覆盖它，但只有 other 的神坛把它当友方，所以也是 −15%；换成没有供奉的玩家的狼才不减
            FakePlayer third = player(h, "rl_ach_third", Age.ARCHAIC);
            Wolf stranger = h.spawn(EntityType.WOLF, 6, 2, 12);
            stranger.tame(third);
            stranger.setNoAi(true);
            close(h, 10, hit(stranger, melee), "别人的狼");
            RelicService.onlineForTest(third, false);
        } finally {
            far.discard();
            peg.discard();
            h.killAllEntitiesOfClass(Villager.class);
            h.killAllEntitiesOfClass(Wolf.class);
            h.killAllEntitiesOfClass(IronGolem.class);
            h.killAllEntitiesOfClass(Zombie.class);
            cleanup(h, List.of(owner, other), a, b);
        }
        h.succeed();
    }

    // ───────────── 3.5 皮格马利翁雕像 ─────────────

    /** TC-T16-60 / 61 / 62：佩戴 +4 不回血；回满后卸下截到 20；反复对账只挂一次。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc60_statue_worn(GameTestHelper h) {
        FakePlayer p = player(h, "rl_statue", Age.ARCHAIC);
        p.setHealth(20);
        wear(p, 0, statue());
        close(h, 24, p.getMaxHealth(), "上限");
        close(h, 20, p.getHealth(), "当前生命不变");
        for (int i = 0; i < 4; i++) {
            RelicService.reconcile(p);
        }
        close(h, 24, p.getMaxHealth(), "反复对账后的上限");
        long n = p.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream()
                .filter(m -> m.id().equals(RelicService.MAX_HEALTH_SELF)).count();
        h.assertTrue(n == 1, "修饰只有一条，实际 " + n);
        p.setHealth(24);
        unwear(p, 0);
        close(h, 20, p.getMaxHealth(), "卸下后上限");
        close(h, 20, p.getHealth(), "当前生命截到 20");
        cleanup(h, List.of(p));
        h.succeed();
    }

    private static VillageRegistry.Village village(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, Blocks.BELL);
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), GlobalPos.of(h.getLevel().dimension(),
                h.absolutePos(rel)), "greek", "zeus", VillageRegistry.Origin.VANILLA, null, "plains", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        return v;
    }

    private static Villager villager(GameTestHelper h, BlockPos rel, VillageRegistry.Village village) {
        Villager v = h.spawn(EntityType.VILLAGER, rel);
        v.setNoAi(true);
        if (village != null) {
            v.getBrain().setMemory(MemoryModuleType.MEETING_POINT, village.bell());
        }
        return v;
    }

    /**
     * TC-T16-63～67：供奉雕像。范围内信徒上限 ×1.4；走出 48 格撤销并截断，走回来不回满；普通村民没有加成；
     * 困难难度下僵尸杀死受保护的信徒不转化，不受保护的照常转化。
     */
    @GameTest(template = T, batch = PYGMALION, timeoutTicks = 200)
    public static void tc63_statue_offered(GameTestHelper h) {
        FakePlayer owner = player(h, "rl_pyg", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, owner);
        VillageRegistry.Village village = village(h, new BlockPos(14, 2, 14));
        Villager believer = villager(h, new BlockPos(10, 2, 10), village);
        Villager plain = villager(h, new BlockPos(5, 2, 12), null);
        h.assertTrue(offer(h, owner, a, statue()), "供奉雕像");
        Difficulty before = h.getLevel().getDifficulty();
        Vec3 home = believer.position();
        Runnable cleanup = () -> {
            h.getLevel().getServer().setDifficulty(before, true);
            h.killAllEntitiesOfClass(Villager.class);
            h.killAllEntitiesOfClass(ZombieVillager.class);
            h.killAllEntitiesOfClass(Zombie.class);
            VillageRegistry.get(h.getLevel().getServer()).remove(village.bell());
            cleanup(h, List.of(owner), a);
        };
        h.runAfterDelay(RELIC_TICK, () -> {
            try {
                close(h, 28, believer.getMaxHealth(), "信徒上限 ×1.4");
                close(h, 20, plain.getMaxHealth(), "普通村民没有加成");
                believer.setHealth(28);
                Vec3 c = Vec3.atCenterOf(a);
                believer.moveTo(c.x, c.y + 49, c.z);
            } catch (RuntimeException e) {
                cleanup.run();
                throw e;
            }
            h.runAfterDelay(RELIC_TICK, () -> {
                try {
                    close(h, 20, believer.getMaxHealth(), "走出 48 格后上限");
                    close(h, 20, believer.getHealth(), "当前生命截到 20");
                    believer.moveTo(home.x, home.y, home.z);
                } catch (RuntimeException e) {
                    cleanup.run();
                    throw e;
                }
                h.runAfterDelay(RELIC_TICK, () -> {
                    try {
                        close(h, 28, believer.getMaxHealth(), "走回来上限恢复");
                        close(h, 20, believer.getHealth(), "不回满");
                        h.getLevel().getServer().setDifficulty(Difficulty.HARD, true);
                        Zombie z = h.spawn(EntityType.ZOMBIE, 8, 2, 14);
                        z.setNoAi(true);
                        z.killedEntity(h.getLevel(), believer);
                        h.assertTrue(h.getLevel().getEntitiesOfClass(ZombieVillager.class, new AABB(a).inflate(20)).isEmpty(),
                                "受保护的信徒不转化");
                        h.assertTrue(believer.isAlive(), "转化被取消，村民没有被替换");
                        z.killedEntity(h.getLevel(), plain);
                        h.assertTrue(h.getLevel().getEntitiesOfClass(ZombieVillager.class, new AABB(a).inflate(20)).size() == 1,
                                "不受保护的村民照常转化");
                    } finally {
                        cleanup.run();
                    }
                    h.succeed();
                });
            });
        });
    }

    private static final int RELIC_TICK = 25;

    // ───────────── 3.6 一致性 ─────────────

    /** TC-T16-70：雕像 佩戴 → 卸下 → 供奉 → 取出 → 佩戴，每步只体现当前状态，绑定表最多一条。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc70_switch_modes(GameTestHelper h) {
        FakePlayer p = player(h, "rl_switch", Age.ARCHAIC);
        BlockPos a = altar(h, ALTAR, p);
        PlayerMythData d = MythData.get(p);
        wear(p, 0, statue());
        close(h, 24, p.getMaxHealth(), "佩戴");
        ItemStack s = unwear(p, 0);
        close(h, 20, p.getMaxHealth(), "卸下");
        h.assertTrue(d.relics().view().isEmpty(), "卸下后没有绑定");
        h.assertTrue(offer(h, p, a, s), "供奉");
        close(h, 20, p.getMaxHealth(), "供奉时没有佩戴效果");
        h.assertTrue(d.relics().view().size() == 1 && state(p, STATUE) == RelicState.OFFERED, "一条 OFFERED");
        RelicService.retrieve(p, h.getLevel(), a, 0);
        h.assertTrue(d.relics().view().isEmpty(), "取出后没有绑定");
        ItemStack back = p.getInventory().items.stream().filter(x -> x.is(GreekRelics.PYGMALION_STATUE.get())).findFirst()
                .orElseThrow(() -> new GameTestAssertException("取出的雕像不在背包"));
        UUID inst = RelicItems.instance(back).orElseThrow();
        p.getInventory().removeItem(back);
        wear(p, 0, back);
        close(h, 24, p.getMaxHealth(), "再佩戴");
        h.assertTrue(d.relics().get(STATUE).orElseThrow().instance().equals(inst), "实例 UUID 一直不变");
        cleanup(h, List.of(p), a);
        h.succeed();
    }

    /** TC-T16-71：佩戴瑞亚之冠死亡掉落（物品离开槽位）后重生对账：绑定删除，上限回到基础值。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc71_death_drop(GameTestHelper h) {
        FakePlayer p = player(h, "rl_death", Age.ARCHAIC);
        wear(p, 0, rheia());
        slots(p).getStacks().setStackInSlot(0, ItemStack.EMPTY);
        RelicService.reconcile(p);
        h.assertTrue(state(p, RHEIA) == RelicState.INACTIVE && cap(p) == 100, "绑定删除，上限 100，实际 " + cap(p));
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-72：保留物品栏死亡重生：仍是 WORN，上限 +50 只算一次。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc72_keep_inventory(GameTestHelper h) {
        FakePlayer p = player(h, "rl_keep", Age.ARCHAIC);
        wear(p, 0, rheia());
        RelicService.reconcile(p);
        RelicService.reconcile(p);
        h.assertTrue(state(p, RHEIA) == RelicState.WORN && cap(p) == 150, "上限 150，实际 " + cap(p));
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-73（含 TC-T16-15 的 NBT 部分）：绑定表 NBT 往返；读回后对账，效果一致。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc73_nbt_round_trip(GameTestHelper h) {
        FakePlayer p = player(h, "rl_nbt", Age.ARCHAIC);
        wear(p, 0, statue());
        PlayerMythData d = MythData.get(p);
        d.relics().restore("greek:armor_of_achilles", new RelicBinding(RelicState.OFFERED, UUID.randomUUID(), Civilization.GREEK,
                "minecraft:overworld|1,2,3"));
        d.relics().restore("atlantean:test", new RelicBinding(RelicState.HERO, UUID.randomUUID(), Civilization.ATLANTEAN, "hero"));
        PlayerMythData back = PlayerDataNbt.read(PlayerDataNbt.write(d));
        h.assertTrue(back.relics().equals(d.relics()), "绑定表往返相等");
        var old = PlayerDataNbt.write(new PlayerMythData());
        old.remove("relics");
        h.assertTrue(PlayerDataNbt.read(old).relics().view().isEmpty(), "旧存档读成空表");
        d.relics().remove("greek:armor_of_achilles");
        d.relics().remove("atlantean:test");
        RelicService.reconcile(p);
        close(h, 24, p.getMaxHealth(), "对账后效果一致");
        cleanup(h, List.of(p));
        h.succeed();
    }

    /** TC-T16-74：三张卡已加载；物品带 Curios 标签，堆叠上限 1。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc74_cards_and_items(GameTestHelper h) {
        for (ItemStack s : List.of(rheia(), armor(), statue())) {
            h.assertTrue(RelicCards.of(s).isPresent(), "卡已加载：" + s);
            h.assertTrue(s.is(RelicItems.CURIOS_TAG), "Curios 标签：" + s);
            h.assertTrue(s.getMaxStackSize() == 1, "堆叠上限 1：" + s);
        }
        close(h, 50, RelicCards.byId(RHEIA).orElseThrow().value(RelicState.WORN, EffectGroups.favorCap(Civilization.GREEK)), "瑞亚之冠数值");
        h.succeed();
    }
}
