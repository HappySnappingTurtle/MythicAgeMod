package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.relic.DamageRules;
import io.github.happysnappingturtle.mythicage.core.relic.EffectGroups;
import io.github.happysnappingturtle.mythicage.core.relic.RelicCard;
import io.github.happysnappingturtle.mythicage.core.relic.RelicState;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 范围类与事件类效果（T16 设计文档 §6）：近战减伤（佩戴 / 供奉）、信徒村民生命上限与防转化（供奉）。
 * 全局类效果（恩宠上限、R）与佩戴属性在 {@link RelicService}。
 */
public final class RelicEffects {
    public static final ResourceLocation MAX_HEALTH_BELIEVER =
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "relic/" + EffectGroups.MAX_HEALTH_BELIEVER_VILLAGER);
    /** 上一轮加过生命上限修饰的村民 → 所在维度。 */
    private static final Map<UUID, ResourceKey<Level>> BLESSED = new HashMap<>();

    private RelicEffects() {
    }

    public static void clear() {
        BLESSED.clear();
    }

    // ───────────── 近战减伤 ─────────────

    public static boolean isMelee(DamageSource s) {
        Entity direct = s.getDirectEntity();
        return DamageRules.isMelee(direct != null && direct == s.getEntity(), direct instanceof LivingEntity,
                s.is(DamageTypeTags.IS_PROJECTILE), s.is(DamageTypeTags.IS_EXPLOSION), s.is(DamageTypes.THORNS));
    }

    /** 受到近战伤害时要乘的减伤（不同组相乘，同组取最大值）；不是近战时为空表。 */
    public static List<Double> meleeReductions(LivingEntity victim, DamageSource s) {
        List<Double> out = new ArrayList<>();
        if (!isMelee(s)) {
            return out;
        }
        if (victim instanceof ServerPlayer p) {
            double self = RelicService.worn(p, EffectGroups.MELEE_TAKEN_SELF);
            if (self > 0) {
                out.add(self);
            }
            return out;
        }
        double area = areaValue(victim, EffectGroups.MELEE_TAKEN_AREA_ALLIES, true);
        if (area > 0) {
            out.add(area);
        }
        return out;
    }

    /**
     * 目标所在位置、所有已加载并供奉了该组圣物的神庙中，该组的最大值。
     *
     * @param allyOfOwner 还要求目标是神庙主人的友方（阿喀琉斯之甲）
     */
    static double areaValue(LivingEntity target, String group, boolean allyOfOwner) {
        if (!(target.level() instanceof ServerLevel level)) {
            return 0;
        }
        double best = 0;
        for (OfferedIndex.Entry e : OfferedIndex.all()) {
            if (e.pos().dimension() != level.dimension()) {
                continue;
            }
            double v = 0;
            for (RelicCard c : e.cards()) {
                v = Math.max(v, c.value(RelicState.OFFERED, group));
            }
            if (v <= best) {
                continue;
            }
            RelicAnchor anchor = RelicAnchor.Registry.of(e.civ()).orElse(null);
            if (anchor == null || !anchor.inRange(level, e.pos().pos(), target)) {
                continue;
            }
            if (allyOfOwner && !isAlly(target, e.owner())) {
                continue;
            }
            best = v;
        }
        return best;
    }

    /** 某个方块位置（铁砧）落在哪些供奉了该组圣物的神庙范围内，取该组的最大值（赫菲斯托斯之砧：范围内铁砧不损坏）。 */
    public static double areaValueAt(ServerLevel level, net.minecraft.core.BlockPos pos, String group) {
        double best = 0;
        for (OfferedIndex.Entry e : OfferedIndex.all()) {
            if (e.pos().dimension() != level.dimension()) {
                continue;
            }
            double v = 0;
            for (RelicCard c : e.cards()) {
                v = Math.max(v, c.value(RelicState.OFFERED, group));
            }
            if (v <= best) {
                continue;
            }
            RelicAnchor anchor = RelicAnchor.Registry.of(e.civ()).orElse(null);
            double r = anchor == null ? 0 : anchor.searchRadius();
            if (anchor != null && e.pos().pos().getCenter().distanceToSqr(pos.getCenter()) <= r * r) {
                best = v;
            }
        }
        return best;
    }

    /** 阿喀琉斯之甲供奉的「友方」（D4）：范围内的村民（含本作居民）、铁傀儡；主人的召唤单位与驯服生物。玩家不算。 */
    public static boolean isAlly(LivingEntity e, UUID owner) {
        if (e instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }
        // 本作的村庄居民也算（GA-13 实机核对：希腊村子里住的主要是居民，不是原版村民）
        if (e instanceof Villager || e instanceof IronGolem
                || e instanceof io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity) {
            return true;
        }
        if (e instanceof MythSummon s) {
            return owner.equals(s.link().owner);
        }
        return e instanceof OwnableEntity o && owner.equals(o.getOwnerUUID());
    }

    // ───────────── 信徒村民 ─────────────

    /** 本文明的信徒：原版村民按锚点判断；本作居民属于这个文明的村子就算。 */
    static boolean believer(RelicAnchor anchor, LivingEntity e) {
        if (e instanceof Villager v) {
            return anchor.believer(v);
        }
        return e instanceof io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity r && r.village()
                .flatMap(id -> io.github.happysnappingturtle.mythicage.village.VillageRegistry.get(r.getServer()).byId(id))
                .map(v -> v.civ().equals(anchor.civ().id())).orElse(false);
    }

    /** 每 20 刻：范围内的信徒（原版村民与本作居民）加生命上限，离开的拿掉（§6）。 */
    public static void tickBelievers(MinecraftServer server) {
        Map<UUID, LivingEntity> now = new HashMap<>();
        Map<UUID, Double> amount = new HashMap<>();
        for (OfferedIndex.Entry e : OfferedIndex.all()) {
            double v = 0;
            for (RelicCard c : e.cards()) {
                v = Math.max(v, c.value(RelicState.OFFERED, EffectGroups.MAX_HEALTH_BELIEVER_VILLAGER));
            }
            ServerLevel level = server.getLevel(e.pos().dimension());
            RelicAnchor anchor = RelicAnchor.Registry.of(e.civ()).orElse(null);
            if (v <= 0 || level == null || anchor == null || !level.isLoaded(e.pos().pos())) {
                continue;
            }
            double r = anchor.searchRadius();
            // 信徒：希腊村子里的原版村民与本作居民
            for (LivingEntity vil : level.getEntitiesOfClass(LivingEntity.class, new AABB(e.pos().pos()).inflate(r),
                    x -> x.isAlive() && anchor.inRange(level, e.pos().pos(), x) && believer(anchor, x))) {
                now.put(vil.getUUID(), vil);
                amount.merge(vil.getUUID(), v, Math::max);
            }
        }
        for (Map.Entry<UUID, ResourceKey<Level>> old : List.copyOf(BLESSED.entrySet())) {
            if (now.containsKey(old.getKey())) {
                continue;
            }
            BLESSED.remove(old.getKey());
            ServerLevel level = server.getLevel(old.getValue());
            if (level != null && level.getEntity(old.getKey()) instanceof LivingEntity v) {
                RelicService.setHealthModifier(v, MAX_HEALTH_BELIEVER, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            }
        }
        now.forEach((id, v) -> {
            RelicService.setHealthModifier(v, MAX_HEALTH_BELIEVER, amount.get(id), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            BLESSED.put(id, v.level().dimension());
        });
    }

    /** 受雕像保护的村民（带生命上限修饰）不会被僵尸转化。 */
    public static boolean protectedFromConversion(LivingEntity e) {
        var attr = e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        return e instanceof Villager && attr != null && attr.getModifier(MAX_HEALTH_BELIEVER) != null;
    }
}
