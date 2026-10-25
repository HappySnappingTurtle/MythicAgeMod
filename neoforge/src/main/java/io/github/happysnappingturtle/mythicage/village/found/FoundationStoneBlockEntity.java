package io.github.happysnappingturtle.mythicage.village.found;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/** 奠基石的方块实体（T19 §1.13 K1）：建立者、立起的村庄（待选主神时为空）。 */
public class FoundationStoneBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    @Nullable
    private UUID village;

    public FoundationStoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    @Nullable
    public UUID village() {
        return village;
    }

    /** 放下了、还没选主神。 */
    public boolean pending() {
        return village == null;
    }

    public void setOwner(UUID o) {
        owner = o;
        setChanged();
    }

    public void setVillage(@Nullable UUID v) {
        village = v;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        if (village != null) {
            tag.putUUID("Village", village);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        village = tag.hasUUID("Village") ? tag.getUUID("Village") : null;
    }
}
