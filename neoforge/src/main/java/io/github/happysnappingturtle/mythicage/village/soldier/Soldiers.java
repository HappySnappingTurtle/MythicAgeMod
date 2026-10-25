package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.soldier.DutyRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.KitRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierCivRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierRoster;
import io.github.happysnappingturtle.mythicage.rite.RiteTables;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.work.BasicJobTypes;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 每村的士兵（T20 设计文档 §4、§5）：日开始时降回、换装、转岗；每次规划时排班派岗、给士兵发命令；
 * 阵亡与兵装耐久。士兵就是转岗的居民：同一个实体、同一个 UUID，名册职业与 {@link SoldierRecord} 同步。
 */
public final class Soldiers {
    public static final ResourceLocation KIT_MODIFIER = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "soldier_kit");
    /** 居民的基础生命（{@code ResidentEntity.createAttributes}）。 */
    static final double BASE_HEALTH = 20;

    private Soldiers() {
    }

    // ───────────── 配置与上限 ─────────────

    static VillagesConfig.Soldiers config() {
        return ModConfigs.villages().current().soldiers();
    }

    static boolean enabled(CivVillageData data) {
        VillagesConfig.Soldiers c = config();
        return c.enabled() && c.hardCap() > 0 && data.soldiers().enabled();
    }

    /** 当前阶段的卫兵上限加成（{@code village_stage/<文明>.json}）。 */
    static int stageBonus(VillageRegistry.Village v) {
        return Civilization.byId(v.civ()).map(c -> RiteTables.stages(c).stage(v.stage()).guards()).orElse(0);
    }

    /** 本村此刻的士兵上限。 */
    public static int cap(VillageContext ctx) {
        if (!enabled(ctx.data())) {
            return 0;
        }
        VillagesConfig.Soldiers c = config();
        return SoldierRoster.cap(ctx.state().roster.size(), stageBonus(ctx.village()), c.hardCap(), c.ratio());
    }

    // ───────────── 日开始 ─────────────

    /** 每 MC 日开始一次：补发（已有存档）、降回、换装、转岗 1 人。 */
    public static void dayStart(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        CivVillageData d = ctx.data();
        sync(ctx);
        boolean on = enabled(d);
        seed(ctx, on);
        int cap = cap(ctx);
        // 降回多出来的（只动已加载的人）
        List<SoldierRoster.Enlisted> enlisted = new ArrayList<>();
        s.soldiers.forEach((id, r) -> enlisted.add(new SoldierRoster.Enlisted(id, r.profession(), r.order())));
        int floor = on ? Math.min(1, initialSoldiers(d)) : 0;
        for (UUID id : SoldierRoster.pickDemote(enlisted, cap, floor)) {
            loaded(ctx, id).ifPresent(r -> demote(ctx, r));
        }
        // 换装：穿简易或没有、村库里有更好的
        for (ResidentEntity r : soldiersOf(ctx)) {
            SoldierRecord rec = s.soldiers.get(r.getUUID());
            Optional<SoldierProfile> p = d.soldier(rec.profession());
            if (p.isPresent() && SoldierRoster.upgrade(rec.tier(), s.stock.count(p.get().kits().item(SoldierProfile.Tier.FULL)),
                    s.stock.count(p.get().kits().item(SoldierProfile.Tier.SIMPLE)))) {
                rekit(ctx.state(), r, p.get(), rec);
            }
        }
        if (on) {
            convertOne(ctx, cap);
        }
    }

    /** 名册与士兵表对齐：没在名册里的士兵行删掉；职业是兵种却没有士兵行的（比如数据改过）补一行。 */
    static void sync(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        s.soldiers.keySet().removeIf(id -> !s.roster.containsKey(id));
        for (ResidentEntity r : ctx.residents()) {
            VillageEconomy.Member m = s.roster.get(r.getUUID());
            if (m == null) {
                continue;
            }
            Optional<SoldierProfile> p = ctx.data().soldier(m.profession());
            SoldierRecord rec = s.soldiers.get(r.getUUID());
            if (p.isPresent() && rec == null) {
                rec = new SoldierRecord(m.profession(), SoldierProfile.Tier.byId(r.kitTier()), 0, Map.of(), ++s.enlistCounter);
                s.soldiers.put(r.getUUID(), rec);
            }
            if (p.isPresent() && !r.isSoldier()) {
                apply(r, p.get(), rec.tier());
            } else if (p.isEmpty() && r.isSoldier()) {
                unapply(r);
                s.soldiers.remove(r.getUUID());
            }
        }
    }

    /** 文明初始居民里的士兵人数（生成时的种子配置，§4.4）。 */
    static int initialSoldiers(CivVillageData d) {
        int n = 0;
        for (Map.Entry<String, Integer> e : d.residents().initial().entrySet()) {
            if (d.soldier(e.getKey()).isPresent()) {
                n += e.getValue();
            }
        }
        return n;
    }

    /** D11：已有存档里的生成村庄（卫兵被登记成了劳力）第一次接管时补发 1 套初始兵装，只补一次。 */
    static void seed(VillageContext ctx, boolean on) {
        VillageEconomy.State s = ctx.state();
        if (!on || s.soldierSeeded) {
            return;
        }
        s.soldierSeeded = true;
        SoldierCivRules rules = ctx.data().soldiers();
        if (ctx.village().origin() != VillageRegistry.Origin.GENERATED || !s.soldiers.isEmpty()
                || s.roster.size() < SoldierRoster.MIN_POPULATION || rules.seedKit().isEmpty()) {
            return;
        }
        Map.Entry<String, SoldierProfile.Tier> first = rules.seedKit().entrySet().iterator().next();
        ctx.data().soldier(first.getKey()).ifPresent(p -> {
            String item = p.kits().item(first.getValue());
            if (!item.isEmpty()) {
                s.stock.deposit(item, 1);
                s.log(ctx.today(), "mythic_age.village.log.kit_seeded");
            }
        });
    }

    static void convertOne(VillageContext ctx, int cap) {
        VillageEconomy.State s = ctx.state();
        CivVillageData d = ctx.data();
        List<SoldierRoster.Branch> branches = new ArrayList<>();
        for (String prof : d.soldiers().order()) {
            d.soldier(prof).ifPresent(p -> {
                CivVillageData.Profession pr = d.professions().get(prof);
                branches.add(new SoldierRoster.Branch(prof, !p.isMelee(), BasicJobTypes.stageOpen(ctx, pr.stage())));
            });
        }
        Map<String, Integer> have = new LinkedHashMap<>();
        s.soldiers.values().forEach(r -> have.merge(r.profession(), 1, Integer::sum));
        Optional<String> next = SoldierRoster.next(branches, have, cap);
        if (next.isEmpty()) {
            return;
        }
        SoldierProfile p = d.soldier(next.get()).orElseThrow();
        SoldierProfile.Tier tier = SoldierRoster.choose(s.stock.count(p.kits().item(SoldierProfile.Tier.FULL)),
                s.stock.count(p.kits().item(SoldierProfile.Tier.SIMPLE)));
        if (tier == SoldierProfile.Tier.NONE) {
            return;
        }
        List<DutyRules.Post> posts = posts(ctx);
        BlockPos first = posts.isEmpty() ? ctx.center() : VillageContext.block(posts.get(0).pos());
        List<SoldierRoster.Candidate> cands = new ArrayList<>();
        Map<UUID, ResidentEntity> byId = new LinkedHashMap<>();
        for (ResidentEntity r : ctx.residents()) {
            VillageEconomy.Member m = s.roster.get(r.getUUID());
            if (m == null) {
                continue;
            }
            byId.put(r.getUUID(), r);
            cands.add(new SoldierRoster.Candidate(r.getUUID(), m.profession(), m.keeper(), r.inTransit(),
                    Math.sqrt(m.home().distSqr(first))));
        }
        // 没加载的名册成员也算劳力人数（只是不会被选中）
        for (VillageEconomy.Member m : s.roster.values()) {
            if (!byId.containsKey(m.id())) {
                cands.add(new SoldierRoster.Candidate(m.id(), m.profession(), true, false, Double.MAX_VALUE));
            }
        }
        SoldierRoster.pickConvert(cands, d.residents().fallback(), d.soldiers().minLaborers())
                .map(byId::get).ifPresent(r -> enlist(ctx, r, next.get(), p, tier));
    }

    /** 转岗：从村库扣一套兵装，改职业，挂属性。 */
    public static boolean enlist(VillageContext ctx, ResidentEntity r, String profession, SoldierProfile p, SoldierProfile.Tier tier) {
        VillageEconomy.State s = ctx.state();
        String item = p.kits().item(tier);
        if (item.isEmpty() || s.stock.take(item, 1, 0) < 1) {
            return false;
        }
        enlistFree(s, r, profession, p, tier);
        s.log(ctx.today(), "mythic_age.village.log.enlisted", r.getName().getString(), "@mythic_age.profession." + profession.replace(':', '.'));
        return true;
    }

    /** 不扣村库的转岗（生成村庄的初始卫兵：身上那套就是 S0 预置的，§4.4）。 */
    public static void enlistFree(VillageEconomy.State s, ResidentEntity r, String profession, SoldierProfile p,
                                  SoldierProfile.Tier tier) {
        r.abortJob(false);
        r.setProfession(profession);
        VillageEconomy.Member m = s.roster.get(r.getUUID());
        if (m != null) {
            s.roster.put(r.getUUID(), m.withProfession(profession));
        }
        s.soldiers.put(r.getUUID(), new SoldierRecord(profession, tier, 0, Map.of(), ++s.enlistCounter));
        apply(r, p, tier);
    }

    /** 降回基础劳力：兵装按磨损退回村库（§4.3）。 */
    public static void demote(VillageContext ctx, ResidentEntity r) {
        VillageEconomy.State s = ctx.state();
        SoldierRecord rec = s.soldiers.remove(r.getUUID());
        if (rec != null) {
            ctx.data().soldier(rec.profession()).ifPresent(p -> giveBack(s, p, rec.tier(),
                    KitRules.onReturn(rec.tier(), rec.wear(), p.durability())));
            // 身上的弹药放回村库
            rec.ammo().forEach((item, n) -> s.stock.deposit(item, n));
        }
        String fallback = ctx.data().residents().fallback();
        r.setProfession(fallback);
        VillageEconomy.Member m = s.roster.get(r.getUUID());
        if (m != null) {
            s.roster.put(r.getUUID(), m.withProfession(fallback));
        }
        unapply(r);
        s.log(ctx.today(), "mythic_age.village.log.discharged", r.getName().getString());
    }

    /** 换一档兵装：旧的按磨损退回，新的从村库扣。 */
    static void rekit(VillageEconomy.State s, ResidentEntity r, SoldierProfile p, SoldierRecord rec) {
        SoldierProfile.Tier want = SoldierRoster.choose(s.stock.count(p.kits().item(SoldierProfile.Tier.FULL)),
                s.stock.count(p.kits().item(SoldierProfile.Tier.SIMPLE)));
        if (want == SoldierProfile.Tier.NONE || s.stock.take(p.kits().item(want), 1, 0) < 1) {
            return;
        }
        giveBack(s, p, rec.tier(), KitRules.onReturn(rec.tier(), rec.wear(), p.durability()));
        s.soldiers.put(r.getUUID(), rec.withTier(want, 0));
        apply(r, p, want);
    }

    /** 兵装耐久用完、走到谷仓后换装（§4.3）；村库没有就进入无兵装模式。 */
    public static void swapKit(ServerLevel level, ResidentEntity r) {
        r.village().flatMap(v -> VillageEconomy.get(level.getServer()).peek(v)).ifPresent(s -> {
            SoldierRecord rec = s.soldiers.get(r.getUUID());
            Optional<SoldierProfile> p = VillageData.civ(r.civ()).flatMap(d -> d.soldier(r.profession()));
            if (rec == null || p.isEmpty()) {
                return;
            }
            giveBack(s, p.get(), rec.tier(), KitRules.onReturn(rec.tier(), rec.wear(), p.get().durability()));
            SoldierProfile.Tier want = SoldierRoster.choose(s.stock.count(p.get().kits().item(SoldierProfile.Tier.FULL)),
                    s.stock.count(p.get().kits().item(SoldierProfile.Tier.SIMPLE)));
            if (want != SoldierProfile.Tier.NONE) {
                s.stock.take(p.get().kits().item(want), 1, 0);
            }
            s.soldiers.put(r.getUUID(), rec.withTier(want, 0));
            apply(r, p.get(), want);
            VillageEconomy.get(level.getServer()).setDirty();
        });
    }

    static void giveBack(VillageEconomy.State s, SoldierProfile p, SoldierProfile.Tier tier, KitRules.Returned what) {
        switch (what) {
            case SAME -> s.stock.deposit(p.kits().item(tier), 1);
            case BROKEN -> {
                if (!p.kits().broken().isEmpty()) {
                    s.stock.deposit(p.kits().broken(), 1);
                }
            }
            case SCRAP, NOTHING -> {
            }
        }
    }

    // ───────────── 属性 ─────────────

    /** 挂上兵种属性（生命、护甲），当前生命按比例换算（§3.2）。 */
    public static void apply(ResidentEntity r, SoldierProfile p, SoldierProfile.Tier tier) {
        float ratio = r.getHealth() / Math.max(1, r.getMaxHealth());
        modifier(r.getAttribute(Attributes.MAX_HEALTH), p.health() - BASE_HEALTH);
        modifier(r.getAttribute(Attributes.ARMOR), tier == SoldierProfile.Tier.NONE ? 0 : p.armor(tier));
        r.setHealth(Math.max(1, ratio * r.getMaxHealth()));
        r.setKitTier(tier.id());
        r.soldierBrain().reset();
    }

    public static void unapply(ResidentEntity r) {
        float ratio = r.getHealth() / Math.max(1, r.getMaxHealth());
        AttributeInstance h = r.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance a = r.getAttribute(Attributes.ARMOR);
        if (h != null) {
            h.removeModifier(KIT_MODIFIER);
        }
        if (a != null) {
            a.removeModifier(KIT_MODIFIER);
        }
        r.setHealth(Math.max(1, ratio * r.getMaxHealth()));
        r.setKitTier("");
        r.soldierBrain().reset();
        r.soldierBrain().setOrders(null);
    }

    private static void modifier(AttributeInstance inst, double amount) {
        if (inst == null) {
            return;
        }
        inst.removeModifier(KIT_MODIFIER);
        if (amount != 0) {
            inst.addPermanentModifier(new AttributeModifier(KIT_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // ───────────── 排班 ─────────────

    static List<DutyRules.Post> posts(VillageContext ctx) {
        Map<String, List<Pos>> anchors = new LinkedHashMap<>();
        for (String k : ctx.data().soldiers().posts()) {
            anchors.put(k, ctx.anchors(k));
        }
        return DutyRules.posts(ctx.data().soldiers().posts(), anchors, VillageContext.pos(ctx.center()));
    }

    /** 只供 GameTest：让某村的士兵全部处于某个状态（不改世界时钟）。 */
    private static final Map<UUID, DutyRules.State> FORCED = new java.util.concurrent.ConcurrentHashMap<>();

    public static void forceStateForTest(UUID village, DutyRules.State st) {
        if (st == null) {
            FORCED.remove(village);
        } else {
            FORCED.put(village, st);
        }
    }

    /** 每次规划：排班、派岗、发命令（§5.1–§5.2）。 */
    public static void plan(VillageContext ctx) {
        if (!ctx.data().soldiers().enabled()) {
            return;
        }
        VillageEconomy.State s = ctx.state();
        List<ResidentEntity> list = soldiersOf(ctx);
        if (list.isEmpty()) {
            return;
        }
        list.sort(Comparator.comparingLong(r -> s.soldiers.get(r.getUUID()).order()));
        List<DutyRules.Post> posts = posts(ctx);
        List<BlockPos> postBlocks = posts.stream().map(p -> VillageContext.block(p.pos())).toList();
        BlockPos keeper = ctx.anchors(Residents.KEEPER_ANCHOR).stream().findFirst().map(VillageContext::block).orElse(ctx.center());
        SoldierCivRules.Shifts shifts = ctx.data().soldiers().shifts();
        List<UUID> onDuty = new ArrayList<>();
        Map<UUID, DutyRules.State> states = new LinkedHashMap<>();
        DutyRules.State forced = FORCED.get(ctx.village().id());
        for (int i = 0; i < list.size(); i++) {
            DutyRules.State st = forced != null ? forced : DutyRules.state(DutyRules.shift(i, list.size()), ctx.timeOfDay(), shifts);
            states.put(list.get(i).getUUID(), st);
            if (st == DutyRules.State.ON_DUTY) {
                onDuty.add(list.get(i).getUUID());
            }
        }
        Map<UUID, DutyRules.Assignment> assign = DutyRules.assign(onDuty, posts);
        for (ResidentEntity r : list) {
            SoldierRecord rec = s.soldiers.get(r.getUUID());
            Optional<SoldierProfile> p = ctx.data().soldier(rec.profession());
            if (p.isEmpty()) {
                continue;
            }
            DutyRules.State st = states.get(r.getUUID());
            DutyRules.Assignment a = assign.get(r.getUUID());
            int post = a == null ? -1 : a.target(ctx.now(), posts.size());
            boolean swap = rec.tier() != SoldierProfile.Tier.NONE && KitRules.exhausted(rec.wear(), p.get().durability());
            SoldierOrders prev = r.soldierBrain().orders();
            if (prev != null && prev.state() != st) {
                r.soldierBrain().reset();
            }
            r.soldierBrain().setOrders(new SoldierOrders(ctx.village().id(), rec.profession(), p.get(), rec.tier(), st, postBlocks,
                    st == DutyRules.State.ON_DUTY ? post : -1, keeper, swap, ctx.center(), ctx.now()));
            if (r.soldierBrain().busy() && r.job().isPresent()) {
                r.abortJob(false);
            }
        }
    }

    /** 本村已加载的士兵（有士兵行的）。 */
    static List<ResidentEntity> soldiersOf(VillageContext ctx) {
        List<ResidentEntity> out = new ArrayList<>();
        for (ResidentEntity r : ctx.residents()) {
            if (ctx.state().soldiers.containsKey(r.getUUID()) && !r.inTransit()) {
                out.add(r);
            }
        }
        return out;
    }

    static Optional<ResidentEntity> loaded(VillageContext ctx, UUID id) {
        return ctx.residents().stream().filter(r -> r.getUUID().equals(id)).findFirst();
    }

    // ───────────── 事件 ─────────────

    /** 阵亡：完整兵装变成破损兵装入库，简易报废；不掉落任何物品（01 号 §0.5）。 */
    public static void onDied(VillageEconomy.State s, ResidentEntity r) {
        SoldierRecord rec = s.soldiers.remove(r.getUUID());
        if (rec == null) {
            return;
        }
        VillageData.civ(r.civ()).flatMap(d -> d.soldier(rec.profession()))
                .ifPresent(p -> giveBack(s, p, rec.tier(), KitRules.onDeath(rec.tier())));
    }

    /** 被命中一次：兵装耐久 −1（§4.5）。 */
    public static void onHit(ServerLevel level, ResidentEntity r) {
        r.village().flatMap(v -> VillageEconomy.get(level.getServer()).peek(v)).ifPresent(s -> {
            SoldierRecord rec = s.soldiers.get(r.getUUID());
            if (rec != null && rec.tier() != SoldierProfile.Tier.NONE) {
                s.soldiers.put(r.getUUID(), rec.withWear(rec.wear() + 1));
                VillageEconomy.get(level.getServer()).setDirty();
            }
        });
    }

    /** 被士兵护着的居民受伤：交给护他的士兵的独有机制（没有就 ×1）。 */
    public static float wardMultiplier(ResidentEntity victim, net.minecraft.world.damagesource.DamageSource source) {
        return SoldierBrain.guardOf(victim.getUUID()).map(g -> {
            SoldierOrders o = g.soldierBrain().orders();
            return o == null ? 1.0F : SoldierAbility.of(o.profile().ability())
                    .map(a -> a.protectedHurt(g, g.soldierBrain(), victim, source)).orElse(1.0F);
        }).orElse(1.0F);
    }

    // ───────────── 弹药（远程兵种，§9.2） ─────────────

    public static Optional<SoldierRecord> record(ServerLevel level, ResidentEntity r) {
        return r.village().flatMap(v -> VillageEconomy.get(level.getServer()).peek(v)).map(s -> s.soldiers.get(r.getUUID()));
    }

    /** 用掉一支。 */
    public static void useAmmo(ServerLevel level, ResidentEntity r, String item) {
        r.village().flatMap(v -> VillageEconomy.get(level.getServer()).peek(v)).ifPresent(s -> {
            SoldierRecord rec = s.soldiers.get(r.getUUID());
            if (rec != null) {
                Map<String, Integer> a = new LinkedHashMap<>(rec.ammo());
                a.merge(item, -1, Integer::sum);
                s.soldiers.put(r.getUUID(), rec.withAmmo(a));
                VillageEconomy.get(level.getServer()).setDirty();
            }
        });
    }

    /** 村库里这个兵种能用的弹药一共几支。 */
    public static int ammoInStock(ServerLevel level, java.util.UUID village, SoldierProfile p) {
        return VillageEconomy.get(level.getServer()).peek(village).map(s -> p.ammo().items().stream()
                .mapToInt(a -> s.stock.count(a.item())).sum()).orElse(0);
    }

    /** 回谷仓补到上限：先拿好的（弹药表靠前的）。 */
    public static void resupply(ServerLevel level, ResidentEntity r, SoldierProfile p) {
        r.village().flatMap(v -> VillageEconomy.get(level.getServer()).peek(v)).ifPresent(s -> {
            SoldierRecord rec = s.soldiers.get(r.getUUID());
            if (rec == null || p.ammo() == null) {
                return;
            }
            Map<String, Integer> a = new LinkedHashMap<>(rec.ammo());
            int need = p.ammo().carry() - rec.ammoTotal();
            for (SoldierProfile.AmmoItem it : p.ammo().items()) {
                if (need <= 0) {
                    break;
                }
                int got = s.stock.take(it.item(), need, 0);
                a.merge(it.item(), got, Integer::sum);
                need -= got;
            }
            s.soldiers.put(r.getUUID(), rec.withAmmo(a));
            VillageEconomy.get(level.getServer()).setDirty();
        });
    }

    /**
     * 捡回一支落地的弹药：按它的 UUID 定种子，{@code recover} 的概率完好回村库，其余计损耗（01 号「每支 60% 完好可回库」）。
     * 返回是否回了库。
     */
    public static boolean recoverAmmo(ServerLevel level, SoldierAmmo ammo, SoldierProfile p) {
        java.util.UUID id = ((net.minecraft.world.entity.Entity) ammo).getUUID();
        boolean intact = intact(id, p.ammo().recover());
        if (intact) {
            VillageEconomy.get(level.getServer()).peek(ammo.ammoVillage()).ifPresent(s -> {
                s.stock.deposit(ammo.ammoItem(), 1);
                VillageEconomy.get(level.getServer()).setDirty();
            });
        }
        return intact;
    }

    /** 这支弹药捡回来是不是完好的（按 UUID 定种子，可复现）。 */
    public static boolean intact(java.util.UUID id, double recover) {
        long h = id.getMostSignificantBits() * 31 + id.getLeastSignificantBits();
        return Math.floorMod(h ^ (h >>> 29), 1000) < recover * 1000;
    }

    /** 村庄 → 已经通知过独有机制的警戒场次。 */
    private static final Map<UUID, Long> BROADCAST = new java.util.concurrent.ConcurrentHashMap<>();

    /** 新一场警戒：本村每名已加载士兵的独有机制各收到一次（同一场只通知一次）。 */
    public static void broadcastAlert(ServerLevel level, UUID village, long serial, net.minecraft.world.entity.LivingEntity threat) {
        Long seen = BROADCAST.put(village, serial);
        if (seen != null && seen == serial) {
            return;
        }
        VillageThreats.setAlertThreat(village, threat);
        for (ResidentEntity s : VillageScheduler.loaded(village)) {
            if (!s.isSoldier()) {
                continue;
            }
            SoldierOrders o = s.soldierBrain().orders();
            if (o != null) {
                SoldierAbility.of(o.profile().ability()).ifPresent(a -> a.onAlert(s, s.soldierBrain(), serial));
            }
        }
    }

    public static void clearForTest(UUID village) {
        BROADCAST.remove(village);
    }

    /** 当前警戒场次（独有机制「每场一次」用）。 */
    public static long alertSerial(UUID village) {
        return VillageScheduler.runtime(village).alertSerial();
    }
}
