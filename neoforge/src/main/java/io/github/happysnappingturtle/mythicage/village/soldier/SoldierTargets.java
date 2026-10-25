package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.conduct.ConductEvents;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierTargetRules;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.power.OwnedSummon;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageZones;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 从真实实体收集士兵的敌我事实（T20 设计文档 §5.4），交给 core 的 {@link SoldierTargetRules}。
 */
public final class SoldierTargets {
    private SoldierTargets() {
    }

    public static SoldierTargetRules.Facts facts(ResidentEntity soldier, LivingEntity e, UUID village, double reach, long now) {
        ServerLevel level = (ServerLevel) soldier.level();
        long harmed = VillageThreats.ticksSince(village, e.getUUID(), now);
        boolean targetsMe = e instanceof Mob m && m.getTarget() == soldier;
        boolean inReach = soldier.distanceTo(e) <= reach + e.getBbWidth() / 2 + 0.3;
        if (e instanceof Player p) {
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.PLAYER).harmed(harmed)
                    .player(p.isCreative() || p.isSpectator(), teammate(level, p, village), level.getServer().isPvpAllowed());
        }
        if (e instanceof ResidentEntity || e instanceof GuardianEntity || e instanceof AbstractVillager || e instanceof IronGolem) {
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.VILLAGE_FOLK);
        }
        UUID owner = owner(e);
        if (owner != null) {
            boolean pvp = level.getServer().isPvpAllowed();
            Player op = level.getPlayerByUUID(owner);
            boolean retaliate = pvp && op != null && !op.isCreative() && !op.isSpectator() && !teammate(level, op, village)
                    && within(VillageThreats.ticksSince(village, owner, now), SoldierTargetRules.PLAYER_MEMORY);
            boolean harming = (e instanceof Mob m && m.getTarget() instanceof ResidentEntity r && r.village().map(village::equals).orElse(false))
                    || within(harmed, SoldierTargetRules.NEUTRAL_MEMORY);
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.OWNED).harmed(harmed).owned(retaliate, harming);
        }
        if (TargetFactsCollector.isBoss(e)) {
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.BOSS).attacking(targetsMe, inReach);
        }
        if (TargetFactsCollector.isElite(e)) {
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.ELITE).attacking(targetsMe, inReach);
        }
        if (e.getType().is(ConductEvents.ANIMALS) || e instanceof Animal) {
            boolean biting = e instanceof Mob m && m.getTarget() instanceof ResidentEntity r && r.village().map(village::equals).orElse(false);
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.ANIMAL).harmed(biting ? 0 : harmed);
        }
        if (e instanceof NeutralMob) {
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.NEUTRAL).harmed(harmed);
        }
        if (e instanceof Enemy) {
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.HOSTILE).attacking(targetsMe, inReach);
        }
        if (e instanceof Mob) {
            return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.NEUTRAL).harmed(harmed);
        }
        return SoldierTargetRules.Facts.of(SoldierTargetRules.Kind.OTHER);
    }

    private static boolean within(long ago, long ticks) {
        return ago >= 0 && ago <= ticks;
    }

    /** 召唤单位、宠物、拴着的生物的主人；没有主人为 null。 */
    static UUID owner(LivingEntity e) {
        if (e instanceof OwnedSummon s && s.summonOwner() != null) {
            return s.summonOwner();
        }
        if (e instanceof TamableAnimal t && t.isTame() && t.getOwnerUUID() != null) {
            return t.getOwnerUUID();
        }
        if (e instanceof AbstractHorse h && h.isTamed() && h.getOwnerUUID() != null) {
            return h.getOwnerUUID();
        }
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) {
            return o.getOwnerUUID();
        }
        if (e instanceof Mob m && m.isLeashed() && m.getLeashHolder() instanceof Player p) {
            return p.getUUID();
        }
        return null;
    }

    /** 与本村协作者同队：建立者本人，或与在线的建立者同队。 */
    static boolean teammate(ServerLevel level, Player p, UUID village) {
        VillageRegistry.Village v = VillageRegistry.get(level.getServer()).byId(village).orElse(null);
        if (v == null || v.founder() == null) {
            return false;
        }
        if (v.founder().equals(p.getUUID())) {
            return true;
        }
        Player founder = level.getServer().getPlayerList().getPlayer(v.founder());
        return founder != null && p.isAlliedTo(founder);
    }

    /** 在这座村庄的已建范围内。 */
    public static boolean inVillage(ServerLevel level, BlockPos pos, UUID village) {
        VillageZones.Hit h = VillageZones.zoneAt(level, pos);
        return h.builtKnown() && h.village().map(x -> x.id().equals(village)).orElse(false);
    }
}
