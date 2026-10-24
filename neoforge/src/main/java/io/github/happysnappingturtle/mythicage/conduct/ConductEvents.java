package io.github.happysnappingturtle.mythicage.conduct;

import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.conduct.IncidentLedger;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductConfig;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.mixin.ZombieVillagerAccessor;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.village.VillageZones;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 善恶事件（T18 设计文档 §4.3，02 号 §2、§3）：
 * <ul>
 *   <li>只在实际扣血后计（LivingDamageEvent.Post，伤害 > 0）：被取消、伤害为 0、免疫都不算</li>
 *   <li>村民、守护者在哪里都算；铁傀儡（D6）与动物（D9）只在受害者所在位置是已知的已建范围时算（D1、D8）</li>
 *   <li>同一事件伤害后致死按合计计（事件账本），窗口 {@code incidentWindowTicks}</li>
 *   <li>合法自卫（D3）：受害者在窗口内先打了你，且你在窗口内没有留下过罚分</li>
 *   <li>治愈僵尸村民 +6：同一个原村民只领一次（身份跟着转化走，防止反复感染刷善）</li>
 * </ul>
 */
public final class ConductEvents {
    public static final TagKey<EntityType<?>> ANIMALS =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "conduct/animals"));
    /** 转化前后保持不变的「原村民」身份。 */
    static final String ORIGIN = "mythic_age:origin_villager";

    public enum Category { VILLAGER, GUARDIAN, DEFENDER, ANIMAL }

    /** 「谁先打了谁」：攻击者 UUID|玩家 UUID → 第一次出手的刻。 */
    private static final Map<String, Long> AGGRESSION = new ConcurrentHashMap<>();

    private ConductEvents() {
    }

    public static void onDamagePost(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || e.getNewDamage() <= 0) {
            return;
        }
        long now = level.getGameTime();
        if (victim instanceof ServerPlayer target && e.getSource().getEntity() instanceof LivingEntity a && !(a instanceof Player)) {
            noteAggression(a, target, now);
        }
        judge(level, victim, e.getSource(), false);
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!e.isCanceled() && e.getEntity().level() instanceof ServerLevel level) {
            judge(level, e.getEntity(), e.getSource(), true);
        }
    }

    /** 判一次伤害或致死；返回实际变化（测试也直接调用）。 */
    public static int judge(ServerLevel level, LivingEntity victim, DamageSource source, boolean kill) {
        ConductConfig cfg = ConductService.config();
        if (!cfg.enabled() || victim instanceof Player) {
            return 0;
        }
        Category cat = category(level, victim);
        if (cat == null) {
            return 0;
        }
        Responsibility.Blame blame = Responsibility.of(source);
        ServerPlayer p = OnlinePlayers.find(level.getServer(), blame.player().orElse(null));
        if (p == null) {
            return 0;
        }
        long now = level.getGameTime();
        long window = cfg.incidentWindowTicks();
        IncidentLedger ledger = MythData.get(p).conduct().incidents();
        if (cfg.selfDefenseExempt() && provoked(victim.getUUID(), p.getUUID(), now, window)
                && !ledger.chargedRecently(p.getUUID().toString(), now, window)) {
            return 0;
        }
        ConductConfig.Events ev = cfg.events();
        int[] v = switch (cat) {
            case VILLAGER -> new int[]{ev.harmVillager(), ev.killVillagerTotal()};
            case GUARDIAN -> new int[]{ev.harmGuardian(), ev.killGuardianTotal()};
            case DEFENDER -> new int[]{ev.harmDefender(), ev.killDefenderTotal()};
            case ANIMAL -> new int[]{ev.harmAnimalInVillage(), ev.killAnimalInVillageTotal()};
        };
        String key = IncidentLedger.key(p.getUUID().toString(), victim.getUUID().toString(), cat.name());
        int delta = ledger.charge(key, kill, now, window, v[0], v[1]);
        if (delta == 0) {
            return 0;
        }
        String reason = (kill ? "kill_" : "harm_") + cat.name().toLowerCase(java.util.Locale.ROOT);
        return ConductService.change(p, reason, delta);
    }

    /** 受害者的类别；不计善恶的返回 null。动物、铁傀儡看受害者此刻的位置。 */
    public static Category category(ServerLevel level, LivingEntity victim) {
        if (victim instanceof GuardianEntity) {
            return Category.GUARDIAN;
        }
        if (victim instanceof Villager) {
            return Category.VILLAGER;
        }
        if (victim instanceof VillageDefender) {
            return Category.DEFENDER;
        }
        if (victim instanceof IronGolem) {
            return VillageZones.zoneAt(level, victim.blockPosition()).builtKnown() ? Category.DEFENDER : null;
        }
        if (victim.getType().is(ANIMALS) && !victim.getType().is(GreekPowers.CURSED)) {
            return VillageZones.zoneAt(level, victim.blockPosition()).builtKnown() ? Category.ANIMAL : null;
        }
        return null;
    }

    /** 记下「a 先打了 player」（窗口内只记第一次）。GameTest 也直接调用（FakePlayer 不会受伤）。 */
    public static void noteAggression(LivingEntity a, Player player, long now) {
        long window = ConductService.config().incidentWindowTicks();
        AGGRESSION.compute(a.getUUID() + "|" + player.getUUID(), (k, t) -> t == null || now - t > window ? now : t);
    }

    static boolean provoked(UUID victim, UUID player, long now, long window) {
        Long t = AGGRESSION.get(victim + "|" + player);
        return t != null && now - t <= window;
    }

    /** 村民 ↔ 僵尸村民：把「原村民」身份带过去；僵尸村民被玩家治愈 → +6，每个原村民只一次。 */
    public static void onConverted(LivingConversionEvent.Post e) {
        LivingEntity from = e.getEntity();
        LivingEntity to = e.getOutcome();
        if (!(from.level() instanceof ServerLevel level)) {
            return;
        }
        UUID origin = from.getPersistentData().hasUUID(ORIGIN) ? from.getPersistentData().getUUID(ORIGIN) : from.getUUID();
        to.getPersistentData().putUUID(ORIGIN, origin);
        if (from instanceof ZombieVillager zv && to instanceof Villager) {
            UUID starter = ((ZombieVillagerAccessor) zv).mythicAge$conversionStarter();
            ServerPlayer p = OnlinePlayers.find(level.getServer(), starter);
            if (p != null && ConductService.config().enabled() && MythData.get(p).conduct().markCured(origin)) {
                ConductService.change(p, "cure_zombie_villager", ConductService.config().events().cureZombieVillager());
            }
        }
    }

    /** 以后的敌袭救援、护送任务调用：每个事件 id 只领一次由调用方的收据保证。 */
    public static int reward(ServerPlayer p, String reason, int amount) {
        return ConductService.change(p, reason, Math.max(0, amount));
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        AGGRESSION.clear();
    }
}
