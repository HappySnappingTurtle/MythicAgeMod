package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.power.ProductGroups;
import io.github.happysnappingturtle.mythicage.core.power.TransientBudget;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * 神力产生的实体的两种账（T12 设计文档 §2.6；R-CAP、总纲 §3.6）：
 * <ul>
 *   <li><b>临时实体</b>：寿命 ≤300 秒；每人 ≤16、全服 ≤96（配置），超额时最早的先退场；到期退场。</li>
 *   <li><b>产物</b>（固定构造物、资源生物、非战斗工人）：不计预算；每人每种神力 1 组，新的一组替换旧的。</li>
 * </ul>
 * 实体上记在持久数据 {@code mythic_age:power}（主人、到期刻、神力 id），服务器重启或区块重新载入时据此补账；
 * 载入时已过期的直接移除。账本只在内存。
 */
public final class TransientEntities {
    static final String TAG = "mythic_age:power";
    private static final TransientBudget BUDGET = new TransientBudget();
    private static final ProductGroups PRODUCTS = new ProductGroups();
    private static final int CHECK_INTERVAL = 20;

    private TransientEntities() {
    }

    /**
     * 登记同一次施放生成的临时实体（实体应已加入世界）。超额的旧实体立即退场。
     *
     * @param lifetimeTicks 单体寿命（≤ 6000）
     */
    public static void registerTransient(ServerPlayer owner, List<? extends Entity> entities, long lifetimeTicks) {
        long now = WorldClock.now(owner.server);
        for (Entity e : entities) {
            CompoundTag t = new CompoundTag();
            t.putUUID("owner", owner.getUUID());
            t.putLong("expires", now + lifetimeTicks);
            t.putString("kind", "transient");
            e.getPersistentData().put(TAG, t);
        }
        List<String> evicted = BUDGET.register(owner.getUUID(), entities.stream().map(e -> e.getUUID().toString()).toList(),
                now, lifetimeTicks, PowerService.config().transientPerPlayer(), PowerService.config().transientEntityCap());
        evicted.forEach(id -> remove(owner.server, id));
    }

    /** 登记一组产物（不计预算），同一玩家同一神力的旧组立即退场。 */
    public static void registerProducts(ServerPlayer owner, String powerId, List<? extends Entity> entities) {
        for (Entity e : entities) {
            CompoundTag t = new CompoundTag();
            t.putUUID("owner", owner.getUUID());
            t.putString("power", powerId);
            t.putString("kind", "product");
            e.getPersistentData().put(TAG, t);
        }
        PRODUCTS.replace(owner.getUUID(), powerId, entities.stream().map(e -> e.getUUID().toString()).toList())
                .forEach(id -> remove(owner.server, id));
    }

    static void remove(MinecraftServer server, String id) {
        UUID uuid = UUID.fromString(id);
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) {
                e.discard();
                return;
            }
        }
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        BUDGET.expire(WorldClock.now(server)).forEach(id -> remove(server, id));
    }

    /** 载入（区块重新载入、服务器重启）时：过期的直接不让进来；没过期的补账。 */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !event.getEntity().getPersistentData().contains(TAG)) {
            return;
        }
        Entity e = event.getEntity();
        CompoundTag t = e.getPersistentData().getCompound(TAG);
        String id = e.getUUID().toString();
        if ("transient".equals(t.getString("kind"))) {
            long now = WorldClock.now(((ServerLevel) event.getLevel()).getServer());
            if (t.getLong("expires") <= now) {
                event.setCanceled(true);
                return;
            }
            BUDGET.restore(id, t.getUUID("owner"), t.getLong("expires"));
        } else if ("product".equals(t.getString("kind"))) {
            if (!PRODUCTS.group(t.getUUID("owner"), t.getString("power")).contains(id)) {
                PRODUCTS.restore(t.getUUID("owner"), t.getString("power"), id);
            }
        }
    }

    /** 被杀死或移除（不是区块卸载）时销账。 */
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || !e.getPersistentData().contains(TAG)) {
            return;
        }
        Entity.RemovalReason reason = e.getRemovalReason();
        if (reason != null && reason.shouldDestroy()) {
            BUDGET.forget(e.getUUID().toString());
            PRODUCTS.forget(e.getUUID().toString());
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        BUDGET.clear();
        PRODUCTS.clear();
    }

    public static int countForTest(UUID owner) {
        return BUDGET.count(owner);
    }

    public static List<String> productsForTest(UUID owner, String power) {
        return PRODUCTS.group(owner, power);
    }

    public static void clearForTest() {
        BUDGET.clear();
        PRODUCTS.clear();
    }
}
