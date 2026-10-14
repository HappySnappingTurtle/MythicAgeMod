package io.github.happysnappingturtle.mythicage.devtools;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 测试用：拼出只含常量池的最小 class 文件字节，不依赖 ASM。
 * 常量池之后的结构（访问标志、类、接口、字段、方法、属性）全部写空。
 */
final class ClassFileBuilder {
    private final ByteArrayOutputStream pool = new ByteArrayOutputStream();
    private final DataOutputStream poolOut = new DataOutputStream(pool);
    private int slots = 1;

    ClassFileBuilder utf8(String value) {
        return write(() -> {
            poolOut.writeByte(1);
            poolOut.writeUTF(value);
        }, 1);
    }

    ClassFileBuilder longConstant(long value) {
        return write(() -> {
            poolOut.writeByte(5);
            poolOut.writeLong(value);
        }, 2);
    }

    ClassFileBuilder doubleConstant(double value) {
        return write(() -> {
            poolOut.writeByte(6);
            poolOut.writeDouble(value);
        }, 2);
    }

    ClassFileBuilder classRef(int nameIndex) {
        return write(() -> {
            poolOut.writeByte(7);
            poolOut.writeShort(nameIndex);
        }, 1);
    }

    byte[] build() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(0xCAFEBABE);
            out.writeShort(0);
            out.writeShort(65);
            out.writeShort(slots);
            out.write(pool.toByteArray());
            for (int i = 0; i < 8; i++) {
                out.writeShort(0); // access, this, super, interfaces, fields, methods, attributes...
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ClassFileBuilder write(IoAction action, int slotCount) {
        try {
            action.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        slots += slotCount;
        return this;
    }

    private interface IoAction {
        void run() throws IOException;
    }
}
