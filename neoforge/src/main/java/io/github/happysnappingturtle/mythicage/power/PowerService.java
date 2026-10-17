package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxStep;
import io.github.happysnappingturtle.mythicage.core.power.CastSession;
import io.github.happysnappingturtle.mythicage.core.power.CooldownRules;
import io.github.happysnappingturtle.mythicage.core.power.PowerReason;
import io.github.happysnappingturtle.mythicage.core.power.PowerSpec;
import io.github.happysnappingturtle.mythicage.core.power.TargetCategory;
import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.core.power.TargetFacts;
import io.github.happysnappingturtle.mythicage.core.power.TargetProfile;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 神力施放（T12 设计文档 §2.3）：右键进入瞄准 → 再次右键确认目标并吟唱 → 吟唱满刻后全部复查 → 事务提交
 * （扣冷却、收据）→ 扣 1 件道具 → 执行效果。提交前任何失败都不消耗道具、不碰恩宠。
 * <p>
 * 打断（D1）：吟唱中受伤、切换手持、打开界面、死亡 / 换维度、目标失效或超距；移动不打断，但吟唱时减速。
 * 瞄准（D2）：潜行 + 右键、切换手持、打开界面取消；超时自动取消。每名玩家同时只有一个会话，只在服务端内存。
 */
public final class PowerService {
    private static final ResourceLocation SLOW_ID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "power_channel");
    private static final double SLOW = -0.6;

    /** 进行中的会话。 */
    static final class Active {
        final ServerPlayer player;
        final CastSession session;
        final PowerSpec spec;
        final InteractionHand hand;
        final int slot;
        final Item item;
        final ResourceKey<Level> dim;
        @Nullable Entity target;
        @Nullable TargetCategory category;
        String reaction = TargetClassifier.NONE;
        Vec3 origin;
        @Nullable BlockPos blockPos;
        @Nullable Direction face;

        Active(ServerPlayer player, CastSession session, PowerSpec spec, InteractionHand hand, int slot, Item item) {
            this.player = player;
            this.session = session;
            this.spec = spec;
            this.hand = hand;
            this.slot = slot;
            this.item = item;
            this.dim = player.level().dimension();
            this.origin = player.position();
        }
    }

    private static final Map<UUID, Active> SESSIONS = new ConcurrentHashMap<>();
    /** 只供 GameTest：各玩家最近一次结束的会话结果。 */
    private static final Map<UUID, PowerReason> LAST = new ConcurrentHashMap<>();

    private PowerService() {
    }

    static WorldRulesConfig.GodPowers config() {
        return ModConfigs.worldRules().current().godPowers();
    }

    // ───────────── 右键 ─────────────

    /** 带 {@code mythic_age:god_power} 组件的普通物品也走这里（测试与数据驱动用）。 */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p && !(event.getItemStack().getItem() instanceof GodPowerItem)
                && event.getItemStack().has(PowerItems.GOD_POWER.get())) {
            use(p, event.getHand());
            event.setCanceled(true);
        }
    }

    /** 右键神力道具。返回这一下的结果（瞄准开始、开始吟唱或原因）。 */
    public static PowerReason use(ServerPlayer player, InteractionHand hand) {
        Active a = SESSIONS.get(player.getUUID());
        if (a != null) {
            if (player.isShiftKeyDown()) {
                end(a, PowerReason.CANCELLED);
                return PowerReason.CANCELLED;
            }
            if (a.session.state() == CastSession.State.AIMING) {
                return confirm(a);
            }
            return PowerReason.OK;
        }
        if (player.isShiftKeyDown()) {
            return PowerReason.CANCELLED;
        }
        ItemStack stack = player.getItemInHand(hand);
        Optional<String> id = PowerItems.powerOf(stack);
        if (id.isEmpty()) {
            return PowerReason.NO_ITEM;
        }
        Optional<PowerSpec> spec = PowerSpecLoader.get(id.get());
        if (spec.isEmpty() || GodPowers.get(id.get()).isEmpty()) {
            return tell(player, PowerReason.NOT_REGISTERED, 0);
        }
        long now = WorldClock.now(player.server);
        long wait = CooldownRules.remaining(MythData.get(player), spec.get().civ(), id.get(), now);
        if (wait > 0) {
            return tell(player, PowerReason.ON_COOLDOWN, CooldownRules.remainingSeconds(wait));
        }
        CastSession s = CastSession.aim(UUID.randomUUID().toString(), id.get(), now, config().aimTimeoutTicks());
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        SESSIONS.put(player.getUUID(), new Active(player, s, spec.get(), hand, slot, stack.getItem()));
        player.displayClientMessage(Component.translatable("mythic_age.power.aiming",
                Component.translatable("mythic_age.power." + id.get())), true);
        return PowerReason.OK;
    }

    /** 确认：按目标入口选目标或点，校验矩阵、距离、视线与效果自己的条件；通过则开始吟唱。 */
    private static PowerReason confirm(Active a) {
        PowerReason r = pick(a);
        if (r == PowerReason.OK && a.spec.large()) {
            long wait = PowerShields.remaining((ServerLevel) a.player.level(), a.origin);
            if (wait > 0) {
                return tell(a.player, PowerReason.SHIELDED, CooldownRules.remainingSeconds(wait));
            }
        }
        if (r == PowerReason.OK) {
            r = GodPowers.get(a.spec.id()).flatMap(p -> p.validate(context(a, 0))).orElse(PowerReason.OK);
        }
        if (r != PowerReason.OK) {
            // 选得不对不结束瞄准，玩家可以换个目标再确认
            return tell(a.player, r, 0);
        }
        a.session.confirm(WorldClock.now(a.player.server), a.spec.castTicks());
        slow(a.player, true);
        a.player.displayClientMessage(Component.translatable("mythic_age.power.channeling",
                Component.translatable("mythic_age.power." + a.spec.id())), true);
        return PowerReason.OK;
    }

    /** 按目标入口选取，写进会话；返回原因。 */
    static PowerReason pick(Active a) {
        ServerPlayer p = a.player;
        PowerSpec spec = a.spec;
        if (spec.selfCentered()) {
            a.origin = p.position();
            return PowerReason.OK;
        }
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1);
        Vec3 end = eye.add(look.scale(spec.range()));
        BlockHitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double reach = block.getType() == HitResult.Type.MISS ? spec.range() : block.getLocation().distanceTo(eye);
        switch (spec.profile().pick()) {
            case ENTITY -> {
                Vec3 to = eye.add(look.scale(reach));
                EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, to,
                        p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0),
                        e -> !e.isSpectator() && e.isPickable() && e != p);
                if (hit == null) {
                    return PowerReason.NO_TARGET;
                }
                Entity e = hit.getEntity();
                TargetFacts facts = TargetFactsCollector.collect(p, e, TargetFactsCollector.ownVillages(p));
                String reaction = TargetClassifier.reaction(spec, facts);
                if (TargetClassifier.NONE.equals(reaction)) {
                    return PowerReason.INVALID_TARGET;
                }
                a.target = e;
                a.category = TargetClassifier.classify(facts);
                a.reaction = reaction;
                a.origin = e.position();
                return PowerReason.OK;
            }
            case BLOCK -> {
                if (block.getType() == HitResult.Type.MISS) {
                    return PowerReason.PLACEMENT_INVALID;
                }
                a.blockPos = block.getBlockPos();
                a.face = block.getDirection();
                a.origin = Vec3.atBottomCenterOf(block.getBlockPos().relative(block.getDirection()));
                return PowerReason.OK;
            }
            default -> {
                a.origin = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
                return PowerReason.OK;
            }
        }
    }

    // ───────────── 逐刻 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = WorldClock.now(server);
        for (Iterator<Active> it = SESSIONS.values().iterator(); it.hasNext(); ) {
            Active a = it.next();
            Optional<PowerReason> interrupt = interruption(a);
            if (interrupt.isPresent()) {
                end(a, interrupt.get());
                continue;
            }
            CastSession.Event ev = a.session.tick(now);
            if (ev == CastSession.Event.AIM_TIMEOUT) {
                end(a, PowerReason.AIM_TIMEOUT);
            } else if (ev == CastSession.Event.CHANNEL_DONE) {
                PowerReason r = commit(a, now);
                end(a, r);
            }
        }
    }

    /** 不论瞄准还是吟唱都会结束会话的情形；以及吟唱中目标失效。 */
    private static Optional<PowerReason> interruption(Active a) {
        ServerPlayer p = a.player;
        if (!p.isAlive() || p.isRemoved() || p.level().dimension() != a.dim) {
            return Optional.of(PowerReason.INTERRUPTED_DEATH);
        }
        ItemStack held = p.getItemInHand(a.hand);
        boolean switched = held.isEmpty() || held.getItem() != a.item
                || (a.hand == InteractionHand.MAIN_HAND && p.getInventory().selected != a.slot);
        if (switched) {
            return Optional.of(PowerReason.INTERRUPTED_SWITCH);
        }
        if (p.containerMenu != p.inventoryMenu) {
            return Optional.of(PowerReason.INTERRUPTED_SCREEN);
        }
        if (a.session.state() == CastSession.State.CHANNELING && a.target != null && !targetStillValid(a)) {
            return Optional.of(PowerReason.TARGET_LOST);
        }
        return Optional.empty();
    }

    private static boolean targetStillValid(Active a) {
        Entity t = a.target;
        return t != null && t.isAlive() && !t.isRemoved() && t.level() == a.player.level()
                && t.distanceTo(a.player) <= a.spec.range() + 1.0;
    }

    /** 吟唱中受伤 → 打断（D1）。 */
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getNewDamage() > 0) {
            Active a = SESSIONS.get(p.getUUID());
            if (a != null && a.session.state() == CastSession.State.CHANNELING) {
                end(a, PowerReason.INTERRUPTED_DAMAGE);
            }
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Active a = SESSIONS.get(event.getEntity().getUUID());
        if (a != null) {
            end(a, PowerReason.CANCELLED);
        }
    }

    // ───────────── 提交 ─────────────

    /** 吟唱结束：全部复查 → 事务（冷却 + 收据）→ 扣道具 → 执行效果。 */
    static PowerReason commit(Active a, long now) {
        ServerPlayer p = a.player;
        Optional<GodPower> power = GodPowers.get(a.spec.id());
        if (power.isEmpty()) {
            return PowerReason.NOT_REGISTERED;
        }
        if (a.target != null && !targetStillValid(a)) {
            return PowerReason.TARGET_LOST;
        }
        if (a.target != null) {
            a.origin = a.target.position();
            String reaction = TargetClassifier.reaction(a.spec,
                    TargetFactsCollector.collect(p, a.target, TargetFactsCollector.ownVillages(p)));
            if (TargetClassifier.NONE.equals(reaction)) {
                return PowerReason.INVALID_TARGET;
            }
            a.reaction = reaction;
        }
        if (a.spec.large()) {
            long wait = PowerShields.remaining((ServerLevel) p.level(), a.origin);
            if (wait > 0) {
                tell(p, PowerReason.SHIELDED, CooldownRules.remainingSeconds(wait));
                return PowerReason.SHIELDED;
            }
        }
        CastContext ctx = context(a, now);
        Optional<PowerReason> bad = power.get().validate(ctx);
        if (bad.isPresent()) {
            return bad.get();
        }
        ItemStack held = p.getItemInHand(a.hand);
        if (held.isEmpty() || held.getItem() != a.item) {
            return PowerReason.NO_ITEM;
        }
        long shared = config().civSharedCooldownTicks();
        long own = a.spec.cooldownTicks();
        Transactions.Result r = MythData.transact(p, new Transactions.Request("power:" + a.session.castId(),
                ReceiptBook.Scope.REQUEST, java.util.List.of(new TxStep() {
                    @Override
                    public Optional<String> check(PlayerMythData data) {
                        return CooldownRules.remaining(data, a.spec.civ(), a.spec.id(), now) > 0
                                ? Optional.of("cooldown") : Optional.empty();
                    }

                    @Override
                    public void apply(PlayerMythData data) {
                        CooldownRules.apply(data, a.spec.civ(), a.spec.id(), now, shared, own);
                    }
                })));
        if (r.status() == Transactions.Status.DUPLICATE) {
            return PowerReason.DUPLICATE;
        }
        if (!r.committed()) {
            tell(p, PowerReason.ON_COOLDOWN, CooldownRules.remainingSeconds(
                    CooldownRules.remaining(MythData.get(p), a.spec.civ(), a.spec.id(), now)));
            return PowerReason.ON_COOLDOWN;
        }
        if (!p.getAbilities().instabuild) {
            held.shrink(1);
        }
        showCooldowns(p, a.spec, shared, own);
        if (a.spec.large()) {
            // 大型神力：框架自动登记护盾（05 号 §1.2），持续 = 数据里的持续上界
            PowerShields.add((ServerLevel) p.level(), a.origin, a.spec.durationTicks());
        }
        power.get().execute(ctx);
        return PowerReason.COMMITTED;
    }

    static CastContext context(Active a, long now) {
        return new CastContext(a.player, (ServerLevel) a.player.level(), a.spec, a.session.castId(), a.target, a.category,
                a.reaction, a.origin, a.blockPos, a.face, a.session.castId().hashCode() * 31L + now, now);
    }

    /** D4：原版物品冷却遮罩——同文明全部神力物品显示短冷却，本神力物品显示长冷却。 */
    static void showCooldowns(ServerPlayer p, PowerSpec spec, long shared, long own) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof GodPowerItem g) {
                PowerSpecLoader.get(g.powerId()).filter(s -> s.civ() == spec.civ()).ifPresent(s ->
                        p.getCooldowns().addCooldown(item, (int) (s.id().equals(spec.id()) ? Math.max(shared, own) : shared)));
            }
        }
    }

    // ───────────── 收尾 ─────────────

    private static void end(Active a, PowerReason reason) {
        SESSIONS.remove(a.player.getUUID(), a);
        LAST.put(a.player.getUUID(), reason);
        if (reason == PowerReason.COMMITTED) {
            a.session.finish();
        } else {
            a.session.cancel(reason);
        }
        slow(a.player, false);
        // 冷却、护盾带剩余秒数，已在源头提示过
        if (reason != PowerReason.COMMITTED && reason != PowerReason.SHIELDED && reason != PowerReason.ON_COOLDOWN) {
            tell(a.player, reason, 0);
        }
    }

    private static void slow(ServerPlayer p, boolean on) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SLOW_ID);
        if (on) {
            speed.addTransientModifier(new AttributeModifier(SLOW_ID, SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static PowerReason tell(ServerPlayer p, PowerReason reason, long seconds) {
        if (reason != PowerReason.OK && reason != PowerReason.DUPLICATE) {
            p.displayClientMessage(Component.translatable(reason.langKey(), seconds), true);
        }
        return reason;
    }

    // ───────────── 测试 ─────────────

    /** 只供 GameTest：当前会话的状态（没有会话为空）。 */
    public static Optional<CastSession> sessionForTest(ServerPlayer p) {
        return Optional.ofNullable(SESSIONS.get(p.getUUID())).map(a -> a.session);
    }

    /** 只供 GameTest：最近一次结束的会话结果。 */
    public static Optional<PowerReason> lastResultForTest(ServerPlayer p) {
        return Optional.ofNullable(LAST.get(p.getUUID()));
    }

    /** 只供 GameTest：清掉全部会话与记录。 */
    public static void clearForTest() {
        SESSIONS.values().forEach(a -> slow(a.player, false));
        SESSIONS.clear();
        LAST.clear();
    }
}
