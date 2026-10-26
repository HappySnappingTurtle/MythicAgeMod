package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.quest.RepairRules;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageZones;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * 建筑损坏记录（T21 设计文档 §6.1、§6.2）：本村建筑盒里的方块被玩家打掉、被炸、被僵尸破门 / 凋灵 / 末影龙毁掉时，
 * 记下原方块状态与责任玩家；玩家放进建筑盒的方块另记一份（它们被打掉不算损坏）。
 * <p>
 * 只认有居民框架的村庄（有村库与文明村庄数据；原版村庄没有，D7）。村民自己的施工、收割、采集是直接改方块，
 * 不经过这些事件，所以不会被记成损坏。作物、农田、树叶、树苗、花草、火、液体、采集点不算设施。
 * 末影人搬方块、火烧没有带位置的事件，首轮不记（§1.2）。
 */
public final class BuildingDamage {
    private BuildingDamage() {
    }

    /** 一个位置落在哪个村的哪座建筑盒里。 */
    public record Located(VillageRegistry.Village village, Box box) {
    }

    public static Optional<Located> locate(ServerLevel level, BlockPos pos) {
        Optional<VillageRegistry.Village> v = VillageZones.zoneAt(level, pos).village();
        if (v.isEmpty() || VillageData.civ(v.get().civ()).isEmpty() || VillageEconomy.get(level.getServer()).peek(v.get().id()).isEmpty()) {
            return Optional.empty();
        }
        return VillagePlans.get(level.getServer()).byVillage(v.get().id()).flatMap(p -> p.info().built().stream()
                .filter(b -> b.containsXZ(pos.getX(), pos.getZ()) && pos.getY() >= b.minY() && pos.getY() <= b.maxY())
                .findFirst()).map(b -> new Located(v.get(), b));
    }

    /** 算不算设施：空气、液体、作物、农田、树叶、树苗、花草、火、可替换的植物、雪层不算。 */
    static boolean facility(BlockState st) {
        if (st.isAir() || st.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            return false;
        }
        return !(st.is(BlockTags.CROPS) || st.is(Blocks.FARMLAND) || st.is(BlockTags.LEAVES) || st.is(BlockTags.SAPLINGS)
                || st.is(BlockTags.FLOWERS) || st.is(BlockTags.FIRE) || st.is(BlockTags.REPLACEABLE) || st.is(Blocks.SNOW));
    }

    /** 双格方块按「主格」记：门记下半、床记床尾（放回时物品放在主格上）。 */
    static BlockPos primary(BlockPos pos, BlockState st) {
        if (st.getBlock() instanceof DoorBlock && st.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        if (st.getBlock() instanceof BedBlock && st.getValue(BedBlock.PART) == BedPart.HEAD) {
            return pos.relative(st.getValue(BedBlock.FACING).getOpposite());
        }
        return pos;
    }

    static BlockState primaryState(ServerLevel level, BlockPos pos, BlockState st) {
        if (st.getBlock() instanceof DoorBlock && st.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.getBlock() == st.getBlock() ? below : st.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        }
        if (st.getBlock() instanceof BedBlock && st.getValue(BedBlock.PART) == BedPart.HEAD) {
            return st.setValue(BedBlock.PART, BedPart.FOOT);
        }
        return st;
    }

    /** 记一格损坏（§6.1）：玩家方块不记；同一格同一 MC 日只记一次；每座建筑最多 {@value RepairRules#MAX_CELLS} 格。 */
    public static void record(ServerLevel level, BlockPos raw, BlockState rawState, @Nullable UUID cause) {
        if (!facility(rawState)) {
            return;
        }
        BlockPos pos = primary(raw, rawState);
        BlockState state = primaryState(level, raw, rawState);
        Optional<Located> loc = locate(level, pos);
        if (loc.isEmpty() || inPoint(level, loc.get().village(), pos)) {
            return;
        }
        VillageQuests all = VillageQuests.get(level.getServer());
        VillageQuests.Book book = all.book(loc.get().village().id());
        long key = pos.asLong();
        if (book.playerBlocks.remove(key) || book.playerBlocks.remove(raw.asLong())) {
            return;
        }
        long today = MythDays.today(level.getServer());
        VillageQuests.Damage d = book.damage.computeIfAbsent(VillageQuests.Damage.key(loc.get().box()),
                k -> new VillageQuests.Damage(loc.get().box()));
        if (d.cells.containsKey(key) || !RepairRules.recordAgain(d.lastDay.get(key), today)) {
            return;
        }
        d.lastDay.put(key, today);
        d.lastDay.values().removeIf(day -> day < today - 1);
        if (d.cells.size() >= RepairRules.MAX_CELLS) {
            d.overflow++;
        } else {
            d.cells.put(key, new VillageQuests.Cell(key, state, cause, today));
        }
        all.setDirty();
    }

    /** 采集点（采石面、黏土坑等）不是设施：村民和玩家都会在那里取料。 */
    private static boolean inPoint(ServerLevel level, VillageRegistry.Village v, BlockPos pos) {
        return VillageEconomy.get(level.getServer()).peek(v.id()).map(s -> s.points.values().stream()
                .anyMatch(p -> Math.abs(p.pos().x() - pos.getX()) <= 2 && Math.abs(p.pos().y() - pos.getY()) <= 2
                        && Math.abs(p.pos().z() - pos.getZ()) <= 2)).orElse(false);
    }

    /** 责任玩家：玩家本人，或玩家的宠物、召唤物（02 号 §4）。 */
    @Nullable
    static UUID owner(@Nullable Entity e) {
        if (e instanceof ServerPlayer p) {
            return p.getUUID();
        }
        if (e instanceof OwnableEntity o && o.getOwner() instanceof ServerPlayer p) {
            return p.getUUID();
        }
        return null;
    }

    // ───────────── 事件 ─────────────

    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof ServerLevel level && !e.isCanceled()) {
            record(level, e.getPos(), e.getState(), e.getPlayer().getUUID());
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof ServerLevel level)) {
            return;
        }
        UUID cause = owner(e.getExplosion().getIndirectSourceEntity());
        for (BlockPos pos : e.getAffectedBlocks()) {
            BlockState st = level.getBlockState(pos);
            if (!st.isAir()) {
                record(level, pos, st, cause);
            }
        }
    }

    public static void onLivingDestroy(LivingDestroyBlockEvent e) {
        if (e.getEntity().level() instanceof ServerLevel level && !e.isCanceled()) {
            record(level, e.getPos(), e.getState(), owner(e.getEntity()));
        }
    }

    /** 玩家放方块：是损坏格的原方块就算修好（{@link RepairQuests#onPlaced}）；否则记成玩家方块。 */
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.isCanceled() || !(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        BlockState placed = e.getPlacedBlock();
        BlockPos pos = primary(e.getPos(), placed);
        Optional<Located> loc = locate(level, pos);
        if (loc.isEmpty()) {
            return;
        }
        if (!RepairQuests.onPlaced(p, level, pos, placed, loc.get())) {
            VillageQuests.get(level.getServer()).book(loc.get().village().id()).addPlayerBlock(pos.asLong());
        }
    }

    /** 方块 id。 */
    static String id(BlockState st) {
        return BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString();
    }

    /** 自修时把原方块放回（门补上半、床补床头）。返回是否放了。 */
    static boolean restore(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState cur = level.getBlockState(pos);
        if (!cur.isAir() && !cur.canBeReplaced()) {
            return false;
        }
        level.setBlock(pos, state, 3);
        if (state.getBlock() instanceof DoorBlock) {
            BlockPos up = pos.above();
            if (level.getBlockState(up).canBeReplaced()) {
                level.setBlock(up, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 3);
            }
        } else if (state.getBlock() instanceof BedBlock) {
            Direction f = state.getValue(BedBlock.FACING);
            BlockPos head = pos.relative(f);
            if (level.getBlockState(head).canBeReplaced()) {
                level.setBlock(head, state.setValue(BedBlock.PART, BedPart.HEAD), 3);
            }
        }
        return true;
    }
}
