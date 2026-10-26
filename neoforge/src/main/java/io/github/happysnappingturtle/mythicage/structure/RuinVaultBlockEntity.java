package io.github.happysnappingturtle.mythicage.structure;

import io.github.happysnappingturtle.mythicage.core.structure.VaultRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.UUID;

/** 宝库的方块实体：用哪张宝库表、哪些玩家开过（只用于提示；防重复靠玩家收据）。 */
public class RuinVaultBlockEntity extends BlockEntity {
    private String table = "";
    private final LinkedHashSet<UUID> opened = new LinkedHashSet<>();

    public RuinVaultBlockEntity(BlockPos pos, BlockState state) {
        super(RuinVaults.BLOCK_ENTITY.get(), pos, state);
    }

    public String table() {
        return table;
    }

    public void setTable(String table) {
        this.table = table == null ? "" : table;
        setChanged();
    }

    public boolean opened(UUID player) {
        return opened.contains(player);
    }

    void markOpened(UUID player) {
        VaultRules.remember(opened, player);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Table", table);
        ListTag l = new ListTag();
        opened.forEach(u -> l.add(StringTag.valueOf(u.toString())));
        tag.put("Opened", l);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        table = tag.getString("Table");
        opened.clear();
        for (Tag t : tag.getList("Opened", Tag.TAG_STRING)) {
            opened.add(UUID.fromString(t.getAsString()));
        }
    }
}
