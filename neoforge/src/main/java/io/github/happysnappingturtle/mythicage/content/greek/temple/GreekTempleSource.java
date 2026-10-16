package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.faith.GraceSource;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 希腊的恩宠来源：神庙（T10b 设计文档 §6）。信仰面板上每座自己的希腊神坛一行：
 * 神庙等级、近 7 日信徒、今日已得、预计日产出、所在村庄。信徒祈祷（T10）的结算也挂在这个来源上；
 * 数字与神坛检视界面取自同一份 {@link PrayerService.Stats}。
 */
public final class GreekTempleSource implements GraceSource {
    public static final GreekTempleSource INSTANCE = new GreekTempleSource();
    private GreekTempleSource() {
    }

    @Override
    public Civilization civ() {
        return Civilization.GREEK;
    }

    @Override
    public String id() {
        return "greek:temple";
    }

    /** 预计日产出（估算，不是发放承诺）：按近 7 日信徒、祈祷位与神庙系数（T10 设计文档 §5.2）。 */
    public static double estimate(GreekAltarBlockEntity be, TempleRules rules) {
        return PrayerService.estimate(be, rules);
    }

    @Override
    public List<Row> rows(ServerPlayer viewer) {
        List<Row> out = new ArrayList<>();
        TempleRules rules = TempleService.rules();
        for (ShrineRegistry.Entry e : ShrineRegistry.get(viewer.server).ownedBy(viewer.getUUID(), Civilization.GREEK)) {
            if (!e.kind().equals(GreekTemple.ALTAR_KIND.id())) {
                continue;
            }
            ServerLevel level = viewer.server.getLevel(e.pos().dimension());
            Optional<UUID> village = level == null ? Optional.empty()
                    : VillageService.villageAt(level, e.pos().pos(), VillageService.RANGE).map(VillageRegistry.Village::id);
            Component name = GreekTemple.GREEK_ALTAR.get().getName();
            if (level != null && level.isLoaded(e.pos().pos())
                    && level.getBlockEntity(e.pos().pos()) instanceof GreekAltarBlockEntity be && be.hasResult()) {
                TempleRules.Tier tier = rules.tier(be.verdict(rules).grantedLevel());
                PrayerService.Stats st = PrayerService.stats(be, rules);
                out.add(new Row(name, e.pos(), Component.translatable("mythic_age.grace.greek_temple.status",
                        Component.translatable("mythic_age.temple.tier." + tier.name()), st.believers(),
                        String.format(java.util.Locale.ROOT, "%.1f", st.today())), st.estimate(), village));
            } else {
                out.add(new Row(name, e.pos(), Component.translatable("mythic_age.grace.unloaded"), 0, village));
            }
        }
        return out;
    }
}
