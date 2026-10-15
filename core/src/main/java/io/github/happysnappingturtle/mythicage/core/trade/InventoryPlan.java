package io.github.happysnappingturtle.mythicage.core.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 背包预留（T9 设计文档 §5 第 2 步）：在一个背包快照上模拟「先扣物品价、再放入产物」，判断能否全部放下。
 * 适配层把真实背包转成槽位列表；同类物品能否合堆由 {@link Slot#stackKey} 决定（适配层把物品与组件编码进去）。
 */
public final class InventoryPlan {
    /** 一个槽位；空槽 stackKey 为 null。 */
    public record Slot(String stackKey, int count, int maxStack) {
        public static Slot empty() {
            return new Slot(null, 0, 0);
        }

        public boolean isEmpty() {
            return stackKey == null || count <= 0;
        }
    }

    private final List<Slot> slots;

    public InventoryPlan(List<Slot> slots) {
        this.slots = new ArrayList<>(slots);
    }

    /** 背包里某种物品的总数。 */
    public int count(String stackKey) {
        int n = 0;
        for (Slot s : slots) {
            if (!s.isEmpty() && s.stackKey().equals(stackKey)) {
                n += s.count();
            }
        }
        return n;
    }

    /** 扣除物品；数量不足时返回 false 且快照不变。从后往前扣，与原版消耗顺序无关，只影响模拟。 */
    public boolean remove(String stackKey, int amount) {
        if (count(stackKey) < amount) {
            return false;
        }
        int left = amount;
        for (int i = slots.size() - 1; i >= 0 && left > 0; i--) {
            Slot s = slots.get(i);
            if (!s.isEmpty() && s.stackKey().equals(stackKey)) {
                int take = Math.min(left, s.count());
                left -= take;
                slots.set(i, s.count() - take == 0 ? Slot.empty() : new Slot(s.stackKey(), s.count() - take, s.maxStack()));
            }
        }
        return true;
    }

    /** 放入物品：先合到同类未满的堆，再用空槽。全部放下时返回 true（并更新快照），否则 false 且快照不变。 */
    public boolean insert(String stackKey, int amount, int maxStack) {
        Objects.requireNonNull(stackKey);
        List<Slot> trial = new ArrayList<>(slots);
        int left = amount;
        for (int i = 0; i < trial.size() && left > 0; i++) {
            Slot s = trial.get(i);
            if (!s.isEmpty() && s.stackKey().equals(stackKey) && s.count() < s.maxStack()) {
                int add = Math.min(left, s.maxStack() - s.count());
                left -= add;
                trial.set(i, new Slot(stackKey, s.count() + add, s.maxStack()));
            }
        }
        for (int i = 0; i < trial.size() && left > 0; i++) {
            if (trial.get(i).isEmpty()) {
                int add = Math.min(left, maxStack);
                left -= add;
                trial.set(i, new Slot(stackKey, add, maxStack));
            }
        }
        if (left > 0) {
            return false;
        }
        slots.clear();
        slots.addAll(trial);
        return true;
    }
}
