package io.github.happysnappingturtle.mythicage.village.life;

import io.github.happysnappingturtle.mythicage.content.greek.village.GreekFestivalItems;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.life.FestivalRules;
import io.github.happysnappingturtle.mythicage.core.village.life.LifeData;
import io.github.happysnappingturtle.mythicage.core.village.life.OracleRules;
import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.wild.WildCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 神谕所（T19 设计文档 §1.10 J6，V3-希腊-居民 §5.6，H6）：神谕日祭司在内室坐诊，右键祭司打开「神谕」场所的兑换（只卖小情报）；
 * 平日右键神谕所提示下一个神谕日，闭门的期提示闭门。答案写在神谕铅板的说明行里。
 */
public final class OracleService {
    public static final String DUTY = "oracle";
    public static final String STORM = "oracle_storm", CREATURE = "oracle_creature";
    /** 神兽踪迹的搜索半径（格）与步长。 */
    public static final int SEARCH_RADIUS = 2400, SEARCH_STEP = 64;

    private OracleService() {
    }

    /** 祭司此刻在神谕所坐诊。 */
    public static boolean seated(GuardianEntity g) {
        return g.atErrand(DUTY);
    }

    /** 村庄神谕所的开闭（没有神谕所、阶段不够为 empty）。 */
    public static Optional<OracleRules.Status> status(net.minecraft.server.MinecraftServer server, VillageRegistry.Village v) {
        Optional<CivVillageData> d = VillageData.civ(v.civ());
        Optional<VillagePlans.Plan> plan = VillagePlans.get(server).byVillage(v.id());
        if (d.isEmpty() || plan.isEmpty()) {
            return Optional.empty();
        }
        LifeData.Oracle o = d.get().life().oracle();
        if (!o.enabled() || plan.get().anchors(o.anchor()).isEmpty() || !FestivalRules.open(o.minAge(), v.stage())) {
            return Optional.empty();
        }
        VillageEconomy.State s = VillageEconomy.get(server).state(v.id());
        return Optional.of(OracleRules.status(d.get().calendar(), VillageData.offset(d.get(), s), MythDays.today(server),
                o.special(), o.closedPeriod()));
    }

    /** 关着时的提示文字。 */
    public static Component closedMessage(OracleRules.Status st) {
        return switch (st.state()) {
            case WINTER -> Component.translatable("mythic_age.oracle.winter", st.nextDay());
            case CLOSED -> Component.translatable("mythic_age.oracle.closed", st.nextDay(), st.daysUntil());
            case OPEN -> Component.translatable("mythic_age.oracle.open");
        };
    }

    /** 右键神谕所的方块：开着说去找祭司，关着说下一个神谕日（不拦截原本的交互）。 */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        oracleAt(player.serverLevel(), event.getPos()).flatMap(v -> status(player.server, v))
                .ifPresent(st -> player.displayClientMessage(closedMessage(st), true));
    }

    /** 这一格属于哪个村庄的神谕所。 */
    public static Optional<VillageRegistry.Village> oracleAt(ServerLevel level, BlockPos pos) {
        VillageEconomy eco = VillageEconomy.get(level.getServer());
        for (VillagePlans.Plan p : VillagePlans.get(level.getServer()).all()) {
            if (!p.dim().equals(level.dimension())) {
                continue;
            }
            Optional<VillageRegistry.Village> v = VillageRegistry.get(level.getServer()).byId(p.village());
            Optional<CivVillageData> d = v.flatMap(x -> VillageData.civ(x.civ()));
            if (d.isEmpty() || !d.get().life().oracle().enabled()) {
                continue;
            }
            String anchor = d.get().life().oracle().anchor();
            Optional<VillageEconomy.State> s = eco.peek(p.village());
            if (s.isEmpty()) {
                continue;
            }
            for (VillageEconomy.Built b : s.get().constructed) {
                BuildingSpec spec = VillageBuildings.all().get(b.building());
                if (spec != null && !spec.anchors(anchor).isEmpty() && b.box().containsXZ(pos.getX(), pos.getZ())
                        && pos.getY() >= b.box().minY() && pos.getY() <= b.box().maxY()) {
                    return v;
                }
            }
        }
        return Optional.empty();
    }

    // ───────────── 答案 ─────────────

    /** 兑换的结果：一块写着答案的神谕铅板。 */
    public static ItemStack answer(ServerPlayer player, String offer) {
        List<Component> lines = offer.equals(STORM) ? storm(player.serverLevel()) : creature(player);
        ItemStack st = tablet(offer);
        List<Component> lore = new ArrayList<>();
        lines.forEach(l -> lore.add(l.copy().withStyle(s -> s.withItalic(false).withColor(net.minecraft.ChatFormatting.GRAY))));
        st.set(DataComponents.LORE, new ItemLore(lore));
        return st;
    }

    /** 写着问题名的空白铅板（界面上显示的商品）。 */
    public static ItemStack tablet(String offer) {
        ItemStack st = new ItemStack(GreekFestivalItems.ORACLE_TABLET.get());
        st.set(DataComponents.ITEM_NAME, Component.translatable("mythic_age.oracle.offer." + offer));
        return st;
    }

    static String days(long ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 24000.0);
    }

    /**
     * 雷雨之兆：按世界当前的天气计时（原版天气循环：下一次切换的时刻是确定的，之后的长短是随机的）。
     * 只说确定的部分：雨什么时候来 / 停，这场雨里会不会起雷（雷的切换落在最短雨期 12000 刻内就是「有」）。
     */
    public static List<Component> storm(ServerLevel level) {
        if (!(level.getLevelData() instanceof ServerLevelData w)) {
            return List.of(Component.translatable("mythic_age.oracle.storm.unknown"));
        }
        List<Component> out = new ArrayList<>();
        if (w.getClearWeatherTime() > 0) {
            out.add(Component.translatable("mythic_age.oracle.storm.clear", days(w.getClearWeatherTime())));
            out.add(Component.translatable("mythic_age.oracle.storm.thunder_yes"));
            return out;
        }
        int rain = w.getRainTime();
        int thunder = w.getThunderTime();
        if (w.isRaining()) {
            out.add(Component.translatable("mythic_age.oracle.storm.ends", days(rain)));
            boolean yes = w.isThundering() || thunder < rain;
            out.add(Component.translatable(yes ? "mythic_age.oracle.storm.thunder_yes" : "mythic_age.oracle.storm.thunder_no"));
        } else {
            out.add(Component.translatable("mythic_age.oracle.storm.comes", days(rain)));
            boolean yes = w.isThundering() ? thunder > rain : thunder >= rain && thunder < rain + 12000;
            out.add(Component.translatable(yes ? "mythic_age.oracle.storm.thunder_yes" : "mythic_age.oracle.storm.thunder_maybe"));
        }
        return out;
    }

    /** 神兽的踪迹：随机一种本文明的野外神话生物，找最近一处它出没的生物群系。 */
    public static List<Component> creature(ServerPlayer player) {
        List<WildCreatureCard> cards = WildCreatures.all().values().stream()
                .filter(c -> c.civ().equals("greek") && !c.biomes().isEmpty()).sorted(java.util.Comparator.comparing(WildCreatureCard::id))
                .toList();
        if (cards.isEmpty()) {
            return List.of(Component.translatable("mythic_age.oracle.creature.silent"));
        }
        WildCreatureCard card = cards.get(player.getRandom().nextInt(cards.size()));
        Component name = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(card.entity())).getDescription();
        Predicate<Holder<Biome>> match = h -> card.biomes().stream().anyMatch(b -> b.startsWith("#")
                ? h.is(TagKey.create(Registries.BIOME, ResourceLocation.parse(b.substring(1))))
                : h.is(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(b))));
        var found = player.serverLevel().findClosestBiome3d(match, player.blockPosition(), SEARCH_RADIUS, SEARCH_STEP, 64);
        if (found == null) {
            return List.of(Component.translatable("mythic_age.oracle.creature.none", name, SEARCH_RADIUS));
        }
        BlockPos at = found.getFirst();
        double dx = at.getX() - player.getX();
        double dz = at.getZ() - player.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz) / 50.0) * 50;
        Component biome = Component.translatable(found.getSecond().unwrapKey()
                .map(k -> net.minecraft.Util.makeDescriptionId("biome", k.location())).orElse("biome.minecraft.plains"));
        return List.of(Component.translatable("mythic_age.oracle.creature", name),
                Component.translatable("mythic_age.oracle.creature.where", Component.translatable("mythic_age.dir." + direction(dx, dz)),
                        Math.max(50, dist), biome));
    }

    /** 8 方位（+x 东、+z 南）。 */
    public static String direction(double dx, double dz) {
        String[] names = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};
        double a = Math.atan2(dz, dx);
        int i = (int) Math.floorMod(Math.round(a / (Math.PI / 4)), 8L);
        return names[i];
    }
}
