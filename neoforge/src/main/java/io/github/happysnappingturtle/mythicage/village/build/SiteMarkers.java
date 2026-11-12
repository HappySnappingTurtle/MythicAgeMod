package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.power.TempBlocks;
import io.github.happysnappingturtle.mythicage.village.work.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class SiteMarkers {
    private SiteMarkers() {}
    /** 工地临时方块（施工牌）的所有者：每村每项目唯一，读档后也能从 TempBlocks 认回来。 */
    public static UUID owner(VillageContext ctx, BuildProject p) {
        return UUID.nameUUIDFromBytes(("construction:" + ctx.village().id() + ":" + p.id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void ensure(VillageContext ctx, BuildProject p, BuildSite site) {
        var entries = TempBlocks.get(ctx.level().getServer()).entries();
        UUID owner = owner(ctx, p);
        boolean hasSign = entries.stream().anyMatch(e -> e.owner().equals(owner) && e.placed().is(ConstructionSignBlock.SIGN.get()));
        if (!hasSign) {
            var box = site.box();
            for (int x = box.minX(); x <= box.maxX(); x++) {
                BlockPos at = new BlockPos(x, site.groundY, box.minZ() - 1);
                if (ctx.loaded(at) && ctx.level().getBlockState(at).isAir() && BuildSite.standable(ctx.level(), at)) {
                    if (Construction.takeBudget(ctx)) TempBlocks.place(ctx.level(), at, ConstructionSignBlock.SIGN.get().defaultBlockState(), Integer.MAX_VALUE, owner, false);
                    break;
                }
            }
        }
        send(ctx, p, site, true);
        if (ctx.level().getGameTime() % 80 == 0) ctx.level().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                (site.box().minX() + site.box().maxX()) / 2.0, site.groundY + 2, (site.box().minZ() + site.box().maxZ()) / 2.0,
                3, 1, .4, 1, .01);
    }
    /** 这个项目的施工牌（还没放下时为空）。 */
    public static Optional<BlockPos> sign(VillageContext ctx, BuildProject p) {
        UUID owner = owner(ctx, p);
        return TempBlocks.get(ctx.level().getServer()).entries().stream()
                .filter(e -> e.owner().equals(owner) && e.placed().is(ConstructionSignBlock.SIGN.get()))
                .map(e -> e.pos().pos()).findFirst();
    }

    static void send(VillageContext ctx, BuildProject p, BuildSite site, boolean active) {
        var box = site.box();
        List<BlockPos> blocked = new ArrayList<>();
        p.blockedCells.forEach((seg, bits) -> bits.stream().forEach(i -> blocked.add(VillageContext.block(site.segments.get(seg).cells().get(i).pos()))));
        var payload = new ConstructionSitePayload(owner(ctx, p), ctx.level().dimension().location(),
                new BlockPos(box.minX(), site.groundY, box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()), active, blocked);
        for (var player : ctx.level().players()) {
            if (!active || new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(payload.min()), net.minecraft.world.phys.Vec3.atLowerCornerOf(payload.max().offset(1,1,1))).inflate(64).contains(player.position()))
                PacketDistributor.sendToPlayer(player, payload);
        }
    }
    public static void clear(VillageContext ctx, BuildProject p) {
        UUID owner = owner(ctx, p);
        var entries = new ArrayList<>(TempBlocks.get(ctx.level().getServer()).entries());
        entries.sort(Comparator.comparingInt((TempBlocks.Entry e) -> e.pos().pos().getY()).reversed());
        for (var e : entries) if (e.owner().equals(owner)) {
            ServerLevel level = ctx.level().getServer().getLevel(e.pos().dimension());
            if (level != null) TempBlocks.restoreNow(level, e.pos().pos());
        }
        Construction.site(ctx, p).ifPresent(s -> send(ctx, p, s, false));
    }
    public static Optional<Component> describe(ServerLevel level, BlockPos sign) {
        var entry = TempBlocks.get(level.getServer()).entries().stream().filter(e -> e.pos().dimension().equals(level.dimension()) && e.pos().pos().equals(sign)).findFirst();
        if (entry.isEmpty()) return Optional.empty();
        for (var village : io.github.happysnappingturtle.mythicage.village.VillageRegistry.get(level.getServer()).all()) {
            var context = VillageScheduler.contextOf(level, village.id());
            if (context.isEmpty()) continue;
            var ctx = context.get();
            for (var p : ctx.state().projects) if (owner(ctx, p).equals(entry.get().owner())) {
                var site = Construction.site(ctx, p);
                if (site.isEmpty()) continue;
                return Optional.of(Component.translatable("mythic_age.construction.summary", Construction.name(site.get().spec, p.god),
                        BuildProject.count(p.placedCells), site.get().segments.stream().mapToInt(s -> s.cells().size()).sum(),
                        p.missing().toString(), BuildProject.count(p.blockedCells)));
            }
        }
        return Optional.empty();
    }
}
