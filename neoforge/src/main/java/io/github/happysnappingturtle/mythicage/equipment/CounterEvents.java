package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.core.equipment.CounterWindow;
import io.github.happysnappingturtle.mythicage.core.equipment.EquipmentTable;
import io.github.happysnappingturtle.mythicage.relic.RelicEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 盾的格挡反击窗口（T17 §4.2）：用圆盾一族格挡成功、盾没被斧子破掉 → 打开窗口；窗口内第一次近战命中 +加成。
 * 窗口长度与加成来自数值表里那面盾的数据（counter_ticks、counter_bonus）。
 */
public final class CounterEvents {
    private record Open(CounterWindow window, double bonus) {
    }

    private static final Map<UUID, Open> WINDOWS = new ConcurrentHashMap<>();
    /** 举盾刺击造成的伤害不消耗窗口（同一次攻击只吃一个来源，合同 10-2）。 */
    private static final ThreadLocal<Boolean> THRUST = ThreadLocal.withInitial(() -> false);

    private CounterEvents() {
    }

    /** 盾牌 → 数值表里的那一条（没有反击窗口时为 null）。 */
    @javax.annotation.Nullable
    static EquipmentTable.Weapon counterStats(ItemStack shield) {
        if (!shield.is(EquipmentTags.ASPIS_FAMILY) || !(shield.getItem() instanceof CounterShield c)) {
            return null;
        }
        EquipmentTable.Weapon w = c.counterStats();
        return w != null && w.counterTicks() > 0 ? w : null;
    }

    /** 带反击窗口的盾。 */
    public interface CounterShield {
        EquipmentTable.Weapon counterStats();
    }

    public static void onShieldBlock(LivingShieldBlockEvent e) {
        if (e.isCanceled() || !e.getBlocked() || !(e.getEntity() instanceof Player p) || p.level().isClientSide) {
            return;
        }
        blocked(p, e.getDamageSource(), p.getUseItem(), p.level().getGameTime());
    }

    /** 格挡成功（GameTest 也直接调用）。破盾的那次不开窗口（D6）。 */
    public static boolean blocked(Player p, DamageSource source, ItemStack shield, long now) {
        EquipmentTable.Weapon w = counterStats(shield);
        if (w == null) {
            return false;
        }
        boolean disables = !source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() instanceof LivingEntity le
                && le.canDisableShield();
        if (disables) {
            WINDOWS.remove(p.getUUID());
            return false;
        }
        CounterWindow win = new CounterWindow();
        win.open(now, w.counterTicks());
        WINDOWS.put(p.getUUID(), new Open(win, w.counterBonus()));
        return true;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (THRUST.get() || !(e.getSource().getEntity() instanceof Player p) || p.level().isClientSide
                || !RelicEffects.isMelee(e.getSource())) {
            return;
        }
        Open o = WINDOWS.get(p.getUUID());
        if (o != null && o.window().consume(p.level().getGameTime())) {
            e.setAmount(e.getAmount() + (float) o.bonus());
            WINDOWS.remove(p.getUUID());
        }
    }

    /** 在举盾刺击里结算伤害。 */
    static void thrusting(Runnable hit) {
        THRUST.set(true);
        try {
            hit.run();
        } finally {
            THRUST.set(false);
        }
    }

    public static boolean isOpen(Player p) {
        Open o = WINDOWS.get(p.getUUID());
        return o != null && o.window().isOpen(p.level().getGameTime());
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        WINDOWS.clear();
    }
}
