package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesSchema;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * T3 配置的集成测试（TC-T3-70 至 TC-T3-74）。
 * 修改配置文件的步骤都在同一个测试函数内顺序执行，并在 finally 中恢复原文件和内存中的值。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigGameTests {
    private static final String BATCH = "config";

    private ConfigGameTests() {
    }

    /** TC-T3-70、TC-T3-71。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void configs_load_and_reload_unchanged(GameTestHelper helper) {
        Map<String, ConfigHolder<?>> all = ModConfigs.all();
        helper.assertTrue(all.size() == 3, "应有 3 份配置");
        for (Map.Entry<String, ConfigHolder<?>> e : all.entrySet()) {
            helper.assertTrue(e.getValue().version() >= 1, e.getKey() + " 版本号应 ≥1");
            helper.assertTrue(Files.isRegularFile(ModConfigs.file(e.getValue().schema())), e.getKey() + " 文件应存在");
        }
        Map<String, Long> before = versions(all);
        run(helper.getLevel().getServer(), op(helper), "mythicage config reload all");
        helper.assertTrue(before.equals(versions(ModConfigs.all())), "未改文件时 reload 不应改变版本号");
        helper.succeed();
    }

    /** TC-T3-72、TC-T3-73、TC-T3-74。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void world_rules_reload_cycle(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Path file = ModConfigs.file(WorldRulesSchema.INSTANCE);
        String original = read(file);
        ConfigHolder<WorldRulesConfig> holder = ModConfigs.worldRules();
        try {
            long v0 = holder.version();
            WorldRulesConfig value0 = holder.current();

            // TC-T3-72：非法 JSON → 保留旧配置
            write(file, "{ broken");
            run(server, op(helper), "mythicage config reload world_rules");
            helper.assertTrue(holder.version() == v0, "非法 JSON 不应改变版本号");
            helper.assertTrue(holder.current().equals(value0), "非法 JSON 不应改变配置值");

            // TC-T3-73：开启 terrainEdits → 生效，版本号 +1
            write(file, "{\"schemaVersion\":1,\"godPowers\":{\"terrainEdits\":true}}");
            run(server, op(helper), "mythicage config reload world_rules");
            helper.assertTrue(holder.version() == v0 + 1, "合法修改应使版本号 +1");
            helper.assertTrue(holder.current().godPowers().terrainEdits(), "terrainEdits 应为 true");

            // TC-T3-74：权限等级 0 不能热更
            write(file, "{\"schemaVersion\":1,\"godPowers\":{\"terrainEdits\":false}}");
            run(server, server.createCommandSourceStack().withPermission(0).withSuppressedOutput(),
                    "mythicage config reload world_rules");
            helper.assertTrue(holder.version() == v0 + 1, "权限等级 0 的 reload 不应生效");
            helper.assertTrue(holder.current().godPowers().terrainEdits(), "权限等级 0 的 reload 不应改变配置");
        } finally {
            restore(server, helper, file, original);
        }
        helper.assertTrue(!holder.current().godPowers().terrainEdits(), "恢复后 terrainEdits 应回到 false");
        helper.succeed();
    }

    private static void restore(MinecraftServer server, GameTestHelper helper, Path file, String original) {
        write(file, original);
        run(server, op(helper), "mythicage config reload world_rules");
    }

    private static CommandSourceStack op(GameTestHelper helper) {
        return helper.getLevel().getServer().createCommandSourceStack().withPermission(4).withSuppressedOutput();
    }

    private static void run(MinecraftServer server, CommandSourceStack source, String command) {
        server.getCommands().performPrefixedCommand(source, command);
    }

    private static Map<String, Long> versions(Map<String, ConfigHolder<?>> all) {
        Map<String, Long> map = new java.util.LinkedHashMap<>();
        all.forEach((k, v) -> map.put(k, v.version()));
        return map;
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, String text) {
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
