package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.BelieverRules;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageResolver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * 信徒的事件与钩子（T10 设计文档 §6、§7、§2 D2）：信徒判定、信仰状态同步、补货上限（F7）。
 */
public final class BelieverEvents {
    private BelieverEvents() {
    }

    /** 信徒 = 已登记的希腊村庄的成年成员（村民 → 聚会点钟 → 村庄）。 */
    public static Optional<VillageRegistry.Village> greekVillage(Villager v) {
        if (v.isBaby()) {
            return Optional.empty();
        }
        return VillageResolver.villageOf(v).filter(village -> village.civ().equals(Civilization.GREEK.id()));
    }

    public static boolean isBeliever(Villager v) {
        return greekVillage(v).isPresent();
    }

    /** 状态变化时发给追踪该村民的玩家。 */
    static void sync(Villager v, PrayerRecord rec, boolean believer, boolean praying) {
        int flags = BelieverStatePayload.flags(believer, praying);
        if (flags != rec.syncedFlags) {
            rec.syncedFlags = flags;
            PacketDistributor.sendToPlayersTrackingEntity(v, new BelieverStatePayload(v.getId(), (byte) flags));
        }
    }

    /** 开始追踪一个村民时补发它的状态（默认状态不发）。 */
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Villager v && event.getEntity() instanceof ServerPlayer player
                && v.level() instanceof ServerLevel) {
            PrayerRecord rec = PrayAtAltar.record(v);
            int before = rec.syncedFlags;
            // 信徒身份顺便刷新（夜里村民不跑祈祷行为，重启后要靠这里补上）；变化时广播，新玩家已在追踪列表里
            sync(v, rec, isBeliever(v), (before & BelieverStatePayload.PRAYING) != 0);
            if (rec.syncedFlags == before && before != 0) {
                PacketDistributor.sendToPlayer(player, new BelieverStatePayload(v.getId(), (byte) before));
            }
        }
    }

    /** 今天（MC 日）计入过祈祷的有职业村民。 */
    public static boolean prayedToday(Villager v) {
        if (!(v.level() instanceof ServerLevel level)) {
            return false;
        }
        VillagerProfession p = v.getVillagerData().getProfession();
        if (p == VillagerProfession.NONE || p == VillagerProfession.NITWIT) {
            return false;
        }
        return PrayAtAltar.record(v).prayedOn(MythDays.today(level.getServer()));
    }

    /** F7：今天的补货次数上限（供 VillagerRestockMixin 使用）。 */
    public static int restockLimit(Villager v) {
        return BelieverRules.restockLimit(prayedToday(v), ModConfigs.worldRules().current().greek().prayer().restockLimit());
    }
}
