package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.MythicAge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 施工蓝图（T19 设计文档 §8.1）：从结构模板读出每一格（相对模板原点的坐标 + 方块状态，含 y ≥ 地面层的空气格）。
 * 走 {@link net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager}，与世界生成读同一份模板。
 * 按模板 id 缓存；数据包重载时清空。
 */
public final class Blueprint {
    public record Cell(BlockPos rel, BlockState state) {
    }

    private static final Map<String, List<Cell>> CACHE = new ConcurrentHashMap<>();

    private Blueprint() {
    }

    public static Optional<List<Cell>> load(ServerLevel level, String template) {
        List<Cell> cached = CACHE.get(template);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<StructureTemplate> t = level.getServer().getStructureManager().get(ResourceLocation.parse(template));
        if (t.isEmpty()) {
            MythicAge.LOGGER.warn("施工蓝图 {} 不存在", template);
            return Optional.empty();
        }
        CompoundTag nbt = t.get().save(new CompoundTag());
        HolderGetter<Block> blocks = level.holderLookup(Registries.BLOCK);
        ListTag paletteTag = nbt.contains("palette") ? nbt.getList("palette", Tag.TAG_COMPOUND)
                : nbt.getList("palettes", Tag.TAG_LIST).getList(0);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(blocks, paletteTag.getCompound(i)));
        }
        List<Cell> out = new ArrayList<>();
        for (Tag b : nbt.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) b;
            ListTag pos = c.getList("pos", Tag.TAG_INT);
            out.add(new Cell(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), palette.get(c.getInt("state"))));
        }
        List<Cell> frozen = List.copyOf(out);
        CACHE.put(template, frozen);
        return Optional.of(frozen);
    }

    public static void clear() {
        CACHE.clear();
    }
}
