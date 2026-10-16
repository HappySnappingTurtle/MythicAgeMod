package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 祈祷位预留（T10 设计文档 §4）：按神坛保存「村民 → 祈祷位 + 过期刻」。同一位置只给一个村民；
 * 村民走路和祈祷期间每 {@link #REPORT_INTERVAL} 刻报到一次，{@link #EXPIRE} 刻没报到就自动释放。
 * 只在内存里：重启后村民重新选位。
 */
final class PrayerSlots {
    static final int REPORT_INTERVAL = 20;
    static final int EXPIRE = 40;

    /**
     * @param arrived  已到达祈祷位（跪拜中）
     * @param counted  计入产出：到达、且家到祈祷位的距离满足朝圣距离
     * @param tooClose 到达但离家太近（面板「离家太近」）
     */
    record Claim(UUID villager, BlockPos slot, long expires, boolean arrived, boolean counted, boolean tooClose) {
        boolean live(long now) {
            return expires >= now;
        }
    }

    private final Map<GlobalPos, Map<UUID, Claim>> byAltar = new HashMap<>();

    /**
     * 为村民在这座神坛预留一个祈祷位：已有预留且位置仍可用时沿用；否则取第一个空位。
     * slots 是神坛当前可用的祈祷位（按优先顺序）。
     */
    Optional<BlockPos> claim(GlobalPos altar, List<BlockPos> slots, UUID villager, long now) {
        Map<UUID, Claim> claims = byAltar.computeIfAbsent(altar, k -> new LinkedHashMap<>());
        claims.values().removeIf(c -> !c.live(now) || !slots.contains(c.slot()));
        Claim mine = claims.get(villager);
        if (mine != null) {
            claims.put(villager, new Claim(villager, mine.slot(), now + EXPIRE, mine.arrived(), mine.counted(), mine.tooClose()));
            return Optional.of(mine.slot());
        }
        for (BlockPos slot : slots) {
            if (claims.values().stream().noneMatch(c -> c.slot().equals(slot))) {
                claims.put(villager, new Claim(villager, slot, now + EXPIRE, false, false, false));
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    /** 报到：续期并更新状态。预留已被释放时返回 false（村民应停止）。 */
    boolean report(GlobalPos altar, UUID villager, long now, boolean arrived, boolean counted, boolean tooClose) {
        Map<UUID, Claim> claims = byAltar.get(altar);
        Claim c = claims == null ? null : claims.get(villager);
        if (c == null) {
            return false;
        }
        claims.put(villager, new Claim(villager, c.slot(), now + EXPIRE, arrived, counted, tooClose));
        return true;
    }

    /** 直接放入一条预留（测试用）。 */
    void put(GlobalPos altar, Claim claim) {
        byAltar.computeIfAbsent(altar, k -> new LinkedHashMap<>()).put(claim.villager(), claim);
    }

    void release(GlobalPos altar, UUID villager) {
        Map<UUID, Claim> claims = byAltar.get(altar);
        if (claims != null) {
            claims.remove(villager);
            if (claims.isEmpty()) {
                byAltar.remove(altar);
            }
        }
    }

    void releaseAll(GlobalPos altar) {
        byAltar.remove(altar);
    }

    /** 未过期的预留。 */
    List<Claim> live(GlobalPos altar, long now) {
        Map<UUID, Claim> claims = byAltar.get(altar);
        if (claims == null) {
            return List.of();
        }
        List<Claim> out = new ArrayList<>();
        for (Claim c : claims.values()) {
            if (c.live(now)) {
                out.add(c);
            }
        }
        return out;
    }

    /** 有未过期预留的神坛（采样只遍历它们）。顺带清掉过期的预留。 */
    List<GlobalPos> altars(long now) {
        byAltar.values().forEach(m -> m.values().removeIf(c -> !c.live(now)));
        byAltar.values().removeIf(Map::isEmpty);
        return new ArrayList<>(byAltar.keySet());
    }

    void clear() {
        byAltar.clear();
    }
}
