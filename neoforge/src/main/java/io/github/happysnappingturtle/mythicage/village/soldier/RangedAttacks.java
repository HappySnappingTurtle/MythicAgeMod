package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程兵种的投射物（T20 设计文档 §8.2）：出手的时间轴、瞄准、射线遮挡、近身后撤都在 {@link SoldierBrain} 里通用处理；
 * 各文明只按弹药物品注册「怎么生成这支投射物」（希腊：投矛、粗投矛）。没有注册的弹药，远程兵种退为护送。
 */
public final class RangedAttacks {
    /** 生成一支投射物并加进世界。 */
    public interface Launcher {
        Entity launch(ServerLevel level, ResidentEntity soldier, UUID village, String item, float damage, Vec3 from, Vec3 velocity,
                      SoldierProfile profile);
    }

    private static final Map<String, Launcher> BY_AMMO = new ConcurrentHashMap<>();

    private RangedAttacks() {
    }

    public static void register(String ammoItem, Launcher l) {
        BY_AMMO.put(ammoItem, l);
    }

    public static Optional<Launcher> launcher(String ammoItem) {
        return Optional.ofNullable(BY_AMMO.get(ammoItem));
    }
}
