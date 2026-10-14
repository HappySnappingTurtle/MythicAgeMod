package io.github.happysnappingturtle.mythicage.devtools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 供 Gradle 任务 {@code checkSourceHygiene} 调用：扫描给定目录下的全部 .java 文件，发现违规就以非 0 退出码结束。
 */
public final class SourceHygieneCli {
    private SourceHygieneCli() {
    }

    public static void main(String[] args) throws IOException {
        List<String> report = new ArrayList<>();
        for (String root : args) {
            Path dir = Path.of(root);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    for (SourceHygiene.Violation v : SourceHygiene.findViolations(content)) {
                        report.add(file + ":" + v.line() + ": " + v.text());
                    }
                }
            }
        }
        if (!report.isEmpty()) {
            String message = "源码卫生检查失败（请改用 LOGGER，或在该行加 // " + SourceHygiene.ALLOW_MARKER + " 原因）：\n"
                    + String.join("\n", report);
            System.err.println(message); // hygiene-allow: 构建检查工具自身的输出
            System.exit(1); // hygiene-allow: 以退出码通知 Gradle
        }
    }
}
