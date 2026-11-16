package io.github.happysnappingturtle.mythicage.core.village.map;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 村庄地图的紧凑二进制编码（GA-12 §4）：变长整数、之字形有符号整数、UTF-8 字符串。
 * 与网络包里写的字节完全相同，所以 core 里量出来的大小就是实际发出去的大小。
 */
final class MapBytes {
    private MapBytes() {
    }

    static final class Writer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        void varInt(int v) {
            while ((v & ~0x7F) != 0) {
                out.write((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            out.write(v);
        }

        /** 之字形编码的有符号整数：−1 → 1，1 → 2，小的负数也只占一个字节。 */
        void sInt(int v) {
            varInt((v << 1) ^ (v >> 31));
        }

        void bool(boolean b) {
            out.write(b ? 1 : 0);
        }

        void str(String s) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            varInt(b.length);
            out.write(b, 0, b.length);
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    static final class Reader {
        private final byte[] in;
        private int at;

        Reader(byte[] in) {
            this.in = in;
        }

        int varInt() {
            int v = 0;
            int shift = 0;
            while (true) {
                if (at >= in.length) {
                    throw new IllegalArgumentException("村庄地图数据截断");
                }
                int b = in[at++] & 0xFF;
                v |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return v;
                }
                shift += 7;
                if (shift > 28) {
                    throw new IllegalArgumentException("变长整数过长");
                }
            }
        }

        int sInt() {
            int v = varInt();
            return (v >>> 1) ^ -(v & 1);
        }

        boolean bool() {
            if (at >= in.length) {
                throw new IllegalArgumentException("村庄地图数据截断");
            }
            return in[at++] != 0;
        }

        String str() {
            int n = varInt();
            if (n < 0 || at + n > in.length) {
                throw new IllegalArgumentException("字符串长度越界");
            }
            String s = new String(in, at, n, StandardCharsets.UTF_8);
            at += n;
            return s;
        }
    }
}
