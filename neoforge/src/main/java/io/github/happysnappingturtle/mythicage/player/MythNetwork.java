package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.trade.TradePayloads;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 网络负载注册。版本号变化时，新旧版本客户端与服务端互相拒绝连接。 */
public final class MythNetwork {
    private static final String PROTOCOL_VERSION = "16";

    private MythNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToClient(PlayerSyncPayload.TYPE, PlayerSyncPayload.STREAM_CODEC,
                        (payload, context) -> ClientMythData.accept(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.content.greek.temple.AltarSurveyPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.content.greek.temple.AltarSurveyPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.openAltar(payload))
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
                // 村务界面（T19）
                .playToClient(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.View.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.View.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.villageAffairs(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Donate.TYPE,
                        io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads.Donate.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs::handleDonate)
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
