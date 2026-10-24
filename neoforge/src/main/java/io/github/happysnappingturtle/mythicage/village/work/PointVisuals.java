package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;

import java.util.HashMap;
import java.util.Map;

/**
 * 采集点的分档外观（T19 设计文档 §6.2）：余量跨档时换方块（如采石点露头逐档换成圆石、砂砾）。
 * 各文明按采集点种类登记；没登记的种类外观不变。
 */
public final class PointVisuals {
    public interface Updater {
        void apply(VillageContext ctx, ResourcePoint point, ResourcePoint.Tier tier);
    }

    private static final Map<String, Updater> BY_KIND = new HashMap<>();

    private PointVisuals() {
    }

    public static void register(String kind, Updater u) {
        BY_KIND.put(kind, u);
    }

    static void update(VillageContext ctx, ResourcePoint p) {
        Updater u = BY_KIND.get(p.kind());
        if (u != null && ctx.loaded(VillageContext.block(p.pos()))) {
            u.apply(ctx, p, p.tier());
        }
    }
}
