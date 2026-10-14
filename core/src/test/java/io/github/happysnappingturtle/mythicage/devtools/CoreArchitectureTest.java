package io.github.happysnappingturtle.mythicage.devtools;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** core 主源集的架构约束：零 Minecraft/NeoForge 依赖，并编译为 Java 21 字节码。 */
class CoreArchitectureTest {
    private static final List<String> FORBIDDEN = List.of("net/minecraft/", "net/neoforged/", "com/mojang/");
    private static final int JAVA_21_MAJOR = 65;

    private static List<Path> coreClassFiles() throws IOException {
        String dirs = System.getProperty("mythicage.coreClassesDirs");
        assertTrue(dirs != null && !dirs.isBlank(), "Gradle 应传入 mythicage.coreClassesDirs");
        List<Path> result = new ArrayList<>();
        for (String dir : dirs.split(File.pathSeparator)) {
            Path root = Path.of(dir);
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(p -> p.toString().endsWith(".class")).forEach(result::add);
            }
        }
        return result;
    }

    @Test
    void tc_t2_03_coreHasNoMinecraftOrLoaderReferences() throws IOException {
        List<Path> classes = coreClassFiles();
        assertFalse(classes.isEmpty(), "core 主源集至少应有一个类");
        List<String> problems = new ArrayList<>();
        for (Path file : classes) {
            for (String hit : ForbiddenReferenceScanner.findForbiddenReferences(Files.readAllBytes(file), FORBIDDEN)) {
                problems.add(file.getFileName() + " -> " + hit);
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void tc_t2_01_coreIsCompiledForJava21() throws IOException {
        for (Path file : coreClassFiles()) {
            try (InputStream raw = Files.newInputStream(file); DataInputStream in = new DataInputStream(raw)) {
                in.readInt();
                in.readUnsignedShort();
                assertEquals(JAVA_21_MAJOR, in.readUnsignedShort(), file.toString());
            }
        }
    }
}
