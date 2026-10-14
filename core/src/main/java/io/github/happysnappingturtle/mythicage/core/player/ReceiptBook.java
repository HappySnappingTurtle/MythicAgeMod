package io.github.happysnappingturtle.mythicage.core.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 事务收据（T4 设计文档 §3.1）。两个范围互不影响：
 * UNIQUE 永久保存（唯一奖励），REQUEST 只保留最近 {@link #REQUEST_CAPACITY} 条（防止重复点击与网络重试）。
 */
public final class ReceiptBook {
    public static final int REQUEST_CAPACITY = 256;

    public enum Scope { UNIQUE, REQUEST }

    private final LinkedHashSet<String> unique = new LinkedHashSet<>();
    private final LinkedHashSet<String> requests = new LinkedHashSet<>();

    public boolean contains(Scope scope, String id) {
        return set(scope).contains(id);
    }

    public void record(Scope scope, String id) {
        LinkedHashSet<String> set = set(scope);
        set.remove(id); // 重新写入时移到最新
        set.add(id);
        if (scope == Scope.REQUEST) {
            Iterator<String> it = set.iterator();
            while (set.size() > REQUEST_CAPACITY && it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    /** 按写入顺序（从旧到新）列出某个范围的收据。 */
    public List<String> list(Scope scope) {
        return new ArrayList<>(set(scope));
    }

    public int size(Scope scope) {
        return set(scope).size();
    }

    ReceiptBook copy() {
        ReceiptBook c = new ReceiptBook();
        c.unique.addAll(unique);
        c.requests.addAll(requests);
        return c;
    }

    private LinkedHashSet<String> set(Scope scope) {
        return scope == Scope.UNIQUE ? unique : requests;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReceiptBook other
                && List.copyOf(unique).equals(List.copyOf(other.unique))
                && List.copyOf(requests).equals(List.copyOf(other.requests));
    }

    @Override
    public int hashCode() {
        return Objects.hash(List.copyOf(unique), List.copyOf(requests));
    }
}
