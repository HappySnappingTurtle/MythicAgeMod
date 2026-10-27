package io.github.happysnappingturtle.mythicage.bestiary;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 图鉴点亮表（T24 §6.3）：数据包 {@code data/<ns>/mythic_age/bestiary/<文明>.json}；解析失败保留上一版。 */
public final class BestiaryLoader extends SimpleJsonResourceReloadListener {
    private static volatile BestiaryTable table = BestiaryTable.empty();

    private BestiaryLoader() {
        super(new Gson(), "mythic_age/bestiary");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BestiaryLoader());
    }

    public static BestiaryTable table() {
        return table;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        try {
            List<BestiaryTable> parts = new ArrayList<>();
            files.forEach((id, json) -> parts.add(BestiaryTable.parse(json.getAsJsonObject(), id.toString())));
            table = BestiaryTable.merge(parts);
            MythicAge.LOGGER.info("图鉴点亮表：{} 个条目、{} 种实体", table.entries().size(), table.byEntity().size());
        } catch (RuntimeException e) {
            MythicAge.LOGGER.error("图鉴点亮表读取失败，保留上一版：{}", e.getMessage());
        }
    }
}
