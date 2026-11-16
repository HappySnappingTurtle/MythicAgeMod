package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.trade.TradePayloads;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 网络负载注册。版本号变化时，新旧版本客户端与服务端互相拒绝连接。 */
public final class MythNetwork {
    // GA-11：新增项目选择负载（候选列表、点名）；GA-13：神坛检视加村庄神庙段、圣物展示、退出供奉。新旧客户端不能混用。
    private static final String PROTOCOL_VERSION = "29";

    private MythNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToClient(io.github.happysnappingturtle.mythicage.village.build.ConstructionSitePayload.TYPE,
                        io.github.happysnappingturtle.mythicage.village.build.ConstructionSitePayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ConstructionSiteRenderer.accept(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.equipment.WeaponMotionState.Payload.TYPE,
                        io.github.happysnappingturtle.mythicage.equipment.WeaponMotionState.Payload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.equipment.WeaponMotionClient.accept(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.rig.RigHeadLookPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.rig.RigHeadLookPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.rig.RigRegistry.accept(payload))
                .playToClient(PlayerSyncPayload.TYPE, PlayerSyncPayload.STREAM_CODEC,
                        (payload, context) -> ClientMythData.accept(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.boss.BossPointerPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.boss.BossPointerPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.bossPointer(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.content.greek.knossos.ThreadTrailPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.content.greek.knossos.ThreadTrailPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.threadTrail(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.shrine.inspect.ShrineSurvey.TYPE,
                        io.github.happysnappingturtle.mythicage.shrine.inspect.ShrineSurvey.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.openShrine(payload))
                // 希腊信徒的状态（T10：橄榄枝饰、跪姿）
                .playToClient(io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverStatePayload.TYPE,
                        io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverStatePayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientBelievers.accept(payload))
                // 仪式与晋升成功界面（T11a）
                .playToClient(io.github.happysnappingturtle.mythicage.rite.RitePayloads.Result.TYPE,
                        io.github.happysnappingturtle.mythicage.rite.RitePayloads.Result.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.riteResult(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.rite.AgeUnlockedPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.rite.AgeUnlockedPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.ageUnlocked(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.rite.RitePayloads.Perform.TYPE,
                        io.github.happysnappingturtle.mythicage.rite.RitePayloads.Perform.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.rite.RiteService::handlePerform)
                // 信仰面板（T11a 阶段 ②）
                .playToClient(io.github.happysnappingturtle.mythicage.faith.FaithPayloads.View.TYPE,
                        io.github.happysnappingturtle.mythicage.faith.FaithPayloads.View.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.faithView(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Request.TYPE,
                        io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Request.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.faith.FaithService::handleRequest)
                .playToServer(io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Replay.TYPE,
                        io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Replay.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.faith.FaithService::handleReplay)
                // 神力表现与瞄准预览（T12 阶段 ③）
                .playToClient(io.github.happysnappingturtle.mythicage.power.PowerFxPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.power.PowerFxPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.powerFx(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.power.PowerAimPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.power.PowerAimPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.powerAim(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.power.AuraPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.power.AuraPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.aura(payload))
                // 祭司与神庙兑换（T9；T10b 起按文明）
                .playToClient(TradePayloads.Open.TYPE, TradePayloads.Open.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.openTrade(payload))
                .playToClient(TradePayloads.Result.TYPE, TradePayloads.Result.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.tradeResult(payload))
                .playToClient(TradePayloads.Close.TYPE, TradePayloads.Close.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.tradeClose())
                .playToServer(TradePayloads.Buy.TYPE, TradePayloads.Buy.STREAM_CODEC, TradeService::handleBuy)
                .playToServer(TradePayloads.Blessing.TYPE, TradePayloads.Blessing.STREAM_CODEC, TradeService::handleBlessing)
                .playToServer(TradePayloads.Closed.TYPE, TradePayloads.Closed.STREAM_CODEC, TradeService::handleClosed)
                // 天马天眼（T14 阶段 ②）：只发给主人
                .playToClient(io.github.happysnappingturtle.mythicage.summon.SkyEyePayload.TYPE,
                        io.github.happysnappingturtle.mythicage.summon.SkyEyePayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.skyEye(payload))
                // 召唤管理界面（T14 阶段 ②）
                .playToClient(io.github.happysnappingturtle.mythicage.summon.SummonPayloads.View.TYPE,
                        io.github.happysnappingturtle.mythicage.summon.SummonPayloads.View.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.summonView(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.summon.SummonPayloads.Request.TYPE,
                        io.github.happysnappingturtle.mythicage.summon.SummonPayloads.Request.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.summon.SummonScreens::handleRequest)
                .playToServer(io.github.happysnappingturtle.mythicage.summon.SummonPayloads.Command.TYPE,
                        io.github.happysnappingturtle.mythicage.summon.SummonPayloads.Command.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.summon.SummonScreens::handleCommand)
                // 神庙供奉栏（T16）
                .playToClient(io.github.happysnappingturtle.mythicage.relic.RelicPayloads.Shelf.TYPE,
                        io.github.happysnappingturtle.mythicage.relic.RelicPayloads.Shelf.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientRelics.accept(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.relic.RelicPayloads.Retrieve.TYPE,
                        io.github.happysnappingturtle.mythicage.relic.RelicPayloads.Retrieve.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.relic.RelicPayloads::handleRetrieve)
                // 村庄神庙（GA-13）：祭坛上方的圣物展示、退出供奉
                .playToClient(io.github.happysnappingturtle.mythicage.relic.TempleRelicDisplay.Display.TYPE,
                        io.github.happysnappingturtle.mythicage.relic.TempleRelicDisplay.Display.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.TempleRelicRenderer.accept(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.content.greek.temple.TempleMembership.Leave.TYPE,
                        io.github.happysnappingturtle.mythicage.content.greek.temple.TempleMembership.Leave.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.content.greek.temple.TempleMembership::handleLeave)
                // 村务界面（T19）
                // 奠基石选主神（T19 阶段 ⑥）
                .playToClient(io.github.happysnappingturtle.mythicage.village.found.FoundationPayloads.Choose.TYPE,
                        io.github.happysnappingturtle.mythicage.village.found.FoundationPayloads.Choose.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.foundation(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.village.found.FoundationPayloads.Found.TYPE,
                        io.github.happysnappingturtle.mythicage.village.found.FoundationPayloads.Found.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.village.FoundationService::handleFound)
                .playToClient(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.View.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.View.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageAffairs(payload))
                // 项目选择（GA-11）：界面是村庄界面的「建设」分页（GA-12）
                .playToServer(io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads.Request.TYPE,
                        io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads.Request.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads::handleRequest)
                .playToServer(io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads.Nominate.TYPE,
                        io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads.Nominate.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads::handleNominate)
                .playToClient(io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads.View.TYPE,
                        io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads.View.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageProjects(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Donate.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Donate.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs::handleDonate)
                // 村庄界面（GA-12）：状态栏、地图全量 / 增量、村民、信仰、建筑详情；打开、关闭、详情请求、操作
                .playToClient(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Opened.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Opened.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageScreenOpened(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.HeaderUpdate.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.HeaderUpdate.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageScreenHeader(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.MapPacket.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.MapPacket.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageScreenMap(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.People.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.People.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageScreenPeople(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Faith.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Faith.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageScreenFaith(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Detail.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Detail.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageScreenDetail(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Open.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Open.STREAM_CODEC, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenService::handleOpen)
                .playToServer(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Close.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Close.STREAM_CODEC, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenService::handleClose)
                .playToServer(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.DetailRequest.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.DetailRequest.STREAM_CODEC, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenService::handleDetail)
                .playToServer(io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Act.TYPE, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads.Act.STREAM_CODEC, io.github.happysnappingturtle.mythicage.village.screen.VillageScreenService::handleAct)
                // 村界、升级 toast（T24 阶段 ①）
                .playToServer(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.BoundsRequest.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.BoundsRequest.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs::handleBounds)
                .playToClient(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Bounds.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Bounds.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageBounds(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Toast.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Toast.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageToast(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.player.FavorSourcePayload.TYPE,
                        io.github.happysnappingturtle.mythicage.player.FavorSourcePayload.STREAM_CODEC,
                        (payload, context) -> ClientMythData.source(payload.civ(), payload.sourceKey()))
                // 试炼 Boss 提示行（T24 阶段 ②）
                .playToClient(io.github.happysnappingturtle.mythicage.boss.ArenaHudPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.boss.ArenaHudPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.arenaHud(payload))
                // 善恶与贡献（T24 阶段 ①）
                .playToServer(io.github.happysnappingturtle.mythicage.conduct.ConductPayloads.Request.TYPE,
                        io.github.happysnappingturtle.mythicage.conduct.ConductPayloads.Request.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.conduct.ConductViewService::handleRequest)
                .playToClient(io.github.happysnappingturtle.mythicage.conduct.ConductPayloads.View.TYPE,
                        io.github.happysnappingturtle.mythicage.conduct.ConductPayloads.View.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.conductView(payload))
                // NPC 任务委托页（T21）
                .playToClient(io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads.View.TYPE,
                        io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads.View.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageQuests(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads.Act.TYPE,
                        io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads.Act.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.village.quest.QuestService::handleAct)
                // 居民交易（T19 阶段 ④b）
                .playToClient(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Trade.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Trade.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.residentTrade(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Deal.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Deal.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.village.economy.ResidentTrade::handleDeal)
                // 举盾时攻击（T17）
                .playToServer(io.github.happysnappingturtle.mythicage.equipment.ShieldAttack.Payload.TYPE,
                        io.github.happysnappingturtle.mythicage.equipment.ShieldAttack.Payload.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.equipment.ShieldAttack::handle);
    }
}
