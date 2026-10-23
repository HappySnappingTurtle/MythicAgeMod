package io.github.happysnappingturtle.mythicage.relic;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 神庙的供奉架（T16 设计文档 §2.3）：最多 3 件（供奉位全局上限是 3）。实际能放几件由主人的供奉位决定，
 * 那是 {@link RelicService} 的事；这里只存物品。存档键 {@code Relics}。
 */
public final class RelicShelf {
    public static final int MAX = 3;
    private final List<ItemStack> stacks = new ArrayList<>();
    private final Runnable changed;

    public RelicShelf(Runnable changed) {
        this.changed = changed;
    }

    public List<ItemStack> stacks() {
        return Collections.unmodifiableList(stacks);
    }

    public boolean isFull() {
        return stacks.size() >= MAX;
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    /** 放入一件（数量 1）；满了返回 false。 */
    public boolean add(ItemStack stack) {
        if (isFull() || stack.isEmpty()) {
            return false;
        }
        stacks.add(stack.copyWithCount(1));
        changed.run();
        return true;
    }

    /** 取出第 slot 件；越界时返回空。 */
    public ItemStack remove(int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack s = stacks.remove(slot);
        changed.run();
        return s;
    }

    /** 全部取出（神庙被移除时）。 */
    public List<ItemStack> clear() {
        List<ItemStack> out = List.copyOf(stacks);
        if (!stacks.isEmpty()) {
            stacks.clear();
            changed.run();
        }
        return out;
    }

    public boolean contains(UUID instance) {
        for (ItemStack s : stacks) {
            if (instance.equals(RelicItems.instance(s).orElse(null))) {
                return true;
            }
        }
        return false;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (stacks.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (ItemStack s : stacks) {
            list.add(s.save(registries));
        }
        tag.put("Relics", list);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stacks.clear();
        for (Tag t : tag.getList("Relics", Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, t).filter(s -> !s.isEmpty()).ifPresent(s -> {
                if (stacks.size() < MAX) {
                    stacks.add(s);
                }
            });
        }
    }
}
