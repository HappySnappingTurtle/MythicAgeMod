package io.github.happysnappingturtle.mythicage.rite;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxStep;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 试炼 Boss 的击败记录与 Boss 凭证（T11a 设计文档 §4.5；12 号 §3）。合法击败立即记录事实：
 * 击败次数 +1、对应试炼档的凭证 +1、首杀另记一次。谁算合法击败（参与者、士兵击杀不算……）由 T23 判定后调用。
 * <p>
 * 信物补发规则（T23 实现信物物品时遵守）：有首杀记录、背包里没有该信物时，可在本文明祭司处免费补领，
 * 每 MC 日一次；补领不计凭证、不重复首杀奖励。
 */
public final class TrialService {
    private TrialService() {
    }

    /** 记一次合法击败；返回得到凭证的文明（Boss 不属于任何文明的试炼时为空）。 */
    public static List<Civilization> recordKill(ServerPlayer player, String bossId) {
        List<Civilization> credited = new ArrayList<>();
        List<Age> tiers = new ArrayList<>();
        for (Civilization civ : Civilization.values()) {
            Optional<Age> tier = RiteTables.promotion(civ).trialTierOf(bossId);
            if (tier.isPresent()) {
                credited.add(civ);
                tiers.add(tier.get());
            }
        }
        MythData.transact(player, Transactions.Request.internal(new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                return Optional.empty();
            }

            @Override
            public void apply(PlayerMythData data) {
                data.trialKills().merge(bossId, 1, Integer::sum);
                data.firstKills().add(bossId);
                for (int i = 0; i < credited.size(); i++) {
                    PlayerMythData.CivState s = data.civ(credited.get(i));
                    s.setVouchers(tiers.get(i), s.vouchers(tiers.get(i)) + 1);
                }
            }
        }));
        return credited;
    }

    /** 管理命令：直接发凭证。 */
    public static void grant(ServerPlayer player, Civilization civ, Age tier, int count) {
        MythData.transact(player, Transactions.Request.internal(new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                return Optional.empty();
            }

            @Override
            public void apply(PlayerMythData data) {
                PlayerMythData.CivState s = data.civ(civ);
                s.setVouchers(tier, Math.max(0, s.vouchers(tier) + count));
            }
        }));
    }
}
