package io.github.happysnappingturtle.mythicage.conduct;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.conduct.ConductRules;
import io.github.happysnappingturtle.mythicage.core.conduct.ConductState;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductConfig;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 善恶值的唯一结算入口（T18 设计文档 §4.4）：截断、正向每日上限、记录原因、只给本人提示（D4）；在线刻回归（D5）。
 * 没有「交物品清零」一类的接口（02 号红线）。
 */
public final class ConductService {
    public static final int TICK = 20;

    private ConductService() {
    }

    public static ConductConfig config() {
        return ModConfigs.conduct().current();
    }

    public static int score(ServerPlayer p) {
        return MythData.get(p).conductScore();
    }

    /** 读取接口（以后的阿米特、麒麟、穷奇用）：按两个阈值分三档。 */
    public static ConductRules.Band band(ServerPlayer p, int good, int evil) {
        return ConductRules.band(score(p), good, evil);
    }

    /**
     * 结算一次变化，返回实际变化（截断、正向上限之后）。reason 是语言键后缀 {@code mythic_age.conduct.reason.<reason>}。
     */
    public static int change(ServerPlayer p, String reason, int delta) {
        ConductConfig cfg = config();
        if (!cfg.enabled() || delta == 0) {
            return 0;
        }
        PlayerMythData data = MythData.get(p);
        ConductState state = data.conduct();
        long today = MythDays.today(p.server);
        if (delta > 0) {
            delta = ConductRules.positiveAllowance(delta, state.positiveGranted(today), cfg.positiveDailyCap());
            if (delta == 0) {
                return 0;
            }
        }
        int old = data.conductScore();
        int now = ConductRules.clamp(old + delta, cfg.range().min(), cfg.range().max());
        int actual = now - old;
        if (actual == 0) {
            return 0;
        }
        if (actual > 0) {
            state.addPositive(today, actual);
        }
        data.setConductScore(now);
        data.addConductEntry(new PlayerMythData.ConductEntry(reason, actual, p.level().getGameTime()));
        MythData.changed(p);
        p.displayClientMessage(Component.translatable("mythic_age.conduct.changed", (actual > 0 ? "+" : "") + actual,
                Component.translatable("mythic_age.conduct.reason." + reason)), true);
        return actual;
    }

    /** 在线刻累计与回归：满 interval 刻向 0 靠近 points 点，不越过 0；离线不计。 */
    public static void tickOnline(ServerPlayer p, long ticks) {
        ConductConfig cfg = config();
        if (!cfg.enabled() || cfg.regression().intervalOnlineTicks() <= 0) {
            return;
        }
        PlayerMythData data = MythData.get(p);
        ConductState state = data.conduct();
        long acc = state.onlineTicks() + ticks;
        long interval = cfg.regression().intervalOnlineTicks();
        int score = data.conductScore();
        int regressed = score;
        while (acc >= interval) {
            acc -= interval;
            regressed = ConductRules.regress(regressed, cfg.regression().points());
        }
        state.setOnlineTicks(acc);
        if (regressed != score) {
            data.setConductScore(regressed);
            MythData.changed(p);
        }
    }

    public static void onServerTickPost(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % TICK != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            tickOnline(p, TICK);
        }
    }
}
