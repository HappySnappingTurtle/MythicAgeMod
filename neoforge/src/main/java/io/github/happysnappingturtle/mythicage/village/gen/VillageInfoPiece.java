package io.github.happysnappingturtle.mythicage.village.gen;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
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
 * 不占空间的信息片段（V0 §4.1）：随区块保存，区块载入时由主线程读出来登记村庄。包围盒是中心的 1 格。
 */
public final class VillageInfoPiece extends StructurePiece {
    /** 登记所需的全部信息。 */
    public record Info(String civ, String patron, String layout, int layoutVersion, String templateSet, Pos center,
                       Map<String, List<Pos>> anchors, List<Box> built, List<VillagePlan.Plot> plots) {
    }

    private final Info info;

    public VillageInfoPiece(VillagePlan plan, String templateSet) {
        super(ModStructures.INFO_PIECE.get(), 0, new BoundingBox(new BlockPos(plan.center().x(), plan.center().y(),
                plan.center().z())));
        this.info = new Info(plan.civ(), plan.patron(), plan.layout(), plan.layoutVersion(), templateSet, plan.center(),
                plan.anchors(), plan.builtBoxes(), plan.plots());
    }

    public VillageInfoPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.INFO_PIECE.get(), tag);
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
        t.putString("civ", i.civ());
        t.putString("patron", i.patron());
        t.putString("layout", i.layout());
        t.putInt("layoutVersion", i.layoutVersion());
        t.putString("templateSet", i.templateSet());
        t.putLong("center", pack(i.center()));
        CompoundTag anchors = new CompoundTag();
        i.anchors().forEach((k, v) -> anchors.put(k, new LongArrayTag(v.stream().mapToLong(VillageInfoPiece::pack).toArray())));
        t.put("anchors", anchors);
        ListTag boxes = new ListTag();
        for (Box b : i.built()) {
            boxes.add(new IntArrayTag(new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()}));
        }
        t.put("built", boxes);
        ListTag plots = new ListTag();
        for (VillagePlan.Plot pl : i.plots()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", pl.id());
            c.putString("kind", pl.kind());
            c.putString("stage", pl.stage());
            Box b = pl.box();
            c.putIntArray("box", new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
            c.putString("front", pl.front());
            plots.add(c);
        }
        t.put("plots", plots);
        return t;
    }

    public static Info read(CompoundTag t) {
        Map<String, List<Pos>> anchors = new LinkedHashMap<>();
        CompoundTag a = t.getCompound("anchors");
        for (String k : a.getAllKeys()) {
            List<Pos> pts = new ArrayList<>();
            for (long l : a.getLongArray(k)) {
                pts.add(unpack(l));
            }
            anchors.put(k, pts);
        }
        List<Box> built = new ArrayList<>();
        for (Tag b : t.getList("built", Tag.TAG_INT_ARRAY)) {
            int[] v = ((IntArrayTag) b).getAsIntArray();
            built.add(new Box(v[0], v[1], v[2], v[3], v[4], v[5]));
        }
        List<VillagePlan.Plot> plots = new ArrayList<>();
        for (Tag x : t.getList("plots", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            int[] v = c.getIntArray("box");
            plots.add(new VillagePlan.Plot(c.getString("id"), c.getString("kind"), c.getString("stage"),
                    new Box(v[0], v[1], v[2], v[3], v[4], v[5]), c.getString("front")));
        }
        return new Info(t.getString("civ"), t.getString("patron"), t.getString("layout"), t.getInt("layoutVersion"),
                t.getString("templateSet"), unpack(t.getLong("center")), anchors, built, plots);
    }

    public static long pack(Pos p) {
        return BlockPos.asLong(p.x(), p.y(), p.z());
    }

    public static Pos unpack(long l) {
        return new Pos(BlockPos.getX(l), BlockPos.getY(l), BlockPos.getZ(l));
    }

    public static BlockPos block(Pos p) {
        return new BlockPos(p.x(), p.y(), p.z());
    }
}
