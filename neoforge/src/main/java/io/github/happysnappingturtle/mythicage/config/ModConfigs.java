package io.github.happysnappingturtle.mythicage.config;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigFiles;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigIssue;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoadResult;
import io.github.happysnappingturtle.mythicage.core.config.ConfigSchema;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductConfig;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductSchema;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfigSchema;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesSchema;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 三份服务端配置的持有者。每次服务器启动（ServerAboutToStartEvent，最高优先级）重新加载并创建 holder；
 * 需要生效前钩子的系统应在同一事件的普通优先级中向新的 holder 注册钩子。
 */
public final class ModConfigs {
    private static ConfigHolder<RuntimeConfig> runtime;
    private static ConfigHolder<WorldRulesConfig> worldRules;
    private static ConfigHolder<ConductConfig> conduct;

    private ModConfigs() {
    }

    public static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve(MythicAgeIds.MOD_ID);
    }

    public static Path file(ConfigSchema<?> schema) {
        return directory().resolve(schema.name() + ".json");
    }

    public static ConfigHolder<RuntimeConfig> runtime() {
        return require(runtime, "runtime");
    }

    public static ConfigHolder<WorldRulesConfig> worldRules() {
        return require(worldRules, "world_rules");
    }

    public static ConfigHolder<ConductConfig> conduct() {
        return require(conduct, "conduct");
    }

    /** 按文件名取 holder，顺序固定：runtime、world_rules、conduct。 */
    public static Map<String, ConfigHolder<?>> all() {
        Map<String, ConfigHolder<?>> map = new LinkedHashMap<>();
        map.put(RuntimeConfigSchema.INSTANCE.name(), runtime());
        map.put(WorldRulesSchema.INSTANCE.name(), worldRules());
        map.put(ConductSchema.INSTANCE.name(), conduct());
        return map;
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        runtime = load(RuntimeConfigSchema.INSTANCE);
        worldRules = load(WorldRulesSchema.INSTANCE);
        conduct = load(ConductSchema.INSTANCE);
    }

    private static <T> ConfigHolder<T> load(ConfigSchema<T> schema) {
        Path file = file(schema);
        ConfigLoadResult<T> result = ConfigFiles.loadOrCreate(file, schema);
        logIssues(schema.name(), result.issues());
        MythicAge.LOGGER.info("Loaded config {} ({} issue(s){})", file, result.issues().size(),
                result.usedFallback() ? ", using fallback values" : "");
        return ConfigHolder.create(schema, result);
    }

    static void logIssues(String name, List<ConfigIssue> issues) {
        for (ConfigIssue issue : issues) {
            if (issue.severity() == ConfigIssue.Severity.ERROR) {
                MythicAge.LOGGER.error("[config {}] {}", name, issue);
            } else {
                MythicAge.LOGGER.warn("[config {}] {}", name, issue);
            }
        }
    }

    private static <T> ConfigHolder<T> require(ConfigHolder<T> holder, String name) {
        if (holder == null) {
            throw new IllegalStateException("配置 " + name + " 尚未加载：服务器还没有启动");
        }
        return holder;
    }
}
