package io.github.happysnappingturtle.mythicage.runtime;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxSteps;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import io.github.happysnappingturtle.mythicage.core.runtime.SettlementEngine;
import io.github.happysnappingturtle.mythicage.player.CapBonusProvider;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * 运行时驱动（T5 设计文档 §6）：每 20 tick 开始一轮，把全部台账排队；每个 tick 最多结算
 * {@link #MAX_PER_TICK} 本，台账很多时分到后续 tick，避免卡顿。结算是按区间算的，延后几 tick 不影响结果。
 */
public final class RuntimeDriver {
    public static final int PASS_INTERVAL = 20;
    public static final int MAX_PER_TICK = 256;

    private static final ArrayDeque<ProducerId> QUEUE = new ArrayDeque<>();
    /** 已告警过时钟回退的台账，避免每轮刷屏。 */
    private static final Set<ProducerId> WARNED = new HashSet<>();

    private RuntimeDriver() {
    }

    /** 普通优先级：此时 {@link ModConfigs} 已在最高优先级中创建了新的 holder。 */
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        reset();
        MinecraftServer server = event.getServer();
        // 03 号 §13.3：新配置生效前，先按旧规则把所有台账结算到当前时刻
        ModConfigs.runtime().addPreApplyHook((oldValue, newValue) -> settleAll(server, oldValue.favor()));
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        reset();
        PlayerActivity.clear();
        TestProducers.clear();
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = WorldClock.now(server);
        RuntimeLedgers ledgers = RuntimeLedgers.get(server);
        if (QUEUE.isEmpty()) {
            if (now % PASS_INTERVAL != 0) {
                return;
            }
            QUEUE.addAll(ledgers.ids());
        }
        RuntimeConfig.Favor cfg = ModConfigs.runtime().current().favor();
        for (int i = 0; i < MAX_PER_TICK && !QUEUE.isEmpty(); i++) {
            ledgers.get(QUEUE.poll()).ifPresent(l -> settle(server, ledgers, l, now, cfg));
        }
    }

    /** 立即把全部台账结算到当前时刻（配置热更前、命令）。 */
    public static void settleAll(MinecraftServer server, RuntimeConfig.Favor cfg) {
        long now = WorldClock.now(server);
        RuntimeLedgers ledgers = RuntimeLedgers.get(server);
        for (ProducerLedger l : ledgers.all()) {
            settle(server, ledgers, l, now, cfg);
        }
    }

    /** 结算一本台账并把整数部分入账给主人。 */
    public static SettlementEngine.Settlement settle(MinecraftServer server, RuntimeLedgers ledgers,
                                                     ProducerLedger l, long now, RuntimeConfig.Favor cfg) {
        ServerPlayer owner = PlayerActivity.onlinePlayer(server, l.owner());
        boolean active = owner != null && PlayerActivity.isActive(server, l.owner(), now, cfg.idle());
        long interval = Math.max(0, now - l.lastSettledTick());
        ProducerProbe.Observation obs = interval == 0 ? ProducerProbe.Observation.NOT_RUNNING
                : ProducerProbes.get(l.id()).observe(server, l, owner, now, interval);
        SettlementEngine.Settlement s = SettlementEngine.settle(l, now,
                new SettlementEngine.Conditions(obs.realRunning(), active, obs.ownerNearby(), obs.realYield()), cfg);
        if (!s.warning().isEmpty()) {
            if (WARNED.add(l.id())) {
                MythicAge.LOGGER.warn("[runtime] {}", s.warning());
            }
            return s;
        }
        if (s.interval() > 0) {
            ledgers.setDirty();
        }
        if (s.credited() > 0) {
            if (owner == null) {
                // 不应发生（入账要求主人活跃）；保险起见退回余量，下次再入账
                l.setCarry(l.carry() + s.credited());
            } else {
                long bonus = CapBonusProvider.Holder.get().capBonus(owner, l.civ());
                MythData.transact(owner, Transactions.Request.internal(TxSteps.addFavor(l.civ(), s.credited(), bonus)));
                io.github.happysnappingturtle.mythicage.advancement.MythEvents.fire(owner, io.github.happysnappingturtle.mythicage.advancement.MythEvents.FAVOR_GAINED, l.civ(), (int) Math.min(Integer.MAX_VALUE, s.credited()));
            }
        }
        return s;
    }

    private static void reset() {
        QUEUE.clear();
        WARNED.clear();
    }
}
