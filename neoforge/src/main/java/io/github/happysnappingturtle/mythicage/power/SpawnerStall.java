package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * 刷怪器临时停摆（T13 设计文档 §3.3；05 号 §4「blockEntityStall」）：Mixin 在刷怪笼、试炼刷怪器每刻开头问这里，
 * 停摆时直接跳过这一刻（试炼刷怪器的冷却与下次生成时刻同时后推一刻，暂停期间不推进）。
 * 由 {@code world_rules.blockEntityStall} 总开关控制；具体哪些位置停摆由各神力登记判定（瘟疫区域）。
 */
public final class SpawnerStall {
    private static final List<BiPredicate<ServerLevel, BlockPos>> SOURCES = new ArrayList<>();

    private SpawnerStall() {
    }

    public static synchronized void register(BiPredicate<ServerLevel, BlockPos> source) {
        SOURCES.add(source);
    }

    public static boolean stalled(ServerLevel level, BlockPos pos) {
        return stalled(level, pos, ModConfigs.worldRules().current().blockEntityStall());
    }

    /** {@code enabled} 为总开关（GameTest 直接传值，免得临时改全局配置影响同组并行的测试）。 */
    public static boolean stalled(ServerLevel level, BlockPos pos, boolean enabled) {
        if (!enabled) {
            return false;
        }
        for (BiPredicate<ServerLevel, BlockPos> s : SOURCES) {
            if (s.test(level, pos)) {
                return true;
            }
        }
        return false;
    }
}
