package io.github.happysnappingturtle.mythicage.structure;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.ArrayList;
import java.util.List;

/**
 * 「出生点附近保证一座」的同心环（T22 §5.5）。原版同心环以世界原点为圆心，只在 112 格内找偏好群系（按 y = 0 取），
 * 找不到就留在原地：出生点不在原点附近时会超出 1500 格，落在海、河、湖里时迷宫因为入口在水里放弃，保证就落空了。
 * 带 {@link #TAG} 标签的结构集，在原版算完环位置后再修一次：
 * <ol>
 *   <li>整体平移到出生点（原版选出生点的气候目标 {@code findSpawnPosition}，同一个种子算出来的就是世界出生点附近）；</li>
 *   <li>以出生点为圆心按极坐标找：离出生点的距离先保持原环的距离（夹在 {@value #MIN_BLOCKS}–{@value #MAX_BLOCKS} 格），
 *       再每次 ±{@value #DIST_STEP} 格；方向先保持，再每次 ±15°，转满一圈（有界，最多约 300 个点）。
 *       先找「偏好且不湿」的地表群系，没有就取最先找到的「不湿」，都没有就保留平移后的位置（四面是海）。</li>
 * </ol>
 * 群系按 y = {@value #SURFACE_Y} 取（地表附近），湿 = {@link #WET} 标签（海、河、沙滩、沼泽）。由
 * {@code ChunkGeneratorStructureStateMixin} 调用，五文明通用。
 */
public final class NearSpawnRings {
    public static final TagKey<StructureSet> TAG = TagKey.create(Registries.STRUCTURE_SET,
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "near_spawn_rings"));
    public static final TagKey<Biome> WET = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "wet_surface"));
    public static final int MIN_BLOCKS = 256;
    public static final int MAX_BLOCKS = 1100;
    public static final int DIST_STEP = 128;
    public static final int ANGLE_STEPS = 24;
    public static final int SURFACE_Y = 64;

    private NearSpawnRings() {
    }

    public static List<ChunkPos> adjust(List<ChunkPos> ring, HolderSet<Biome> preferred, BiomeSource source, Climate.Sampler sampler) {
        BlockPos spawn = sampler.findSpawnPosition();
        ChunkPos sc = new ChunkPos(spawn);
        List<ChunkPos> out = new ArrayList<>(ring.size());
        for (ChunkPos c : ring) {
            ChunkPos shifted = new ChunkPos(c.x + sc.x, c.z + sc.z);
            ChunkPos moved = search(spawn, shifted, preferred, source, sampler);
            MythicAge.LOGGER.info("出生点附近的同心环：原版 {} → 以出生点区块 {} 为圆心 {} → {}（{}）", c, sc, shifted, moved,
                    biome(moved, source, sampler).getRegisteredName());
            out.add(moved);
        }
        return out;
    }

    /** 候选点（极坐标，按「方向偏转步数 + 距离偏移步数」由少到多），第一个就是平移后的原位。 */
    static List<ChunkPos> candidates(BlockPos spawn, ChunkPos start) {
        double dx = start.getMiddleBlockX() - spawn.getX();
        double dz = start.getMiddleBlockZ() - spawn.getZ();
        double d0 = Math.max(MIN_BLOCKS, Math.min(MAX_BLOCKS, Math.hypot(dx, dz)));
        double a0 = Math.atan2(dz, dx);
        int maxOff = (MAX_BLOCKS - MIN_BLOCKS) / DIST_STEP;
        List<int[]> steps = new ArrayList<>();
        for (int j = 0; j <= ANGLE_STEPS / 2; j++) {
            for (int sj : j == 0 || j == ANGLE_STEPS / 2 ? new int[]{j} : new int[]{j, -j}) {
                for (int k = 0; k <= maxOff; k++) {
                    for (int sk : k == 0 ? new int[]{0} : new int[]{-k, k}) {
                        steps.add(new int[]{sj, sk, j + k});
                    }
                }
            }
        }
        steps.sort(java.util.Comparator.comparingInt(t -> t[2]));
        List<ChunkPos> out = new ArrayList<>();
        out.add(start);
        for (int[] t : steps) {
            double d = d0 + t[1] * DIST_STEP;
            if ((t[0] == 0 && t[1] == 0) || d < MIN_BLOCKS || d > MAX_BLOCKS) {
                continue;
            }
            double a = a0 + t[0] * (2 * Math.PI / ANGLE_STEPS);
            out.add(new ChunkPos(SectionPos.blockToSectionCoord(spawn.getX() + (int) Math.round(Math.cos(a) * d)),
                    SectionPos.blockToSectionCoord(spawn.getZ() + (int) Math.round(Math.sin(a) * d))));
        }
        return out;
    }

    static ChunkPos search(BlockPos spawn, ChunkPos start, HolderSet<Biome> preferred, BiomeSource source, Climate.Sampler sampler) {
        ChunkPos dry = null;
        for (ChunkPos p : candidates(spawn, start)) {
            Holder<Biome> b = biome(p, source, sampler);
            if (b.is(WET)) {
                continue;
            }
            if (preferred.contains(b)) {
                return p;
            }
            if (dry == null) {
                dry = p;
            }
        }
        return dry != null ? dry : start;
    }

    public static Holder<Biome> biome(ChunkPos c, BiomeSource source, Climate.Sampler sampler) {
        return source.getNoiseBiome(QuartPos.fromBlock(SectionPos.sectionToBlockCoord(c.x, 8)), QuartPos.fromBlock(SURFACE_Y),
                QuartPos.fromBlock(SectionPos.sectionToBlockCoord(c.z, 8)), sampler);
    }
}
