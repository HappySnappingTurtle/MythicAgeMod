package io.github.happysnappingturtle.mythicage.village.gen;

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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 建筑数据（T13b §1）：数据包 {@code data/<ns>/mythic_age/village_building/<文明>/<id>.json}，由 tools/buildings 导出。
 * 世界生成线程会读，所以整表替换（volatile），不原地修改。
 */
public final class VillageBuildings extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, BuildingSpec> specs = Map.of();

    private VillageBuildings() {
        super(new Gson(), "mythic_age/village_building");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new VillageBuildings());
    }

    /** 全部建筑：建筑 id（如 {@code greek:house_small}）→ 数据。 */
    public static Map<String, BuildingSpec> all() {
        return specs;
    }

    /** 已有正式模板的文明（有核心建筑「广场」）。 */
    public static Set<String> civsWithTemplates() {
        return specs.values().stream().filter(s -> s.id().endsWith(":agora")).map(BuildingSpec::civ)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, BuildingSpec> next = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                BuildingSpec s = BuildingSpec.parse(json.getAsJsonObject());
                next.put(s.id(), s);
            } catch (RuntimeException e) {
                MythicAge.LOGGER.error("建筑数据 {} 解析失败，已忽略：{}", id, e.getMessage());
            }
        });
        specs = Map.copyOf(next);
        MythicAge.LOGGER.info("村庄建筑数据：{} 份", next.size());
    }
}
