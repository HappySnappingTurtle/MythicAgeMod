package io.github.happysnappingturtle.mythicage.rig;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.rig.RigDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 服务端骨架数据（T7 设计文档 §6.2）：读取数据包 {@code data/<命名空间>/mythic_age/rig/<名>.json}，
 * id 为 {@code <命名空间>:<名>}。解析失败的文件记错误日志并跳过，不影响其他文件。
 */
public final class RigRegistry extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "mythic_age/rig";
    private static volatile Map<ResourceLocation, RigDefinition> rigs = Map.of();

    // 与服务端注册表分开，避免依赖单机的共享静态变量；新快照会替换旧服务器的数据。
    private static volatile Map<ResourceLocation, RigDefinition.HeadLook> clientHeadLooks = Map.of();

    private RigRegistry() {
        super(new Gson(), DIRECTORY);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RigRegistry());
    }

    public static Optional<RigDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(rigs.get(id));
    }

    public static RigDefinition.HeadLook clientHeadLook(ResourceLocation id) {
        return clientHeadLooks.getOrDefault(id, RigDefinition.HeadLook.DEFAULT);
    }

    public static void accept(RigHeadLookPayload payload) {
        clientHeadLooks = payload.limits();
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        Map<ResourceLocation, RigDefinition.HeadLook> limits = new HashMap<>();
        rigs.forEach((id, rig) -> limits.put(id, rig.headLook()));
        RigHeadLookPayload payload = new RigHeadLookPayload(limits);
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    public static int size() {
        return rigs.size();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, RigDefinition> next = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                next.put(id, RigDefinition.parse(json));
            } catch (RigDefinition.RigParseException e) {
                MythicAge.LOGGER.error("骨架数据 {} 解析失败：{}", id, e.getMessage());
            }
        });
        rigs = Map.copyOf(next);
        MythicAge.LOGGER.info("已加载 {} 份骨架数据", next.size());
    }
}
