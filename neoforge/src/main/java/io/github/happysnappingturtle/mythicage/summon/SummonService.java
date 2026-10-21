package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.summon.RespawnRules;
import io.github.happysnappingturtle.mythicage.core.summon.SummonCap;
import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import io.github.happysnappingturtle.mythicage.core.summon.SummonUnitCard;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 召唤框架的服务端逻辑（T14 设计文档 §2–§5）：召唤与上限、跟随瞬移、主人上线拉回、换维度随行、
 * 死亡 / 重生 / 遣散、实体载入时与登记对照、指令。
 */
public final class SummonService {
    /** 换维度时随主人一起过去的半径（§9 D5）。 */
    public static final int DIMENSION_FOLLOW_RADIUS = 16;
    /** 传令杖指令作用半径（§9 D3）。 */
    public static final int COMMAND_RADIUS = 32;

    /** 主人换维度前，16 格内跟随单位的 unitId（换维度事件之后再搬过去）。 */
    private static final Map<UUID, List<UUID>> TRAVELLING = new HashMap<>();

    public enum Result {
        OK, CAP_FULL, NO_CARD, NO_ENTITY, NO_SPACE
    }

    private SummonService() {
    }

    // ───────────── 上限 ─────────────

    public static Age highestAge(ServerPlayer p) {
        List<Age> ages = new ArrayList<>();
        MythData.get(p).civsView().values().forEach(s -> ages.add(s.age()));
        return SummonCap.highest(ages);
    }

    public static int cap(ServerPlayer p) {
        return SummonCap.cap(highestAge(p));
    }

    public static int occupied(ServerPlayer p) {
        return SummonRegistry.get(p.server).occupied(p.getUUID());
    }

    public static boolean fits(ServerPlayer p, SummonUnitCard card) {
        return SummonCap.fits(occupied(p), card.cost(), cap(p));
    }

    // ───────────── 召唤 ─────────────

    /** 在 pos 召出 card 对应的单位，主人为 owner。不扣召唤符（由调用方处理）。 */
    public static Result summon(ServerPlayer owner, SummonUnitCard card, Vec3 pos) {
        if (!fits(owner, card)) {
            return Result.CAP_FULL;
        }
        ServerLevel level = owner.serverLevel();
        Entity e = create(level, card);
        if (!(e instanceof MythSummon s) || !(e instanceof PathfinderMob mob)) {
            return Result.NO_ENTITY;
        }
        UUID unitId = e.getUUID();
        bind(s, owner.getUUID(), unitId, card.key());
        float facing = owner.getYRot() + 180;
        e.moveTo(pos.x, pos.y, pos.z, facing, 0);
        mob.setYBodyRot(facing);
        mob.setYHeadRot(facing);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        s.applyStats(SummonUnits.stats(card.key()));
        SummonRegistry.Unit u = SummonRegistry.get(owner.server).add(unitId, owner.getUUID(), card.key());
        u.entity = unitId;
        u.last = GlobalPos.of(level.dimension(), e.blockPosition());
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 30, 0.6, 1.0, 0.6, 0.05);
        return Result.OK;
    }

    @Nullable
    private static Entity create(ServerLevel level, SummonUnitCard card) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(card.entity())).orElse(null);
        return type == null ? null : type.create(level);
    }

    private static void bind(MythSummon s, UUID owner, UUID unitId, String card) {
        SummonLink l = s.link();
        l.owner = owner;
        l.unitId = unitId;
        l.card = card;
        l.mode = SummonMode.FOLLOW;
        l.guard = null;
        l.forcedTarget = null;
    }

    /** 主人面前 2 格的落点（脚下有方块、身体位置空）；找不到时用主人脚下。 */
    public static Vec3 frontOf(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 base = p.position().add(look.scale(2.0));
        return Vec3.atBottomCenterOf(BlockPos.containing(base));
    }

    /** 瞬移到主人附近的空地（同原版狼：主人周围 2–3 格，脚下实心、身体位置空）。 */
    public static void teleportNear(Mob mob, ServerPlayer owner) {
        BlockPos o = owner.blockPosition();
        for (int i = 0; i < 12; i++) {
            int dx = mob.getRandom().nextIntBetweenInclusive(-3, 3);
            int dz = mob.getRandom().nextIntBetweenInclusive(-3, 3);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos p = o.offset(dx, dy, dz);
                if (owner.level().getBlockState(p.below()).isSolidRender(owner.level(), p.below())
                        && owner.level().noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(p)))) {
                    mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, mob.getYRot(), mob.getXRot());
                    mob.getNavigation().stop();
                    return;
                }
            }
        }
    }

    // ───────────── 指令 ─────────────

    /** 主人周围 radius 格内、同维度的自己的单位实体。 */
    public static List<Mob> nearbyUnits(ServerPlayer owner, int radius) {
        List<Mob> out = new ArrayList<>();
        for (Entity e : owner.serverLevel().getEntitiesOfClass(Mob.class, new AABB(owner.blockPosition()).inflate(radius),
                x -> x instanceof MythSummon s && owner.getUUID().equals(s.link().owner))) {
            out.add((Mob) e);
        }
        return out;
    }

    public static void setMode(Mob unit, SummonMode mode, @Nullable BlockPos guard) {
        MythSummon s = (MythSummon) unit;
        s.link().mode = mode;
        s.link().guard = mode == SummonMode.GUARD ? guard : null;
        s.link().forcedTarget = null;
        unit.setTarget(null);
        unit.getNavigation().stop();
        if (unit.level() instanceof ServerLevel sl && s.link().unitId != null) {
            SummonRegistry reg = SummonRegistry.get(sl.getServer());
            reg.get(s.link().unitId).ifPresent(u -> {
                u.mode = mode;
                u.guard = guard == null ? null : GlobalPos.of(sl.dimension(), guard);
                reg.setDirty();
            });
        }
    }

    /** 传令杖右键方块：32 格内跟随中的单位改为驻守该点。返回受影响的数量。 */
    public static int commandGuard(ServerPlayer owner, BlockPos post) {
        int n = 0;
        for (Mob m : nearbyUnits(owner, COMMAND_RADIUS)) {
            if (((MythSummon) m).link().mode == SummonMode.FOLLOW) {
                setMode(m, SummonMode.GUARD, post);
                n++;
            }
        }
        return n;
    }

    /** 传令杖右键生物：32 格内战斗单位集火；目标不合法返回 -1。 */
    public static int commandAttack(ServerPlayer owner, LivingEntity target) {
        List<Mob> units = nearbyUnits(owner, COMMAND_RADIUS).stream().filter(m -> ((MythSummon) m).combatUnit()).toList();
        if (units.isEmpty()) {
            return 0;
        }
        if (!SummonTargets.canAttack(units.get(0), target)) {
            return -1;
        }
        for (Mob m : units) {
            ((MythSummon) m).link().forcedTarget = target.getUUID();
            m.setTarget(target);
        }
        return units.size();
    }

    /** 传令杖潜行右键：32 格内战斗单位立即放特殊攻击（冷却中的跳过）。返回放出的数量；目标不合法 -1。 */
    public static int commandSpecial(ServerPlayer owner, LivingEntity target) {
        int n = 0;
        boolean anyCombat = false;
        for (Mob m : nearbyUnits(owner, COMMAND_RADIUS)) {
            MythSummon s = (MythSummon) m;
            if (!s.combatUnit()) {
                continue;
            }
            anyCombat = true;
            if (!SummonTargets.canAttack(m, target)) {
                return -1;
            }
            if (s.requestSpecial(target)) {
                n++;
            }
        }
        return anyCombat ? n : 0;
    }

    // ───────────── 脱战（顶飞） ─────────────

    /** 被顶飞的生物：UUID → 到这一刻之前不能重新选目标。 */
    private static final Map<UUID, Long> DISENGAGED = new HashMap<>();

    public static void disengage(Mob mob, int ticks) {
        DISENGAGED.put(mob.getUUID(), mob.level().getGameTime() + ticks);
    }

    public static boolean disengaged(Entity e) {
        Long until = DISENGAGED.get(e.getUUID());
        if (until == null) {
            return false;
        }
        if (e.level().getGameTime() >= until) {
            DISENGAGED.remove(e.getUUID());
            return false;
        }
        return true;
    }

    public static void onChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && disengaged(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ───────────── 遣散 ─────────────

    public static boolean dismiss(MinecraftServer server, UUID unitId) {
        SummonRegistry reg = SummonRegistry.get(server);
        Optional<SummonRegistry.Unit> u = reg.get(unitId);
        if (u.isEmpty()) {
            return false;
        }
        Entity e = find(server, u.get());
        if (e != null) {
            if (e.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
            }
            e.discard();
        }
        reg.remove(unitId);
        return true;
    }

    /** 已载入的实体（按登记里的实体 UUID 在所有维度里找）。 */
    @Nullable
    public static Entity find(MinecraftServer server, SummonRegistry.Unit u) {
        if (u.entity == null) {
            return null;
        }
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(u.entity);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    // ───────────── 事件 ─────────────

    /** 实体载入：与登记对照（已遣散 / 已死亡 / 有更新的化身 → 自删），并以登记的模式与驻守点为准。 */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof MythSummon s) || !s.link().bound()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        SummonRegistry reg = SummonRegistry.get(level.getServer());
        Optional<SummonRegistry.Unit> u = reg.get(s.link().unitId);
        if (u.isEmpty() || u.get().respawning || (u.get().entity != null && !u.get().entity.equals(event.getEntity().getUUID()))) {
            MythicAge.LOGGER.debug("召唤单位 {} 与登记不符，移除", event.getEntity().getUUID());
            event.setCanceled(true);
            return;
        }
        s.link().mode = u.get().mode;
        s.link().guard = u.get().guard != null && u.get().guard.dimension().equals(level.dimension()) ? u.get().guard.pos() : null;
    }

    /** 实体离开世界（区块卸载、换维度、死亡）：写回最后位置。 */
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof MythSummon s) || !s.link().bound()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        SummonRegistry reg = SummonRegistry.get(level.getServer());
        reg.get(s.link().unitId).ifPresent(u -> {
            if (event.getEntity().getUUID().equals(u.entity)) {
                u.last = GlobalPos.of(level.dimension(), event.getEntity().blockPosition());
                reg.setDirty();
            }
        });
    }

    /** 主人打不到自己的单位；单位之间、单位对主人也不互伤。 */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity src = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();
        if (victim instanceof MythSummon s && src != null && s.link().owner != null) {
            if (src.getUUID().equals(s.link().owner) || (src instanceof MythSummon o && s.link().owner.equals(o.link().owner))) {
                event.setCanceled(true);
            }
        } else if (src instanceof MythSummon s && s.link().owner != null && victim.getUUID().equals(s.link().owner)) {
            event.setCanceled(true);
        }
    }

    /** 吟唱召唤符时受伤：打断，召唤符不消耗（同神力吟唱，T12）。 */
    public static void onPlayerHurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.isUsingItem() && event.getNewDamage() > 0
                && p.getUseItem().getItem() instanceof SummonTokenItem) {
            p.stopUsingItem();
            p.displayClientMessage(Component.translatable("mythic_age.summon.interrupted"), true);
        }
    }

    /** 单位死亡：没有重生规则的删登记、释放上限；侦察单位转为等待重生（仍占位）。 */
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof MythSummon s) || !s.link().bound() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        SummonRegistry reg = SummonRegistry.get(server);
        Optional<SummonRegistry.Unit> u = reg.get(s.link().unitId);
        if (u.isEmpty()) {
            return;
        }
        Optional<SummonUnitCard> card = SummonUnits.card(u.get().card);
        ServerPlayer owner = server.getPlayerList().getPlayer(u.get().owner);
        if (card.isPresent() && card.get().respawns()) {
            u.get().respawning = true;
            u.get().entity = null;
            u.get().respawnAt = RespawnRules.respawnAt(level.getGameTime(), card.get().respawnDelayDays());
            reg.setDirty();
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("mythic_age.summon.fallen_respawn", event.getEntity().getDisplayName(),
                        card.get().respawnDelayDays()));
            }
        } else {
            reg.remove(u.get().unitId);
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("mythic_age.summon.fallen", event.getEntity().getDisplayName()));
            }
        }
    }

    /** 主人上线：同维度的跟随单位拉回身边（未载入的区块临时载入取出实体，§9 D9）。 */
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            pullFollowers(p);
        }
    }

    /** 等待拉回的单位：unitId → (主人, 区块, 剩余尝试次数)。实体随区块异步读入，所以要重试几次。 */
    private record Pending(UUID owner, ServerLevel level, ChunkPos chunk, int tries) {
    }

    private static final Map<UUID, Pending> PENDING_PULL = new HashMap<>();

    public static void pullFollowers(ServerPlayer p) {
        SummonRegistry reg = SummonRegistry.get(p.server);
        for (SummonRegistry.Unit u : reg.byOwner(p.getUUID())) {
            if (u.respawning || u.mode != SummonMode.FOLLOW || u.last == null || !u.last.dimension().equals(p.level().dimension())) {
                continue;
            }
            Entity e = find(p.server, u);
            if (e instanceof Mob m && e.level() == p.level()) {
                teleportNear(m, p);
            } else if (e == null && !PENDING_PULL.containsKey(u.unitId)) {
                ChunkPos c = new ChunkPos(u.last.pos());
                p.serverLevel().setChunkForced(c.x, c.z, true);
                PENDING_PULL.put(u.unitId, new Pending(p.getUUID(), p.serverLevel(), c, 10));
            }
        }
    }

    /** 每 20 刻处理一次等待拉回的单位：取到实体就拉到主人身边并解除临时强制加载。 */
    private static void tickPending(MinecraftServer server) {
        if (PENDING_PULL.isEmpty()) {
            return;
        }
        SummonRegistry reg = SummonRegistry.get(server);
        for (UUID id : List.copyOf(PENDING_PULL.keySet())) {
            Pending pd = PENDING_PULL.get(id);
            ServerPlayer owner = server.getPlayerList().getPlayer(pd.owner());
            Optional<SummonRegistry.Unit> u = reg.get(id);
            Entity e = u.map(x -> find(server, x)).orElse(null);
            boolean done = owner == null || u.isEmpty() || pd.tries() <= 1;
            if (e instanceof Mob m && owner != null && e.level() == owner.level()) {
                teleportNear(m, owner);
                done = true;
            }
            if (done) {
                pd.level().setChunkForced(pd.chunk().x, pd.chunk().z, false);
                PENDING_PULL.remove(id);
            } else {
                PENDING_PULL.put(id, new Pending(pd.owner(), pd.level(), pd.chunk(), pd.tries() - 1));
            }
        }
    }

    /** 主人换维度之前：记下 16 格内跟随中的单位。 */
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        List<UUID> ids = new ArrayList<>();
        for (Mob m : nearbyUnits(p, DIMENSION_FOLLOW_RADIUS)) {
            if (((MythSummon) m).link().mode == SummonMode.FOLLOW) {
                ids.add(m.getUUID());
            }
        }
        if (!ids.isEmpty()) {
            TRAVELLING.put(p.getUUID(), ids);
        }
    }

    /** 主人换维度之后：把记下的单位一起搬过去（实体换新 UUID，登记跟着改）。 */
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        List<UUID> ids = TRAVELLING.remove(p.getUUID());
        if (ids == null) {
            return;
        }
        ServerLevel from = p.server.getLevel(event.getFrom());
        if (from == null) {
            return;
        }
        SummonRegistry reg = SummonRegistry.get(p.server);
        for (UUID id : ids) {
            Entity e = from.getEntity(id);
            if (!(e instanceof MythSummon s)) {
                continue;
            }
            UUID unitId = s.link().unitId;
            Entity moved = e.changeDimension(new DimensionTransition(p.serverLevel(), p.position(), Vec3.ZERO, p.getYRot(),
                    0, DimensionTransition.DO_NOTHING));
            if (moved != null) {
                reg.get(unitId).ifPresent(u -> {
                    u.entity = moved.getUUID();
                    u.last = GlobalPos.of(p.level().dimension(), moved.blockPosition());
                    reg.setDirty();
                });
                if (moved instanceof Mob m) {
                    teleportNear(m, p);
                }
            }
        }
    }

    /** 每 100 刻：到时间且主人有本文明神坛的侦察单位重生（§5.4）。 */
    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 == 0) {
            tickPending(server);
        }
        if (server.getTickCount() % 100 == 0) {
            tickRespawns(server);
        }
    }

    public static void tickRespawns(MinecraftServer server) {
        SummonRegistry reg = SummonRegistry.get(server);
        long now = server.overworld().getGameTime();
        for (SummonRegistry.Unit u : reg.all()) {
            if (!u.respawning || now < u.respawnAt) {
                continue;
            }
            Optional<SummonUnitCard> card = SummonUnits.card(u.card);
            if (card.isEmpty()) {
                continue;
            }
            Optional<ShrineRegistry.Entry> shrine = Civilization.byId(card.get().civ())
                    .flatMap(c -> ShrineRegistry.get(server).ownedBy(u.owner, c).stream().findFirst());
            if (!RespawnRules.ready(now, u.respawnAt, shrine.isPresent())) {
                continue;
            }
            ServerLevel level = server.getLevel(shrine.get().pos().dimension());
            if (level == null) {
                continue;
            }
            Entity e = create(level, card.get());
            if (!(e instanceof MythSummon s) || !(e instanceof PathfinderMob mob)) {
                continue;
            }
            bind(s, u.owner, u.unitId, u.card);
            s.link().mode = u.mode == SummonMode.GUARD ? SummonMode.FOLLOW : u.mode;
            BlockPos at = shrine.get().pos().pos().relative(shrine.get().facing(), 2);
            e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null);
            s.applyStats(SummonUnits.stats(card.get().key()));
            u.respawning = false;
            u.entity = e.getUUID();
            u.mode = s.link().mode;
            u.guard = null;
            u.last = GlobalPos.of(level.dimension(), at);
            reg.setDirty();
            level.addFreshEntity(e);
            level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 30, 0.6, 1.0, 0.6, 0.05);
            ServerPlayer owner = server.getPlayerList().getPlayer(u.owner);
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("mythic_age.summon.respawned", e.getDisplayName()));
            }
        }
    }

    /** 开发命令：{@code /mythicage dev summon <文明:id>} 直接在面前召唤（不用召唤符、不吟唱，仍受上限约束）。 */
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("mythicage")
                .then(net.minecraft.commands.Commands.literal("dev").requires(s -> s.hasPermission(2))
                        .then(net.minecraft.commands.Commands.literal("summon")
                                .then(net.minecraft.commands.Commands.argument("card",
                                                com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String key = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "card");
                                            Optional<SummonUnitCard> c = SummonUnits.card(key);
                                            if (c.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("没有单位卡 " + key));
                                                return 0;
                                            }
                                            Result r = summon(p, c.get(), frontOf(p));
                                            ctx.getSource().sendSuccess(() -> Component.literal("召唤 " + key + "：" + r), false);
                                            return r == Result.OK ? 1 : 0;
                                        })))));
    }

    public static void onServerStopped() {
        TRAVELLING.clear();
        PENDING_PULL.clear();
        DISENGAGED.clear();
        SummonTargets.clear();
    }
}
