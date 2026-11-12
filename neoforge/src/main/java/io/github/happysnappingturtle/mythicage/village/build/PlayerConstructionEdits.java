package io.github.happysnappingturtle.mythicage.village.build;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import java.util.*;

/** Explicit player provenance survives project creation and server restarts, including natural-looking blocks. */
public final class PlayerConstructionEdits extends SavedData {
    private final Set<Long> placed = new HashSet<>();
    private final Set<Long> knownChunks = new HashSet<>();
    private static PlayerConstructionEdits get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(PlayerConstructionEdits::new, (tag, lookup) -> {
            var data = new PlayerConstructionEdits();
            for (long pos : tag.getLongArray("placed")) data.placed.add(pos);
            for (long pos : tag.getLongArray("knownChunks")) data.knownChunks.add(pos);
            return data;
        }, null), "mythic_age_player_construction_edits");
    }
    public static boolean contains(ServerLevel level, BlockPos pos) { return get(level).placed.contains(pos.asLong()); }
    public static boolean knownTerrain(ServerLevel level, BlockPos pos) {
        return get(level).knownChunks.contains(new net.minecraft.world.level.ChunkPos(pos).toLong());
    }
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.isNewChunk() && event.getLevel() instanceof ServerLevel level) {
            var data = get(level); data.knownChunks.add(event.getChunk().getPos().toLong()); data.setDirty();
        }
    }
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player && event.getLevel() instanceof ServerLevel level && !event.isCanceled()) {
            var data = get(level);
            if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
                multi.getReplacedBlockSnapshots().forEach(s -> {
                    data.placed.add(s.getPos().asLong());
                    io.github.happysnappingturtle.mythicage.power.TempBlocks.forget(level,s.getPos());
                });
            } else {
                data.placed.add(event.getPos().asLong());
                io.github.happysnappingturtle.mythicage.power.TempBlocks.forget(level,event.getPos());
            }
            data.setDirty();
        }
    }
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && !event.isCanceled()) {
            var data = get(level); data.placed.remove(event.getPos().asLong()); data.setDirty();
        }
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putLongArray("placed", placed.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("knownChunks", knownChunks.stream().mapToLong(Long::longValue).toArray()); return tag;
    }
}
