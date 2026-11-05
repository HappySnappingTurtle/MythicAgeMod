package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.OfferingTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 供奉的供品表（GA-4 设计文档 §2.5）：读取数据包 {@code data/<ns>/mythic_age/offering/<文明>.json}。
 * 解析失败时记错误日志并保留上一次的有效表；一份都没有时为空表（不能献供）。
 */
public final class OfferingLoader extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation GREEK = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "greek");

    private static volatile OfferingTable greek = OfferingTable.EMPTY;
    private static volatile OfferingTable testOverride;

    private OfferingLoader() {
        super(new Gson(), "mythic_age/offering");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new OfferingLoader());
    }

    public static OfferingTable greek() {
        OfferingTable t = testOverride;
        return t != null ? t : greek;
    }

    /** 只供 GameTest：替换供品表；传 null 恢复。 */
    public static void overrideForTest(OfferingTable t) {
        testOverride = t;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        JsonElement json = files.get(GREEK);
        if (json == null) {
            greek = OfferingTable.EMPTY;
            return;
        }
        try {
            List<String> warnings = new ArrayList<>();
            greek = OfferingTable.parse(json.toString(), warnings);
            warnings.forEach(w -> MythicAge.LOGGER.warn("供品表 {}：{}", GREEK, w));
        } catch (RuntimeException e) {
            MythicAge.LOGGER.error("供品表 {} 解析失败，保留上一次的有效表：{}", GREEK, e.getMessage());
        }
    }
}
