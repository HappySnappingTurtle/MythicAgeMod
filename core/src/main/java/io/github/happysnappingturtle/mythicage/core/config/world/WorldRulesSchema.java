package io.github.happysnappingturtle.mythicage.core.config.world;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.config.ConfigSchema;
import io.github.happysnappingturtle.mythicage.core.config.ObjectReader;

/**
 * {@code world_rules.json} 的 schema（T3 设计文档 §5.2）。
 */
public final class WorldRulesSchema implements ConfigSchema<WorldRulesConfig> {
    public static final WorldRulesSchema INSTANCE = new WorldRulesSchema();

    private static final WorldRulesConfig DEFAULTS =
            new WorldRulesConfig(true, true, true, new WorldRulesConfig.GodPowers(false),
                    new WorldRulesConfig.Recipes(new WorldRulesConfig.AgeGated(true, false)),
                    new WorldRulesConfig.Growth(24000, 12000, new WorldRulesConfig.BoneMeal(0.5, false)),
                    WorldRulesConfig.Temples.DEFAULTS, WorldRulesConfig.Guardians.DEFAULTS);

    private WorldRulesSchema() {
    }

    @Override
    public String name() {
        return "world_rules";
    }

    @Override
    public int currentVersion() {
        return 1;
    }

    @Override
    public WorldRulesConfig defaults() {
        return DEFAULTS;
    }

    @Override
    public WorldRulesConfig read(ObjectReader root, WorldRulesConfig base) {
        return new WorldRulesConfig(
                root.bool("beneficialConversions", base.beneficialConversions()),
                root.bool("mobBlockDamage", base.mobBlockDamage()),
                root.bool("blockEntityStall", base.blockEntityStall()),
                new WorldRulesConfig.GodPowers(
                        root.object("godPowers").bool("terrainEdits", base.godPowers().terrainEdits())),
                readRecipes(root.object("recipes"), base.recipes()),
                readGrowth(root.object("growth"), base.growth()),
                readTemples(root.object("temples"), base.temples()),
                readGuardians(root.object("guardians"), base.guardians()));
    }

    private static WorldRulesConfig.Guardians readGuardians(ObjectReader g, WorldRulesConfig.Guardians base) {
        return new WorldRulesConfig.Guardians(
                g.bool("spawnInVillages", base.spawnInVillages()),
                g.intIn("respawnDays", base.respawnDays(), 0, 30),
                g.intIn("interactRange", base.interactRange(), 4, 32));
    }

    private static WorldRulesConfig.Temples readTemples(ObjectReader t, WorldRulesConfig.Temples base) {
        ObjectReader limits = t.object("altarLimitByAge");
        return new WorldRulesConfig.Temples(
                limits.intIn("archaic", base.altarLimitArchaic(), 0, 16),
                limits.intIn("classical", base.altarLimitClassical(), 0, 16),
                limits.intIn("heroic", base.altarLimitHeroic(), 0, 16),
                limits.intIn("mythic", base.altarLimitMythic(), 0, 16),
                t.intIn("altarSpacing", base.altarSpacing(), 1, 64),
                t.bool("protectAltar", base.protectAltar()),
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
        JsonObject growth = new JsonObject();
        growth.addProperty("oliveFruitTicks", value.growth().oliveFruitTicks());
        growth.addProperty("grapeStageTicks", value.growth().grapeStageTicks());
        JsonObject bone = new JsonObject();
        bone.addProperty("grapeChance", value.growth().boneMeal().grapeChance());
        bone.addProperty("oliveLeaves", value.growth().boneMeal().oliveLeaves());
        growth.add("boneMeal", bone);
        root.add("growth", growth);
        WorldRulesConfig.Temples t = value.temples();
        JsonObject temples = new JsonObject();
        JsonObject limits = new JsonObject();
        limits.addProperty("archaic", t.altarLimitArchaic());
        limits.addProperty("classical", t.altarLimitClassical());
        limits.addProperty("heroic", t.altarLimitHeroic());
        limits.addProperty("mythic", t.altarLimitMythic());
        temples.add("altarLimitByAge", limits);
        temples.addProperty("altarSpacing", t.altarSpacing());
        temples.addProperty("protectAltar", t.protectAltar());
        temples.addProperty("scanCellsPerTick", t.scanCellsPerTick());
        temples.addProperty("rescanIntervalTicks", t.rescanIntervalTicks());
        root.add("temples", temples);
        WorldRulesConfig.Guardians gd = value.guardians();
        JsonObject guardians = new JsonObject();
        guardians.addProperty("spawnInVillages", gd.spawnInVillages());
        guardians.addProperty("respawnDays", gd.respawnDays());
        guardians.addProperty("interactRange", gd.interactRange());
        root.add("guardians", guardians);
        return root;
    }
}
