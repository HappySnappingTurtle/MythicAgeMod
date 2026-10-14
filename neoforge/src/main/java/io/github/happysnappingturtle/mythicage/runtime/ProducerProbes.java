package io.github.happysnappingturtle.mythicage.runtime;

import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 产出点类型 → 探针。类型取 id 中第一个冒号之前的部分（例如 {@code shrine:...} 的 shrine）。 */
public final class ProducerProbes {
    private static final Map<String, ProducerProbe> PROBES = new ConcurrentHashMap<>();

    private ProducerProbes() {
    }

    public static void register(String type, ProducerProbe probe) {
        PROBES.put(type, probe);
    }

    public static String type(ProducerId id) {
        int i = id.value().indexOf(':');
        return i < 0 ? id.value() : id.value().substring(0, i);
    }

    /** 未注册的类型视为「从不真实运行」，只能走快照。 */
    public static ProducerProbe get(ProducerId id) {
        return PROBES.getOrDefault(type(id), (server, ledger, owner, now, interval) -> ProducerProbe.Observation.NOT_RUNNING);
    }

    /**
     * 私人产出点的「真实运行」判定（A2-R1）：只看区块是否已加载。
     * 只加载到边缘、不跑方块刻的区块按卸载处理——否则那里既没有真实产出，也不能走快照，两头落空。
     */
    public static boolean chunkRunning(ServerLevel level, BlockPos pos) {
        return level.shouldTickBlocksAt(pos);
    }
}
