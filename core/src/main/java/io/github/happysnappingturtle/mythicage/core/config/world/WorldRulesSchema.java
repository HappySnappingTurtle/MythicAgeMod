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
            new WorldRulesConfig(true, true, true, new WorldRulesConfig.GodPowers(false));

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
                        root.object("godPowers").bool("terrainEdits", base.godPowers().terrainEdits())));
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
        return root;
    }
}
