package io.github.happysnappingturtle.mythicage.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 不占空间的元数据件（T22 §3.3，同 {@code VillageInfoPiece} 的做法）：结构种类与锚点（世界坐标，已按旋转变换），
 * 随区块保存；区块载入后由 {@link StructureRegistration} 在主线程读出来生成守卫、设好宝库。包围盒是 1 格。
 *
 * @param kind    结构种类（如 {@code greek:abandoned_temple}），决定由哪个 {@link StructureRegistration.SiteHandler} 处理
 * @param anchors 锚点名 → 世界坐标（{@code guardian}、{@code vault}、{@code loot}、{@code entrance}、{@code exit} …）
 */
public final class StructureInfoPiece extends StructurePiece {
    public record Info(String kind, Map<String, List<BlockPos>> anchors) {
        public List<BlockPos> anchors(String name) {
            return anchors.getOrDefault(name, List.of());
        }
    }

    private final Info info;

    public StructureInfoPiece(Info info, BlockPos at) {
        super(MythStructures.INFO_PIECE.get(), 0, new BoundingBox(at));
        this.info = info;
    }

    public StructureInfoPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(MythStructures.INFO_PIECE.get(), tag);
        this.info = read(tag.getCompound("Info"));
    }

    public Info info() {
        return info;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.put("Info", write(info));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
    }

    public static CompoundTag write(Info i) {
        CompoundTag t = new CompoundTag();
        t.putString("kind", i.kind());
        CompoundTag a = new CompoundTag();
        i.anchors().forEach((k, v) -> a.put(k, new LongArrayTag(v.stream().mapToLong(BlockPos::asLong).toArray())));
        t.put("anchors", a);
        return t;
    }

    public static Info read(CompoundTag t) {
        Map<String, List<BlockPos>> anchors = new LinkedHashMap<>();
        CompoundTag a = t.getCompound("anchors");
        for (String k : a.getAllKeys()) {
            List<BlockPos> pts = new ArrayList<>();
            for (long l : a.getLongArray(k)) {
                pts.add(BlockPos.of(l));
            }
            anchors.put(k, List.copyOf(pts));
        }
        return new Info(t.getString("kind"), anchors);
    }
}
