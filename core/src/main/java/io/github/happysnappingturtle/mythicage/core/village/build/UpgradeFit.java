package io.github.happysnappingturtle.mythicage.core.village.build;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

import java.util.List;
import java.util.function.Predicate;

/**
 * 原地升级放不放得下（GA-11 §2.4）：新模板按白模坐标系（{@code frame}）对齐旧建筑、沿用旧建筑的转向，
 * 门要朝同一个方向（升级后门仍对着原来的路）；新占地比旧占地多出来的格子不能压到别的建筑、别的地块，地面要能盖房。
 * 村庄规划没有记道路，路面是可清掉的天然方块，这里不单独判断。纯函数。
 */
public final class UpgradeFit {
    /** 放得下。 */
    public static final String OK = "";
    /** 门朝向和旧建筑不同。 */
    public static final String DOOR = "door";
    /** 多出来的格子压到别的建筑或地块。 */
    public static final String OCCUPIED = "occupied";
    /** 多出来的格子下面是水、悬空或陡坡。 */
    public static final String GROUND = "ground";

    /** 规划里的一块地块：种类与范围。 */
    public record PlotArea(String kind, Box box) {
    }

    private UpgradeFit() {
    }

    /** 新模板在旧建筑原位的位置。 */
    public static BuildPlacement.Placed place(BuildingSpec next, BuildingSpec old, BuildPlacement.Placed oldAt) {
        return BuildPlacement.replacing(next, old, oldAt);
    }

    /**
     * @param others    规划里其他已建建筑的包围盒（可以含旧建筑自己，会按相等跳过）
     * @param plots     规划里的地块；新旧建筑本身能用的地块种类不算占用（如神庙扩建地块）
     * @param badGround 多出来的一列（世界 x、z，y 为地面层）能不能盖房：返回 true 表示不能
     */
    public static String check(BuildingSpec next, BuildingSpec old, BuildPlacement.Placed oldAt, List<Box> others,
                               List<PlotArea> plots, Predicate<Pos> badGround) {
        if (!next.doorFacing().equals(old.doorFacing())) {
            return DOOR;
        }
        Box nb = place(next, old, oldAt).box();
        Box ob = oldAt.box();
        int ground = oldAt.origin().y() + old.floorY();
        for (int x = nb.minX(); x <= nb.maxX(); x++) {
            for (int z = nb.minZ(); z <= nb.maxZ(); z++) {
                if (ob.containsXZ(x, z)) {
                    continue;
                }
                for (Box o : others) {
                    if (!o.equals(ob) && o.containsXZ(x, z)) {
                        return OCCUPIED;
                    }
                }
                for (PlotArea p : plots) {
                    if (!next.plotKinds().contains(p.kind()) && !old.plotKinds().contains(p.kind()) && p.box().containsXZ(x, z)) {
                        return OCCUPIED;
                    }
                }
                if (badGround.test(new Pos(x, ground, z))) {
                    return GROUND;
                }
            }
        }
        return OK;
    }
}
