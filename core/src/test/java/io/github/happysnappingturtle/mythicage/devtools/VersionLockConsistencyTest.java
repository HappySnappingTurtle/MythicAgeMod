package io.github.happysnappingturtle.mythicage.devtools;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T2-34：gradle.properties、Wrapper、MDG 插件版本与 08 号技术依据文档的锁定表一致。 */
class VersionLockConsistencyTest {
    private static final List<String> REQUIRED_KEYS = List.of(
            "minecraft_version", "neo_version", "parchment_minecraft_version", "parchment_mappings_version",
            "geckolib_version", "curios_version", "patchouli_version", "jei_version");

    private static Path root() {
        String root = System.getProperty("mythicage.rootDir");
        assertNotNull(root, "Gradle 应传入 mythicage.rootDir");
        return Path.of(root);
    }

    private static Properties load(String text) throws IOException {
        Properties p = new Properties();
        p.load(new StringReader(text));
        return p;
    }

    private static String lockDocument() throws IOException {
        return Files.readString(root().resolve("docs/重构/设计/细化规划/08_技术依据与版本边界.md"), StandardCharsets.UTF_8);
    }

    @Test
    void gradlePropertiesMatchLockTable() throws IOException {
        Properties gradle = load(Files.readString(root().resolve("gradle.properties"), StandardCharsets.UTF_8));
        String doc = lockDocument();
        int start = doc.indexOf("## 版本锁定");
        assertTrue(start >= 0, "08 号文档缺少「版本锁定」一节");
        Matcher block = Pattern.compile("```properties\\n(.*?)```", Pattern.DOTALL).matcher(doc.substring(start));
        assertTrue(block.find(), "锁定表应包含 properties 代码块");
        Properties locked = load(block.group(1));
        for (String key : REQUIRED_KEYS) {
            assertNotNull(gradle.getProperty(key), "gradle.properties 缺少 " + key);
            assertEquals(locked.getProperty(key), gradle.getProperty(key), key);
        }
    }

    @Test
    void wrapperAndPluginVersionsMatchLockTable() throws IOException {
        String doc = lockDocument();
        String wrapper = Files.readString(root().resolve("gradle/wrapper/gradle-wrapper.properties"), StandardCharsets.UTF_8);
        String neoforgeBuild = Files.readString(root().resolve("neoforge/build.gradle"), StandardCharsets.UTF_8);

        Matcher gradleRow = Pattern.compile("\\| Gradle \\| ([0-9.]+) \\|").matcher(doc);
        assertTrue(gradleRow.find(), "锁定表缺少 Gradle 行");
        assertTrue(wrapper.contains("gradle-" + gradleRow.group(1) + "-bin.zip"), "Wrapper 版本应为 " + gradleRow.group(1));

        Matcher mdgRow = Pattern.compile("\\| ModDevGradle \\| ([0-9.]+)").matcher(doc);
        assertTrue(mdgRow.find(), "锁定表缺少 ModDevGradle 行");
        assertTrue(neoforgeBuild.contains("id 'net.neoforged.moddev' version '" + mdgRow.group(1) + "'"),
                "neoforge/build.gradle 的 MDG 版本应为 " + mdgRow.group(1));
    }
}
