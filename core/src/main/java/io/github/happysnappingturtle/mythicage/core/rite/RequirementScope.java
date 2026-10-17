package io.github.happysnappingturtle.mythicage.core.rite;

/**
 * 建筑条件的范围（T11a 设计文档 §4.3，U9）：玩家的（如自己神坛的神庙等级）或村庄的（举行地村庄由村民建成的建筑）。
 * 晋升两类都查；立祀只查村庄的；改奉都不查。
 */
public enum RequirementScope {
    PLAYER, VILLAGE
}
