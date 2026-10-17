package io.github.happysnappingturtle.mythicage.rite;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.rite.PromotionTable;
import io.github.happysnappingturtle.mythicage.core.rite.VillageStageTable;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 晋升表 {@code mythic_age/promotion/<文明>.json} 与村庄阶段表 {@code mythic_age/village_stage/<文明>.json} 的加载
 * （T11a 设计文档 §3.3、§3.4）。文件名就是文明 id；整份解析失败时保留上一版；缺文件的文明为空表
 * （晋升页显示「该文明的晋升尚未开放」）。
 */
public final class RiteTables {
    private static volatile Map<Civilization, PromotionTable> promotions = Map.of();
    private static volatile Map<Civilization, VillageStageTable> stages = Map.of();
    private static final Map<Civilization, PromotionTable> TEST_PROMOTIONS = new EnumMap<>(Civilization.class);
    private static final Map<Civilization, VillageStageTable> TEST_STAGES = new EnumMap<>(Civilization.class);

    private RiteTables() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader<>("mythic_age/promotion", "晋升表",
                (json, w) -> PromotionTable.parse(json, id -> TradeTableLoader.ITEMS.maxStack(id),
                        PromotionRequirements::known, w), PromotionTable::civ, m -> promotions = m, () -> promotions));
        event.addListener(new Loader<>("mythic_age/village_stage", "村庄阶段表", VillageStageTable::parse,
                VillageStageTable::civ, m -> stages = m, () -> stages));
    }

    public static PromotionTable promotion(Civilization civ) {
        synchronized (TEST_PROMOTIONS) {
            PromotionTable t = TEST_PROMOTIONS.get(civ);
            if (t != null) {
                return t;
            }
        }
        return promotions.getOrDefault(civ, PromotionTable.empty(civ));
    }

    public static VillageStageTable stages(Civilization civ) {
        synchronized (TEST_STAGES) {
            VillageStageTable t = TEST_STAGES.get(civ);
            if (t != null) {
                return t;
            }
        }
        return stages.getOrDefault(civ, VillageStageTable.empty(civ));
    }

    /** 只供 GameTest：临时替换；传 null 清除全部替换。 */
    public static void overridePromotionForTest(@Nullable PromotionTable t) {
        synchronized (TEST_PROMOTIONS) {
            if (t == null) {
                TEST_PROMOTIONS.clear();
            } else {
                TEST_PROMOTIONS.put(t.civ(), t);
            }
        }
    }

    public static void overrideStagesForTest(@Nullable VillageStageTable t) {
        synchronized (TEST_STAGES) {
            if (t == null) {
                TEST_STAGES.clear();
            } else {
                TEST_STAGES.put(t.civ(), t);
            }
        }
    }

    private static final class Loader<T> extends SimpleJsonResourceReloadListener {
        private final String what;
        private final BiFunction<JsonElement, List<String>, T> parser;
        private final java.util.function.Function<T, Civilization> civOf;
        private final java.util.function.Consumer<Map<Civilization, T>> store;
        private final java.util.function.Supplier<Map<Civilization, T>> current;

        Loader(String dir, String what, BiFunction<JsonElement, List<String>, T> parser,
               java.util.function.Function<T, Civilization> civOf, java.util.function.Consumer<Map<Civilization, T>> store,
               java.util.function.Supplier<Map<Civilization, T>> current) {
            super(new Gson(), dir);
            this.what = what;
            this.parser = parser;
            this.civOf = civOf;
            this.store = store;
            this.current = current;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<Civilization, T> next = new EnumMap<>(Civilization.class);
            Map<Civilization, T> old = current.get();
            files.forEach((id, json) -> {
                Optional<Civilization> civ = Civilization.byId(id.getPath());
                if (civ.isEmpty()) {
                    MythicAge.LOGGER.error("{} {}：文件名不是文明 id，已忽略", what, id);
                    return;
                }
                List<String> warnings = new ArrayList<>();
                try {
                    T t = parser.apply(json, warnings);
                    if (civOf.apply(t) != civ.get()) {
                        throw new PromotionTable.PromotionTableException("civilization: 与文件名 " + id.getPath() + " 不一致");
                    }
                    warnings.forEach(w -> MythicAge.LOGGER.warn("{} {}：{}", what, id, w));
                    next.put(civ.get(), t);
                } catch (RuntimeException e) {
                    MythicAge.LOGGER.error("{} {} 解析失败，保留上一版：{}", what, id, e.getMessage());
                    if (old.containsKey(civ.get())) {
                        next.put(civ.get(), old.get(civ.get()));
                    }
                }
            });
            store.accept(Map.copyOf(next));
        }
    }
}
