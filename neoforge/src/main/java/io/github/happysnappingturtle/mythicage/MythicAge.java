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
        // 客户端选项（T24 §7.1）：只在客户端载入；服务端用默认值
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, io.github.happysnappingturtle.mythicage.config.ClientConfig.SPEC);
        // 内容声明（T6）：先触发各内容类的类加载，再把 DeferredRegister 挂到 mod 总线
        io.github.happysnappingturtle.mythicage.village.build.ConstructionSignBlock.init();
        GreekMaterials.init();
        GreekAgriculture.init();
        GreekWood.init();
        GreekFoods.init();
        GreekTemple.init();
        GreekArchitecture.init();
        io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks.init();
        io.github.happysnappingturtle.mythicage.content.greek.GreekTrees.init();
        io.github.happysnappingturtle.mythicage.content.greek.village.GreekFestivalItems.init();
        io.github.happysnappingturtle.mythicage.content.greek.village.soldier.GreekSoldierItems.init();
        io.github.happysnappingturtle.mythicage.content.greek.village.soldier.GreekSoldiers.init();
        io.github.happysnappingturtle.mythicage.content.greek.village.GreekFoundation.init();
        io.github.happysnappingturtle.mythicage.village.VillageGuide.init();
        io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons.init(modBus);
        io.github.happysnappingturtle.mythicage.wild.greek.GreekWild.init(modBus);
        GreekGuardian.init();
        io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics.init();
        io.github.happysnappingturtle.mythicage.structure.RuinVaults.init();
        io.github.happysnappingturtle.mythicage.content.greek.ruin.GreekRuins.init(modBus);
        io.github.happysnappingturtle.mythicage.content.greek.knossos.GreekKnossos.init(modBus);
        io.github.happysnappingturtle.mythicage.content.greek.equipment.GreekEquipment.init(modBus);
        io.github.happysnappingturtle.mythicage.statue.PatronStatues.init(modBus, NeoForge.EVENT_BUS);
        io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers.init(modBus, NeoForge.EVENT_BUS);
        GreekAgricultureEvents.register(modBus);
        ModCreativeTabs.init();
        ModRecipes.init();
        ModEntities.init(modBus);
        ModRegistries.register(modBus);
        io.github.happysnappingturtle.mythicage.village.gen.ModStructures.register(modBus);
        io.github.happysnappingturtle.mythicage.structure.MythStructures.register(modBus);
        io.github.happysnappingturtle.mythicage.advancement.MythEvents.register(modBus);
        io.github.happysnappingturtle.mythicage.power.PowerItems.register(modBus);
        io.github.happysnappingturtle.mythicage.relic.RelicItems.register(modBus);
        io.github.happysnappingturtle.mythicage.power.PowerRegistries.register(modBus);
        modBus.addListener(ModDataGen::gather);
        ModAttachments.register(modBus);
        modBus.addListener(MythNetwork::onRegisterPayloads);

        IEventBus game = NeoForge.EVENT_BUS;
        // 配置必须先于其他系统加载：其他系统在同一事件的普通优先级中注册钩子
        game.addListener(EventPriority.HIGHEST, ModConfigs::onServerAboutToStart);
        game.addListener(ConfigCommand::onRegisterCommands);
        game.addListener(MythDataCommand::onRegisterCommands);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "myth_data", MythData::onServerTickPost);
        game.addListener(MythData::onLoggedIn);
        game.addListener(MythData::onRespawn);
        game.addListener(MythData::onChangedDimension);

        // 运行时钟（T5）：钩子在普通优先级注册到 ModConfigs 刚创建的 holder 上
        game.addListener(RuntimeDriver::onServerAboutToStart);
        game.addListener(RuntimeDriver::onServerStopped);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "runtime_driver", RuntimeDriver::onServerTickPost);
        game.addListener(RuntimeCommand::onRegisterCommands);
        PlayerActivity.register(game);
        io.github.happysnappingturtle.mythicage.boss.TrialArenaEvents.register(game);
        TestProducers.register();
        game.addListener(ModRecipes::onServerTickPre);

        // 实体与美术管线（T7）
        game.addListener(RigRegistry::onAddReloadListeners);
        game.addListener(RigRegistry::onDatapackSync);
        game.addListener(EntityDevCommand::onRegisterCommands);
        game.addListener(io.github.happysnappingturtle.mythicage.wild.WildDevCommand::onRegisterCommands);

        // 神庙通用职责（T10b）；希腊神庙规则（T8a；GA-13 神庙归村庄）
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "shrine_service", ShrineService::onServerTickPost);
        game.addListener(ShrineService::onServerStopped);
        game.addListener(ShrineService::onBreak);
        game.addListener(TempleRulesLoader::onAddReloadListeners);
        game.addListener(io.github.happysnappingturtle.mythicage.content.greek.prayer.OfferingLoader::onAddReloadListeners);
        game.addListener(TempleService::onServerStarted);

        // 文明档案（T10b）
        game.addListener(FaithProfiles::onAddReloadListeners);

        // 祭司与恩宠兑换（T9）
        game.addListener(TradeTableLoader::onAddReloadListeners);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "trade_service", TradeService::onServerTickPost);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "guardian_spawner", GuardianSpawner::onServerTickPost);
        game.addListener(GuardianSpawner::onEntityJoin);
        game.addListener(GuardianCommand::onRegisterCommands);

        // 聚落归属与主神（T10a）
        game.addListener(PantheonLoader::onAddReloadListeners);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "village_service", VillageService::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.advancement.MythEvents::onLoggedIn);
        // 性能探针（T24 阶段 ④）：每刻处理器都经 Perf.tick 注册
        io.github.happysnappingturtle.mythicage.perf.Perf.register(game);
        // 神话图鉴点亮（T24 阶段 ③）
        game.addListener(io.github.happysnappingturtle.mythicage.bestiary.BestiaryLoader::onAddReloadListeners);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "bestiary", io.github.happysnappingturtle.mythicage.bestiary.Bestiary::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.bestiary.Bestiary::onDamagePost);
        game.addListener(io.github.happysnappingturtle.mythicage.bestiary.Bestiary::onDeath);
        game.addListener(io.github.happysnappingturtle.mythicage.bestiary.Bestiary::onLoggedIn);
        game.addListener(io.github.happysnappingturtle.mythicage.handbook.Handbook::onLoggedIn);
        game.addListener(io.github.happysnappingturtle.mythicage.handbook.Handbook::onRegisterCommands);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "village_population", io.github.happysnappingturtle.mythicage.village.VillagePopulation::onServerTickPost);
        game.addListener(VillageService::onServerStopped);
        game.addListener(VillageCommand::onRegisterCommands);
        // 文明村庄生成与登记（T13b）
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings::onAddReloadListeners);
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration::onChunkLoad);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "village_registration", io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration::onServerStopped);
        // T22 遗迹与迷宫：生成即登记、遗迹宝库表
        game.addListener(io.github.happysnappingturtle.mythicage.structure.StructureRegistration::onChunkLoad);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "structure_registration", io.github.happysnappingturtle.mythicage.structure.StructureRegistration::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.structure.StructureRegistration::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.structure.RuinVaults::onAddReloadListeners);
        game.addListener(io.github.happysnappingturtle.mythicage.structure.StructureSpecs::onAddReloadListeners);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "plot_watch", io.github.happysnappingturtle.mythicage.village.PlotWatch::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.village.gen.DensityTool::onRegisterCommands);
        // 居民框架（T19）
        io.github.happysnappingturtle.mythicage.village.work.BasicJobTypes.registerAll();
        io.github.happysnappingturtle.mythicage.content.greek.village.GreekVillageWork.init();
        game.addListener(io.github.happysnappingturtle.mythicage.village.work.VillageData::onAddReloadListeners);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "village_scheduler", io.github.happysnappingturtle.mythicage.village.work.VillageScheduler::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.village.life.OracleService::onRightClickBlock);
        game.addListener(io.github.happysnappingturtle.mythicage.village.FoundationService::onBreak);
        game.addListener(io.github.happysnappingturtle.mythicage.village.work.VillageScheduler::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.village.build.Construction::onServerStopped);
        // 村庄界面（GA-12）：开着的界面每 20 刻发地图增量；地图物品入口与村庄标记
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "village_screen", io.github.happysnappingturtle.mythicage.village.screen.VillageScreenService::onServerTickPost);
        game.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> io.github.happysnappingturtle.mythicage.village.screen.VillageScreenService.onServerStopped());
        game.addListener(io.github.happysnappingturtle.mythicage.village.map.VillageMapItems::onRightClickItem);
        game.addListener(io.github.happysnappingturtle.mythicage.village.map.VillageMapItems::onTooltip);
        game.addListener(io.github.happysnappingturtle.mythicage.village.build.PlayerConstructionEdits::onChunkLoad);
        game.addListener(EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.build.PlayerConstructionEdits::onPlace);
        game.addListener(EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.build.PlayerConstructionEdits::onBreak);
        game.addListener(io.github.happysnappingturtle.mythicage.village.work.VillageScheduler::onEntityJoin);
        game.addListener(io.github.happysnappingturtle.mythicage.village.work.VillageScheduler::onEntityLeave);
        game.addListener(io.github.happysnappingturtle.mythicage.village.resident.ResidentHostility::onEntityJoin);
        // T20 村民士兵：谁打过本村的人、士兵击杀的归因 ×0
        game.addListener(io.github.happysnappingturtle.mythicage.village.soldier.VillageThreats::onDamagePost);
        game.addListener(io.github.happysnappingturtle.mythicage.village.soldier.VillageThreats::onServerStopped);
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                io.github.happysnappingturtle.mythicage.village.soldier.SoldierAttribution::onDeath);
        game.addListener(io.github.happysnappingturtle.mythicage.village.soldier.SoldierAttribution::onExperienceDrop);
        game.addListener(io.github.happysnappingturtle.mythicage.village.soldier.VillageLandmarks::onPlace);
        game.addListener(io.github.happysnappingturtle.mythicage.village.soldier.VillageLandmarks::onBreak);
        game.addListener(io.github.happysnappingturtle.mythicage.village.soldier.VillageLandmarks::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.village.work.PointEvents::onBreak);
        // NPC 任务（T21）：建筑损坏记录与修复核验；最低优先级，只看没被别的模块取消的
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.quest.BuildingDamage::onBreak);
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.quest.BuildingDamage::onExplosion);
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.quest.BuildingDamage::onLivingDestroy);
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.quest.BuildingDamage::onPlace);
        // T21 阶段 ③：护送与救援盯着的威胁、玩家刷出来的生物
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.quest.ThreatWatch::onFinalizeSpawn);
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.quest.ThreatWatch::onChangeTarget);
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.quest.ThreatWatch::onDamagePost);
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, io.github.happysnappingturtle.mythicage.village.quest.ThreatWatch::onDeath);
        game.addListener(io.github.happysnappingturtle.mythicage.village.quest.ThreatWatch::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.village.VillageDevCommand::onRegisterCommands);

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
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "summon_service", io.github.happysnappingturtle.mythicage.summon.SummonService::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onChangeTarget);
        game.addListener(io.github.happysnappingturtle.mythicage.summon.SummonService::onRegisterCommands);
        game.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) ->
                io.github.happysnappingturtle.mythicage.summon.SummonService.onServerStopped());
        // 神力执行框架（T12）
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerSpecLoader::onAddReloadListeners);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "power_service", io.github.happysnappingturtle.mythicage.power.PowerService::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onDamage);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onLoggedOut);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onLoggedIn);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerFx::onChunkWatch);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerFx::onServerStopped);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "power_tasks", io.github.happysnappingturtle.mythicage.power.PowerTasks::onServerTickPost);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "power_zones", io.github.happysnappingturtle.mythicage.power.PowerZones::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerZones::onChunkWatch);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerTasks::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.power.Auras::onAdded);
        game.addListener(io.github.happysnappingturtle.mythicage.power.Auras::onRemoved);
        game.addListener(io.github.happysnappingturtle.mythicage.power.Auras::onExpired);
        game.addListener(io.github.happysnappingturtle.mythicage.power.Auras::onStartTracking);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerDevCommand::onRegisterCommands);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerService::onRightClickItem);
        game.addListener(io.github.happysnappingturtle.mythicage.power.PowerShields::onServerStopped);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "transient_entities", io.github.happysnappingturtle.mythicage.power.TransientEntities::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TransientEntities::onEntityJoin);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TransientEntities::onEntityLeave);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TransientEntities::onServerStopped);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "temp_blocks", io.github.happysnappingturtle.mythicage.power.TempBlocks::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TempBlocks::onChunkUnload);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TempBlocks::onChunkLoad);
        game.addListener(io.github.happysnappingturtle.mythicage.power.TempBlocks::onServerStopping);
        game.addListener(io.github.happysnappingturtle.mythicage.rite.RiteCommand::onRegisterCommands);

        // 快速验证（GA-3）：分站进入、试玩记录
        game.addListener(io.github.happysnappingturtle.mythicage.playtest.PlaytestCommand::onRegisterCommands);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "playtest",
                io.github.happysnappingturtle.mythicage.playtest.PlaytestJobs::onServerTickPost);
        game.addListener(io.github.happysnappingturtle.mythicage.playtest.PlaytestJobs::onLogout);
        io.github.happysnappingturtle.mythicage.playtest.PlaytestLog.init();
        game.addListener(io.github.happysnappingturtle.mythicage.rite.UnlockService::onLoggedIn);
        game.addListener(io.github.happysnappingturtle.mythicage.entity.karkinos.KarkinosEvents::onPlayerAttack);
        game.addListener(io.github.happysnappingturtle.mythicage.entity.hamadryad.HamadryadEvents::onBreak);
        game.addListener(io.github.happysnappingturtle.mythicage.entity.hamadryad.HamadryadEvents::onRightClickBlock);

        // 圣物框架（T16）
        io.github.happysnappingturtle.mythicage.relic.RelicService.init();
        io.github.happysnappingturtle.mythicage.relic.RelicEvents.register(game);

        // 村庄范围与善恶值（T18）
        io.github.happysnappingturtle.mythicage.conduct.ConductEventsRegistration.register(game);

        // 文明装备（T17）
        io.github.happysnappingturtle.mythicage.equipment.EquipmentEvents.register(game);

        // 希腊恩宠来源：信徒祈祷（T10）
        GreekPrayer.init(modBus, game);
    }
}
