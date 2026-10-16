package io.github.happysnappingturtle.mythicage.content.greek.temple;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 神庙等级规则（T8a 设计文档 §3.3）：读取数据包 {@code data/<ns>/mythic_age/temple/<文明>.json}。
 * 解析失败的文件记错误日志并保留上一次的有效规则；同时记录内容哈希，规则变化后神坛重新扫描。
 */
public final class TempleRulesLoader extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation GREEK = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "greek");

    public record Loaded(TempleRules rules, int hash) {
    }

    private static volatile Map<ResourceLocation, Loaded> rules = Map.of();

    private TempleRulesLoader() {
        super(new Gson(), "mythic_age/temple");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new TempleRulesLoader());
    }

    public static Optional<Loaded> get(ResourceLocation id) {
        return Optional.ofNullable(rules.get(id));
    }

    public static Loaded greek() {
        Loaded l = rules.get(GREEK);
        if (l == null) {
            throw new IllegalStateException("缺少神庙规则 " + GREEK + "（数据包 data/mythic_age/mythic_age/temple/greek.json）");
        }
        return l;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Loaded> next = new HashMap<>(rules);
        files.forEach((id, json) -> {
            try {
                next.put(id, new Loaded(TempleRules.parse(json), json.toString().hashCode()));
            } catch (TempleRules.TempleRulesException e) {
                MythicAge.LOGGER.error("神庙规则 {} 解析失败，保留上一次的有效规则：{}", id, e.getMessage());
            }
        });
        next.keySet().retainAll(files.keySet());
        rules = Map.copyOf(next);
        TempleService.onRulesReloaded();
    }
}
