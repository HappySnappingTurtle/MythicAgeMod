package io.github.happysnappingturtle.mythicage.village.work;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采果器（T19 设计文档 §5.1 {@code pick_fruit}）：某种果树 / 果架在锚点附近哪些果子熟了、采下来得到什么。
 * 各文明在自己的内容包里登记（希腊：橄榄树、葡萄架），文明数据的 {@code fruits.<锚点>.harvester} 按 id 引用。
 * 只认真实的成熟状态：没熟不采，同一成熟状态只结算一次（采完回到未成熟）。
 */
public interface FruitHarvester {
    /** 锚点附近已经熟了的果子（方块坐标）。 */
    List<BlockPos> ripe(ServerLevel level, BlockPos anchor);

    /** 采下这些（逐个再确认一次仍然成熟），回到未成熟。返回物品 id → 数量。 */
    Map<String, Integer> harvest(ServerLevel level, List<BlockPos> ripe);

    Map<String, FruitHarvester> REGISTRY = new HashMap<>();

    static void register(String id, FruitHarvester h) {
        REGISTRY.put(id, h);
    }
}
