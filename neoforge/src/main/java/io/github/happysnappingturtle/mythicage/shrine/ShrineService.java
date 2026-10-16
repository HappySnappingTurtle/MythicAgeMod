package io.github.happysnappingturtle.mythicage.shrine;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 神庙的通用职责（T10b 设计文档 §3.3）：放置检查（数量上限、同种间距、文明规则）、绑定与登记、
 * 无主移除、挖掘保护、移除注销。与具体文明无关；文明特有的规则走 {@link ShrineKind.Hooks}。
 */
public final class ShrineService {
    private static final Map<GlobalPos, Long> OWNER_CHECK = new HashMap<>();

    private ShrineService() {
    }

    static WorldRulesConfig.Shrine rule(ShrineKind kind) {
        return ModConfigs.worldRules().current().shrine(kind.id());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        OWNER_CHECK.clear();
    }

    // ───────────── 放置 ─────────────

    /** 放置前的检查：数量上限（按玩家在该文明的时代）、同种间距、文明规则。返回拒绝原因。 */
    public static Optional<Component> placementError(ServerPlayer player, ServerLevel level, BlockPos pos, Direction facing,
                                                     ShrineKind kind) {
        WorldRulesConfig.Shrine rule = rule(kind);
        ShrineRegistry reg = ShrineRegistry.get(level.getServer());
        Age age = MythData.get(player).civ(kind.civ()).age();
        int limit = rule.limit(age.id());
        Component name = kind.block().get().getName();
        if (reg.countOwned(player.getUUID(), kind.id()) >= limit) {
            return Optional.of(limit == 0
                    ? Component.translatable("mythic_age.shrine.place.no_age",
                    Component.translatable("mythic_age.civ." + kind.civ().id()), name)
                    : Component.translatable("mythic_age.shrine.place.limit", name, limit));
        }
        Optional<ShrineRegistry.Entry> near = reg.tooClose(GlobalPos.of(level.dimension(), pos), kind.id(),
                rule.spacing(), kind.verticalSpacing());
        if (near.isPresent()) {
            BlockPos c = near.get().pos().pos();
            return Optional.of(Component.translatable("mythic_age.shrine.place.spacing", name, rule.spacing(),
                    c.getX(), c.getY(), c.getZ(), near.get().ownerName()));
        }
        return kind.hooks().placementError(player, level, pos, facing, reg);
    }

    /** 玩家放下神庙：绑定主人、登记，再交给文明规则。 */
    public static void onPlaced(ServerLevel level, BlockPos pos, ServerPlayer player, ShrineBlockEntity be, Direction facing) {
        ShrineKind kind = be.kind();
        be.bind(player.getUUID(), player.getGameProfile().getName(), MythData.get(player).civ(kind.civ()).age());
        ShrineRegistry.get(level.getServer()).add(new ShrineRegistry.Entry(GlobalPos.of(level.dimension(), pos.immutable()),
                player.getUUID(), player.getGameProfile().getName(), facing, kind.civ(), kind.id()));
        OWNER_CHECK.remove(GlobalPos.of(level.dimension(), pos));
        kind.hooks().onBound(level, pos, Optional.of(player));
    }

    /** 测试与管理用：直接绑定主人（不经过放置检查）。 */
    public static void bindForTest(ServerLevel level, BlockPos pos, UUID owner, String name, Age age, Direction facing) {
        if (level.getBlockEntity(pos) instanceof ShrineBlockEntity be) {
            ShrineKind kind = be.kind();
            be.bind(owner, name, age);
            ShrineRegistry.get(level.getServer()).add(new ShrineRegistry.Entry(GlobalPos.of(level.dimension(), pos.immutable()),
                    owner, name, facing, kind.civ(), kind.id()));
            OWNER_CHECK.remove(GlobalPos.of(level.dimension(), pos));
            kind.hooks().onBound(level, pos, Optional.empty());
        }
    }

    // ───────────── 加载与无主检查 ─────────────

    /** 方块实体加载：没有主人的（命令、结构放置）给绑定留出同一刻，下一刻仍无主就移除。 */
    static void onLoaded(ServerLevel level, ShrineBlockEntity be) {
        if (be.owner().isEmpty()) {
            OWNER_CHECK.put(GlobalPos.of(level.dimension(), be.getBlockPos()), level.getGameTime() + 1);
            return;
        }
        level.getServer().execute(() -> {
            if (level.getBlockEntity(be.getBlockPos()) == be) {
                be.kind().hooks().onBound(level, be.getBlockPos(), Optional.empty());
            }
        });
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (OWNER_CHECK.isEmpty()) {
            return;
        }
        MinecraftServer s = event.getServer();
        var it = OWNER_CHECK.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerLevel level = s.getLevel(e.getKey().dimension());
            if (level == null || level.getGameTime() < e.getValue()) {
                continue;
            }
            it.remove();
            BlockPos pos = e.getKey().pos();
            if (level.getBlockEntity(pos) instanceof ShrineBlockEntity be) {
                if (be.owner().isEmpty()) {
                    MythicAge.LOGGER.info("移除没有主人的神庙 {} {}（只能由玩家放置）", be.kind().id(), pos);
                    level.destroyBlock(pos, true);
                } else {
                    be.kind().hooks().onBound(level, pos, Optional.empty());
                }
            }
        }
    }

    // ───────────── 移除与保护 ─────────────

    public static void onRemoved(Level level, BlockPos pos, ShrineKind kind) {
        if (level instanceof ServerLevel server) {
            GlobalPos key = GlobalPos.of(server.dimension(), pos);
            ShrineRegistry.get(server.getServer()).remove(key);
            OWNER_CHECK.remove(key);
            kind.hooks().onRemoved(server, key);
        }
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockEntity(event.getPos()) instanceof ShrineBlockEntity be)
                || !rule(be.kind()).protect()) {
            return;
        }
        var player = event.getPlayer();
        boolean owner = be.owner().map(id -> id.equals(player.getUUID())).orElse(true);
        boolean admin = player.isCreative() && player.hasPermissions(2);
        if (!owner && !admin) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("mythic_age.shrine.protected", be.ownerName(),
                    be.kind().block().get().getName()), true);
        }
    }
}
