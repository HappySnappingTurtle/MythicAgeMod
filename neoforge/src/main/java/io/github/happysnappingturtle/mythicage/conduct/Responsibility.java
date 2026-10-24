package io.github.happysnappingturtle.mythicage.conduct;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.OwnedSummon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Optional;
import java.util.UUID;

/**
 * 伤害的责任归因（02 号 §4、T18 设计文档 §3）：直接攻击 → 攻击者；投射物 → 服务器记录的发射者；
 * 召唤单位 → 主人；驯服生物 → 主人（D2）；神力 → 施法者；村民士兵与无主来源 → 没有责任人。不捏造责任人。
 */
public final class Responsibility {
    /** 神力造成的伤害类型（雷击等）：伤害源的施法者就是责任人。 */
    public static final TagKey<DamageType> GOD_POWER =
            TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "god_power"));
    private static final String POWER_TAG = "mythic_age:power";

    public enum Via { DIRECT, PROJECTILE, SUMMON, PET, POWER, SOLDIER, NONE }

    public record Blame(Optional<UUID> player, Via via) {
        static final Blame NONE = new Blame(Optional.empty(), Via.NONE);
        static final Blame SOLDIER = new Blame(Optional.empty(), Via.SOLDIER);
    }

    private Responsibility() {
    }

    public static Blame of(DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker == null) {
            return direct == null ? Blame.NONE : resolve(direct, Via.NONE, 0);
        }
        if (attacker instanceof Player p) {
            Via via = source.is(GOD_POWER) || powerOwner(direct).isPresent() ? Via.POWER
                    : direct instanceof Projectile || direct instanceof TraceableEntity && direct != p ? Via.PROJECTILE
                    : Via.DIRECT;
            return new Blame(Optional.of(p.getUUID()), via);
        }
        Via via = direct instanceof Projectile && direct != attacker ? Via.PROJECTILE : Via.NONE;
        return resolve(attacker, via, 0);
    }

    /** 从实体往上找到玩家（最多几层，防止循环）。 */
    private static Blame resolve(Entity e, Via via, int depth) {
        if (e == null || depth > 4) {
            return Blame.NONE;
        }
        if (e instanceof VillageDefender) {
            return Blame.SOLDIER;
        }
        if (e instanceof Player p) {
            return new Blame(Optional.of(p.getUUID()), via == Via.NONE ? Via.DIRECT : via);
        }
        Optional<UUID> power = powerOwner(e);
        if (power.isPresent()) {
            return new Blame(power, Via.POWER);
        }
        if (e instanceof OwnedSummon s && s.summonOwner() != null) {
            return new Blame(Optional.of(s.summonOwner()), Via.SUMMON);
        }
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) {
            return new Blame(Optional.of(o.getOwnerUUID()), Via.PET);
        }
        if (e instanceof Projectile pr) {
            return resolve(pr.getOwner(), Via.PROJECTILE, depth + 1);
        }
        if (e instanceof TraceableEntity t) {
            return resolve(t.getOwner(), Via.PROJECTILE, depth + 1);
        }
        return Blame.NONE;
    }

    /** 神力产生的临时实体（{@code mythic_age:power} 标签里的施法者）。 */
    private static Optional<UUID> powerOwner(Entity e) {
        if (e != null && e.getPersistentData().contains(POWER_TAG)) {
            var t = e.getPersistentData().getCompound(POWER_TAG);
            if (t.hasUUID("owner")) {
                return Optional.of(t.getUUID("owner"));
            }
        }
        return Optional.empty();
    }
}
