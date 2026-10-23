package io.github.happysnappingturtle.mythicage.relic;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.relic.RelicCard;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 圣物卡（T16 设计文档 §2.1，数据包 {@code data/<ns>/mythic_age/relic/<文明>/<id>.json}）。五文明通用。
 * 物品必须是 {@link RelicItem}；不合格的卡记录日志后跳过。
 */
public final class RelicCards {
    private static volatile Map<String, RelicCard> cards = Map.of();
    private static volatile Map<Item, RelicCard> byItem = Map.of();
    private static final Map<String, RelicCard> TEST = new LinkedHashMap<>();
    private static final Map<Item, RelicCard> TEST_ITEMS = new HashMap<>();

    private RelicCards() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static Optional<RelicCard> byId(String id) {
        RelicCard t = TEST.get(id);
        return Optional.ofNullable(t != null ? t : cards.get(id));
    }

    public static Optional<RelicCard> of(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof RelicItem)) {
            return Optional.empty();
        }
        RelicCard t = TEST_ITEMS.get(stack.getItem());
        return Optional.ofNullable(t != null ? t : byItem.get(stack.getItem()));
    }

    public static Map<String, RelicCard> all() {
        Map<String, RelicCard> m = new LinkedHashMap<>(cards);
        m.putAll(TEST);
        return m;
    }

    /** 只供 GameTest：把某个圣物物品临时当成另一张卡（例如测试用的埃及圣物）；传 null 清除。 */
    public static synchronized void overrideForTest(@Nullable Item item, @Nullable RelicCard card) {
        if (item == null || card == null) {
            TEST.clear();
            TEST_ITEMS.clear();
            return;
        }
        TEST.put(card.id(), card);
        TEST_ITEMS.put(item, card);
    }

    static void apply(List<RelicCard> parsed) {
        Map<String, RelicCard> next = RelicCard.dedupe(parsed, m -> MythicAge.LOGGER.error(m));
        Map<Item, RelicCard> items = new HashMap<>();
        Map<String, RelicCard> ok = new LinkedHashMap<>();
        next.forEach((id, c) -> {
            ResourceLocation rl = ResourceLocation.tryParse(c.item());
            Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (!(item instanceof RelicItem)) {
                MythicAge.LOGGER.error("圣物卡 {}：物品 {} 不存在或不是圣物物品，已忽略", id, c.item());
                return;
            }
            ok.put(id, c);
            items.put(item, c);
        });
        cards = Map.copyOf(ok);
        byItem = Map.copyOf(items);
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(new Gson(), "mythic_age/relic");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            List<RelicCard> parsed = new ArrayList<>();
            files.forEach((file, json) -> {
                String path = file.getPath();
                int slash = path.indexOf('/');
                if (slash < 0) {
                    MythicAge.LOGGER.error("圣物卡 {} 应放在 relic/<文明>/ 下，已忽略", file);
                    return;
                }
                try {
                    parsed.add(RelicCard.parse(path.substring(0, slash), path.substring(slash + 1), json.getAsJsonObject()));
                } catch (RuntimeException e) {
                    MythicAge.LOGGER.error("圣物卡 {} 解析失败，已忽略：{}", file, e.getMessage());
                }
            });
            RelicCards.apply(parsed);
            MythicAge.LOGGER.info("圣物卡：{} 张", cards.size());
        }
    }
}
