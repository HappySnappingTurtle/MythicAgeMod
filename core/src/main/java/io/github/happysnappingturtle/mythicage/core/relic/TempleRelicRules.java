package io.github.happysnappingturtle.mythicage.core.relic;

/**
 * 村庄神庙的圣物供奉判定（GA-13 §2.5）：每位供奉者在每个村子有自己的供奉位（数量按 {@link RelicSlots#offerSlots}），
 * 各村互不占用；同一件圣物同一时刻只能在一处（绑定表管）。按「不是供奉者 → 文明不对 → 已在别处 → 这里满了」的顺序拒绝。
 */
public final class TempleRelicRules {
    public enum Refusal { NONE, NOT_DEVOTEE, WRONG_CIV, OCCUPIED, FULL }

    private TempleRelicRules() {
    }

    /**
     * @param devotee  是这个村子的供奉者
     * @param sameCiv  圣物属于神庙的文明
     * @param bound    这件圣物已经戴着或供奉在别处
     * @param usedHere 这位玩家在这个村子已经供奉的件数
     * @param cap      这位玩家的供奉位数（按时代）
     */
    public static Refusal check(boolean devotee, boolean sameCiv, boolean bound, int usedHere, int cap) {
        if (!devotee) {
            return Refusal.NOT_DEVOTEE;
        }
        if (!sameCiv) {
            return Refusal.WRONG_CIV;
        }
        if (bound) {
            return Refusal.OCCUPIED;
        }
        return usedHere >= cap ? Refusal.FULL : Refusal.NONE;
    }
}
