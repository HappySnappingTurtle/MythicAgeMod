package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    @Nullable
    private static volatile BiFunction<ServerLevel, BlockPos, Set<String>> biomeOverride;

    /** 只供 GameTest：替换「钟周围有哪些群系」的判定（返回群系 id 与标签，标签带 #）；传 null 恢复。 */
    public static void overrideBiomesForTest(@Nullable BiFunction<ServerLevel, BlockPos, Set<String>> f) {
        biomeOverride = f;
    }

    /**
     * 群系加成命中的主神 → 倍率（T10b 设计文档 §7）：在钟周围 radius 格（中心 + 8 个方向）采样，
     * 任一点的群系 id 或标签在加成表里即命中。
     */
    public static Map<String, Double> matchedBonuses(ServerLevel level, BlockPos bell,
                                                     Map<String, VillagesConfig.BiomeBonus> bonuses) {
        Map<String, Double> out = new LinkedHashMap<>();
        bonuses.forEach((patron, b) -> {
            if (matches(level, bell, b)) {
                out.put(patron, b.multiplier());
            }
        });
        return out;
    }

    private static boolean matches(ServerLevel level, BlockPos bell, VillagesConfig.BiomeBonus b) {
        BiFunction<ServerLevel, BlockPos, Set<String>> o = biomeOverride;
        if (o != null) {
            Set<String> here = o.apply(level, bell);
            return b.biomes().stream().anyMatch(here::contains);
        }
        int r = b.radius();
        int d = (int) Math.round(r * 0.7071);
        int[][] dirs = {{0, 0}, {r, 0}, {-r, 0}, {0, r}, {0, -r}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
        for (int[] off : dirs) {
            Holder<Biome> biome = level.getBiome(bell.offset(off[0], 0, off[1]));
            for (String id : b.biomes()) {
                if (id.startsWith("#")) {
                    ResourceLocation rl = ResourceLocation.tryParse(id.substring(1));
                    if (rl != null && biome.is(TagKey.create(Registries.BIOME, rl))) {
                        return true;
                    }
                } else if (biome.unwrapKey().map(k -> k.location().toString().equals(id)).orElse(false)) {
                    return true;
                }
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
