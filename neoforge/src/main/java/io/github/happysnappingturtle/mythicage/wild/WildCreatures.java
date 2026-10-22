package io.github.happysnappingturtle.mythicage.wild;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 野外神话生物卡（T15 设计文档 §1.1，数据包 {@code data/<ns>/mythic_age/wild_creature/<文明>/<id>.json}）。五文明通用。
 */
public final class WildCreatures {
    private static volatile Map<String, WildCreatureCard> cards = Map.of();

    private WildCreatures() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    /** 文明:id → 卡。 */
    public static Optional<WildCreatureCard> card(String key) {
        return Optional.ofNullable(cards.get(key));
    }

    public static Map<String, WildCreatureCard> all() {
        return cards;
    }

    /** 测试用：直接放一张卡。 */
    public static void putForTest(WildCreatureCard card) {
        Map<String, WildCreatureCard> next = new HashMap<>(cards);
        next.put(card.key(), card);
        cards = Map.copyOf(next);
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(new Gson(), "mythic_age/wild_creature");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, WildCreatureCard> next = new HashMap<>();
            files.forEach((file, json) -> {
                String path = file.getPath();
                int slash = path.indexOf('/');
                if (slash < 0) {
                    MythicAge.LOGGER.error("野外生物卡 {} 应放在 wild_creature/<文明>/ 下，已忽略", file);
                    return;
                }
                try {
                    WildCreatureCard c = WildCreatureCard.parse(path.substring(0, slash), path.substring(slash + 1),
                            json.getAsJsonObject());
                    next.put(c.key(), c);
                } catch (RuntimeException e) {
                    MythicAge.LOGGER.error("野外生物卡 {} 解析失败，已忽略：{}", file, e.getMessage());
                }
            });
            cards = Map.copyOf(next);
            MythicAge.LOGGER.info("野外神话生物卡：{} 张", next.size());
        }
    }
}
