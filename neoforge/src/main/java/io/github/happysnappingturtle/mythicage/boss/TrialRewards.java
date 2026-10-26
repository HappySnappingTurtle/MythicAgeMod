package io.github.happysnappingturtle.mythicage.boss;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxSteps;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.rite.TrialService;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 试炼结算（T23 §7.1，D7–D9）：Boss 死时，每位合法参战者各自——
 * <ul>
 *   <li>{@link TrialService#recordKill}：试炼击杀 +1、首杀记录、试炼凭证 +1（每场都记）；</li>
 *   <li>竞技场数据里的物品与经验（每场都给）；首杀另解锁图纸（只写记录）；</li>
 *   <li>收据 {@code trial:<实例>:<场次>}（UNIQUE）：同一人同一场只结算一次（死亡事件触发两次、重启后再触发都不重发）。</li>
 * </ul>
 * 结算时不在线的参战者记进待发表（世界存档 {@value #NAME}），上线时补发。奖励全走这里，Boss 没有原版掉落（D9）。
 */
public final class TrialRewards extends SavedData {
    public static final String NAME = "mythic_age_trial_pending";

    /** 待发：玩家 → [竞技场种类, 收据] 列表。 */
    private final Map<UUID, List<String[]>> pending = new LinkedHashMap<>();

    private TrialRewards() {
    }

    public static TrialRewards get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TrialRewards::new, TrialRewards::read, null), NAME);
    }

    public static String receipt(TrialArena a) {
        return "trial:" + a.id() + ":" + a.fightId();
    }

    /** 结算一场：返回当场发了奖励的在线玩家数（离线的记进待发表）。 */
    public static int settle(ServerLevel level, TrialArena arena) {
        List<UUID> legal = arena.participants().legal(id -> {
            ServerPlayer p = online(level.getServer(), id);
            return p != null && (p.isCreative() || p.isSpectator());
        });
        String receipt = receipt(arena);
        int given = 0;
        for (UUID id : legal) {
            ServerPlayer p = online(level.getServer(), id);
            if (p != null) {
                if (grant(p, arena.kind(), receipt)) {
                    given++;
                }
            } else {
                TrialRewards t = get(level.getServer());
                t.pending.computeIfAbsent(id, k -> new ArrayList<>()).add(new String[]{arena.kind(), receipt});
                t.setDirty();
            }
        }
        MythicAge.LOGGER.info("竞技场 {} 第 {} 场结算：合法参战者 {} 人", arena.id(), arena.fightId(), legal.size());
        return given;
    }

    static ServerPlayer online(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) {
            return p;
        }
        for (ServerPlayer t : TrialArena.TEST_PLAYERS) {
            if (t.getUUID().equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** 给一个人发一场的奖励；已有收据（发过）返回 false。 */
    public static boolean grant(ServerPlayer player, String kind, String receipt) {
        ArenaSpec spec = ArenaSpec.Loader.get(kind).orElse(null);
        if (spec == null) {
            return false;
        }
        if (MythData.get(player).receipts().contains(ReceiptBook.Scope.UNIQUE, receipt)
                || MythData.transact(player, Transactions.Request.unique(receipt)).status() != Transactions.Status.COMMITTED) {
            return false;
        }
        boolean first = !MythData.get(player).firstKills().contains(spec.trial());
        TrialService.recordKill(player, spec.trial());
        ArenaSpec.Rewards r = spec.rewards();
        if (first && !r.firstKillBlueprint().isEmpty()) {
            MythData.transact(player, Transactions.Request.internal(TxSteps.unlockBlueprint(r.firstKillBlueprint())));
        }
        net.minecraft.network.chat.MutableComponent list = Component.empty();
        for (ArenaSpec.Reward item : r.items()) {
            BuiltInRegistries.ITEM.getOptional(item.item()).ifPresent(it -> {
                ItemStack st = new ItemStack(it, item.count());
                if (!list.getSiblings().isEmpty()) {
                    list.append(Component.translatable("mythic_age.arena.rewards.sep"));
                }
                list.append(st.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.YELLOW)).append(" ×" + item.count());
                TradeService.give(player, st);
            });
        }
        if (r.xp() > 0) {
            player.giveExperiencePoints(r.xp());
            if (!list.getSiblings().isEmpty()) {
                list.append(Component.translatable("mythic_age.arena.rewards.sep"));
            }
            list.append(Component.translatable("mythic_age.arena.rewards.xp", r.xp()).withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        if (first && !r.firstKillBlueprint().isEmpty()) {
            list.append(Component.translatable("mythic_age.arena.rewards.sep")).append(Component.translatable("mythic_age.arena.rewards.blueprint",
                    Component.translatable("mythic_age.blueprint." + r.firstKillBlueprint().replace(':', '.')))
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        player.displayClientMessage(Component.translatable(first ? "mythic_age.arena.victory.first" : "mythic_age.arena.victory"),
                false);
        player.displayClientMessage(Component.translatable("mythic_age.arena.rewards", list), false);
        celebrate(player, spec, first);
        return true;
    }

    /** 胜利的界面效果：屏幕大标题与副标题、挑战完成音效（只给本人）。 */
    static void celebrate(ServerPlayer player, ArenaSpec spec, boolean first) {
        Component boss = BuiltInRegistries.ENTITY_TYPE.get(spec.boss()).getDescription();
        player.playNotifySound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F, 1.0F);
        if (player.connection == null) {
            return;
        }
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 80, 25));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.translatable("mythic_age.arena.title").withStyle(net.minecraft.ChatFormatting.GOLD,
                        net.minecraft.ChatFormatting.BOLD)));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                Component.translatable(first ? "mythic_age.arena.subtitle.first" : "mythic_age.arena.subtitle", boss)));
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            deliver(p);
        }
    }

    /** 补发待发的奖励。 */
    public static int deliver(ServerPlayer p) {
        TrialRewards t = get(p.server);
        List<String[]> list = t.pending.remove(p.getUUID());
        if (list == null) {
            return 0;
        }
        t.setDirty();
        int n = 0;
        for (String[] e : list) {
            if (grant(p, e[0], e[1])) {
                n++;
            }
        }
        return n;
    }

    public int pendingFor(UUID id) {
        List<String[]> l = pending.get(id);
        return l == null ? 0 : l.size();
    }

    static TrialRewards read(CompoundTag tag, HolderLookup.Provider registries) {
        TrialRewards t = new TrialRewards();
        for (Tag x : tag.getList("pending", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            t.pending.computeIfAbsent(c.getUUID("Player"), k -> new ArrayList<>())
                    .add(new String[]{c.getString("Kind"), c.getString("Receipt")});
        }
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag l = new ListTag();
        pending.forEach((id, list) -> list.forEach(e -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Player", id);
            c.putString("Kind", e[0]);
            c.putString("Receipt", e[1]);
            l.add(c);
        }));
        tag.put("pending", l);
        return tag;
    }
}
