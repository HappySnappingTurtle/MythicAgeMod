package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.content.greek.GreekArchitecture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.village.quest.VillageQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 卫城的完好度（GA-13 §2.6）：模板里的关键方块（柱、屋顶瓦、火盆、神像、三角楣石、祭坛）有多少格，
 * 村子的建筑损坏记录（T21 {@code BuildingDamage}）里这座卫城缺了多少格关键方块。{@code 完好度 = 1 − 缺 / 总}。
 * <p>
 * 损坏的格走现有的修缮流程：发修复委托，没人接就到期转村民自修（T21 §6），修好一格就从损坏记录里去掉，完好度随之回升。
 */
public final class TempleIntegrity {
    /** 模板 id → 关键方块格数。 */
    private static final Map<String, Integer> TOTAL = new ConcurrentHashMap<>();

    private TempleIntegrity() {
    }

    public static void clearCache() {
        TOTAL.clear();
    }

    /** 关键方块：柱、屋顶瓦、火盆、神像、三角楣石、村庄祭坛。 */
    public static boolean key(BlockState st) {
        return st.is(GreekTemple.GREEK_COLUMN) || st.is(GreekTemple.BRAZIERS) || st.is(GreekTemple.TEMPLE_STATUES)
                || st.is(GreekTemple.TEMPLE_PEDIMENTS) || st.is(GreekVillageBlocks.VILLAGE_ALTAR.get())
                || st.is(GreekArchitecture.TERRACOTTA_ROOF_TILES.get()) || st.is(GreekArchitecture.TERRACOTTA_ROOF_STAIRS.get())
                || st.is(GreekArchitecture.TERRACOTTA_ROOF_SLAB.get());
    }

    /** 这个模板里关键方块的格数；模板读不到为 0。 */
    public static int total(MinecraftServer server, BuildingSpec spec) {
        return TOTAL.computeIfAbsent(spec.template(), id -> {
            Optional<StructureTemplate> t = server.getStructureManager().get(ResourceLocation.parse(id));
            if (t.isEmpty()) {
                return 0;
            }
            Set<Block> blocks = new LinkedHashSet<>();
            for (Block b : BuiltInRegistries.BLOCK) {
                if (key(b.defaultBlockState())) {
                    blocks.add(b);
                }
            }
            int n = 0;
            StructurePlaceSettings s = new StructurePlaceSettings();
            for (Block b : blocks) {
                n += t.get().filterBlocks(BlockPos.ZERO, s, b).size();
            }
            return n;
        });
    }

    /** 损坏记录里这座卫城缺了几格关键方块。 */
    public static int missing(MinecraftServer server, UUID village, Box box) {
        return VillageQuests.get(server).peek(village)
                .map(b -> b.damage.get(VillageQuests.Damage.key(box)))
                .map(d -> (int) d.cells.values().stream().filter(c -> key(c.state)).count())
                .orElse(0);
    }

    /** 完好度 0–1；没有关键方块的模板按完好算。 */
    public static double of(MinecraftServer server, UUID village, BuildingSpec spec, Box box) {
        int total = total(server, spec);
        if (total <= 0) {
            return 1;
        }
        return Math.max(0, total - missing(server, village, box)) / (double) total;
    }
}
