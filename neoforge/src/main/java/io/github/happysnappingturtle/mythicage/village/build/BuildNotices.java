package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import io.github.happysnappingturtle.mythicage.village.VillageZones;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * 村庄升级提示（12 号 §4.3-B，T19 设计文档 §8.6、§1.4 E7；T24 §4.5 改为 toast）：落成时村界内或钟 64 格内的玩家看到
 * toast「宙斯之村扩建 · 大仓库：仓储 +1024」；只写建筑、仓储、床位、工作站，不出现恩宠或时代字样。
 * 通告存起来，{@code seenBy}（捐过东西的玩家）里当时不在场的，下次进村补发（同样是 toast，可能有好几条）。
 */
public final class BuildNotices {
    private BuildNotices() {
    }

    static void built(VillageContext ctx, BuildingSpec spec, String god) {
        VillageEconomy.Notice n = new VillageEconomy.Notice(ctx.today(), spec.id(), god, new LinkedHashSet<>());
        for (ServerPlayer p : io.github.happysnappingturtle.mythicage.player.OnlinePlayers.in(ctx.level())) {
            if (near(ctx, p)) {
                toast(p, ctx.village(), spec, god);
                n.sent().add(p.getUUID());
            }
        }
        List<VillageEconomy.Notice> list = ctx.state().notices;
        list.add(n);
        while (list.size() > Construction.NOTICE_LIMIT) {
            list.remove(0);
        }
    }

    static boolean near(VillageContext ctx, ServerPlayer p) {
        if (p.isSpectator()) {
            return false;
        }
        if (p.blockPosition().closerThan(ctx.center(), Construction.NOTICE_RANGE)) {
            return true;
        }
        VillageZones.Hit hit = VillageZones.zoneAt(ctx.level(), p.blockPosition());
        return hit.builtKnown() && hit.village().map(v -> v.id().equals(ctx.village().id())).orElse(false);
    }

    static void blocked(VillageContext ctx, BuildingSpec spec, BuildProject p, BlockPos at,
                        net.minecraft.world.level.block.state.BlockState what) {
        Construction.notifyPlayers(ctx.level(), ctx.center(), Component.translatable("mythic_age.village.project.blocked",
                Construction.name(spec, p.god), at.getX(), at.getY(), at.getZ(), what.getBlock().getName()));
    }

    /** 玩家进村（{@link VillageService} 的进村标题处调用）：补发没看过的落成通告。返回补发条数。 */
    public static int onEnter(ServerPlayer player, VillageRegistry.Village village) {
        VillageEconomy eco = VillageEconomy.get(player.server);
        return eco.peek(village.id()).map(s -> {
            if (!s.seenBy.contains(player.getUUID())) {
                return 0;
            }
            int n = 0;
            for (VillageEconomy.Notice notice : s.notices) {
                BuildingSpec spec = VillageBuildings.all().get(notice.building());
                if (spec == null || !notice.sent().add(player.getUUID())) {
                    continue;
                }
                toast(player, village, spec, notice.god());
                n++;
            }
            if (n > 0) {
                eco.setDirty();
            }
            return n;
        }).orElse(0);
    }

    /** 发一条落成 toast：标题「宙斯之村扩建」，正文「大仓库：仓储 +1024、床位 +4」。 */
    public static void toast(ServerPlayer player, VillageRegistry.Village village, BuildingSpec spec, String god) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new AffairsPayloads.Toast(icon(spec),
                Component.translatable("mythic_age.village.toast.built", VillageService.title(village)), body(spec, god)));
    }

    /** toast 图标：有仓储的用箱子，有床位的用床，有工作站的用工作台，其余用砖。 */
    static String icon(BuildingSpec spec) {
        if (spec.provides().getOrDefault("storage", 0) > 0) {
            return "minecraft:chest";
        }
        if (spec.provides().getOrDefault("beds", 0) > 0) {
            return "minecraft:red_bed";
        }
        return spec.anchors("workstation").isEmpty() ? "minecraft:bricks" : "minecraft:crafting_table";
    }

    /** 「大仓库：仓储 +1024、床位 +4」或只有名字。 */
    public static Component body(BuildingSpec spec, String god) {
        Component details = details(spec);
        Component name = Construction.name(spec, god);
        return details == null ? name : Component.translatable("mythic_age.village.toast.built_body", name, details);
    }

    /** 「仓储 +1024、床位 +4」；什么都不提供时为 null。 */
    @javax.annotation.Nullable
    static Component details(BuildingSpec spec) {
        List<Component> parts = new ArrayList<>();
        int storage = spec.provides().getOrDefault("storage", 0);
        if (storage > 0) {
            int unit = VillageData.civ(spec.civ()).map(CivVillageData::storageUnit).orElse(0);
            parts.add(Component.translatable("mythic_age.village.built.storage", storage * unit));
        }
        int beds = spec.provides().getOrDefault("beds", 0);
        if (beds > 0) {
            parts.add(Component.translatable("mythic_age.village.built.beds", beds));
        }
        int stations = spec.anchors("workstation").size();
        if (stations > 0) {
            parts.add(Component.translatable("mythic_age.village.built.stations", stations));
        }
        if (parts.isEmpty()) {
            return null;
        }
        MutableComponent details = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                details.append(Component.translatable("mythic_age.village.built.sep"));
            }
            details.append(parts.get(i));
        }
        return details;
    }

    /** 「宙斯之村建成了大仓库（仓储 +1024、床位 +4）」（聊天栏用，如管理命令）。 */
    public static Component message(VillageRegistry.Village village, BuildingSpec spec, String god) {
        Component details = details(spec);
        Component name = Construction.name(spec, god);
        if (details == null) {
            return Component.translatable("mythic_age.village.built.plain", VillageService.title(village), name);
        }
        return Component.translatable("mythic_age.village.built", VillageService.title(village), name, details);
    }

    /** 只供 GameTest：某条通告有没有发给这名玩家。 */
    public static boolean sentForTest(VillageEconomy.State s, String building, UUID player) {
        return s.notices.stream().anyMatch(n -> n.building().equals(building) && n.sent().contains(player));
    }
}
