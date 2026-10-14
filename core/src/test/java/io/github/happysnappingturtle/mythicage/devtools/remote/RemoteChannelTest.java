package io.github.happysnappingturtle.mythicage.devtools.remote;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteChannelTest {

    private static void append(Path file, byte[] bytes) throws IOException {
        Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void append(Path file, String text) throws IOException {
        append(file, text.getBytes(StandardCharsets.UTF_8));
    }

    private static RemoteChannel fresh(Path dir) throws IOException {
        RemoteChannel ch = new RemoteChannel(dir);
        ch.rotateSession("old");
        return ch;
    }

    @Test
    void tc_t2b_10_twoCompleteLines(@TempDir Path dir) throws IOException {
        RemoteChannel ch = fresh(dir);
        append(dir.resolve("commands.jsonl"), "{\"id\":1,\"cmd\":\"a\"}\n{\"id\":2,\"cmd\":\"b\"}\n");
        assertEquals("{\"id\":1,\"cmd\":\"a\"}", ch.nextCompleteLine());
        assertEquals("{\"id\":2,\"cmd\":\"b\"}", ch.nextCompleteLine());
        assertNull(ch.nextCompleteLine());
    }

    @Test
    void tc_t2b_11_partialLineWaitsForNewline(@TempDir Path dir) throws IOException {
        RemoteChannel ch = fresh(dir);
        Path cmds = dir.resolve("commands.jsonl");
        append(cmds, "{\"id\":1,\"cmd\":");
        assertNull(ch.nextCompleteLine());
        append(cmds, "\"state\"}\n");
        assertEquals("{\"id\":1,\"cmd\":\"state\"}", ch.nextCompleteLine());
    }

    @Test
    void multiByteCharacterSplitAcrossReads(@TempDir Path dir) throws IOException {
        RemoteChannel ch = fresh(dir);
        Path cmds = dir.resolve("commands.jsonl");
        byte[] line = "{\"id\":1,\"cmd\":\"chat\",\"text\":\"你好\"}\n".getBytes(StandardCharsets.UTF_8);
        int cut = new String(line, StandardCharsets.UTF_8).indexOf('你') + 1; // 「你」三个字节中间切开
        int byteCut = "{\"id\":1,\"cmd\":\"chat\",\"text\":\"".getBytes(StandardCharsets.UTF_8).length + 1;
        assertTrue(cut > 0);
        append(cmds, Arrays.copyOfRange(line, 0, byteCut));
        assertNull(ch.nextCompleteLine());
        append(cmds, Arrays.copyOfRange(line, byteCut, line.length));
        assertEquals("{\"id\":1,\"cmd\":\"chat\",\"text\":\"你好\"}", ch.nextCompleteLine());
    }

    @Test
    void tc_t2b_12_blankLinesAreSkipped(@TempDir Path dir) throws IOException {
        RemoteChannel ch = fresh(dir);
        append(dir.resolve("commands.jsonl"), "\n   \n{\"id\":1,\"cmd\":\"a\"}\n\n");
        assertEquals("{\"id\":1,\"cmd\":\"a\"}", ch.nextCompleteLine());
        assertNull(ch.nextCompleteLine());
    }

    @Test
    void tc_t2b_13_rotationMovesLeftoversToHistory(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("commands.jsonl"), "{\"id\":1,\"cmd\":\"old\"}\n");
        Files.writeString(dir.resolve("results.jsonl"), "{}\n");
        Files.createDirectories(dir.resolve("frames"));
        Files.writeString(dir.resolve("frames/0001_x.png"), "png");
        Files.writeString(dir.resolve("ready.json"), "{\"sessionId\":\"20261013-120000\"}");

        RemoteChannel ch = new RemoteChannel(dir);
        String historyName = ch.previousSessionIdOr("fallback");
        assertEquals("20261013-120000", historyName);
        ch.rotateSession(historyName);

        Path history = dir.resolve("history/20261013-120000");
        assertTrue(Files.exists(history.resolve("commands.jsonl")));
        assertTrue(Files.exists(history.resolve("results.jsonl")));
        assertTrue(Files.exists(history.resolve("frames/0001_x.png")));
        assertEquals(0, Files.size(dir.resolve("commands.jsonl")));
        assertFalse(Files.exists(dir.resolve("results.jsonl")));
        assertFalse(Files.exists(dir.resolve("ready.json")), "旧 ready.json 应删除，外部工具才能等到新会话");
        assertNull(ch.nextCompleteLine(), "旧指令不能被重放");
    }

    @Test
    void tc_t2b_14_corruptReadyUsesFallback(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("ready.json"), "{ broken");
        Files.writeString(dir.resolve("commands.jsonl"), "x\n");
        RemoteChannel ch = new RemoteChannel(dir);
        String name = ch.previousSessionIdOr("20261013-130000");
        assertEquals("20261013-130000", name);
        ch.rotateSession(name);
        assertTrue(Files.exists(dir.resolve("history/20261013-130000/commands.jsonl")));
    }

    @Test
    void rotationWithSameHistoryNameDoesNotOverwrite(@TempDir Path dir) throws IOException {
        RemoteChannel ch = new RemoteChannel(dir);
        Files.writeString(dir.resolve("commands.jsonl"), "a\n");
        ch.rotateSession("same");
        Files.writeString(dir.resolve("results.jsonl"), "b\n");
        ch.rotateSession("same");
        assertTrue(Files.exists(dir.resolve("history/same/commands.jsonl")));
        assertTrue(Files.exists(dir.resolve("history/same-2/results.jsonl")));
    }

    @Test
    void tc_t2b_15_resultIsOnDiskImmediately(@TempDir Path dir) throws IOException {
        RemoteChannel ch = fresh(dir);
        ch.writeResult("{\"id\":1}");
        ch.writeResult("{\"id\":2}");
        assertEquals("{\"id\":1}\n{\"id\":2}\n", Files.readString(dir.resolve("results.jsonl")));
        JsonObject ready = new JsonObject();
        ready.addProperty("sessionId", "s1");
        ch.writeReady(ready);
        assertEquals("s1", ch.previousSessionIdOr("x"));
    }

    @Test
    void tc_t2b_16_frameFileNames() {
        assertEquals("0001_hud.png", RemoteChannel.frameFileName(1, "hud"));
        assertEquals("0012_神殿_正面.png", RemoteChannel.frameFileName(12, "神殿 正面"));
        assertEquals("0003_.._.._x.png", RemoteChannel.frameFileName(3, "../../x"));
        assertEquals("0004_frame.png", RemoteChannel.frameFileName(4, ""));
    }
}
