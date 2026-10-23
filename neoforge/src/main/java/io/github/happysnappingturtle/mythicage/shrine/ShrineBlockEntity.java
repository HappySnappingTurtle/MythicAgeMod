package io.github.happysnappingturtle.mythicage.shrine;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.relic.OfferedIndex;
import io.github.happysnappingturtle.mythicage.relic.RelicShelf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

/**
 * 神庙方块实体的通用部分（T10b 设计文档 §3.3）：主人、主人名、主人在本文明的时代缓存。
 * 存档键（Owner / OwnerName / OwnerAge）沿用 T8a，旧存档直接读取。
 */
public abstract class ShrineBlockEntity extends BlockEntity {
    private UUID owner;
    private String ownerName = "";
    private Age ownerAge = Age.NONE;
    /** 供奉架（T16）：各文明通用，能不能供奉由 {@link io.github.happysnappingturtle.mythicage.relic.RelicAnchor} 决定。 */
    private final RelicShelf relics = new RelicShelf(this::setChanged);

    protected ShrineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 本方块实体所属的神庙种类。 */
    public abstract ShrineKind kind();

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    public String ownerName() {
        return ownerName;
    }

    public Age ownerAge() {
        return ownerAge;
    }

    public RelicShelf relics() {
        return relics;
    }

    public void bind(UUID owner, String name, Age age) {
        this.owner = owner;
        this.ownerName = name;
        this.ownerAge = age;
        setChanged();
    }

    public void refreshOwnerAge(Age age) {
        if (age != ownerAge) {
            ownerAge = age;
            setChanged();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            ShrineService.onLoaded(server, this);
            OfferedIndex.update(server, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) {
            OfferedIndex.remove(server, getBlockPos());
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel server) {
            OfferedIndex.remove(server, getBlockPos());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("OwnerName", ownerName);
        tag.putString("OwnerAge", ownerAge.id());
        relics.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        ownerAge = Age.byId(tag.getString("OwnerAge")).orElse(Age.NONE);
        relics.load(tag, registries);
    }
}
