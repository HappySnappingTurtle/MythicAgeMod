package io.github.happysnappingturtle.mythicage.core.relic;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 一名玩家的圣物绑定表（06 号 §0.1 唯一性）：键为圣物 ID，同一 ID 同一时间只有一条绑定，
 * 所以不论手里有几个物品，同一件圣物只能处于一种状态。不同玩家各有一本，互不影响。
 */
public final class RelicBook {
    private final Map<String, RelicBinding> bindings = new LinkedHashMap<>();

    /** 绑定结果：成功，或被已有绑定占用（返回占用它的那一条，供提示「当前在哪里」）。 */
    public record BindResult(boolean ok, Optional<RelicBinding> occupiedBy) {
        static final BindResult OK = new BindResult(true, Optional.empty());
    }

    public Optional<RelicBinding> get(String relicId) {
        return Optional.ofNullable(bindings.get(relicId));
    }

    public RelicState state(String relicId) {
        RelicBinding b = bindings.get(relicId);
        return b == null ? RelicState.INACTIVE : b.state();
    }

    /** 登记一条绑定；该 ID 已有任何绑定时拒绝（激活态之间必须先经过未激活）。 */
    public BindResult bind(String relicId, RelicBinding binding) {
        RelicBinding old = bindings.get(relicId);
        if (old != null) {
            return new BindResult(false, Optional.of(old));
        }
        bindings.put(relicId, binding);
        return BindResult.OK;
    }

    /** 解除绑定；实例不同时什么都不做（防止复制品把别处的绑定解除）。返回是否解除。 */
    public boolean release(String relicId, UUID instance) {
        RelicBinding b = bindings.get(relicId);
        if (b == null || !b.instance().equals(instance)) {
            return false;
        }
        bindings.remove(relicId);
        return true;
    }

    /** 对账时强制删除（物品已不在记录的位置）。 */
    public boolean remove(String relicId) {
        return bindings.remove(relicId) != null;
    }

    /** 某文明某状态的条数；供奉位用量 = used(OFFERED, civ)，多座神庙共用。 */
    public int used(RelicState state, Civilization civ) {
        int n = 0;
        for (RelicBinding b : bindings.values()) {
            if (b.state() == state && b.civ() == civ) {
                n++;
            }
        }
        return n;
    }

    public Map<String, RelicBinding> view() {
        return Collections.unmodifiableMap(bindings);
    }

    /** 读档用：直接放入（不检查占用）。 */
    public void restore(String relicId, RelicBinding binding) {
        bindings.put(relicId, binding);
    }

    public void copyFrom(RelicBook other) {
        bindings.clear();
        bindings.putAll(other.bindings);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RelicBook b && bindings.equals(b.bindings);
    }

    @Override
    public int hashCode() {
        return bindings.hashCode();
    }
}
