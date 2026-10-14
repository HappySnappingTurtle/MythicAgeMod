package io.github.happysnappingturtle.mythicage.core.config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件读写。只在文件不存在时写出默认文件，从不改写已有文件
 * （服主写的未知字段和自定义键因此会被保留）。
 */
public final class ConfigFiles {
    /** 读取结果：text 与 error 二者有且只有一个非空。 */
    public record ReadResult(String text, ConfigIssue error) {
    }

    private ConfigFiles() {
    }

    /** 严格按 UTF-8 读取；不是合法 UTF-8 或无法读取时返回错误。 */
    public static ReadResult read(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return new ReadResult(text, null);
        } catch (CharacterCodingException e) {
            return new ReadResult(null, ConfigIssue.error("", "文件不是合法的 UTF-8：" + file));
        } catch (IOException e) {
            return new ReadResult(null, ConfigIssue.error("", "无法读取文件 " + file + "：" + e));
        }
    }

    /**
     * 首次加载：文件不存在时写出默认文件并返回默认值；存在时按 schema 读取。
     * 永远返回可用的值，不因配置问题抛出异常。
     */
    public static <T> ConfigLoadResult<T> loadOrCreate(Path file, ConfigSchema<T> schema) {
        if (!Files.exists(file)) {
            List<ConfigIssue> issues = new ArrayList<>();
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, ConfigLoader.defaultFileContent(schema), StandardCharsets.UTF_8);
            } catch (IOException e) {
                issues.add(ConfigIssue.warn("", "无法写出默认文件 " + file + "：" + e + "；本次使用默认值"));
            }
            return new ConfigLoadResult<>(schema.defaults(), issues, false);
        }
        ReadResult read = read(file);
        if (read.text() == null) {
            return new ConfigLoadResult<>(schema.defaults(), List.of(read.error()), true);
        }
        return ConfigLoader.load(schema, read.text(), null);
    }
}
