package io.github.happysnappingturtle.mythicage.core.village.gen;

/** 调试用：把规划画成字符平面图（每格 2 方块）。 */
final class PlanAscii {
    private PlanAscii() {
    }

    static String render(VillagePlan p, int cx, int cz, int r) {
        int n = r; // 2r 方块 / 2 方块每格
        char[][] g = new char[n][n];
        for (char[] row : g) {
            java.util.Arrays.fill(row, '.');
        }
        for (VillagePlan.Plot pl : p.plots()) {
            fill(g, pl.box(), cx, cz, r, Character.toLowerCase(pl.kind().charAt(0)));
        }
        for (VillagePlan.Placement b : p.buildings()) {
            String id = b.building().substring(b.building().indexOf(':') + 1);
            fill(g, b.box(), cx, cz, r, Character.toUpperCase(id.charAt(0)));
        }
        for (VillagePlan.Path path : p.paths()) {
            for (int i = 0; i + 1 < path.points().size(); i++) {
                Pos a = path.points().get(i);
                Pos b = path.points().get(i + 1);
                fill(g, Box.of(new Pos(a.x(), 0, a.z()), new Pos(b.x(), 0, b.z())), cx, cz, r, '#');
            }
        }
        for (Pos e : p.anchors("entrance")) {
            fill(g, new Box(e.x(), 0, e.z(), e.x(), 0, e.z()), cx, cz, r, '@');
        }
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                sb.append(g[i][j]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static void fill(char[][] g, Box b, int cx, int cz, int r, char c) {
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                int i = (x - cx + r) / 2;
                int j = (z - cz + r) / 2;
                if (i >= 0 && j >= 0 && i < g.length && j < g.length) {
                    g[i][j] = c;
                }
            }
        }
    }
}
