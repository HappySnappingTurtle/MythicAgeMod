package io.github.happysnappingturtle.mythicage.rite;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.FavorRules;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.rite.AgeCaps;
import io.github.happysnappingturtle.mythicage.core.rite.PromotionTable;
import io.github.happysnappingturtle.mythicage.core.rite.RiteRules;
import io.github.happysnappingturtle.mythicage.core.rite.UnlockSummary;
import io.github.happysnappingturtle.mythicage.player.CapBonusProvider;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.recipe.AgeGatedRecipe;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKinds;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 晋升成功界面（12 号 §4.3-A；T11a 设计文档 §6.2）：按玩家数据里的 {@code pendingUnlocks} 弹出，发出后清空；
 * 离线时发生的晋升在下次登录补弹；同一文明一次积压多级（管理命令跨级）时合并为一个窗口。
 * 解锁清单全部从注册数据汇总。
 */
public final class UnlockService {
    private UnlockService() {
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            flush(p);
        }
    }

    /** 弹出积压的成功界面；返回各文明的（合并后）清单。 */
    public static List<AgeUnlockedPayload> flush(ServerPlayer player) {
        PlayerMythData data = MythData.get(player);
        List<AgeUnlockedPayload> out = new ArrayList<>();
        for (Civilization civ : Civilization.values()) {
            List<Age> pending = data.civ(civ).pendingUnlocks();
            if (pending.isEmpty()) {
                continue;
            }
            List<UnlockSummary> parts = new ArrayList<>();
            for (Age a : List.copyOf(pending)) {
                parts.add(summary(player, civ, a));
            }
            pending.clear();
            MythData.changed(player);
            AgeUnlockedPayload p = payload(UnlockSummary.merge(parts));
            out.add(p);
            TradeService.send(player, p);
        }
        return out;
    }

    /** 信仰面板「重看」：再弹一次某个已到达时代的成功界面（不改数据）。 */
    public static AgeUnlockedPayload replay(ServerPlayer player, Civilization civ, Age age) {
        AgeUnlockedPayload p = payload(summary(player, civ, age));
        TradeService.send(player, p);
        return p;
    }

    /** 某文明进入某时代的清单（上限按「上一时代 → 该时代」求差）。 */
    public static UnlockSummary summary(ServerPlayer player, Civilization civ, Age age) {
        PlayerMythData data = MythData.get(player);
        Age prev = Age.values()[Math.max(Age.ARCHAIC.ordinal(), age.ordinal() - 1)];
        long bonus = CapBonusProvider.Holder.get().capBonus(player, civ);
        List<UnlockSummary.CapChange> caps = new ArrayList<>();
        caps.add(new UnlockSummary.CapChange("favor_cap", FavorRules.cap(prev, bonus), FavorRules.cap(age, bonus)));
        caps.add(new UnlockSummary.CapChange("shrines", shrineLimit(civ, prev), shrineLimit(civ, age)));
        Age others = Age.NONE;
        for (Civilization c : Civilization.values()) {
            if (c != civ && data.civ(c).age().atLeast(others)) {
                others = data.civ(c).age();
            }
        }
        Age before = prev.atLeast(others) ? prev : others;
        Age after = age.atLeast(others) ? age : others;
        caps.add(new UnlockSummary.CapChange("mythic_units", AgeCaps.mythicUnits(before), AgeCaps.mythicUnits(after)));
        caps.add(new UnlockSummary.CapChange("relic_slots", AgeCaps.relicSlots(before), AgeCaps.relicSlots(after)));
        List<UnlockSummary.GatedRecipe> recipes = new ArrayList<>();
        if (ModConfigs.worldRules().current().recipes().ageGated().enabled()) {
            for (var holder : player.server.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                if (holder.value() instanceof AgeGatedRecipe g) {
                    ItemStack result = g.getResultItem(player.server.registryAccess());
                    recipes.add(new UnlockSummary.GatedRecipe(BuiltInRegistries.ITEM.getKey(result.getItem()).toString(),
                            g.civilization(), g.age()));
                }
            }
        }
        PromotionTable table = RiteTables.promotion(civ);
        List<PromotionTable.ExtraUnlock> extras = table.entry(age).map(PromotionTable.AgeEntry::extraUnlocks).orElse(List.of());
        return UnlockSummary.of(civ, age, data.civ(civ).minorGods().getOrDefault(age, ""),
                TradeTableLoader.table(civ).offers(), recipes, caps, extras);
    }

    /** 本文明各种神庙（不含主神像）的数量上限之和。 */
    static long shrineLimit(Civilization civ, Age age) {
        WorldRulesConfig rules = ModConfigs.worldRules().current();
        long total = 0;
        for (ShrineKind k : ShrineKinds.of(civ)) {
            if (!k.id().contains(":statue_")) {
                total += rules.shrine(k.id()).limit(age.id());
            }
        }
        return total;
    }

    static AgeUnlockedPayload payload(UnlockSummary s) {
        PromotionTable table = RiteTables.promotion(s.civ());
        Optional<AgeUnlockedPayload.Next> next = RiteRules.next(s.age()).flatMap(n -> table.entry(n).map(e ->
                new AgeUnlockedPayload.Next(n, e.favor(), e.offerings().stream()
                        .map(a -> TradeService.stack(a.item(), a.count())).toList(), e.trial(),
                        e.requirements().stream().map(r -> PromotionRequirements.get(r.type())
                                .map(req -> req.preview(r.params())).orElse(null))
                                .filter(java.util.Objects::nonNull).toList())));
        return new AgeUnlockedPayload(s.civ(), s.age(), s.minorGod(), stacks(s.powers()), stacks(s.summons()),
                stacks(s.recipes()), s.caps().stream().map(c -> new AgeUnlockedPayload.Cap(c.key(), c.before(), c.after())).toList(),
                s.extras().stream().map(e -> new AgeUnlockedPayload.Extra(TradeService.stack(e.icon(), 1), e.key())).toList(), next);
    }

    private static List<ItemStack> stacks(List<String> ids) {
        return ids.stream().map(id -> TradeService.stack(id, 1)).filter(st -> !st.isEmpty()).toList();
    }
}
