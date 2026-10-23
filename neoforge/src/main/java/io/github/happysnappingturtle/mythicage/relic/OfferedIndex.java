package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.relic.RelicCard;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 已加载神庙的供奉索引（T16 设计文档 §1）：神庙位置 → 主人、文明、架上的圣物卡。
 * 范围类效果（阿喀琉斯之甲、皮格马利翁雕像的供奉）只在神庙所在区块加载时结算（06 号 §0.7），所以只索引已加载的神庙；
 * 方块实体加载、卸载、供奉与取出时更新。只在服务器线程访问。
 */
public final class OfferedIndex {
    public record Entry(GlobalPos pos, UUID owner, Civilization civ, List<RelicCard> cards) {
    }

    private static final Map<GlobalPos, Entry> ENTRIES = new LinkedHashMap<>();

    private OfferedIndex() {
    }

    public static void update(ServerLevel level, ShrineBlockEntity be) {
        GlobalPos key = GlobalPos.of(level.dimension(), be.getBlockPos().immutable());
        if (be.owner().isEmpty() || be.relics().isEmpty()) {
            ENTRIES.remove(key);
            return;
        }
        Set<RelicCard> cards = new LinkedHashSet<>();
        for (ItemStack s : be.relics().stacks()) {
            RelicCards.of(s).ifPresent(cards::add);
        }
        if (cards.isEmpty()) {
            ENTRIES.remove(key);
            return;
        }
        ENTRIES.put(key, new Entry(key, be.owner().get(), be.kind().civ(), List.copyOf(cards)));
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        ENTRIES.remove(GlobalPos.of(level.dimension(), pos));
    }

    public static Collection<Entry> all() {
        return Collections.unmodifiableCollection(ENTRIES.values());
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
