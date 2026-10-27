package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonUnitCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 召唤管理界面的服务端入口（T14 设计文档 §7、§11.2）：组装快照、处理打开 / 刷新与指令。
 * 入口：传令杖右键空气、信仰面板「神话单位」按钮。
 */
public final class SummonScreens {
    private SummonScreens() {
    }

    public static void open(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new SummonPayloads.View(view(p)));
    }

    public static SummonView view(ServerPlayer p) {
        List<SummonView.Row> rows = new ArrayList<>();
        long now = p.server.overworld().getGameTime();
        for (SummonRegistry.Unit u : SummonRegistry.get(p.server).byOwner(p.getUUID())) {
            SummonUnitCard card = SummonUnits.card(u.card).orElse(null);
            String entity = card == null ? "" : card.entity();
            String token = card == null ? "" : card.token();
            boolean combat = card == null || card.combat();
            Entity e = u.respawning ? null : SummonService.find(p.server, u);
            float hp = e instanceof LivingEntity le ? le.getHealth() : -1;
            float max = e instanceof LivingEntity le ? le.getMaxHealth() : -1;
            SummonView.Where where;
            int value = 0;
            String dim = "";
            if (u.respawning) {
                where = SummonView.Where.RESPAWNING;
                value = (int) Math.max(0, (u.respawnAt - now) / 20);
            } else if (e != null && e.level() != p.level()) {
                where = SummonView.Where.DIMENSION;
                dim = e.level().dimension().location().toString();
            } else if (e == null && u.last != null && !u.last.dimension().equals(p.level().dimension())) {
                where = SummonView.Where.DIMENSION;
                dim = u.last.dimension().location().toString();
            } else if (e == null) {
                where = SummonView.Where.UNLOADED;
            } else {
                where = SummonView.Where.NEAR;
                value = (int) e.distanceTo(p);
            }
            boolean recallable = where == SummonView.Where.NEAR || where == SummonView.Where.UNLOADED;
            rows.add(new SummonView.Row(u.unitId, u.card, entity, token, combat, hp, max, u.mode, where, value, dim, recallable));
        }
        return new SummonView(SummonService.occupied(p), SummonService.cap(p), List.copyOf(rows), temps(p));
    }

    /** 最多列几行临时效果。 */
    public static final int TEMP_ROWS = 3;

    /** 临时效果（T24 §5.2）：神力的临时实体与产物，按实体种类合成一行；未载入的单独一行（不当作消失）。 */
    public static List<SummonView.Temp> temps(ServerPlayer p) {
        long now = io.github.happysnappingturtle.mythicage.runtime.WorldClock.now(p.server);
        java.util.Map<String, long[]> expires = new java.util.HashMap<>();
        List<String> ids = new ArrayList<>();
        for (var e : io.github.happysnappingturtle.mythicage.power.TransientEntities.transientsOf(p.getUUID())) {
            ids.add(e.id());
            expires.put(e.id(), new long[]{e.expiresAt()});
        }
        io.github.happysnappingturtle.mythicage.power.TransientEntities.productsOf(p.getUUID()).values().forEach(ids::addAll);
        java.util.Map<String, SummonView.Temp> byType = new java.util.LinkedHashMap<>();
        for (String id : ids) {
            Entity e = find(p, java.util.UUID.fromString(id));
            String type = e == null ? "" : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
            SummonView.Where where;
            int value = 0;
            String dim = "";
            if (e == null) {
                where = SummonView.Where.UNLOADED;
            } else if (e.level() != p.level()) {
                where = SummonView.Where.DIMENSION;
                dim = e.level().dimension().location().toString();
            } else {
                where = SummonView.Where.NEAR;
                value = (int) e.distanceTo(p);
            }
            long[] ex = expires.get(id);
            int left = ex == null ? -1 : (int) Math.max(0, (ex[0] - now) / 20);
            SummonView.Temp old = byType.get(type);
            if (old == null) {
                byType.put(type, new SummonView.Temp(type, 1, where, value, dim, left));
            } else {
                boolean closer = where == SummonView.Where.NEAR && (old.where() != SummonView.Where.NEAR || value < old.value());
                int soonest = old.secondsLeft() < 0 ? left : left < 0 ? old.secondsLeft() : Math.min(left, old.secondsLeft());
                byType.put(type, closer ? new SummonView.Temp(type, old.count() + 1, where, value, dim, soonest)
                        : new SummonView.Temp(type, old.count() + 1, old.where(), old.value(), old.dimension(), soonest));
            }
        }
        return byType.values().stream().limit(TEMP_ROWS).toList();
    }

    private static Entity find(ServerPlayer p, java.util.UUID id) {
        for (net.minecraft.server.level.ServerLevel level : p.server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    public static void handleRequest(SummonPayloads.Request payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                open(p);
            }
        });
    }

    /** 执行指令后立即回一份新快照，界面不用等下一次刷新。 */
    public static void handleCommand(SummonPayloads.Command payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                SummonService.command(p, payload);
                open(p);
            }
        });
    }
}
