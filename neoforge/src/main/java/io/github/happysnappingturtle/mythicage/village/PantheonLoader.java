package io.github.happysnappingturtle.mythicage.village;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.Pantheon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 主神与次神表（T10a 设计文档 §3）：数据包 {@code data/<ns>/mythic_age/pantheon/<文明>.json}；解析失败保留上一版。 */
public final class PantheonLoader extends SimpleJsonResourceReloadListener {
    private static volatile Map<Civilization, Pantheon> tables = Map.of();

    private PantheonLoader() {
        super(new Gson(), "mythic_age/pantheon");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PantheonLoader());
    }

    private static final Map<Civilization, Pantheon> TEST = new EnumMap<>(Civilization.class);

    public static Pantheon get(Civilization civ) {
        synchronized (TEST) {
            Pantheon t = TEST.get(civ);
            if (t != null) {
                return t;
            }
        }
        return tables.getOrDefault(civ, Pantheon.empty(civ));
    }

    /** 只供 GameTest：临时替换该表所属文明的主神表；传 null 清除全部替换。 */
    public static void overrideForTest(@Nullable Pantheon table) {
        synchronized (TEST) {
            if (table == null) {
                TEST.clear();
            } else {
                TEST.put(table.civ(), table);
            }
        }
        warnDuplicates();
    }

    /** 跨文明重名的神 id 会让神名翻译 key 冲突（T10b 设计文档 §9）。返回重名列表。 */
    public static List<String> warnDuplicates() {
        Map<Civilization, Pantheon> all = new EnumMap<>(Civilization.class);
        all.putAll(tables);
        synchronized (TEST) {
            all.putAll(TEST);
        }
        List<String> dup = Pantheon.duplicateIds(all.values());
        if (!dup.isEmpty()) {
            MythicAge.LOGGER.warn("主神表：以下神 id 在多个文明中重复，神名翻译会冲突：{}", dup);
        }
        return dup;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Civilization, Pantheon> next = new EnumMap<>(Civilization.class);
        next.putAll(tables);
        files.forEach((id, json) -> {
            Optional<Civilization> civ = Civilization.byId(id.getPath());
            if (civ.isEmpty()) {
                MythicAge.LOGGER.error("主神表 {}：文件名不是文明 id，已忽略", id);
                return;
            }
            try {
                Pantheon p = Pantheon.parse(json);
                if (p.civ() != civ.get()) {
                    throw new Pantheon.PantheonException("civilization: 与文件名不一致");
                }
                next.put(civ.get(), p);
            } catch (Pantheon.PantheonException e) {
                MythicAge.LOGGER.error("主神表 {} 解析失败，保留上一版：{}", id, e.getMessage());
            }
        });
        tables = Map.copyOf(next);
        warnDuplicates();
    }
}
