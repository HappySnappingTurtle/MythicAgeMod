package io.github.happysnappingturtle.mythicage.core.greek.temple;

/**
 * 一次神庙扫描的统计结果（T8a 设计文档 §3.2、修订 01 §2.2）。
 *
 * @param columns           合格的柱（6 连通一组，最长竖直段达到门槛）
 * @param raisedColumns     其中柱脚高出神坛所在地面至少 1 格的柱（站在台基上）
 * @param roofCovered       柱子围出的矩形中有屋顶的列数
 * @param roofArea          柱子围出的矩形面积；任一边小于最小边长时为 0（没有围出空间）
 * @param openAltar         神坛正上方没有遮盖
 * @param platformReachable 台基可以走上去（从扫描体边缘出发，每步高差 ≤ 1）
 * @param litBraziers       点燃的圣火盆
 * @param statues           神像（两格高的只算下半）
 * @param pediments         三角楣石
 * @param marbleFloor       能走到的大理石地面
 */
public record TempleSurvey(int columns, int raisedColumns, int roofCovered, int roofArea, boolean openAltar,
                           boolean platformReachable, int litBraziers, int statues, int pediments, int marbleFloor) {
    public static final TempleSurvey EMPTY = new TempleSurvey(0, 0, 0, 0, true, false, 0, 0, 0, 0);
}
