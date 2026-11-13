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
 * 希腊的恩宠来源：神庙（T10b 设计文档 §6；GA-13 神庙归村庄）。信仰面板上每个供奉的村子一行：
 * 神庙等级、近 7 日信徒、今天在这里已得、预计日产出；数字与神庙检视取自同一份 {@code TemplePrayer.Stats}。
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

    /** GA-13：每个供奉的村子一行：村名、神庙等级、近 7 日信徒、你今天在这里已得、预计日产。 */
    @Override
    public List<Row> rows(ServerPlayer viewer) {
        List<Row> out = new ArrayList<>();
        for (UUID v : io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.devotedVillages(viewer.server, viewer.getUUID())) {
            Optional<VillageRegistry.Village> village = VillageRegistry.get(viewer.server).byId(v);
            Optional<io.github.happysnappingturtle.mythicage.village.temple.VillageTemple> t =
                    io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.of(viewer.server, v);
            if (village.isEmpty() || t.isEmpty()) {
                continue;
            }
            Component name = io.github.happysnappingturtle.mythicage.village.VillageService.title(village.get());
            io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.Stats st =
                    io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.stats(viewer.server, t.get(), viewer.getUUID());
            out.add(new Row(name, t.get().altar(), Component.translatable("mythic_age.grace.greek_temple.status",
                    Component.translatable("mythic_age.temple.tier." + t.get().tierName()), st.believers(),
                    String.format(java.util.Locale.ROOT, "%.1f", st.today())), st.estimate(), Optional.of(v)));
        }
        return out;
    }
}
