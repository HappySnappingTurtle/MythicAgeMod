package io.github.happysnappingturtle.mythicage.combat;

import io.github.happysnappingturtle.mythicage.boss.TrialBoss;
import io.github.happysnappingturtle.mythicage.core.combat.DamageZones;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 伤害乘区的平台层（T25.1）：本作唯一改 {@link LivingIncomingDamageEvent} 伤害量的地方。
 * <p>
 * 每次伤害先问各个 {@link Gate}（停火、无敌这类「不进乘区、直接不算」的判定），再按登记顺序问各个 {@link Provider} 要修正项，
 * 交给 core {@link DamageZones} 算一次，最后 {@code setAmount} 一次。原版护甲、韧性、保护附魔、抗性提升都在这之后照常算。
 * <p>
 * 用 {@link EventPriority#LOW}：别的模组在更早的优先级改过的伤害量，本作在它的结果上算（TC-T25.1-31）。
 * 各内容只登记「哪个区、哪个效果组、多少」，不自己乘。
 */
public final class DamagePipeline {
    /** 一次伤害：谁挨打、从哪来、攻击者是哪一类、目标是哪一类。 */
    public record Hit(LivingEntity victim, DamageSource source, @Nullable Entity attacker, DamageZones.Source kind,
                      DamageZones.Target target) {
    }

    /** 修正项提供者。 */
    @FunctionalInterface
    public interface Provider {
        void collect(Hit hit, List<DamageZones.Mod> out);
    }

    /** 不进乘区的判定：返回 true 时这次伤害取消（伤害归零）。 */
    @FunctionalInterface
    public interface Gate {
        boolean blocks(Hit hit);
    }

    /** 攻击者自己说明属于哪一类（士兵、哨兵石像）；没实现的按 {@link #classify} 的默认规则。 */
    public interface Attacker {
        DamageZones.Source damageKind();
    }

    /** 目标自己说明属于哪一类（精英）；Boss 按 {@link TrialBoss} 认。 */
    public interface Targeted {
        DamageZones.Target damageTarget();
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<Gate> GATES = new CopyOnWriteArrayList<>();
    private static final Set<ResourceKey<DamageType>> POWER_TYPES = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> DEBUG = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Component> LAST_DEBUG = new ConcurrentHashMap<>();

    private DamagePipeline() {
    }

    public static void register(IEventBus game) {
        game.addListener(EventPriority.LOW, DamagePipeline::onIncomingDamage);
        game.addListener(DamagePipeline::onRegisterCommands);
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> debug(e.getEntity().getUUID(), false));
    }

    /** {@code /mythicage dev damage on|off}：只有管理员能开，正式包里也有（D8），不影响玩法。 */
    static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent e) {
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("mythicage")
                .then(net.minecraft.commands.Commands.literal("dev").requires(src -> src.hasPermission(2))
                        .then(net.minecraft.commands.Commands.literal("damage")
                                .then(net.minecraft.commands.Commands.literal("on").executes(c -> toggle(c.getSource(), true)))
                                .then(net.minecraft.commands.Commands.literal("off").executes(c -> toggle(c.getSource(), false))))));
    }

    private static int toggle(net.minecraft.commands.CommandSourceStack src, boolean on) throws
            com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        debug(p.getUUID(), on);
        src.sendSuccess(() -> Component.translatable(on ? "mythic_age.dev.damage.on" : "mythic_age.dev.damage.off"), false);
        return 1;
    }

    public static void provider(Provider p) {
        PROVIDERS.add(p);
    }

    public static void gate(Gate g) {
        GATES.add(g);
    }

    /** 这种伤害类型是神力造成的（施法者是玩家，也按神力算，不吃装备与弹药区）。 */
    public static void powerType(ResourceKey<DamageType> type) {
        POWER_TYPES.add(type);
    }

    // ───────────── 分类 ─────────────

    public static DamageZones.Source classify(DamageSource source, @Nullable Entity attacker) {
        for (ResourceKey<DamageType> k : POWER_TYPES) {
            if (source.is(k)) {
                return DamageZones.Source.POWER;
            }
        }
        if (attacker instanceof Attacker a) {
            return a.damageKind();
        }
        if (attacker instanceof Player) {
            return DamageZones.Source.PLAYER;
        }
        if (attacker instanceof MythSummon) {
            return DamageZones.Source.SUMMON;
        }
        return DamageZones.Source.MOB;
    }

    public static DamageZones.Target targetOf(LivingEntity victim) {
        if (victim instanceof TrialBoss) {
            return DamageZones.Target.BOSS;
        }
        if (victim instanceof Targeted t) {
            return t.damageTarget();
        }
        return DamageZones.Target.NORMAL;
    }

    public static Hit hit(LivingEntity victim, DamageSource source) {
        Entity attacker = source.getEntity();
        return new Hit(victim, source, attacker, classify(source, attacker), targetOf(victim));
    }

    // ───────────── 事件 ─────────────

    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Hit hit = hit(victim, e.getSource());
        for (Gate g : GATES) {
            if (g.blocks(hit)) {
                e.setCanceled(true);
                return;
            }
        }
        List<DamageZones.Mod> mods = new ArrayList<>();
        for (Provider p : PROVIDERS) {
            p.collect(hit, mods);
        }
        if (mods.isEmpty() && !debugging(hit)) {
            return;
        }
        float before = e.getAmount();
        DamageZones.Result r = DamageZones.compute(before, hit.kind(), hit.target(), mods);
        if (!mods.isEmpty()) {
            e.setAmount((float) r.total());
        }
        if (debugging(hit)) {
            Component line = Component.translatable("mythic_age.dev.damage.line", victim.getDisplayName(),
                    Component.translatable("mythic_age.dev.damage.source." + hit.kind().name().toLowerCase(java.util.Locale.ROOT)),
                    breakdown(before, r));
            for (Entity who : new Entity[]{hit.attacker(), victim}) {
                if (who instanceof ServerPlayer sp && DEBUG.contains(sp.getUUID())) {
                    LAST_DEBUG.put(sp.getUUID(), line);
                    sp.sendSystemMessage(line);
                }
            }
        }
    }

    /** 明细：「9.0 ×1.25 装备 ×1.5 弹药 = 16.88（护甲前）」，区名按看的人的语言翻译；写法同 {@link DamageZones#describe}。 */
    static MutableComponent breakdown(double amount, DamageZones.Result r) {
        MutableComponent out = Component.literal(num(amount));
        for (DamageZones.Line l : r.lines()) {
            if (l.zone() == DamageZones.Zone.BASE) {
                out.append((l.factor() >= 0 ? "+" : "") + num(l.factor()));
            } else {
                out.append(" ×" + num(l.factor()) + " ")
                        .append(Component.translatable("mythic_age.dev.damage.zone." + l.zone().name().toLowerCase(java.util.Locale.ROOT)));
            }
        }
        if (r.capped()) {
            out.append(" ≤×" + num(DamageZones.BOOST_CAP));
        }
        return out.append(" = " + num(r.total())).append(Component.translatable("mythic_age.dev.damage.before_armor"));
    }

    private static String num(double v) {
        return DamageZones.fmt(v);
    }

    // ───────────── 调试（/mythicage dev damage on|off，D8） ─────────────

    public static void debug(UUID player, boolean on) {
        if (on) {
            DEBUG.add(player);
        } else {
            DEBUG.remove(player);
            LAST_DEBUG.remove(player);
        }
    }

    public static boolean debugging(UUID player) {
        return DEBUG.contains(player);
    }

    private static boolean debugging(Hit hit) {
        return !DEBUG.isEmpty() && ((hit.attacker() != null && DEBUG.contains(hit.attacker().getUUID()))
                || DEBUG.contains(hit.victim().getUUID()));
    }

    /** 只算不扣：这次伤害按各提供者会算成多少（不经判定、不改事件；提供者若有一次性的窗口会被消耗）。GameTest 与调试用。 */
    public static DamageZones.Result evaluate(LivingEntity victim, DamageSource source, float amount) {
        Hit hit = hit(victim, source);
        List<DamageZones.Mod> mods = new ArrayList<>();
        for (Provider p : PROVIDERS) {
            p.collect(hit, mods);
        }
        return DamageZones.compute(amount, hit.kind(), hit.target(), mods);
    }

    /** 只供 GameTest：发给这名（假）玩家的最近一行明细。 */
    public static @Nullable Component lastDebugForTest(UUID player) {
        return LAST_DEBUG.get(player);
    }

    /** 只供 GameTest：临时登记一个判定 / 提供者，用完 {@link #unregisterForTest}。 */
    public static void registerForTest(@Nullable Gate g, @Nullable Provider p) {
        if (g != null) {
            GATES.add(g);
        }
        if (p != null) {
            PROVIDERS.add(p);
        }
    }

    public static void unregisterForTest(@Nullable Gate g, @Nullable Provider p) {
        if (g != null) {
            GATES.remove(g);
        }
        if (p != null) {
            PROVIDERS.remove(p);
        }
    }
}
