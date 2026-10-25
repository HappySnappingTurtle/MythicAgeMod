package io.github.happysnappingturtle.mythicage.village.soldier;

import java.util.UUID;

/**
 * 士兵投出、落在地上的弹药（T20 设计文档 §9.2）：警戒结束后由远程兵捡回，按完好率回村库；玩家捡不起来。
 */
public interface SoldierAmmo {
    UUID ammoVillage();

    String ammoItem();

    /** 已经落地（插在地上）。 */
    boolean landed();
}
