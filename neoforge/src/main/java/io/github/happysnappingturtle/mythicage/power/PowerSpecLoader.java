package io.github.happysnappingturtle.mythicage.power;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.power.PowerSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 神力数据 {@code data/<ns>/mythic_age/god_power/<id>.json}（T12 设计文档 §2.1）。文件名必须等于 {@code id}；
 * 单个文件解析失败时保留它的上一版；没有效果代码的 id 只告警。
 */
public final class PowerSpecLoader extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, PowerSpec> specs = Map.of();
    private static final Map<String, PowerSpec> TEST = new HashMap<>();

    private PowerSpecLoader() {
        super(new Gson(), "mythic_age/god_power");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PowerSpecLoader());
    }

    public static Optional<PowerSpec> get(String id) {
        synchronized (TEST) {
            PowerSpec t = TEST.get(id);
            if (t != null) {
                return Optional.of(t);
            }
        }
        return Optional.ofNullable(specs.get(id));
    }

    public static Map<String, PowerSpec> all() {
        Map<String, PowerSpec> out = new HashMap<>(specs);
        synchronized (TEST) {
            out.putAll(TEST);
        }
        return out;
    }

    /** 只供 GameTest：临时登记；传 null 清除全部。 */
    public static void overrideForTest(@Nullable PowerSpec spec) {
        synchronized (TEST) {
            if (spec == null) {
                TEST.clear();
            } else {
                TEST.put(spec.id(), spec);
            }
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, PowerSpec> next = new HashMap<>();
        Map<String, PowerSpec> old = specs;
        files.forEach((rl, json) -> {
            String id = rl.getPath();
            List<String> warnings = new ArrayList<>();
            try {
                PowerSpec s = PowerSpec.parse(json, warnings);
                if (!s.id().equals(id)) {
                    throw new PowerSpec.PowerSpecException("$.id: 与文件名 " + id + " 不一致");
                }
                warnings.forEach(w -> MythicAge.LOGGER.warn("神力 {}：{}", rl, w));
                if (GodPowers.get(id).isEmpty()) {
                    MythicAge.LOGGER.warn("神力 {}：还没有效果代码，暂不可用", rl);
                }
                next.put(id, s);
            } catch (RuntimeException e) {
                MythicAge.LOGGER.error("神力 {} 解析失败，保留上一版：{}", rl, e.getMessage());
                if (old.containsKey(id)) {
                    next.put(id, old.get(id));
                }
            }
        });
        specs = Map.copyOf(next);
    }
}
