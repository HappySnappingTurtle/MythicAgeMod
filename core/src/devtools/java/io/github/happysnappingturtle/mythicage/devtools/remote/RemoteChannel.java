package io.github.happysnappingturtle.mythicage.devtools.remote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * 遥控口文件通道（T2b 设计文档 §2）：commands.jsonl / results.jsonl / ready.json / frames / history。
 * <p>
 * 只吐出以换行结尾的完整行；半行留在缓冲区，等补齐换行再一起吐出。缓冲按<b>字节</b>保存，
 * 在 0x0A 处切行（UTF-8 多字节字符中不会出现 0x0A），避免一个中文字符被截在两次读取之间而解码出错。
 */
public final class RemoteChannel {
    private final Path root;
    private final Path commandsFile;
    private final Path resultsFile;
    private final Path readyFile;
    private final Path framesDir;

    private long offset;
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    private final Deque<String> queue = new ArrayDeque<>();

    public RemoteChannel(Path root) {
        this.root = root;
        this.commandsFile = root.resolve("commands.jsonl");
        this.resultsFile = root.resolve("results.jsonl");
        this.readyFile = root.resolve("ready.json");
        this.framesDir = root.resolve("frames");
    }

    public Path root() {
        return root;
    }

    public Path framesDir() {
        return framesDir;
    }

    /** 上一会话的 sessionId（读旧 ready.json）；文件不存在或损坏时返回 fallback。 */
    public String previousSessionIdOr(String fallback) {
        if (!Files.exists(readyFile)) {
            return fallback;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(readyFile, StandardCharsets.UTF_8));
            if (parsed.isJsonObject() && parsed.getAsJsonObject().has("sessionId")) {
                return parsed.getAsJsonObject().get("sessionId").getAsString();
            }
        } catch (IOException | RuntimeException e) {
            // 旧文件损坏不阻塞新会话
        }
        return fallback;
    }

    /** 会话轮换：上一会话的 commands、results、frames 整体移入 history/&lt;historyName&gt;/，偏移归零。 */
    public void rotateSession(String historyName) throws IOException {
        Files.createDirectories(root);
        boolean leftover = Files.exists(commandsFile) || Files.exists(resultsFile) || Files.exists(framesDir);
        if (leftover) {
            Path historyDir = uniqueDir(root.resolve("history").resolve(safeName(historyName)));
            Files.createDirectories(historyDir);
            moveIfExists(commandsFile, historyDir.resolve("commands.jsonl"));
            moveIfExists(resultsFile, historyDir.resolve("results.jsonl"));
            moveIfExists(framesDir, historyDir.resolve("frames"));
        }
        Files.deleteIfExists(readyFile);
        Files.createFile(commandsFile);
        offset = 0;
        pending.reset();
        queue.clear();
    }

    /** @return 下一条完整指令行；没有就返回 null。空行和只有空白的行会被跳过。 */
    public String nextCompleteLine() throws IOException {
        if (!queue.isEmpty()) {
            return queue.poll();
        }
        if (!Files.exists(commandsFile)) {
            return null;
        }
        long size = Files.size(commandsFile);
        if (size <= offset) {
            return null;
        }
        byte[] fresh = new byte[(int) (size - offset)];
        try (RandomAccessFile raf = new RandomAccessFile(commandsFile.toFile(), "r")) {
            raf.seek(offset);
            raf.readFully(fresh);
        }
        offset = size;
        pending.write(fresh);
        byte[] all = pending.toByteArray();
        int lastNewline = -1;
        for (int i = all.length - 1; i >= 0; i--) {
            if (all[i] == '\n') {
                lastNewline = i;
                break;
            }
        }
        if (lastNewline < 0) {
            return null;
        }
        String block = new String(all, 0, lastNewline, StandardCharsets.UTF_8);
        pending.reset();
        pending.write(all, lastNewline + 1, all.length - lastNewline - 1);
        for (String line : block.split("\n", -1)) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                queue.add(trimmed);
            }
        }
        return queue.poll();
    }

    /** 追加一行结果并立即落盘。 */
    public void writeResult(String line) throws IOException {
        Files.writeString(resultsFile, line + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC);
    }

    public void writeReady(JsonObject payload) throws IOException {
        Files.writeString(readyFile, RemoteProtocol.toJson(payload), StandardCharsets.UTF_8);
    }

    /** 帧文件名：4 位序号 + 名字；名字中的非法字符替换为下划线。 */
    public static String frameFileName(int seq, String name) {
        return String.format(Locale.ROOT, "%04d_%s.png", seq, safeName(name));
    }

    static String safeName(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        return cleaned.isEmpty() ? "frame" : cleaned;
    }

    private static Path uniqueDir(Path base) {
        Path candidate = base;
        for (int i = 2; Files.exists(candidate); i++) {
            candidate = base.resolveSibling(base.getFileName() + "-" + i);
        }
        return candidate;
    }

    private static void moveIfExists(Path from, Path to) throws IOException {
        if (Files.exists(from)) {
            Files.move(from, to);
        }
    }
}
