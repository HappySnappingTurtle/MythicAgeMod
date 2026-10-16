package io.github.happysnappingturtle.mythicage.core.config.world;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.config.ConfigSchema;
import io.github.happysnappingturtle.mythicage.core.config.ObjectReader;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@code world_rules.json} 的 schema（T3 设计文档 §5.2；T10b 设计文档 §3.4 升为第 2 版）。
 * <p>
 * 第 1 版的 {@code temples}（神坛上限、间距、保护、扫描预算）与顶层 {@code growth} 仍可读取，
 * 分别读作 {@code shrines.greek.altar}、{@code greek.temple}、{@code greek.growth}，并告警指明新位置。
 * 按 T3 规则不改写用户的文件；新旧同时存在时以新结构为准。
 */
public final class WorldRulesSchema implements ConfigSchema<WorldRulesConfig> {
    public static final WorldRulesSchema INSTANCE = new WorldRulesSchema();
    /** 神庙种类 id 的后半段（文明 id 之后）。 */
    private static final Pattern KIND = Pattern.compile("[a-z0-9_]+");

    private static final WorldRulesConfig DEFAULTS =
            new WorldRulesConfig(true, true, true, new WorldRulesConfig.GodPowers(false),
                    new WorldRulesConfig.Recipes(new WorldRulesConfig.AgeGated(true, false)),
                    Map.of("greek:altar", WorldRulesConfig.Shrine.GREEK_ALTAR),
                    WorldRulesConfig.Guardians.DEFAULTS, WorldRulesConfig.Greek.DEFAULTS);

    private WorldRulesSchema() {
    }

    @Override
    public String name() {
        return "world_rules";
    }

    @Override
    public int currentVersion() {
        return 2;
    }

    @Override
    public WorldRulesConfig defaults() {
        return DEFAULTS;
    }

    @Override
    public WorldRulesConfig read(ObjectReader root, WorldRulesConfig base) {
        Map<String, WorldRulesConfig.Shrine> shrines = new LinkedHashMap<>(base.shrines());
        WorldRulesConfig.Greek greek = base.greek();
        // 第 1 版的位置：先读，作为新结构的底
        if (root.isObject("temples")) {
            ObjectReader t = root.object("temples");
            WorldRulesConfig.Shrine altar = shrines.getOrDefault("greek:altar", WorldRulesConfig.Shrine.GREEK_ALTAR);
            ObjectReader limits = t.object("altarLimitByAge");
            altar = new WorldRulesConfig.Shrine(
                    limits.intIn("archaic", altar.limitArchaic(), 0, 16),
                    limits.intIn("classical", altar.limitClassical(), 0, 16),
                    limits.intIn("heroic", altar.limitHeroic(), 0, 16),
                    limits.intIn("mythic", altar.limitMythic(), 0, 16),
                    t.intIn("altarSpacing", altar.spacing(), 1, 64),
                    t.bool("protectAltar", altar.protect()));
            shrines.put("greek:altar", altar);
            greek = greek.withTemple(readTempleBudget(t, greek.temple()));
            root.warn("temples", "第 1 版字段：神坛上限、间距、保护已改为 shrines.greek.altar，扫描预算已改为 greek.temple；本次按旧字段读取");
        }
        if (root.isObject("growth")) {
            greek = greek.withGrowth(readGrowth(root.object("growth"), greek.growth()));
            root.warn("growth", "第 1 版字段：已改为 greek.growth；本次按旧字段读取");
        }
        readShrines(root.object("shrines"), shrines);
        ObjectReader g = root.object("greek");
        greek = new WorldRulesConfig.Greek(readGrowth(g.object("growth"), greek.growth()),
                readTempleBudget(g.object("temple"), greek.temple()),
                new WorldRulesConfig.Prayer(g.object("prayer").intIn("restockLimit", greek.prayer().restockLimit(), 0, 2)));
        return new WorldRulesConfig(
                root.bool("beneficialConversions", base.beneficialConversions()),
                root.bool("mobBlockDamage", base.mobBlockDamage()),
                root.bool("blockEntityStall", base.blockEntityStall()),
                new WorldRulesConfig.GodPowers(
                        root.object("godPowers").bool("terrainEdits", base.godPowers().terrainEdits())),
                readRecipes(root.object("recipes"), base.recipes()),
                shrines,
                readGuardians(root.object("guardians"), base.guardians()),
                greek);
    }

    /** {@code shrines.<文明>.<种类>}：按文明、按种类与已有值合并。 */
    private static void readShrines(ObjectReader s, Map<String, WorldRulesConfig.Shrine> out) {
        for (String civ : s.keys()) {
            ObjectReader c = s.object(civ);
            if (Civilization.byId(civ).isEmpty()) {
                s.warn(civ, "未知文明，已忽略");
                continue;
            }
            for (String kind : c.keys()) {
                if (!KIND.matcher(kind).matches()) {
                    c.warn(kind, "神庙种类只能由小写字母、数字、下划线组成，已忽略");
                    continue;
                }
                ObjectReader k = c.object(kind);
                String id = civ + ":" + kind;
                out.put(id, readShrine(k, out.getOrDefault(id, WorldRulesConfig.Shrine.FALLBACK)));
            }
        }
    }

    private static WorldRulesConfig.Shrine readShrine(ObjectReader k, WorldRulesConfig.Shrine base) {
        ObjectReader limits = k.object("limitByAge");
        return new WorldRulesConfig.Shrine(
                limits.intIn("archaic", base.limitArchaic(), 0, 16),
                limits.intIn("classical", base.limitClassical(), 0, 16),
                limits.intIn("heroic", base.limitHeroic(), 0, 16),
                limits.intIn("mythic", base.limitMythic(), 0, 16),
                k.intIn("spacing", base.spacing(), 1, 64),
                k.bool("protect", base.protect()));
    }

    private static WorldRulesConfig.Guardians readGuardians(ObjectReader g, WorldRulesConfig.Guardians base) {
        return new WorldRulesConfig.Guardians(
                g.bool("spawnInVillages", base.spawnInVillages()),
                g.intIn("respawnDays", base.respawnDays(), 0, 30),
                g.intIn("interactRange", base.interactRange(), 4, 32));
    }

    private static WorldRulesConfig.TempleBudget readTempleBudget(ObjectReader t, WorldRulesConfig.TempleBudget base) {
        return new WorldRulesConfig.TempleBudget(
                t.intIn("scanCellsPerTick", base.scanCellsPerTick(), 256, 65536),
                t.intIn("rescanIntervalTicks", base.rescanIntervalTicks(), 1200, 72000));
    }

    private static WorldRulesConfig.Growth readGrowth(ObjectReader growth, WorldRulesConfig.Growth base) {
        ObjectReader bone = growth.object("boneMeal");
        return new WorldRulesConfig.Growth(
                growth.longIn("oliveFruitTicks", base.oliveFruitTicks(), 1200, 24000L * 30),
                growth.longIn("grapeStageTicks", base.grapeStageTicks(), 1200, 24000L * 30),
                new WorldRulesConfig.BoneMeal(
                        bone.doubleIn("grapeChance", base.boneMeal().grapeChance(), 0, 1),
                        bone.bool("oliveLeaves", base.boneMeal().oliveLeaves())));
    }

    private static WorldRulesConfig.Recipes readRecipes(ObjectReader recipes, WorldRulesConfig.Recipes base) {
        ObjectReader gated = recipes.object("ageGated");
        return new WorldRulesConfig.Recipes(new WorldRulesConfig.AgeGated(
                gated.bool("enabled", base.ageGated().enabled()),
                gated.bool("allowAutomation", base.ageGated().allowAutomation())));
    }

    @Override
    public JsonObject toJson(WorldRulesConfig value) {
        JsonObject root = new JsonObject();
        root.addProperty("beneficialConversions", value.beneficialConversions());
        root.addProperty("mobBlockDamage", value.mobBlockDamage());
        root.addProperty("blockEntityStall", value.blockEntityStall());
        JsonObject godPowers = new JsonObject();
        godPowers.addProperty("terrainEdits", value.godPowers().terrainEdits());
        root.add("godPowers", godPowers);
        JsonObject gated = new JsonObject();
        gated.addProperty("enabled", value.recipes().ageGated().enabled());
        gated.addProperty("allowAutomation", value.recipes().ageGated().allowAutomation());
        JsonObject recipes = new JsonObject();
        recipes.add("ageGated", gated);
        root.add("recipes", recipes);
        JsonObject shrines = new JsonObject();
        value.shrines().keySet().stream().sorted().forEach(id -> {
            WorldRulesConfig.Shrine s = value.shrines().get(id);
            String[] parts = id.split(":", 2);
            JsonObject civ = shrines.has(parts[0]) ? shrines.getAsJsonObject(parts[0]) : new JsonObject();
            JsonObject kind = new JsonObject();
            JsonObject limits = new JsonObject();
            limits.addProperty("archaic", s.limitArchaic());
            limits.addProperty("classical", s.limitClassical());
            limits.addProperty("heroic", s.limitHeroic());
            limits.addProperty("mythic", s.limitMythic());
            kind.add("limitByAge", limits);
            kind.addProperty("spacing", s.spacing());
            kind.addProperty("protect", s.protect());
            civ.add(parts[1], kind);
            shrines.add(parts[0], civ);
        });
        root.add("shrines", shrines);
        WorldRulesConfig.Guardians gd = value.guardians();
        JsonObject guardians = new JsonObject();
        guardians.addProperty("spawnInVillages", gd.spawnInVillages());
        guardians.addProperty("respawnDays", gd.respawnDays());
        guardians.addProperty("interactRange", gd.interactRange());
        root.add("guardians", guardians);
        JsonObject greek = new JsonObject();
        JsonObject growth = new JsonObject();
        WorldRulesConfig.Growth gr = value.greek().growth();
        growth.addProperty("oliveFruitTicks", gr.oliveFruitTicks());
        growth.addProperty("grapeStageTicks", gr.grapeStageTicks());
        JsonObject bone = new JsonObject();
        bone.addProperty("grapeChance", gr.boneMeal().grapeChance());
        bone.addProperty("oliveLeaves", gr.boneMeal().oliveLeaves());
        growth.add("boneMeal", bone);
        greek.add("growth", growth);
        JsonObject temple = new JsonObject();
        temple.addProperty("scanCellsPerTick", value.greek().temple().scanCellsPerTick());
        temple.addProperty("rescanIntervalTicks", value.greek().temple().rescanIntervalTicks());
        greek.add("temple", temple);
        JsonObject prayer = new JsonObject();
        prayer.addProperty("restockLimit", value.greek().prayer().restockLimit());
        greek.add("prayer", prayer);
        root.add("greek", greek);
        return root;
    }
}
