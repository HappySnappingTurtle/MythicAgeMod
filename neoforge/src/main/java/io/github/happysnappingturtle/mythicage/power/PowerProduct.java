package io.github.happysnappingturtle.mythicage.power;

/**
 * 有退场表现的神力产物（哨兵石像逐尊碎化等）：被新的一组替换、或临时实体超额时，
 * 由 {@link TransientEntities} 调用 {@link #retire}，而不是直接移除。实现方负责在表现结束后自行移除。
 */
public interface PowerProduct {
    /** @param delayTicks 同一组里排在第几尊就延后多少刻（逐尊退场） */
    void retire(int delayTicks);
}
