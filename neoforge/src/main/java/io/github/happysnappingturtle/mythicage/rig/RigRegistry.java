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

    private RigRegistry() {
        super(new Gson(), DIRECTORY);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RigRegistry());
    }

    public static Optional<RigDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(rigs.get(id));
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
