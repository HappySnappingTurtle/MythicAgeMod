package io.github.happysnappingturtle.mythicage.devtools;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * 扫描 class 文件的常量池，找出对被禁止包或类的引用。
 * <p>
 * 类名、字段与方法的类型描述符、签名都以 CONSTANT_Utf8 的形式存放在常量池中，
 * 因此只要检查全部 Utf8 项，就能发现所有类型层面的依赖。
 */
public final class ForbiddenReferenceScanner {
    private static final int MAGIC = 0xCAFEBABE;

    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACE_METHODREF = 11;
    private static final int CONSTANT_NAME_AND_TYPE = 12;
    private static final int CONSTANT_METHOD_HANDLE = 15;
    private static final int CONSTANT_METHOD_TYPE = 16;
    private static final int CONSTANT_DYNAMIC = 17;
    private static final int CONSTANT_INVOKE_DYNAMIC = 18;
    private static final int CONSTANT_MODULE = 19;
    private static final int CONSTANT_PACKAGE = 20;

    private ForbiddenReferenceScanner() {
    }

    /**
     * 按包前缀查找引用。
     *
     * @param forbiddenPrefixes 内部名前缀，必须以 {@code /} 结尾，例如 {@code net/minecraft/}
     * @return 命中的常量池字符串，去重并排序
     * @throws IllegalArgumentException 不是合法 class 文件，或前缀不以 {@code /} 结尾
     */
    public static List<String> findForbiddenReferences(byte[] classFile, List<String> forbiddenPrefixes) {
        for (String prefix : forbiddenPrefixes) {
            if (!prefix.endsWith("/")) {
                throw new IllegalArgumentException("前缀必须以 / 结尾，才能按完整包路径匹配: " + prefix);
            }
        }
        TreeSet<String> hits = new TreeSet<>();
        for (String value : utf8Constants(classFile)) {
            for (String prefix : forbiddenPrefixes) {
                // 内部名直接以前缀开头，或在类型描述符 / 签名中以 L<prefix> 出现
                if (value.startsWith(prefix) || value.contains("L" + prefix)) {
                    hits.add(value);
                }
            }
        }
        return List.copyOf(hits);
    }

    /**
     * 按<b>具体类</b>查找引用（整类名匹配，例如 {@code java/lang/Runtime}）。
     * 与前缀匹配不同，{@code java/lang/RuntimeException} 不会被当成 {@code java/lang/Runtime}。
     *
     * @param classNames 内部类名，不以 {@code /} 结尾
     */
    public static List<String> findForbiddenClasses(byte[] classFile, List<String> classNames) {
        TreeSet<String> hits = new TreeSet<>();
        for (String value : utf8Constants(classFile)) {
            for (String name : classNames) {
                if (value.equals(name) || value.contains("L" + name + ";")) {
                    hits.add(value);
                }
            }
        }
        return List.copyOf(hits);
    }

    /**
     * 读出常量池中的全部 CONSTANT_Utf8 字符串。
     *
     * @throws IllegalArgumentException 不是合法 class 文件或被截断
     */
    public static List<String> utf8Constants(byte[] classFile) {
        List<String> values = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classFile))) {
            if (in.readInt() != MAGIC) {
                throw new IllegalArgumentException("不是 class 文件：魔数不符");
            }
            in.readUnsignedShort(); // minor_version
            in.readUnsignedShort(); // major_version
            int count = in.readUnsignedShort();
            for (int index = 1; index < count; index++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case CONSTANT_UTF8 -> values.add(in.readUTF());
                    case CONSTANT_INTEGER, CONSTANT_FLOAT -> in.skipNBytes(4);
                    case CONSTANT_LONG, CONSTANT_DOUBLE -> {
                        in.skipNBytes(8);
                        index++; // Long 与 Double 占两个常量池槽
                    }
                    case CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_METHOD_TYPE, CONSTANT_MODULE, CONSTANT_PACKAGE ->
                            in.skipNBytes(2);
                    case CONSTANT_FIELDREF, CONSTANT_METHODREF, CONSTANT_INTERFACE_METHODREF, CONSTANT_NAME_AND_TYPE,
                         CONSTANT_DYNAMIC, CONSTANT_INVOKE_DYNAMIC -> in.skipNBytes(4);
                    case CONSTANT_METHOD_HANDLE -> in.skipNBytes(3);
                    default -> throw new IllegalArgumentException("未知的常量池标签 " + tag + "（位置 " + index + "）");
                }
            }
        } catch (EOFException e) {
            throw new IllegalArgumentException("class 文件被截断", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return values;
    }
}
