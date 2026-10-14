package io.github.happysnappingturtle.mythicage.content.greek.agri;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.core.growth.TickGuard;
import io.github.happysnappingturtle.mythicage.recipe.ModRecipes;
import io.github.happysnappingturtle.mythicage.recipe.PressingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * 橄榄压榨器的方块实体（T6b 设计文档 §5）：
 * 槽 0 原料、槽 1 容器、槽 2 产物。原料够一批、有容器、产物放得下时计时，满 time 刻后扣料出货；
 * 条件不满足时暂停，进度保留。进度只随游戏时间推进：同一游戏刻内被多次调用只计一次（{@link TickGuard}，§10.2-4）。
 * 漏斗：顶面放原料，侧面放容器，底面取产物。
 */
public class OlivePressBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUT = 0;
    public static final int CONTAINER = 1;
    public static final int OUTPUT = 2;
    private static final int[] TOP = {INPUT};
    private static final int[] SIDE = {CONTAINER};
    private static final int[] BOTTOM = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;
    private int total;
    private final TickGuard guard = new TickGuard();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return i == 0 ? progress : total;
        }

        @Override
        public void set(int i, int v) {
            if (i == 0) {
                progress = v;
            } else {
                total = v;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public OlivePressBlockEntity(BlockPos pos, BlockState state) {
        super(GreekAgriculture.OLIVE_PRESS_BE.get(), pos, state);
    }

    public int progress() {
        return progress;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OlivePressBlockEntity be) {
        be.tick(level, pos, state);
    }

    /** 推进一刻（同一游戏刻只计一次）。 */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!guard.firstCallAt(level.getGameTime())) {
            return;
        }
        Optional<RecipeHolder<PressingRecipe>> recipe = currentRecipe(level);
        boolean working = recipe.isPresent() && canOutput(recipe.get().value().result());
        if (working) {
            PressingRecipe r = recipe.get().value();
            total = r.time();
            progress++;
            if (progress >= r.time()) {
                items.get(INPUT).shrink(r.count());
                items.get(CONTAINER).shrink(1);
                ItemStack out = items.get(OUTPUT);
                if (out.isEmpty()) {
                    items.set(OUTPUT, r.result().copy());
                } else {
                    out.grow(r.result().getCount());
                }
                progress = 0;
            }
            setChanged();
        }
        if (state.getValue(OlivePressBlock.PRESSING) != working) {
            level.setBlock(pos, state.setValue(OlivePressBlock.PRESSING, working), 3);
        }
    }

    private Optional<RecipeHolder<PressingRecipe>> currentRecipe(Level level) {
        return level.getRecipeManager().getRecipeFor(ModRecipes.PRESSING.get(),
                new PressingRecipe.Input(items.get(INPUT), items.get(CONTAINER)), level);
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = items.get(OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    // ---- 容器与漏斗 ----

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == OUTPUT || level == null) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.PRESSING.get()).stream()
                .anyMatch(h -> slot == INPUT ? h.value().acceptsIngredient(stack) : h.value().acceptsContainer(stack));
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mythic_age.olive_press");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new OlivePressMenu(id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("progress");
        total = tag.getInt("total");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("progress", progress);
        tag.putInt("total", total);
    }
}
