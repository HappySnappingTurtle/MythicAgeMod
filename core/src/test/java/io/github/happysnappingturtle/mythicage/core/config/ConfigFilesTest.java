package io.github.happysnappingturtle.mythicage.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFilesTest {
    private static final TestSchema S = TestSchema.INSTANCE;

    @Test
    void tc_t3_60_missingFileIsCreatedWithDefaults(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("nested/mythic_age/test.json");
        ConfigLoadResult<TestSchema.Value> r = ConfigFiles.loadOrCreate(file, S);
        assertEquals(TestSchema.DEFAULTS, r.value());
        assertTrue(r.issues().isEmpty());
        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file).contains("\"schemaVersion\": 1"));
    }

    @Test
    void tc_t3_61_existingFileIsNeverRewritten(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("test.json");
        byte[] original = "{\n  \"amount\": 33,\n  \"myNote\": \"服主的备注\"\n}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);
        ConfigLoadResult<TestSchema.Value> r = ConfigFiles.loadOrCreate(file, S);
        assertEquals(33, r.value().amount());
        assertEquals(1, r.issues().size());
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void tc_t3_62_invalidUtf8IsWholeFileError(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("test.json");
        Files.write(file, new byte[]{'{', (byte) 0xC3, (byte) 0x28, '}'});
        ConfigLoadResult<TestSchema.Value> r = ConfigFiles.loadOrCreate(file, S);
        assertTrue(r.usedFallback());
        assertTrue(r.hasErrors());
        assertEquals(TestSchema.DEFAULTS, r.value());
    }

    @Test
    void tc_t3_63_writtenDefaultFileReadsBackCleanly(@TempDir Path dir) {
        Path file = dir.resolve("test.json");
        ConfigFiles.loadOrCreate(file, S);
        ConfigLoadResult<TestSchema.Value> second = ConfigFiles.loadOrCreate(file, S);
        assertEquals(TestSchema.DEFAULTS, second.value());
        assertTrue(second.issues().isEmpty());
        assertFalse(second.usedFallback());
    }

    @Test
    void holderReloadFileKeepsOldWhenUnreadable(@TempDir Path dir) {
        ConfigHolder<TestSchema.Value> h = ConfigHolder.create(S, ConfigLoader.load(S, "{}", null));
        ConfigHolder.ReloadOutcome out = h.reloadFile(dir.resolve("does-not-exist.json"));
        assertEquals(ConfigHolder.Status.KEPT_OLD, out.status());
        assertEquals(1, h.version());
    }
}
