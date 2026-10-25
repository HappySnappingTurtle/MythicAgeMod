package io.github.happysnappingturtle.mythicage.village.work;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 村庄经济数据（T19 设计文档 §11.1）：三个目录，按文明汇总成 {@link CivVillageData}。
 * <pre>
 * data/&lt;ns&gt;/mythic_age/village_civ/&lt;civ&gt;.json               文明：作息、历法、仓储、口粮、燃料、采集点、作物、居民
 * data/&lt;ns&gt;/mythic_age/village_profession/&lt;civ&gt;/&lt;id&gt;.json   职业（id = civ:文件名）
 * data/&lt;ns&gt;/mythic_age/village_recipe/&lt;civ&gt;/&lt;id&gt;.json       配方（id = civ:文件名）
 * </pre>
 * 解析失败记错误并保留上一版（12 号 §5）。GameTest 可以注入一个文明（测试文明 {@code test}）。
 */
public final class VillageData {
    private static volatile Map<String, CivVillageData> civs = Map.of();
    private static volatile Map<String, Map<String, CivVillageData.Profession>> professions = Map.of();
    private static volatile Map<String, Map<String, VillageRecipe>> recipes = Map.of();
    private static final Map<String, CivVillageData> TEST = new HashMap<>();

    private VillageData() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader("mythic_age/village_civ", Kind.CIV));
        event.addListener(new Loader("mythic_age/village_profession", Kind.PROFESSION));
        event.addListener(new Loader("mythic_age/village_recipe", Kind.RECIPE));
    }

    public static Optional<CivVillageData> civ(String civ) {
        synchronized (TEST) {
            CivVillageData t = TEST.get(civ);
            if (t != null) {
                return Optional.of(t);
            }
        }
        CivVillageData base = civs.get(civ);
        if (base == null) {
            return Optional.empty();
        }
        return Optional.of(base.withProfessions(professions.getOrDefault(civ, Map.of()), recipes.getOrDefault(civ, Map.of())));
    }

    /** 村庄的历法偏移：村庄 id 派生 + 开发命令的调整（{@code calendarShift}）。 */
    public static int offset(CivVillageData data,
                             io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy.State state) {
        return data.calendar().offset(state.village) + state.calendarShift;
    }

    /** 只供 GameTest：注入或清除（null）一个文明的数据。 */
    public static void overrideForTest(String civ, @Nullable CivVillageData data) {
        synchronized (TEST) {
            if (data == null) {
                TEST.remove(civ);
            } else {
                TEST.put(civ, data);
            }
        }
    }

    private enum Kind { CIV, PROFESSION, RECIPE }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private final Kind kind;

        Loader(String dir, Kind kind) {
            super(new Gson(), dir);
            this.kind = kind;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            switch (kind) {
                case CIV -> {
                    Map<String, CivVillageData> next = new LinkedHashMap<>(civs);
                    files.forEach((id, json) -> {
                        try {
                            next.put(id.getPath(), CivVillageData.parse(id.getPath(), json.getAsJsonObject()));
                        } catch (RuntimeException e) {
                            MythicAge.LOGGER.error("村庄数据 {} 解析失败，保留上一版：{}", id, e.getMessage());
                        }
                    });
                    civs = Map.copyOf(next);
                }
                case PROFESSION -> {
                    Map<String, Map<String, CivVillageData.Profession>> next = new LinkedHashMap<>();
                    files.forEach((id, json) -> split(id).ifPresent(s -> {
                        try {
                            next.computeIfAbsent(s[0], k -> new LinkedHashMap<>()).put(s[0] + ":" + s[1],
                                    CivVillageData.parseProfession(s[0] + ":" + s[1], json.getAsJsonObject()));
                        } catch (RuntimeException e) {
                            MythicAge.LOGGER.error("村庄职业 {} 解析失败：{}", id, e.getMessage());
                        }
                    }));
                    professions = freeze(next);
                }
                case RECIPE -> {
                    Map<String, Map<String, VillageRecipe>> next = new LinkedHashMap<>();
                    files.forEach((id, json) -> split(id).ifPresent(s -> {
                        try {
                            next.computeIfAbsent(s[0], k -> new LinkedHashMap<>()).put(s[0] + ":" + s[1],
                                    VillageRecipe.parse(s[0] + ":" + s[1], s[0], json.getAsJsonObject()));
                        } catch (RuntimeException e) {
                            MythicAge.LOGGER.error("村庄配方 {} 解析失败：{}", id, e.getMessage());
                        }
                    }));
                    recipes = freeze(next);
                }
            }
        }

        private static Optional<String[]> split(ResourceLocation id) {
            int i = id.getPath().indexOf('/');
            if (i <= 0) {
                MythicAge.LOGGER.error("村庄数据 {} 要放在 <文明>/ 子目录下", id);
                return Optional.empty();
            }
            return Optional.of(new String[]{id.getPath().substring(0, i), id.getPath().substring(i + 1)});
        }

        private static <T> Map<String, Map<String, T>> freeze(Map<String, Map<String, T>> m) {
            Map<String, Map<String, T>> r = new LinkedHashMap<>();
            m.forEach((k, v) -> r.put(k, Map.copyOf(v)));
            return Map.copyOf(r);
        }
    }
}
