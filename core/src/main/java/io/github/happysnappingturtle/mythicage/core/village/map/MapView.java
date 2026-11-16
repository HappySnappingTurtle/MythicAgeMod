package io.github.happysnappingturtle.mythicage.core.village.map;

import java.util.Optional;

/**
 * 地图控件的视口（GA-12 §3.1）：屏幕区域 + 视口中心（方块坐标，可带小数）+ 缩放（每格几个像素，{@value #MIN_SCALE}–{@value #MAX_SCALE}）。
 * 拖动平移、滚轮以鼠标所在的方块为不动点缩放、屏幕坐标与方块坐标互换、点选。界面类只管画，换算都在这里，方便单测。
 * <p>
 * 方块 x 占屏幕上 [screenX(x), screenX(x + 1))；屏幕 y 向下对应方块 z 增大（北在上）。
 */
public final class MapView {
    public static final double MIN_SCALE = 0.5;
    public static final double MAX_SCALE = 4;
    /** 滚轮一格的缩放倍数。 */
    public static final double STEP = 1.25;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private double centerX;
    private double centerZ;
    private double scale;

    public MapView(int left, int top, int width, int height, double centerX, double centerZ, double scale) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.scale = clamp(scale);
    }

    /** 让 r 整个放进视口（留一圈 4 像素），缩放限制在范围内。 */
    public static MapView fit(int left, int top, int width, int height, VillageMapData.Rect r) {
        double w = r.maxX() - r.minX() + 1;
        double h = r.maxZ() - r.minZ() + 1;
        double s = Math.min((width - 8) / w, (height - 8) / h);
        return new MapView(left, top, width, height, r.minX() + w / 2, r.minZ() + h / 2, s);
    }

    /** 换了屏幕区域（窗口缩放）时保留中心与缩放。 */
    public MapView resized(int left, int top, int width, int height) {
        return new MapView(left, top, width, height, centerX, centerZ, scale);
    }

    private static double clamp(double s) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public double scale() {
        return scale;
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public boolean inside(double sx, double sy) {
        return sx >= left && sx < left + width && sy >= top && sy < top + height;
    }

    public double screenX(double blockX) {
        return left + width / 2.0 + (blockX - centerX) * scale;
    }

    public double screenY(double blockZ) {
        return top + height / 2.0 + (blockZ - centerZ) * scale;
    }

    public double blockX(double sx) {
        return centerX + (sx - left - width / 2.0) / scale;
    }

    public double blockZ(double sy) {
        return centerZ + (sy - top - height / 2.0) / scale;
    }

    /** 拖动：内容跟着鼠标走 dx、dy 像素。 */
    public void pan(double dx, double dy) {
        centerX -= dx / scale;
        centerZ -= dy / scale;
    }

    /** 以屏幕点 (sx, sy) 下的方块为不动点缩放；steps &gt; 0 放大。 */
    public void zoom(double steps, double sx, double sy) {
        double bx = blockX(sx);
        double bz = blockZ(sy);
        scale = clamp(scale * Math.pow(STEP, steps));
        centerX = bx - (sx - left - width / 2.0) / scale;
        centerZ = bz - (sy - top - height / 2.0) / scale;
    }

    /** 屏幕点下的地图内容：先找建筑（取包围盒最小的，祠压在卫城范围里时点到祠），没有再找空地块。 */
    public Optional<VillageMapData.Feature> pick(VillageMapData data, double sx, double sy) {
        if (!inside(sx, sy)) {
            return Optional.empty();
        }
        int x = (int) Math.floor(blockX(sx));
        int z = (int) Math.floor(blockZ(sy));
        VillageMapData.Feature best = null;
        for (VillageMapData.Building b : data.buildings()) {
            if (b.box().contains(x, z) && (best == null || b.box().area() < best.box().area())) {
                best = b;
            }
        }
        if (best == null) {
            for (VillageMapData.Plot p : data.plots()) {
                if (p.box().contains(x, z) && (best == null || p.box().area() < best.box().area())) {
                    best = p;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
