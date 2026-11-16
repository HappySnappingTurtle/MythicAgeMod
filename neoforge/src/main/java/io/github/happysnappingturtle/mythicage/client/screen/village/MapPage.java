package io.github.happysnappingturtle.mythicage.client.screen.village;

import com.google.gson.JsonParser;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.map.MapPalette;
import io.github.happysnappingturtle.mythicage.core.village.map.MapView;
import io.github.happysnappingturtle.mythicage.core.village.map.VillageMapData;
import io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 地图分页（GA-12 §3.1，打开界面默认在这一页）：左边按村庄规划画的俯视图，右边详情栏。
 * 村界虚线、道路浅色粗线、建筑按类别着色并画类别符号、在建琥珀色虚线框写百分比、空地块点线框写种类、你自己是一个箭头；
 * 左下图例。拖动平移、滚轮缩放（每格 0.5–4 像素）、悬停看名字、点击选中。
 */
final class MapPage extends Page {
    private static final int ROAD = 0xFFCBB488;
    private static final int BORDER = 0xFFA08050;
    private static final int PLOT = 0xFF7A6A4A;
    private static final int SITE_FILL = 0xFFFFF3D6;
    private static final int HIGHLIGHT = 0xFFFFD84A;
    private static final Map<String, MapPalette> PALETTES = new HashMap<>();

    private MapView view;
    private UUID viewFor;
    private int mapX, mapY, mapW, mapH;
    private int detailX, detailW;
    private String selected = "";
    private Set<String> highlight = Set.of();
    private boolean dragging;
    private double dragDist;

    MapPage(VillageScreen s) {
        super(s);
    }

    /** 村民分页点了岗位行：在地图上高亮这些建筑。 */
    void highlight(List<String> keys) {
        highlight = Set.copyOf(keys);
    }

    @Override
    void layout(int x, int y, int w, int h) {
        super.layout(x, y, w, h);
        detailW = Math.max(118, w / 3);
        detailX = x + w - detailW - 6;
        mapX = x + 6;
        mapW = detailX - 6 - mapX;
        mapY = y + 6;
        mapH = h - 12 - legendHeight();
        if (view != null) {
            view = view.resized(mapX, mapY, mapW, mapH);
        }
    }

    private int legendHeight() {
        return legendRows() * 11 + 2;
    }

    private int legendRows() {
        int width = 0;
        int rows = 1;
        for (String key : legendKeys()) {
            int iw = 12 + font().width(Component.translatable("mythic_age.village.map.legend." + key)) + 6;
            if (width + iw > mapW && width > 0) {
                rows++;
                width = 0;
            }
            width += iw;
        }
        return rows;
    }

    private static List<String> legendKeys() {
        List<String> out = new ArrayList<>(MapPalette.REQUIRED);
        out.add("site");
        out.add("plot");
        out.add("you");
        return out;
    }

    static MapPalette palette(String civ) {
        return PALETTES.computeIfAbsent(civ, c -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "village_map/" + c + ".json");
            try {
                Optional<net.minecraft.server.packs.resources.Resource> r = Minecraft.getInstance().getResourceManager().getResource(id);
                if (r.isEmpty()) {
                    MythicAge.LOGGER.warn("缺少村庄地图配色表 {}，用默认色", id);
                    return MapPalette.EMPTY;
                }
                try (Reader in = r.get().openAsReader()) {
                    return MapPalette.parse(JsonParser.parseReader(in).getAsJsonObject(), msg -> MythicAge.LOGGER.warn("{}：{}", id, msg));
                }
            } catch (Exception e) {
                MythicAge.LOGGER.warn("村庄地图配色表 {} 读不了：{}", id, e.getMessage());
                return MapPalette.EMPTY;
            }
        });
    }

    private MapPalette palette() {
        return palette(s.header.civ());
    }

    private MapView view() {
        if ((view == null || !s.village().equals(viewFor)) && s.map != VillageMapData.EMPTY) {
            view = MapView.fit(mapX, mapY, mapW, mapH, s.map.extent());
            viewFor = s.village();
        }
        return view;
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.blitSprite(VillageScreen.INSET, mapX - 2, mapY - 2, mapW + 4, mapH + 4);
        MapView v = view();
        if (v == null) {
            Component wait = Component.translatable("mythic_age.village.map.loading");
            g.drawString(font(), wait, mapX + (mapW - font().width(wait)) / 2, mapY + mapH / 2 - 4, VillageScreen.INK_DIM, false);
        } else {
            g.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
            drawMap(g, v, mouseX, mouseY);
            g.disableScissor();
        }
        drawLegend(g);
        renderDetail(g, mouseX, mouseY);
    }

    // ───────────── 地图 ─────────────

    private void drawMap(GuiGraphics g, MapView v, int mouseX, int mouseY) {
        VillageMapData d = s.map;
        MapPalette pal = palette();
        // 道路
        for (VillageMapData.Road r : d.roads()) {
            double thick = Math.max(1, r.width() * v.scale());
            for (int i = 0; i + 1 < r.points().size(); i++) {
                VillageMapData.Point a = r.points().get(i);
                VillageMapData.Point b = r.points().get(i + 1);
                thickLine(g, v.screenX(a.x() + 0.5), v.screenY(a.z() + 0.5), v.screenX(b.x() + 0.5), v.screenY(b.z() + 0.5), thick, ROAD);
            }
        }
        // 村界虚线（先画，建筑压在上面）
        for (VillageMapData.Segment seg : d.border()) {
            dashedLine(g, v.screenX(seg.x1()), v.screenY(seg.z1()), v.screenX(seg.x2()), v.screenY(seg.z2()), 1, BORDER);
        }
        // 空地块：点线框 + 将来盖的那一类的淡色符号（不写字，和建筑一样靠符号；种类在悬停与详情里）
        for (VillageMapData.Plot p : d.plots()) {
            int[] r = rect(v, p.box());
            dotted(g, r, PLOT, 1);
            pal.plotCategory(p.kind()).map(pal::style).filter(st -> !st.symbol().isEmpty()).ifPresent(st -> {
                if (r[2] - r[0] >= 9 && r[3] - r[1] >= 9) {
                    int sw = font().width(st.symbol());
                    g.drawString(font(), st.symbol(), (r[0] + r[2] - sw) / 2 + 1, (r[1] + r[3]) / 2 - 3,
                            (st.border() & 0x00FFFFFF) | 0x70000000, false);
                }
            });
            if (selected.equals(plotKey(p))) {
                g.renderOutline(r[0] - 1, r[1] - 1, r[2] - r[0] + 2, r[3] - r[1] + 2, 0xFFFFFFFF);
            }
        }
        // 建筑：大的先画，祠压在卫城上面也看得见
        List<VillageMapData.Building> order = new ArrayList<>(d.buildings());
        order.sort((a, b) -> Long.compare(b.box().area(), a.box().area()));
        for (VillageMapData.Building b : order) {
            int[] r = rect(v, b.box());
            MapPalette.Style st = pal.styleOf(b.building());
            boolean newSite = b.underConstruction() && b.target().isEmpty();
            g.fill(r[0], r[1], r[2], r[3], newSite ? SITE_FILL : st.fill());
            if (b.underConstruction()) {
                dashed(g, r, VillageScreen.AMBER, v.scale() >= 2 ? 2 : 1);
                Component pct = Component.literal(b.progress() + "%");
                if (r[2] - r[0] >= font().width(pct) + 2 && r[3] - r[1] >= 9) {
                    g.drawString(font(), pct, (r[0] + r[2] - font().width(pct)) / 2, (r[1] + r[3]) / 2 - 4, 0xFF6B3F06, false);
                }
            } else {
                g.renderOutline(r[0], r[1], r[2] - r[0], r[3] - r[1], st.border());
                if (!st.symbol().isEmpty() && r[2] - r[0] >= 9 && r[3] - r[1] >= 9) {
                    int sw = font().width(st.symbol());
                    g.drawString(font(), st.symbol(), (r[0] + r[2] - sw) / 2 + 1, (r[1] + r[3]) / 2 - 3, st.border(), false);
                }
            }
            if (highlight.contains(b.key())) {
                g.renderOutline(r[0] - 2, r[1] - 2, r[2] - r[0] + 4, r[3] - r[1] + 4, HIGHLIGHT);
                g.renderOutline(r[0] - 1, r[1] - 1, r[2] - r[0] + 2, r[3] - r[1] + 2, HIGHLIGHT);
            }
            if (selected.equals(b.key())) {
                g.renderOutline(r[0] - 2, r[1] - 2, r[2] - r[0] + 4, r[3] - r[1] + 4, 0xFF000000);
                g.renderOutline(r[0] - 1, r[1] - 1, r[2] - r[0] + 2, r[3] - r[1] + 2, 0xFFFFFFFF);
            }
        }
        // 神庙祭坛
        d.temple().ifPresent(t -> {
            int tx = (int) v.screenX(t.x() + 0.5);
            int ty = (int) v.screenY(t.z() + 0.5);
            g.fill(tx - 2, ty - 1, tx + 3, ty + 2, 0xFFF2D27A);
            g.fill(tx - 1, ty - 2, tx + 2, ty + 3, 0xFFF2D27A);
            g.renderOutline(tx - 2, ty - 2, 5, 5, 0xFF6B3F06);
        });
        drawPlayer(g, v);
        // 悬停
        if (v.inside(mouseX, mouseY) && !dragging) {
            v.pick(d, mouseX, mouseY).ifPresent(f -> s.tooltip(tooltipOf(f)));
        }
    }

    private void drawPlayer(GuiGraphics g, MapView v) {
        Player p = Minecraft.getInstance().player;
        if (p == null) {
            return;
        }
        double px = v.screenX(p.getX());
        double py = v.screenY(p.getZ());
        if (!v.inside(px, py)) {
            return;
        }
        var pose = g.pose();
        pose.pushPose();
        pose.translate(px, py, 0);
        // 箭头尖朝上（北）；玩家朝南（yRot 0）时转 180°
        pose.mulPose(Axis.ZP.rotationDegrees(p.getYRot() + 180));
        arrow(g);
        pose.popPose();
    }

    /**
     * 「你」的箭头：白色三角、黑边、尾部开口（同原版地图的玩家标记），尖朝 −y，以原点为中心。
     * 原来三块方块叠成的箭头像一只指着的手（用户 11-15 看不出是什么）。
     */
    private static void arrow(GuiGraphics g) {
        // 每行半宽：尖在 y = −4，底在 y = 3；最后两行中间开口
        int[] half = {0, 1, 1, 2, 2, 3, 3, 3};
        int[] notch = {-1, -1, -1, -1, -1, -1, 0, 1};
        for (int pass = 0; pass < 2; pass++) {
            int grow = pass == 0 ? 1 : 0;
            int color = pass == 0 ? 0xFF1A140C : 0xFFFFFFFF;
            for (int i = 0; i < half.length; i++) {
                int y = i - 4;
                int hw = half[i] + grow;
                int n = notch[i] - grow;
                if (n < 0) {
                    g.fill(-hw, y, hw + 1, y + 1, color);
                } else {
                    g.fill(-hw, y, -n, y + 1, color);
                    g.fill(n + 1, y, hw + 1, y + 1, color);
                }
            }
            if (pass == 0) {
                // 黑边多出尖上一行与底下一行
                g.fill(0, -5, 1, -4, color);
                g.fill(-4, 4, -1, 5, color);
                g.fill(2, 4, 5, 5, color);
            }
        }
    }

    private List<Component> tooltipOf(VillageMapData.Feature f) {
        List<Component> out = new ArrayList<>();
        if (f instanceof VillageMapData.Building b) {
            MutableComponent name = VillageScreen.buildingName(b.building(), b.god()).copy();
            if (b.tier() > 0) {
                name.append(" · ").append(Component.translatable("mythic_age.village.map.tier", b.tier()));
            }
            out.add(name);
            if (b.underConstruction()) {
                out.add(b.target().isEmpty()
                        ? Component.translatable("mythic_age.village.map.building_progress", b.progress())
                        : Component.translatable("mythic_age.village.map.upgrading", VillageScreen.buildingName(b.target(), ""), b.progress()));
            }
            if (!b.building().isEmpty()) {
                out.add(Component.translatable("mythic_age.village.map.legend." + palette().categoryOf(b.building()))
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        } else if (f instanceof VillageMapData.Plot p) {
            out.add(Component.translatable("mythic_age.village.map.plot",
                    Component.translatable("mythic_age.village.plot." + p.kind())));
        }
        return out;
    }

    private static String plotKey(VillageMapData.Plot p) {
        return "plot:" + p.id();
    }

    private static int[] rect(MapView v, VillageMapData.Rect r) {
        int x1 = (int) Math.floor(v.screenX(r.minX()));
        int y1 = (int) Math.floor(v.screenY(r.minZ()));
        int x2 = Math.max(x1 + 1, (int) Math.floor(v.screenX(r.maxX() + 1)));
        int y2 = Math.max(y1 + 1, (int) Math.floor(v.screenY(r.maxZ() + 1)));
        return new int[]{x1, y1, x2, y2};
    }

    private static void thickLine(GuiGraphics g, double x1, double y1, double x2, double y2, double thick, int color) {
        double len = Math.hypot(x2 - x1, y2 - y1);
        int steps = Math.max(1, (int) Math.ceil(len / Math.max(0.75, thick / 2)));
        int t = (int) Math.max(1, Math.round(thick));
        for (int i = 0; i <= steps; i++) {
            double f = (double) i / steps;
            int cx = (int) Math.round(x1 + (x2 - x1) * f - t / 2.0);
            int cy = (int) Math.round(y1 + (y2 - y1) * f - t / 2.0);
            g.fill(cx, cy, cx + t, cy + t, color);
        }
    }

    /** 轴向虚线：3 像素实、2 像素空。 */
    private static void dashedLine(GuiGraphics g, double x1, double y1, double x2, double y2, int t, int color) {
        int ax = (int) Math.round(Math.min(x1, x2));
        int bx = (int) Math.round(Math.max(x1, x2));
        int ay = (int) Math.round(Math.min(y1, y2));
        int by = (int) Math.round(Math.max(y1, y2));
        if (by - ay <= bx - ax) {
            for (int x = ax; x < bx; x += 5) {
                g.fill(x, ay, Math.min(bx, x + 3), ay + t, color);
            }
        } else {
            for (int y = ay; y < by; y += 5) {
                g.fill(ax, y, ax + t, Math.min(by, y + 3), color);
            }
        }
    }

    private static void dashed(GuiGraphics g, int[] r, int color, int t) {
        dashedLine(g, r[0], r[1], r[2], r[1], t, color);
        dashedLine(g, r[0], r[3] - t, r[2], r[3] - t, t, color);
        dashedLine(g, r[0], r[1], r[0], r[3], t, color);
        dashedLine(g, r[2] - t, r[1], r[2] - t, r[3], t, color);
    }

    /** 点线框：每隔一格一个点。 */
    private static void dotted(GuiGraphics g, int[] r, int color, int t) {
        for (int x = r[0]; x < r[2]; x += 3) {
            g.fill(x, r[1], x + t, r[1] + t, color);
            g.fill(x, r[3] - t, x + t, r[3], color);
        }
        for (int y = r[1]; y < r[3]; y += 3) {
            g.fill(r[0], y, r[0] + t, y + t, color);
            g.fill(r[2] - t, y, r[2], y + t, color);
        }
    }

    private void drawLegend(GuiGraphics g) {
        MapPalette pal = palette();
        int lx = mapX;
        int ly = mapY + mapH + 4;
        for (String key : legendKeys()) {
            Component name = Component.translatable("mythic_age.village.map.legend." + key);
            int iw = 12 + font().width(name) + 6;
            if (lx + iw > mapX + mapW && lx > mapX) {
                lx = mapX;
                ly += 11;
            }
            switch (key) {
                case "site" -> {
                    g.fill(lx, ly, lx + 9, ly + 8, SITE_FILL);
                    dashed(g, new int[]{lx, ly, lx + 9, ly + 8}, VillageScreen.AMBER, 1);
                }
                case "plot" -> dotted(g, new int[]{lx, ly, lx + 9, ly + 8}, PLOT, 1);
                case "you" -> {
                    g.pose().pushPose();
                    g.pose().translate(lx + 4, ly + 4, 0);
                    g.pose().scale(0.8f, 0.8f, 1f);
                    arrow(g);
                    g.pose().popPose();
                }
                default -> {
                    MapPalette.Style st = pal.style(key);
                    g.fill(lx, ly, lx + 9, ly + 8, st.fill());
                    g.renderOutline(lx, ly, 9, 8, st.border());
                }
            }
            g.drawString(font(), name, lx + 12, ly, VillageScreen.INK, false);
            lx += iw;
        }
    }

    // ───────────── 详情栏 ─────────────

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY) {
        int dx = detailX;
        int dy = y + 6;
        int dw = detailW;
        int bottom = y + h - 6;
        g.blitSprite(VillageScreen.INSET, dx - 2, dy - 2, dw + 4, bottom - dy + 4);
        int tx = dx + 4;
        int tw = dw - 8;
        int ty = dy + 2;
        Optional<VillageMapData.Feature> sel = selectedFeature();
        if (sel.isEmpty()) {
            ty = projectSummary(g, tx, ty, tw, mouseX, mouseY);
            ty += 6;
            text(g, Component.translatable("mythic_age.village.map.hint"), tx, ty, tw, VillageScreen.INK_DIM);
            Component bounds = Component.translatable("mythic_age.village.overview.bounds");
            s.button(g, tx, bottom - 18, bounds, true, List.of(Component.translatable("mythic_age.village.map.bounds_tip")), mouseX,
                    mouseY, y, y + h, () -> {
                        s.send(new AffairsPayloads.BoundsRequest(s.village()));
                        s.onClose();
                    });
            return;
        }
        VillageMapData.Feature f = sel.get();
        if (f instanceof VillageMapData.Building b) {
            MutableComponent name = VillageScreen.buildingName(b.building(), b.god()).copy();
            ty = text(g, name, tx, ty, tw, VillageScreen.INK_HEAD) + 2;
        } else if (f instanceof VillageMapData.Plot p) {
            ty = text(g, Component.translatable("mythic_age.village.map.plot",
                    Component.translatable("mythic_age.village.plot." + p.kind())), tx, ty, tw, VillageScreen.INK_HEAD) + 2;
        }
        VillageScreenPayloads.Detail d = s.detail;
        if (d == null || !d.key().equals(selected)) {
            text(g, Component.translatable("mythic_age.village.map.loading"), tx, ty, tw, VillageScreen.INK_DIM);
            return;
        }
        for (var l : d.lines()) {
            ty = text(g, VillageScreen.line(l), tx, ty, tw, VillageScreen.INK);
        }
        if (!d.options().isEmpty()) {
            ty = text(g, Component.translatable("mythic_age.village.map.options"), tx, ty + 2, tw, VillageScreen.INK_HEAD);
            for (var l : d.options()) {
                ty = text(g, Component.literal("· ").append(VillageScreen.line(l)), tx, ty, tw, VillageScreen.INK);
            }
        } else if (f instanceof VillageMapData.Plot) {
            ty = text(g, Component.translatable("mythic_age.village.map.no_options"), tx, ty + 2, tw, VillageScreen.INK_DIM);
        }
        if (f instanceof VillageMapData.Building b && b.underConstruction()) {
            ty = projectSummary(g, tx, ty + 4, tw, mouseX, mouseY);
        }
        if (!d.upgradeKey().isEmpty()) {
            ProjectPayloads.View pv = s.projects;
            int have = pv == null ? 0 : pv.contribution();
            int need = pv == null ? Integer.MAX_VALUE : pv.threshold();
            boolean already = pv != null && pv.nextKey().equals(d.upgradeKey());
            boolean ok = d.upgradeReason().isEmpty() && have >= need && !already;
            List<Component> tip = new ArrayList<>();
            if (!d.upgradeReason().isEmpty()) {
                tip.add(Component.translatable("mythic_age.village.project.reason." + d.upgradeReason()));
            } else if (have < need) {
                tip.add(Component.translatable("mythic_age.village.project.nominate.low", have, need, need - have));
            } else if (already) {
                tip.add(Component.translatable("mythic_age.village.build.nominated"));
            }
            s.button(g, tx, Math.min(ty + 4, bottom - 18), Component.translatable("mythic_age.village.map.nominate_upgrade"), ok, tip,
                    mouseX, mouseY, y, y + h, () -> s.send(new ProjectPayloads.Nominate(s.village(), d.upgradeKey())));
            if (!ok && !tip.isEmpty()) {
                text(g, tip.get(0), tx, Math.min(ty + 20, bottom - 10), tw, VillageScreen.INK_DIM);
            }
        }
    }

    /** 当前项目摘要（没选中时、选中在建项目时）：名字、进度条、工人数、还缺什么、下一个。 */
    private int projectSummary(GuiGraphics g, int tx, int ty, int tw, int mouseX, int mouseY) {
        VillageScreenPayloads.Header hd = s.header;
        ProjectPayloads.View pv = s.projects;
        ty = text(g, Component.translatable("mythic_age.village.build.current"), tx, ty, tw, VillageScreen.INK_HEAD);
        if (hd.progress() < 0) {
            ty = text(g, Component.translatable("mythic_age.village.overview.no_project"), tx, ty, tw, VillageScreen.INK_DIM);
        } else {
            ty = text(g, VillageScreen.line(hd.project()), tx, ty, tw, VillageScreen.INK);
            VillageScreen.bar(g, tx, ty + 1, tw, 6, hd.progress() / 100.0, VillageScreen.AMBER);
            ty += 10;
            if (pv != null) {
                ty = text(g, Component.translatable("mythic_age.village.build.progress_line", hd.progress(), pv.workers()), tx, ty, tw,
                        VillageScreen.INK_DIM);
                if (!pv.missing().isEmpty()) {
                    ty = text(g, Component.translatable("mythic_age.village.build.missing"), tx, ty, tw, VillageScreen.INK_WARN);
                    int n = s.items(g, pv.missing(), tx, ty, tw, mouseX, mouseY, y, y + h);
                    ty += 18;
                    if (n < pv.missing().size()) {
                        ty = text(g, Component.translatable("mythic_age.village.build.more", pv.missing().size() - n), tx, ty, tw,
                                VillageScreen.INK_DIM);
                    }
                }
            }
        }
        if (pv != null) {
            ty += 2;
            Component next = pv.nextKey().isEmpty() ? Component.translatable("mythic_age.village.build.next_none")
                    : Component.translatable("mythic_age.village.build.next", nextName(pv), pv.nextPlayer());
            ty = text(g, next, tx, ty, tw, VillageScreen.INK);
        }
        ty += 2;
        s.button(g, tx, ty, Component.translatable("mythic_age.village.map.change_next"), true, List.of(), mouseX, mouseY, y, y + h,
                () -> s.switchTab(VillageScreenPayloads.TAB_BUILD));
        return ty + 16;
    }

    static Component nextName(ProjectPayloads.View pv) {
        return pv.entries().stream().filter(e -> e.key().equals(pv.nextKey())).findFirst()
                .map(e -> VillageScreen.line(e.name())).orElse(Component.literal(pv.nextKey()));
    }

    private Optional<VillageMapData.Feature> selectedFeature() {
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        if (selected.startsWith("plot:")) {
            String id = selected.substring(5);
            return s.map.plots().stream().filter(p -> p.id().equals(id)).map(p -> (VillageMapData.Feature) p).findFirst();
        }
        return s.map.building(selected).map(b -> b);
    }

    // ───────────── 输入 ─────────────

    @Override
    boolean mouseClicked(double mx, double my, int button) {
        MapView v = view();
        if (v != null && v.inside(mx, my) && button == 0) {
            dragging = true;
            dragDist = 0;
            return true;
        }
        return false;
    }

    @Override
    boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && view != null) {
            view.pan(dx, dy);
            dragDist += Math.abs(dx) + Math.abs(dy);
            return true;
        }
        return false;
    }

    @Override
    boolean mouseReleased(double mx, double my, int button) {
        if (!dragging) {
            return false;
        }
        dragging = false;
        if (dragDist < 3 && view != null) {
            Optional<VillageMapData.Feature> f = view.pick(s.map, mx, my);
            String key = f.map(x -> x instanceof VillageMapData.Building b ? b.key() : plotKey((VillageMapData.Plot) x)).orElse("");
            if (!key.equals(selected)) {
                selected = key;
                highlight = Set.of();
                if (!key.isEmpty()) {
                    s.click();
                    s.send(new VillageScreenPayloads.DetailRequest(s.village(), key));
                }
            }
        }
        return true;
    }

    @Override
    boolean mouseScrolled(double mx, double my, double dy) {
        if (view != null && view.inside(mx, my)) {
            view.zoom(Math.signum(dy), mx, my);
            return true;
        }
        return false;
    }
}
