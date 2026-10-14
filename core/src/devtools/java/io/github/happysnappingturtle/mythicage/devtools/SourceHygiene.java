package io.github.happysnappingturtle.mythicage.devtools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 源码卫生规则：生产代码不得使用 {@code System.out}、{@code System.err} 或 {@code printStackTrace()}。
 * <p>
 * 规则刻意保持简单：只跳过以 {@code //} 开头的整行注释，不解析字符串字面量。
 * 确有需要的行，在同一行加 {@code // hygiene-allow: 原因} 豁免。
 */
public final class SourceHygiene {
    public static final String ALLOW_MARKER = "hygiene-allow:";

    private static final Pattern FORBIDDEN = Pattern.compile("System\\.out\\.|System\\.err\\.|\\.printStackTrace\\(\\)");

    /** 一处违规：行号从 1 开始。 */
    public record Violation(int line, String text) {
    }

    private SourceHygiene() {
    }

    public static List<Violation> findViolations(String fileContent) {
        List<Violation> violations = new ArrayList<>();
        String[] lines = fileContent.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.strip();
            if (trimmed.startsWith("//") || line.contains(ALLOW_MARKER)) {
                continue;
            }
            if (FORBIDDEN.matcher(line).find()) {
                violations.add(new Violation(i + 1, trimmed));
            }
        }
        return violations;
    }
}
