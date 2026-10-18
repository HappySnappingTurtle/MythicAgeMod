package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.power.BoltPower;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.power.PowerReason;
import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.core.trade.TradeRules;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.power.AuraPayload;
import io.github.happysnappingturtle.mythicage.power.Auras;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerService;
import io.github.happysnappingturtle.mythicage.power.PowerSpecLoader;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * T13 阶段 ①：雷击、复原、青铜（TC-T13-20～35）。用正式的神力数据与道具；施法者是 T12 的「会受伤的假玩家」。
 * 精英标签目前为空，本批次把卫道士临时当作精英。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GreekPowerGameTests {
    private static final String BATCH = "greek_power";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final Civilization GR = Civilization.GREEK;

    private GreekPowerGameTests() {
    }

    @BeforeBatch(batch = BATCH)
    public static void before(ServerLevel level) {
        TargetFactsCollector.eliteForTest(EntityType.VINDICATOR);
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        TargetFactsCollector.eliteForTest(null);
        PowerService.clearForTest();
        level.getServer().setPvpAllowed(true);
    }

    // ───────────── 搭建 ─────────────

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    private static PowerGameTests.Vulnerable caster(GameTestHelper h, String name, Item power, int count) {
        PowerGameTests.Vulnerable p = new PowerGameTests.Vulnerable(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        BlockPos at = h.absolutePos(new BlockPos(8, 2, 4));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        p.getInventory().clearContent();
        p.getInventory().selected = 0;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(power, count));
        return p;
    }

    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, int x, int z) {
        T m = h.spawn(type, new BlockPos(x, 2, z));
        m.setNoAi(true);
        return m;
    }

    private static void maxHealth(LivingEntity e, double max) {
        e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(max);
        e.setHealth((float) max);
    }

    private static void look(Player p, Entity e) {
        p.lookAt(EntityAnchorArgument.Anchor.EYES, e.getEyePosition());
    }

    private static PlayerMythData data(Player p) {
        return MythData.get((ServerPlayer) p);
    }

    /** 瞄准 + 确认；返回本次施放的 castId。 */
    private static String cast(Player p) {
        PowerService.use((ServerPlayer) p, InteractionHand.MAIN_HAND);
        String id = PowerService.sessionForTest((ServerPlayer) p).orElseThrow().castId();
        PowerService.use((ServerPlayer) p, InteractionHand.MAIN_HAND);
        return id;
    }

    private static boolean committed(Player p) {
        return PowerService.lastResultForTest((ServerPlayer) p).orElse(null) == PowerReason.COMMITTED;
    }

    private static List<PowerFxPayload> fx(String castId) {
        return PowerFx.sentForTest(castId).stream().filter(x -> x instanceof PowerFxPayload).map(x -> (PowerFxPayload) x).toList();
    }

    private static boolean hasFx(String castId, PowerFxPayload.Phase phase, String tag) {
        return fx(castId).stream().anyMatch(f -> f.phase() == phase && f.tag().equals(tag));
    }

    // ───────────── 雷击（TC-T13-20～25、35） ─────────────

    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void bolt_kills_common_mob(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = caster(h, "gb_kill", GreekPowers.BOLT_ITEM.get(), 3);
        Mob husk = mob(h, EntityType.HUSK, 8, 10);
        maxHealth(husk, 60);
        look(p, husk);
        String castId = cast(p);
        h.succeedWhen(() -> {
            h.assertTrue(!husk.isAlive(), "最大生命恰 60：即死");
            h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2, "扣 1 件");
            AABB box = new AABB(h.absolutePos(BlockPos.ZERO)).inflate(20);
            h.assertTrue(h.getLevel().getEntitiesOfClass(LightningBolt.class, box).isEmpty(), "不生成原版闪电");
            for (BlockPos b : BlockPos.betweenClosed(h.absolutePos(new BlockPos(5, 1, 7)), h.absolutePos(new BlockPos(11, 4, 13)))) {
                h.assertTrue(!h.getLevel().getBlockState(b).is(Blocks.FIRE), "不点火");
            }
            // TC-T13-25：表现序列
            List<PowerFxPayload> l = fx(castId);
            int commit = indexOf(l, PowerFxPayload.Phase.COMMIT);
            int hit = indexOf(l, PowerFxPayload.Phase.HIT);
            h.assertTrue(commit >= 0 && l.get(commit).duration() == 15 && hit > commit
                    && l.get(hit).targets().equals(List.of(husk.getId())), "COMMIT（15 刻）→ HIT：" + l);
            // TC-T13-35：同文明全部神力道具进入冷却遮罩；重登后恢复
            Item[] all = {GreekPowers.BOLT_ITEM.get(), GreekPowers.RESTORATION_ITEM.get(), GreekPowers.BRONZE_ITEM.get()};
            for (Item i : all) {
                h.assertTrue(p.getCooldowns().isOnCooldown(i), "冷却遮罩：" + i);
                p.getCooldowns().removeCooldown(i);
            }
            PowerService.onLoggedIn(new PlayerEvent.PlayerLoggedInEvent(p));
            for (Item i : all) {
                h.assertTrue(p.getCooldowns().isOnCooldown(i), "重登后恢复遮罩：" + i);
            }
        });
    }

    private static int indexOf(List<PowerFxPayload> l, PowerFxPayload.Phase phase) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).phase() == phase) {
                return i;
            }
        }
        return -1;
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void bolt_damages_tough_and_elite(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable a = caster(h, "gb_tough", GreekPowers.BOLT_ITEM.get(), 3);
        Mob tough = mob(h, EntityType.HUSK, 8, 10);
        maxHealth(tough, 61);
        look(a, tough);
        cast(a);
        PowerGameTests.Vulnerable b = caster(h, "gb_elite", GreekPowers.BOLT_ITEM.get(), 3);
        b.moveTo(b.getX(), b.getY(), b.getZ() + 14);
        Mob elite = mob(h, EntityType.VINDICATOR, 8, 24);
        maxHealth(elite, 50);
        look(b, elite);
        cast(b);
        h.succeedWhen(() -> {
            h.assertTrue(committed(a) && committed(b), "两次都提交");
            h.assertTrue(tough.isAlive() && tough.getHealth() == 21, "最大生命 61：只受 40 点：" + tough.getHealth());
            h.assertTrue(elite.isAlive() && elite.getHealth() == 10, "精英 50：剩 10、不即死：" + elite.getHealth());
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void bolt_pvp(GameTestHelper h) throws ReflectiveOperationException {
        PowerGameTests.Vulnerable p = caster(h, "gb_pvp_a", GreekPowers.BOLT_ITEM.get(), 1);
        PowerGameTests.Vulnerable foe = caster(h, "gb_pvp_b", Items.STICK, 1);
        // 新建的服务端玩家有 60 刻出生保护，假玩家不走 tick，直接清零
        var f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        f.setAccessible(true);
        f.setInt(foe, 0);
        var spec = PowerSpecLoader.get(GreekPowers.BOLT).orElseThrow();
        h.getLevel().getServer().setPvpAllowed(false);
        h.assertTrue(TargetClassifier.NONE.equals(TargetClassifier.reaction(spec,
                TargetFactsCollector.collect(p, foe, Set.of()))), "PvP 关：不能锁定玩家");
        h.getLevel().getServer().setPvpAllowed(true);
        h.assertTrue("pvp".equals(TargetClassifier.reaction(spec, TargetFactsCollector.collect(p, foe, Set.of()))),
                "PvP 开：可锁定");
        BoltPower.strike(new CastContext(p, h.getLevel(), spec, "pvp", foe, null, "pvp", foe.position(), null, null, 1, 0),
                foe);
        h.assertTrue(foe.getHealth() == 8, "PvP 落雷 12 点：" + foe.getHealth());
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void bolt_follows_target_and_ends_on_death(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable a = caster(h, "gb_follow", GreekPowers.BOLT_ITEM.get(), 3);
        Mob runner = mob(h, EntityType.HUSK, 8, 10);
        maxHealth(runner, 61);
        look(a, runner);
        String castA = cast(a);
        PowerGameTests.Vulnerable b = caster(h, "gb_lost", GreekPowers.BOLT_ITEM.get(), 3);
        b.moveTo(b.getX(), b.getY(), b.getZ() + 14);
        Mob doomed = mob(h, EntityType.HUSK, 8, 24);
        look(b, doomed);
        String castB = cast(b);
        // 吟唱 20 刻后进入 15 刻预警：预警中把一个挪开 5 格、把另一个杀掉
        h.runAfterDelay(27, () -> {
            h.assertTrue(committed(a) && committed(b), "已提交，正在预警");
            runner.teleportTo(runner.getX() + 5, runner.getY(), runner.getZ());
            doomed.kill();
        });
        h.runAfterDelay(55, () -> {
            h.assertTrue(runner.getHealth() == 21, "目标走开仍被劈中：" + runner.getHealth());
            h.assertTrue(hasFx(castA, PowerFxPayload.Phase.HIT, ""), "A 有 HIT");
            h.assertTrue(hasFx(castB, PowerFxPayload.Phase.END, "") && !hasFx(castB, PowerFxPayload.Phase.HIT, ""),
                    "目标先死：END、没有 HIT：" + fx(castB));
            h.assertTrue(b.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2, "不退道具");
            h.succeed();
        });
    }

    // ───────────── 复原（TC-T13-26～29） ─────────────

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void restoration_heals_cleanses_repairs(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = caster(h, "gr_resto", GreekPowers.RESTORATION_ITEM.get(), 2);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.setDamageValue(chest.getMaxDamage() / 20);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        Mob near = mob(h, EntityType.VILLAGER, 8, 10);
        near.setHealth(1);
        Mob far = mob(h, EntityType.VILLAGER, 3, 4);
        far.moveTo(p.getX(), p.getY(), p.getZ() + 12.5);
        far.setHealth(1);
        Mob husk = mob(h, EntityType.HUSK, 11, 6);
        husk.setHealth(5);
        Wolf wolf = mob(h, EntityType.WOLF, 6, 6);
        wolf.tame(p);
        wolf.setHealth(2);
        wolf.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
        String castId = cast(p);
        h.runAfterDelay(35, () -> {
            h.assertTrue(committed(p), "已提交");
            h.assertTrue(!p.hasEffect(MobEffects.POISON) && !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "第 1 刻清除中毒缓慢");
            h.assertTrue(!wolf.hasEffect(MobEffects.POISON), "驯服狼也清除");
            h.assertTrue(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getDamageValue() == 0,
                    "剩 95% 的胸甲修满、不溢出");
            h.assertTrue(near.getHealth() == 5, "第一次补血 +4：" + near.getHealth());
            h.assertTrue(hasFx(castId, PowerFxPayload.Phase.HIT, "cleanse") && hasFx(castId, PowerFxPayload.Phase.HIT, "repair"),
                    "净化、修复子事件");
            // 名单已锁定：走出半径照样回血
            near.teleportTo(near.getX(), near.getY(), near.getZ() + 15);
        });
        h.runAfterDelay(150, () -> {
            h.assertTrue(near.getHealth() == near.getMaxHealth(), "5 秒回满（离开半径也继续）：" + near.getHealth());
            h.assertTrue(far.getHealth() == 1, "半径 12.5 不受益");
            h.assertTrue(husk.getHealth() == 5, "敌对生物不回血");
            h.assertTrue(wolf.getHealth() == wolf.getMaxHealth(), "驯服狼回满");
            h.assertTrue(hasFx(castId, PowerFxPayload.Phase.END, ""), "第 5 秒结束");
            h.succeed();
        });
    }

    // ───────────── 青铜（TC-T13-30～33） ─────────────

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void bronze_allies_and_refresh(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = caster(h, "gbz", GreekPowers.BRONZE_ITEM.get(), 3);
        Wolf wolf = mob(h, EntityType.WOLF, 6, 6);
        wolf.tame(p);
        Mob golem = mob(h, EntityType.IRON_GOLEM, 11, 7);
        Mob villager = mob(h, EntityType.VILLAGER, 8, 9);
        Mob husk = mob(h, EntityType.HUSK, 5, 9);
        cast(p);
        h.runAfterDelay(35, () -> {
            h.assertTrue(committed(p), "已提交");
            h.assertTrue(p.hasEffect(GreekPowers.BRONZE_BODY) && p.getAttributeValue(Attributes.ARMOR) == 6
                    && p.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 2, "施法者护甲 +6、韧性 +2");
            h.assertTrue(wolf.hasEffect(GreekPowers.BRONZE_BODY) && golem.hasEffect(GreekPowers.BRONZE_BODY), "驯服狼与铁傀儡获得");
            h.assertTrue(!villager.hasEffect(GreekPowers.BRONZE_BODY) && !husk.hasEffect(GreekPowers.BRONZE_BODY), "村民、僵尸不获得");
            // TC-T13-33：外观同步
            List<AuraPayload> sent = Auras.sentForTest(wolf.getId());
            h.assertTrue(!sent.isEmpty() && sent.getLast().aura().equals("bronze") && sent.getLast().remainingTicks() > 1700,
                    "添加时发外观：" + sent);
            h.assertTrue(Auras.currentForTest(wolf).size() == 1, "补发用的当前外观");
            wolf.removeEffect(GreekPowers.BRONZE_BODY);
            h.assertTrue(Auras.sentForTest(wolf.getId()).getLast().remainingTicks() == 0, "移除时发 0");
            // 再施放一次：只刷新时长
            data(p).civ(GR).setPowerCooldownUntil(0);
            data(p).powerCooldowns().clear();
            cast(p);
        });
        h.runAfterDelay(80, () -> {
            MobEffectInstance e = p.getEffect(GreekPowers.BRONZE_BODY);
            h.assertTrue(e != null && e.getDuration() > 1790, "时长回到 90 秒：" + e);
            h.assertTrue(p.getAttributeValue(Attributes.ARMOR) == 6, "数值不叠加");
            h.succeed();
        });
    }

    // ───────────── 兑换（TC-T13-34） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void power_offers(GameTestHelper h) {
        TradeTable table = TradeTableLoader.table(GR);
        TradeOffer bolt = offer(table, "gp_gr_bolt");
        TradeOffer resto = offer(table, "gp_gr_restoration");
        TradeOffer bronze = offer(table, "gp_gr_bronze");
        h.assertTrue(TradeRules.visible(bolt, TradeRules.VenueContext.statue("zeus", Set.of())), "宙斯像前有雷击");
        h.assertTrue(!TradeRules.visible(bolt, TradeRules.VenueContext.statue("poseidon", Set.of())), "波塞冬像前没有雷击");
        h.assertTrue(!TradeRules.visible(bolt, TradeRules.VenueContext.guardian(Age.MYTHIC, "zeus", Set.of())), "守护者不卖主神神力");
        var shrine = TradeRules.VenueContext.guardian(Age.CLASSICAL, "zeus", Set.of("athena"));
        h.assertTrue(TradeRules.visible(resto, shrine) && !TradeRules.visible(bronze, shrine), "雅典娜祠：复原有、青铜无");
        PowerGameTests.Vulnerable p = caster(h, "gtrade", Items.STICK, 1);
        PlayerMythData d = data(p);
        d.civ(GR).setAge(Age.CLASSICAL);
        h.assertTrue(TradeRules.check(d, GR, resto, shrine, 42, 42, 0) == TradeReason.MINOR_GOD_LOCKED, "没供奉雅典娜：锁住");
        d.civ(GR).minorGods().put(Age.CLASSICAL, "athena");
        d.civ(GR).setFavor(50);
        h.assertTrue(TradeRules.check(d, GR, resto, shrine, 42, 42, 0) == TradeReason.OK, "供奉雅典娜：可买");
        h.assertTrue(TradeRules.check(d, GR, bolt, TradeRules.VenueContext.statue("zeus", Set.of()), 20, 20, 0) == TradeReason.OK,
                "远古也能在宙斯像前买雷击");
        h.succeed();
    }

    private static TradeOffer offer(TradeTable t, String id) {
        return t.offers().stream().filter(o -> o.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("没有商品 " + id));
    }
}
