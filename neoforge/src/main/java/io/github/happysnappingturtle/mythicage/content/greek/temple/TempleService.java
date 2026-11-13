package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.Optional;

/**
 * 希腊神庙规则的入口（T8a；GA-13 起神庙归村庄）：神庙等级与系数表（{@code temple/greek.json}）、旧祭坛这一种神庙的钩子。
 * 原来的玩家神坛结构扫描（扫描队列、置脏、兜底重扫、检视等待）随玩家祭坛一起去掉；村庄神庙的等级看卫城，完好度看损坏记录。
 */
public final class TempleService {
    /** 旧祭坛之间垂直方向的最小距离（神庙种类登记要一个值；旧祭坛不再检查间距）。 */
    public static final int VERTICAL_SPACING = 12;

    /** 旧祭坛：有主人的一加载就迁移（{@link LegacyAltars#migrate}），没有主人的留着当装饰。 */
    public static final ShrineKind.Hooks HOOKS = new ShrineKind.Hooks() {
        @Override
        public void onBound(ServerLevel level, BlockPos pos, Optional<ServerPlayer> placer) {
            LegacyAltars.migrate(level, pos);
        }

        @Override
        public boolean keepUnowned() {
            return true;
        }

        @Override
        public void onRemoved(ServerLevel level, GlobalPos key) {
            PrayerService.resetForTest(key);
        }
    };

    private TempleService() {
    }

    public static TempleRules rules() {
        return TempleRulesLoader.greek().rules();
    }

    /** 开服时注销旧的玩家祭坛台账（{@code greek_temple:…}）：它们按快照还会记账，神庙归村庄后不该再产恩宠。 */
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        RuntimeLedgers ledgers = RuntimeLedgers.get(server);
        ledgers.ids().stream().filter(id -> id.value().startsWith(PrayerService.PROBE_TYPE + ":")).toList().forEach(ledgers::remove);
    }
}
