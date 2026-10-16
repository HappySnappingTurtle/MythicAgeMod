package io.github.happysnappingturtle.mythicage.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 村庄判定（T10a 设计文档 §1、§2）：钟属于哪种原版村庄结构；是否沿海；村民属于哪个村庄。
 * 结构判定可在测试中替换（GameTest 的测试结构里放不下原版村庄）。
 */
public final class VillageResolver {
    private static final Map<String, ResourceKey<Structure>> VANILLA = new LinkedHashMap<>();

    static {
        VANILLA.put("plains", BuiltinStructures.VILLAGE_PLAINS);
        VANILLA.put("desert", BuiltinStructures.VILLAGE_DESERT);
        VANILLA.put("savanna", BuiltinStructures.VILLAGE_SAVANNA);
        VANILLA.put("taiga", BuiltinStructures.VILLAGE_TAIGA);
        VANILLA.put("snowy", BuiltinStructures.VILLAGE_SNOWY);
    }

    @Nullable
    private static volatile BiFunction<ServerLevel, BlockPos, Optional<String>> typeOverride;

    private VillageResolver() {
    }

    /** 只供 GameTest：替换「钟属于哪种原版村庄」的判定；传 null 恢复。 */
    public static void overrideTypeForTest(@Nullable BiFunction<ServerLevel, BlockPos, Optional<String>> f) {
        typeOverride = f;
    }

    /** 钟所在的原版村庄类型；不在任何原版村庄结构里时为空。 */
    public static Optional<String> vanillaType(ServerLevel level, BlockPos bell) {
        BiFunction<ServerLevel, BlockPos, Optional<String>> o = typeOverride;
        if (o != null) {
            return o.apply(level, bell);
        }
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<String, ResourceKey<Structure>> e : VANILLA.entrySet()) {
            Structure s = structures.get(e.getValue());
            if (s == null) {
                continue;
            }
            StructureStart start = level.structureManager().getStructureWithPieceAt(bell, s);
            if (!start.isValid()) {
                start = level.structureManager().getStructureAt(bell, s);
            }
            if (start.isValid()) {
                return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }

    /** 钟周围 32 格（中心 + 8 个方向）有海洋或海滩群系即为沿海。 */
    public static boolean coastal(ServerLevel level, BlockPos bell) {
        int[][] dirs = {{0, 0}, {32, 0}, {-32, 0}, {0, 32}, {0, -32}, {23, 23}, {23, -23}, {-23, 23}, {-23, -23}};
        for (int[] d : dirs) {
            BlockPos p = bell.offset(d[0], 0, d[1]);
            var biome = level.getBiome(p);
            if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) {
                return true;
            }
        }
        return false;
    }

    /** 村民所属的已登记村庄：聚会点记忆指向的钟。 */
    public static Optional<VillageRegistry.Village> villageOf(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        Optional<GlobalPos> meeting = villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
        return meeting.flatMap(m -> VillageRegistry.get(level.getServer()).byBell(m));
    }
}
