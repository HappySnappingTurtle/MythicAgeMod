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
 * 已加载神庙的供奉索引（T16 设计文档 §1）：神庙位置 → 主人、文明、架上的圣物卡。GA-13 起村庄神庙也进来：
 * 一座村庄神庙有多位供奉者，每人一条（键 {@code village|<村>|<玩家>}，位置是 48 格的圆心）；玩家神坛的键是 {@code shrine|<维度>|<坐标>}。
 * 范围类效果（阿喀琉斯之甲、皮格马利翁雕像的供奉）只在神庙所在区块加载时结算（06 号 §0.7），所以只索引已加载的神庙；
 * 方块实体加载、卸载、供奉与取出时更新。只在服务器线程访问。
 */
public final class OfferedIndex {
    public record Entry(GlobalPos pos, UUID owner, Civilization civ, List<RelicCard> cards) {
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static String shrineKey(GlobalPos pos) {
        return "shrine|" + pos.dimension().location() + "|" + pos.pos().asLong();
    }

    static String villageKey(UUID village, UUID owner) {
        return "village|" + village + "|" + owner;
    }

    /** 村庄神庙里某位供奉者的圣物；cards 为空时去掉这条。 */
    public static void putVillage(UUID village, UUID owner, GlobalPos center, Civilization civ, List<RelicCard> cards) {
        String key = villageKey(village, owner);
        if (cards.isEmpty()) {
            ENTRIES.remove(key);
        } else {
            ENTRIES.put(key, new Entry(center, owner, civ, List.copyOf(cards)));
        }
    }

    /** 去掉某个村子的全部条目（神庙所在区块卸载、村子没了）。 */
    public static void removeVillage(UUID village) {
        String prefix = "village|" + village + "|";
        ENTRIES.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private OfferedIndex() {
    }

    public static void update(ServerLevel level, ShrineBlockEntity be) {
        GlobalPos pos = GlobalPos.of(level.dimension(), be.getBlockPos().immutable());
        String key = shrineKey(pos);
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
        ENTRIES.put(key, new Entry(pos, be.owner().get(), be.kind().civ(), List.copyOf(cards)));
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        ENTRIES.remove(shrineKey(GlobalPos.of(level.dimension(), pos)));
    }

    public static Collection<Entry> all() {
        return Collections.unmodifiableCollection(ENTRIES.values());
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
