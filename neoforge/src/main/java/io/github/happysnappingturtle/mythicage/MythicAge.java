package io.github.happysnappingturtle.mythicage;

import com.mojang.logging.LogUtils;
import io.github.happysnappingturtle.mythicage.config.ConfigCommand;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.player.ModAttachments;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.MythDataCommand;
import io.github.happysnappingturtle.mythicage.player.MythNetwork;
import io.github.happysnappingturtle.mythicage.runtime.PlayerActivity;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeCommand;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeDriver;
import io.github.happysnappingturtle.mythicage.runtime.TestProducers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Mythic Age 的 NeoForge 入口。领域逻辑在 core 子工程中，这里只负责把它接到加载器上。
 */
@Mod(MythicAgeIds.MOD_ID)
public final class MythicAge {
    public static final Logger LOGGER = LogUtils.getLogger();

    public MythicAge(IEventBus modBus, ModContainer container) {
        LOGGER.info("Mythic Age loading ({} {})", MythicAgeIds.MOD_ID, container.getModInfo().getVersion());
        ModAttachments.register(modBus);
        modBus.addListener(MythNetwork::onRegisterPayloads);

        IEventBus game = NeoForge.EVENT_BUS;
        // 配置必须先于其他系统加载：其他系统在同一事件的普通优先级中注册钩子
        game.addListener(EventPriority.HIGHEST, ModConfigs::onServerAboutToStart);
        game.addListener(ConfigCommand::onRegisterCommands);
        game.addListener(MythDataCommand::onRegisterCommands);
        game.addListener(MythData::onServerTickPost);
        game.addListener(MythData::onLoggedIn);
        game.addListener(MythData::onRespawn);
        game.addListener(MythData::onChangedDimension);

        // 运行时钟（T5）：钩子在普通优先级注册到 ModConfigs 刚创建的 holder 上
        game.addListener(RuntimeDriver::onServerAboutToStart);
        game.addListener(RuntimeDriver::onServerStopped);
        game.addListener(RuntimeDriver::onServerTickPost);
        game.addListener(RuntimeCommand::onRegisterCommands);
        PlayerActivity.register(game);
        TestProducers.register();
    }
}
