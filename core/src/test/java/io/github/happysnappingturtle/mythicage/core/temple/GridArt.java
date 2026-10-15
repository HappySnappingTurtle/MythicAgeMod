package io.github.happysnappingturtle.mythicage.core.temple;

/** 用字符画描述位图：layers[y][z] 是一行字符，第 x 个字符为 '#' 时置位。 */
final class GridArt {
    private GridArt() {
    }

    static Grid3 of(String[]... layers) {
        int sy = layers.length;
        int sz = layers[0].length;
        int sx = layers[0][0].length();
        Grid3 g = new Grid3(sx, sy, sz);
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                String row = layers[y][z];
                for (int x = 0; x < sx; x++) {
                    if (row.charAt(x) == '#') {
                        g.set(x, y, z, true);
                    }
                }
            }
        }
        return g;
    }

    /** 竖直方向重复同一层 n 次。 */
    static String[][] repeat(String[] layer, int n) {
        String[][] out = new String[n][];
        for (int i = 0; i < n; i++) {
            out[i] = layer;
        }
        return out;
    }
}
