package io.github.happysnappingturtle.mythicage.structure;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 村庄以外的结构模板数据（T22）：{@code data/<ns>/mythic_age/structure_spec/<文明>/<名>.json}，格式同村庄建筑数据
 * （模板 id、尺寸、地面高度、锚点），由 {@code tools/buildings/export_structures.py} 生成。和村庄建筑分开放，村庄的规划、
 * 岗位、施工都读不到它们。
 */
public final class StructureSpecs extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, BuildingSpec> specs = Map.of();

    private StructureSpecs() {
        super(new Gson(), "mythic_age/structure_spec");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new StructureSpecs());
    }

    public static Optional<BuildingSpec> get(String id) {
        return Optional.ofNullable(specs.get(id));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, BuildingSpec> next = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                BuildingSpec s = BuildingSpec.parse(json.getAsJsonObject());
                next.put(s.id(), s);
            } catch (RuntimeException e) {
                MythicAge.LOGGER.error("结构模板数据 {} 解析失败，已忽略：{}", id, e.getMessage());
            }
        });
        specs = Map.copyOf(next);
        MythicAge.LOGGER.info("结构模板数据：{} 份", next.size());
    }
}
