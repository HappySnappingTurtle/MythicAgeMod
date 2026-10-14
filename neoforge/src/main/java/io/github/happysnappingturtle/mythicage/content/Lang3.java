package io.github.happysnappingturtle.mythicage.content;

import java.util.Objects;

/** 一个语言键的三语文本：简体中文、英文、日文（T6 设计文档 §2.4）。 */
public record Lang3(String zh, String en, String ja) {
    public Lang3 {
        Objects.requireNonNull(zh);
        Objects.requireNonNull(en);
        Objects.requireNonNull(ja);
    }

    public String get(String locale) {
        return switch (locale) {
            case "zh_cn" -> zh;
            case "en_us" -> en;
            case "ja_jp" -> ja;
            default -> throw new IllegalArgumentException("不支持的语言：" + locale);
        };
    }
}
