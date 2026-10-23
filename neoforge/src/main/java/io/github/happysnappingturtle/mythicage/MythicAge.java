package io.github.happysnappingturtle.mythicage;

import com.mojang.logging.LogUtils;
import io.github.happysnappingturtle.mythicage.config.ConfigCommand;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.ModCreativeTabs;
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekArchitecture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekFoods;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.GreekWood;
import io.github.happysnappingturtle.mythicage.content.greek.agri.GreekAgricultureEvents;
import io.github.happysnappingturtle.mythicage.content.greek.guardian.GreekGuardian;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.GreekPrayer;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleRulesLoader;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.datagen.ModDataGen;
import io.github.happysnappingturtle.mythicage.entity.EntityDevCommand;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.faith.FaithProfiles;
import io.github.happysnappingturtle.mythicage.guardian.GuardianCommand;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.player.ModAttachments;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.MythDataCommand;
import io.github.happysnappingturtle.mythicage.player.MythNetwork;
import io.github.happysnappingturtle.mythicage.recipe.ModRecipes;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.rig.RigRegistry;
import io.github.happysnappingturtle.mythicage.runtime.PlayerActivity;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeCommand;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeDriver;
import io.github.happysnappingturtle.mythicage.runtime.TestProducers;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import io.github.happysnappingturtle.mythicage.village.PantheonLoader;
import io.github.happysnappingturtle.mythicage.village.VillageCommand;
import io.github.happysnappingturtle.mythicage.village.VillageService;
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
        // 内容声明（T6）：先触发各内容类的类加载，再把 DeferredRegister 挂到 mod 总线
        GreekMaterials.init();
        GreekAgriculture.init();
        GreekWood.init();
        GreekFoods.init();
        GreekTemple.init();
        GreekArchitecture.init();
        io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks.init();
        io.github.happysnappingturtle.mythicage.content.greek.GreekTrees.init();
        io.github.happysnappingturtle.mythicage.village.VillageGuide.init();
        io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons.init(modBus);
        io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.init(modBus);
        GreekGuardian.init();
        io.github.happysnappingturtle.mythicage.statue.PatronStatues.init(modBus, NeoForge.EVENT_BUS);
        io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers.init(modBus, NeoForge.EVENT_BUS);
        GreekAgricultureEvents.register(modBus);
        ModCreativeTabs.init();
        ModRecipes.init();
        ModEntities.init(modBus);
        ModRegistries.register(modBus);
        io.github.happysnappingturtle.mythicage.village.gen.ModStructures.register(modBus);
        io.github.happysnappingturtle.mythicage.advancement.MythEvents.register(modBus);
        io.github.happysnappingturtle.mythicage.power.PowerItems.register(modBus);
        io.github.happysnappingturtle.mythicage.power.PowerRegistries.register(modBus);
        modBus.addListener(ModDataGen::gather);
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
        game.addListener(ModRecipes::onServerTickPre);

        // 实体与美术管线（T7）
        game.addListener(RigRegistry::onAddReloadListeners);
        game.addListener(EntityDevCommand::onRegisterCommands);
        game.addListener(io.github.happysnappingturtle.mythicage.wild.WildDevCommand::onRegisterCommands);

        // 神庙通用职责（T10b）与希腊神庙判定（T8a）
        game.addListener(ShrineService::onServerTickPost);
        game.addListener(ShrineService::onServerStopped);
        game.addListener(ShrineService::onBreak);
        game.addListener(TempleRulesLoader::onAddReloadListeners);
        game.addListener(TempleService::onServerStarted);
        game.addListener(TempleService::onServerStopped);
        game.addListener(TempleService::onServerTickPost);
        game.addListener(TempleService::onNeighborNotify);

        // 文明档案（T10b）
        game.addListener(FaithProfiles::onAddReloadListeners);

        // 祭司与恩宠兑换（T9）
        game.addListener(TradeTableLoader::onAddReloadListeners);
        game.addListener(TradeService::onServerTickPost);
        game.addListener(GuardianSpawner::onServerTickPost);
        game.addListener(GuardianSpawner::onEntityJoin);
        game.addListener(GuardianCommand::onRegisterCommands);

        // 聚落归属与主神（T10a）
        game.addListener(PantheonLoader::onAddReloadListeners);
        game.addListener(VillageService::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.advancement.MythEvents::onLoggedIn);
        game.addListener(io.github.happysnappingturtle.mythicage.village.VillagePopulation::onServerTickPost);
        game.addListener(VillageService::onServerStopped);
        game.addListener(VillageCommand::onRegisterCommands);
        // 文明村庄生成与登记（T13b）
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings::onAddReloadListeners);
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration::onChunkLoad);
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.village.PlotWatch::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.DensityTool::onRegisterCommands);

        // 晋升、立祀、改奉与主神像（T11a）
        game.addListener(io.github.happysnappingturtle.mythicage.rite.RiteTables::onAddReloadListeners);
        // 召唤框架（T14）
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonUnits::onAddReloadListeners);
        game.addListener(io.github.happysnappingturtle.mythicage.wild.WildCreatures::onAddReloadListeners);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onEntityJoin);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onEntityLeave);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onIncomingDamage);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onPlayerHurt);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onDeath);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onLoggedIn);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onTravel);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onChangedDimension);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onChangeTarget);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onRegisterCommands);
        game.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) ->
                io.github.happysnappingturtle.mythicage.summon.SummonService.onServerStopped());
        // 神力执行框架（T12）
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerSpecLoader::onAddReloadListeners);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onDamage);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onLoggedOut);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onLoggedIn);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerFx::onChunkWatch);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerFx::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerTasks::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerZones::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerZones::onChunkWatch);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerTasks::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.power.Auras::onAdded);
        game.addListener(io.github.happysnappingturtle.mythicage.power.Auras::onRemoved);
        game.addListener(io.github.happysnappingturtle.mythicage.power.Auras::onExpired);
        game.addListener(io.github.happysnappingturtle.mythicage.power.Auras::onStartTracking);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerDevCommand::onRegisterCommands);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onRightClickItem);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerShields::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TransientEntities::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TransientEntities::onEntityJoin);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TransientEntities::onEntityLeave);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TransientEntities::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TempBlocks::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TempBlocks::onChunkUnload);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TempBlocks::onChunkLoad);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TempBlocks::onServerStopping);
        game.addListener(io.github.happysnappingturtle.mythicage.rite.RiteCommand::onRegisterCommands);
        game.addListener(io.github.happysnappingturtle.mythicage.rite.UnlockService::onLoggedIn);
        game.addListener(io.github.happysnappingturtle.mythicage.entity.karkinos.KarkinosEvents::onPlayerAttack);
        game.addListener(io.github.happysnappingturtle.mythicage.entity.hamadryad.HamadryadEvents::onBreak);
        game.addListener(io.github.happysnappingturtle.mythicage.entity.hamadryad.HamadryadEvents::onRightClickBlock);

        // 希腊恩宠来源：信徒祈祷（T10）
        GreekPrayer.init(modBus, game);
    }
}
