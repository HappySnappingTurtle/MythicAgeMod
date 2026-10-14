package io.github.happysnappingturtle.mythicage.content.greek.agri;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 压榨器界面：原料、容器、产物三个槽，加玩家背包；进度通过 ContainerData 同步。 */
public class OlivePressMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    /** 客户端构造。 */
    public OlivePressMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(3), new SimpleContainerData(2));
    }

    public OlivePressMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(GreekAgriculture.OLIVE_PRESS_MENU.get(), id);
        checkContainerSize(container, 3);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, OlivePressBlockEntity.INPUT, 44, 26));
        addSlot(new Slot(container, OlivePressBlockEntity.CONTAINER, 44, 48));
        addSlot(new Slot(container, OlivePressBlockEntity.OUTPUT, 116, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inventory, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inventory, c, 8 + c * 18, 142));
        }
        addDataSlots(data);
    }

    /** 进度（0–1），供界面画箭头。 */
    public float progress() {
        int total = data.get(1);
        return total <= 0 ? 0 : (float) data.get(0) / total;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
        } else if (container.canPlaceItem(OlivePressBlockEntity.INPUT, stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (container.canPlaceItem(OlivePressBlockEntity.CONTAINER, stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
