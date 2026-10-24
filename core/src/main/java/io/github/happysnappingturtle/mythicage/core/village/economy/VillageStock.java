package io.github.happysnappingturtle.mythicage.core.village.economy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 村庄逻辑库存（T19 设计文档 §4，V1 §5.1）：物品 → 数量，另有托管表（已被工作单、项目、酒瓮锁定的材料）。
 * <ul>
 *   <li>容量只算库存（不含托管），所以退回托管不受容量限制；</li>
 *   <li>托管要么全部扣下、要么一件不扣；</li>
 *   <li>工作单不存进度：读档时 {@link #releaseJobEscrows()} 把 {@code job:} 开头的托管全部退回，重启既不丢也不复制。</li>
 * </ul>
 * 物品用字符串 id（如 {@code mythic_age:olive}），core 不依赖注册表。
 */
public final class VillageStock {
    public static final String JOB_PREFIX = "job:";

    private final Map<String, Integer> items = new TreeMap<>();
    private final Map<String, Map<String, Integer>> escrow = new LinkedHashMap<>();
    private int capacity;

    public VillageStock(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public int capacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    /** 库存件数合计（不含托管）。 */
    public int total() {
        int t = 0;
        for (int n : items.values()) {
            t += n;
        }
        return t;
    }

    public int free() {
        return Math.max(0, capacity - total());
    }

    public int count(String item) {
        return items.getOrDefault(item, 0);
    }

    /** 托管中的件数（全部托管合计）。 */
    public int escrowed(String item) {
        int t = 0;
        for (Map<String, Integer> m : escrow.values()) {
            t += m.getOrDefault(item, 0);
        }
        return t;
    }

    public Map<String, Integer> items() {
        return Collections.unmodifiableMap(items);
    }

    public Map<String, Map<String, Integer>> escrows() {
        Map<String, Map<String, Integer>> r = new LinkedHashMap<>();
        escrow.forEach((k, v) -> r.put(k, Map.copyOf(v)));
        return Collections.unmodifiableMap(r);
    }

    public boolean hasEscrow(String id) {
        return escrow.containsKey(id);
    }

    /** 存入：容量不够就只收能收的。返回实收数。 */
    public int deposit(String item, int n) {
        if (n <= 0) {
            return 0;
        }
        int accepted = Math.min(n, free());
        if (accepted > 0) {
            items.merge(item, accepted, Integer::sum);
        }
        return accepted;
    }

    /** 不受容量限制地放入（读档、退回托管用）。 */
    public void putUnchecked(String item, int n) {
        if (n > 0) {
            items.merge(item, n, Integer::sum);
        }
    }

    /** 读档用：直接恢复一笔托管。 */
    public void putEscrowUnchecked(String id, Map<String, Integer> what) {
        Map<String, Integer> m = escrow.computeIfAbsent(id, k -> new TreeMap<>());
        what.forEach((k, v) -> {
            if (v > 0) {
                m.merge(k, v, Integer::sum);
            }
        });
    }

    /** 托管：全部够才扣，否则一件都不扣。同一 id 再次托管会追加。 */
    public boolean reserve(String id, Map<String, Integer> what) {
        for (Map.Entry<String, Integer> e : what.entrySet()) {
            if (e.getValue() > 0 && count(e.getKey()) < e.getValue()) {
                return false;
            }
        }
        Map<String, Integer> m = escrow.computeIfAbsent(id, k -> new TreeMap<>());
        what.forEach((k, v) -> {
            if (v > 0) {
                remove(k, v);
                m.merge(k, v, Integer::sum);
            }
        });
        return true;
    }

    /** 退回托管（工作单作废、项目取消）。返回退回的物品。 */
    public Map<String, Integer> release(String id) {
        Map<String, Integer> m = escrow.remove(id);
        if (m == null) {
            return Map.of();
        }
        m.forEach(this::putUnchecked);
        return m;
    }

    /** 消耗托管（工作单完成、施工段落块）。返回被消耗的物品。 */
    public Map<String, Integer> consume(String id) {
        Map<String, Integer> m = escrow.remove(id);
        return m == null ? Map.of() : m;
    }

    /** 消耗一笔托管里的一部分（施工按段消耗）；不够时消耗现有的全部。返回实际消耗。 */
    public int consumePart(String id, String item, int n) {
        Map<String, Integer> m = escrow.get(id);
        if (m == null || n <= 0) {
            return 0;
        }
        int have = m.getOrDefault(item, 0);
        int used = Math.min(have, n);
        if (used == have) {
            m.remove(item);
        } else {
            m.put(item, have - used);
        }
        if (m.isEmpty()) {
            escrow.remove(id);
        }
        return used;
    }

    /** 直接取出（卖给玩家、扣口粮）：最多取到 {@code count − keep}。返回实取数。 */
    public int take(String item, int n, int keep) {
        int can = Math.max(0, count(item) - Math.max(0, keep));
        int taken = Math.min(can, Math.max(0, n));
        if (taken > 0) {
            remove(item, taken);
        }
        return taken;
    }

    /** 读档后调用：工作单的托管全部退回（工作单不存进度）。返回退回的笔数。 */
    public int releaseJobEscrows() {
        int n = 0;
        for (String id : java.util.List.copyOf(escrow.keySet())) {
            if (id.startsWith(JOB_PREFIX)) {
                release(id);
                n++;
            }
        }
        return n;
    }

    private void remove(String item, int n) {
        int left = count(item) - n;
        if (left > 0) {
            items.put(item, left);
        } else {
            items.remove(item);
        }
    }
}
