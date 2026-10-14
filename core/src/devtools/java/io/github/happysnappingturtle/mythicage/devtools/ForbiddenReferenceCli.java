package io.github.happysnappingturtle.mythicage.devtools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 供 Gradle 检查任务调用：扫描一个 class 目录，发现被禁止的引用就以非 0 退出码结束。
 * <p>
 * 参数：{@code <class 目录> <规则>...}。规则以 {@code /} 结尾时按包前缀匹配，否则按整类名匹配。
 */
public final class ForbiddenReferenceCli {
    private ForbiddenReferenceCli() {
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args[0]);
        List<String> prefixes = new ArrayList<>();
        List<String> classes = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            (args[i].endsWith("/") ? prefixes : classes).add(args[i]);
        }
        List<String> report = new ArrayList<>();
        int scanned = 0;
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".class")).sorted().toList()) {
                    scanned++;
                    byte[] bytes = Files.readAllBytes(file);
                    List<String> hits = new ArrayList<>(ForbiddenReferenceScanner.findForbiddenReferences(bytes, prefixes));
                    hits.addAll(ForbiddenReferenceScanner.findForbiddenClasses(bytes, classes));
                    for (String hit : hits) {
                        report.add(dir.relativize(file) + " 引用了 " + hit);
                    }
                }
            }
        }
        if (!report.isEmpty()) {
            System.err.println("禁止引用检查失败（扫描 " + scanned + " 个类）：\n" + String.join("\n", report)); // hygiene-allow: 构建检查工具自身的输出
            System.exit(1); // hygiene-allow: 以退出码通知 Gradle
        }
    }
}
