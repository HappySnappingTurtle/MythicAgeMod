package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.relic.EffectGroups;
import io.github.happysnappingturtle.mythicage.core.relic.RelicBinding;
import io.github.happysnappingturtle.mythicage.core.relic.RelicBook;
import io.github.happysnappingturtle.mythicage.core.relic.RelicCard;
import io.github.happysnappingturtle.mythicage.core.relic.RelicSlots;
import io.github.happysnappingturtle.mythicage.core.relic.RelicState;
import io.github.happysnappingturtle.mythicage.player.CapBonusProvider;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.rite.RiteHooks;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 圣物的唯一入口（T16 设计文档 §4–§6）：佩戴检查、供奉与取出、对账，以及全局类效果的查询。
 * 物品实际放在 Curios 槽或神庙供奉架里，绑定表只记录「这名玩家的这件圣物现在在哪」；两者对不上时以实际位置为准。
 */
public final class RelicService {
    public static final ResourceLocation SLOT_HEROIC = id("relic_slots/heroic");
    public static final ResourceLocation SLOT_MYTHIC = id("relic_slots/mythic");
    /** 属性修饰按效果组使用固定 id（06 号 §0.4），重进或重复对账不会叠加。 */
    public static final ResourceLocation MAX_HEALTH_SELF = id("relic/" + EffectGroups.MAX_HEALTH_SELF_FLAT);

    /** 只供 GameTest：FakePlayer 不在玩家列表里，登记后按「在线」处理。 */
    private static final Map<UUID, ServerPlayer> TEST_ONLINE = new java.util.concurrent.ConcurrentHashMap<>();

    private RelicService() {
    }

    /** 在线的玩家（玩家列表里的，或测试登记的）；不在线为 null。 */
    @org.jetbrains.annotations.Nullable
    public static ServerPlayer online(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p != null ? p : TEST_ONLINE.get(id);
    }

    public static void onlineForTest(ServerPlayer player, boolean online) {
        if (online) {
            TEST_ONLINE.put(player.getUUID(), player);
        } else {
            TEST_ONLINE.remove(player.getUUID());
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }

    public static void init() {
        CapBonusProvider.Holder.set(RelicService::capBonus);
        PrayerService.setRelicBonus(RelicService::prayerR);
        RiteHooks.afterPromotion((player, civ, age, god, village) -> reconcile(player));
    }

    // ───────────── Curios ─────────────

    static Optional<ICurioStacksHandler> handler(LivingEntity e) {
        return CuriosApi.getCuriosInventory(e).flatMap(i -> i.getStacksHandler(RelicItems.SLOT));
    }

    /** 另一个圣物槽里已有同一件圣物（同一物品 = 同一张卡，D3）。客户端也能判断。 */
    static boolean duplicateElsewhere(LivingEntity e, int index, ItemStack stack) {
        Optional<ICurioStacksHandler> h = handler(e);
        if (h.isEmpty()) {
            return false;
        }
        IDynamicStackHandler stacks = h.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (i != index && stacks.getStackInSlot(i).is(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    /** Curios 的 canEquip：服务器端还要看绑定表（§4.1）；客户端没有绑定表，只检查同件不重复。 */
    public static boolean canWear(LivingEntity e, int index, ItemStack stack) {
        if (!(e instanceof Player) || duplicateElsewhere(e, index, stack)) {
            return false;
        }
        if (!(e instanceof ServerPlayer p)) {
            return true;
        }
        Optional<RelicCard> card = RelicCards.of(stack);
        if (card.isEmpty()) {
            return false;
        }
        Optional<RelicBinding> b = MythData.get(p).relics().get(card.get().id());
        if (b.isEmpty()) {
            return true;
        }
        UUID inst = RelicItems.instance(stack).orElse(null);
        if (b.get().state() == RelicState.WORN && b.get().instance().equals(inst)) {
            return true;
        }
        p.displayClientMessage(occupied(stack, b.get()), true);
        return false;
    }

    static Component occupied(ItemStack stack, RelicBinding b) {
        return switch (b.state()) {
            case WORN -> Component.translatable("mythic_age.relic.occupied.worn", stack.getHoverName());
            case OFFERED -> Component.translatable("mythic_age.relic.occupied.offered", stack.getHoverName(),
                    describe(b.where()));
            default -> Component.translatable("mythic_age.relic.occupied.other", stack.getHoverName());
        };
    }

    private static String describe(String where) {
        Optional<GlobalPos> g = parseWhere(where);
        return g.map(p -> p.pos().getX() + ", " + p.pos().getY() + ", " + p.pos().getZ()).orElse(where);
    }

    static String whereOf(ServerLevel level, BlockPos pos) {
        return RelicBinding.shrineWhere(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    static Optional<GlobalPos> parseWhere(String where) {
        int bar = where.indexOf('|');
        if (bar < 0) {
            return Optional.empty();
        }
        ResourceLocation dim = ResourceLocation.tryParse(where.substring(0, bar));
        String[] xyz = where.substring(bar + 1).split(",");
        if (dim == null || xyz.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim),
                    new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]))));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    // ───────────── 供奉与取出（§4.2） ─────────────

    /** 手持圣物潜行右键神庙。返回是否处理了这次交互（不是圣物、或这种神庙不接受供奉时为 false）。 */
    public static boolean offer(ServerPlayer player, ServerLevel level, BlockPos pos, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        Optional<RelicCard> card = RelicCards.of(held);
        if (card.isEmpty() || !(level.getBlockEntity(pos) instanceof ShrineBlockEntity be)) {
            return false;
        }
        Optional<RelicAnchor> anchor = RelicAnchor.Registry.forKind(be.kind());
        if (anchor.isEmpty()) {
            return false;
        }
        Optional<Component> error = offerError(player, be, card.get(), held);
        if (error.isPresent()) {
            player.displayClientMessage(error.get(), true);
            return true;
        }
        UUID inst = RelicItems.ensureInstance(held);
        RelicBook book = MythData.get(player).relics();
        book.bind(card.get().id(), new RelicBinding(RelicState.OFFERED, inst, card.get().civ(), whereOf(level, pos)));
        if (!be.relics().add(held)) {
            book.release(card.get().id(), inst);
            return true;
        }
        held.shrink(1);
        OfferedIndex.update(level, be);
        changed(player);
        player.displayClientMessage(Component.translatable("mythic_age.relic.offered", be.relics().stacks()
                .get(be.relics().stacks().size() - 1).getHoverName()), true);
        RelicPayloads.sendShelf(player, level, pos);
        return true;
    }

    /** 供奉的拒绝原因（§4.2 的顺序）；可以供奉时为空。 */
    public static Optional<Component> offerError(ServerPlayer player, ShrineBlockEntity be, RelicCard card, ItemStack held) {
        if (!be.owner().map(player.getUUID()::equals).orElse(false)) {
            return Optional.of(Component.translatable("mythic_age.relic.not_owner"));
        }
        Civilization civ = be.kind().civ();
        if (card.civ() != civ) {
            return Optional.of(Component.translatable("mythic_age.relic.wrong_civ",
                    Component.translatable("mythic_age.civ." + card.civ().id())));
        }
        PlayerMythData data = MythData.get(player);
        Optional<RelicBinding> b = data.relics().get(card.id());
        if (b.isPresent()) {
            return Optional.of(occupied(held, b.get()));
        }
        int used = data.relics().used(RelicState.OFFERED, civ);
        int cap = RelicSlots.offerSlots(data.civ(civ).age());
        if (used >= cap) {
            return Optional.of(Component.translatable("mythic_age.relic.full", used, cap));
        }
        if (be.relics().isFull()) {
            return Optional.of(Component.translatable("mythic_age.relic.shelf_full"));
        }
        return Optional.empty();
    }

    /** 神坛界面里点「取出」。 */
    public static boolean retrieve(ServerPlayer player, ServerLevel level, BlockPos pos, int slot) {
        if (!(level.getBlockEntity(pos) instanceof ShrineBlockEntity be)
                || !be.owner().map(player.getUUID()::equals).orElse(false)) {
            player.displayClientMessage(Component.translatable("mythic_age.relic.not_owner"), true);
            return false;
        }
        ItemStack s = be.relics().remove(slot);
        if (s.isEmpty()) {
            return false;
        }
        RelicCards.of(s).ifPresent(c -> RelicItems.instance(s).ifPresent(i -> MythData.get(player).relics().release(c.id(), i)));
        OfferedIndex.update(level, be);
        player.displayClientMessage(Component.translatable("mythic_age.relic.retrieved", s.getHoverName()), true);
        if (!player.getInventory().add(s)) {
            player.drop(s, false);
        }
        changed(player);
        RelicPayloads.sendShelf(player, level, pos);
        return true;
    }

    /** 神庙被移除（§2.3）：架上的物品原地掉落；主人在线就立刻解除绑定，不在线等他上线对账。 */
    public static void onShrineRemoved(ServerLevel level, BlockPos pos, ShrineBlockEntity be) {
        List<ItemStack> dropped = be.relics().clear();
        OfferedIndex.remove(level, pos);
        if (dropped.isEmpty()) {
            return;
        }
        ServerPlayer owner = be.owner().map(id -> online(level.getServer(), id)).orElse(null);
        for (ItemStack s : dropped) {
            if (owner != null) {
                RelicCards.of(s).ifPresent(c -> RelicItems.instance(s).ifPresent(i -> MythData.get(owner).relics().release(c.id(), i)));
            }
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, s);
        }
        if (owner != null) {
            changed(owner);
        }
    }

    // ───────────── 对账（§5） ─────────────

    /** 按实际槽位与神庙重建绑定表与佩戴效果。可以重复调用，结果不变。 */
    public static void reconcile(ServerPlayer player) {
        PlayerMythData data = MythData.get(player);
        RelicBook book = data.relics();
        boolean changed = updateSlots(player, data);

        // 槽里的圣物
        record Worn(int slot, ItemStack stack, RelicCard card, UUID instance) {
        }
        List<Worn> worn = new ArrayList<>();
        Optional<ICurioStacksHandler> h = handler(player);
        if (h.isPresent()) {
            IDynamicStackHandler stacks = h.get().getStacks();
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack s = stacks.getStackInSlot(i);
                Optional<RelicCard> c = RelicCards.of(s);
                if (c.isEmpty()) {
                    continue;
                }
                if (!seen.add(c.get().id())) {
                    eject(player, stacks, i, null);
                    continue;
                }
                worn.add(new Worn(i, s, c.get(), RelicItems.ensureInstance(s)));
            }
        }
        // WORN：槽里没有同 ID、同实例的物品 → 删除
        for (Map.Entry<String, RelicBinding> e : List.copyOf(book.view().entrySet())) {
            if (e.getValue().state() == RelicState.WORN && worn.stream()
                    .noneMatch(w -> w.card().id().equals(e.getKey()) && w.instance().equals(e.getValue().instance()))) {
                book.remove(e.getKey());
                changed = true;
            }
        }
        // 槽里没有绑定的补上 WORN；被别处占用的退回
        for (Worn w : worn) {
            Optional<RelicBinding> b = book.get(w.card().id());
            if (b.isEmpty()) {
                book.bind(w.card().id(), new RelicBinding(RelicState.WORN, w.instance(), w.card().civ(),
                        RelicBinding.curioWhere(w.slot())));
                changed = true;
            } else if (b.get().state() != RelicState.WORN) {
                eject(player, h.get().getStacks(), w.slot(), b.get());
            }
        }
        // OFFERED：神庙已不存在，或已加载但架上没有这件 → 删除；区块未加载 → 保留
        ShrineRegistry reg = ShrineRegistry.get(player.server);
        for (Map.Entry<String, RelicBinding> e : List.copyOf(book.view().entrySet())) {
            RelicBinding b = e.getValue();
            if (b.state() != RelicState.OFFERED) {
                continue;
            }
            Optional<GlobalPos> g = parseWhere(b.where());
            if (g.isEmpty() || reg.get(g.get()).isEmpty()) {
                book.remove(e.getKey());
                changed = true;
                continue;
            }
            ServerLevel level = player.server.getLevel(g.get().dimension());
            if (level != null && level.isLoaded(g.get().pos())
                    && !(level.getBlockEntity(g.get().pos()) instanceof ShrineBlockEntity be && be.relics().contains(b.instance()))) {
                book.remove(e.getKey());
                changed = true;
            }
        }
        applyWorn(player);
        if (changed) {
            changed(player);
        }
    }

    private static void eject(ServerPlayer player, IDynamicStackHandler stacks, int slot, RelicBinding occupiedBy) {
        ItemStack s = stacks.getStackInSlot(slot).copy();
        stacks.setStackInSlot(slot, ItemStack.EMPTY);
        player.displayClientMessage(occupiedBy == null
                ? Component.translatable("mythic_age.relic.ejected.duplicate", s.getHoverName())
                : occupied(s, occupiedBy), true);
        if (!player.getInventory().add(s)) {
            player.drop(s, false);
        }
    }

    /** 圣物槽数：基础 1 + 英雄 + 神话（固定 id 的临时修饰）。返回是否改动。 */
    static boolean updateSlots(ServerPlayer player, PlayerMythData data) {
        int n = RelicSlots.curioSlots(data.civsView().values().stream().map(PlayerMythData.CivState::age).toList());
        Optional<ICurioStacksHandler> h = handler(player);
        if (h.isEmpty()) {
            return false;
        }
        boolean a = slotModifier(h.get(), SLOT_HEROIC, n >= 2);
        boolean b = slotModifier(h.get(), SLOT_MYTHIC, n >= 3);
        return a | b;
    }

    private static boolean slotModifier(ICurioStacksHandler h, ResourceLocation id, boolean want) {
        boolean has = h.getModifiers().containsKey(id);
        if (want && !has) {
            h.addTransientModifier(new AttributeModifier(id, 1, AttributeModifier.Operation.ADD_VALUE));
            return true;
        }
        if (!want && has) {
            h.removeModifier(id);
            return true;
        }
        return false;
    }

    /** 绑定表变化之后：同步（恩宠上限在同步包里）、刷新主人希腊神坛的产出估算（快照里的 R）。 */
    static void changed(ServerPlayer player) {
        MythData.changed(player);
        applyWorn(player);
        for (ShrineRegistry.Entry e : ShrineRegistry.get(player.server).ownedBy(player.getUUID(), Civilization.GREEK)) {
            ServerLevel level = player.server.getLevel(e.pos().dimension());
            if (level != null && level.isLoaded(e.pos().pos()) && level.getBlockEntity(e.pos().pos()) instanceof GreekAltarBlockEntity be) {
                PrayerService.refreshEstimate(be);
            }
        }
    }

    // ───────────── 效果（§6） ─────────────

    /** 某名玩家某模式下某效果组的数值（同组取最大值）。 */
    public static double value(RelicBook book, RelicState mode, String group) {
        double v = 0;
        for (Map.Entry<String, RelicBinding> e : book.view().entrySet()) {
            if (e.getValue().state() != mode) {
                continue;
            }
            Optional<RelicCard> c = RelicCards.byId(e.getKey());
            if (c.isPresent()) {
                v = Math.max(v, c.get().value(mode, group));
            }
        }
        return v;
    }

    public static double worn(ServerPlayer player, String group) {
        return value(MythData.get(player).relics(), RelicState.WORN, group);
    }

    /** 恩宠上限的圣物加成（替换 {@link CapBonusProvider}）：只算佩戴。 */
    public static long capBonus(ServerPlayer player, Civilization civ) {
        return Math.round(worn(player, EffectGroups.favorCap(civ)));
    }

    /** 祈祷公式里的 R：主人在线时读供奉的圣物；不在线为 0（A2：下线不入账）。 */
    public static double prayerR(UUID owner) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer p = server == null ? null : online(server, owner);
        if (p == null) {
            return 0;
        }
        return value(MythData.get(p).relics(), RelicState.OFFERED, EffectGroups.prayerR(Civilization.GREEK));
    }

    /** 佩戴的属性效果：生命上限固定值。加上时不回血；拿掉后把当前生命截到新上限。 */
    static void applyWorn(ServerPlayer player) {
        setHealthModifier(player, MAX_HEALTH_SELF, worn(player, EffectGroups.MAX_HEALTH_SELF_FLAT),
                AttributeModifier.Operation.ADD_VALUE);
    }

    /** 设置（或在 amount ≤ 0 时移除）一条固定 id 的生命上限临时修饰，并截断当前生命。 */
    static void setHealthModifier(LivingEntity e, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance attr = e.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        AttributeModifier old = attr.getModifier(id);
        if (amount <= 0) {
            if (old != null) {
                attr.removeModifier(id);
            }
        } else if (old == null || old.amount() != amount || old.operation() != op) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
        if (e.getHealth() > e.getMaxHealth()) {
            e.setHealth(e.getMaxHealth());
        }
    }

    /** 供 GameTest：当前已用供奉位与上限。 */
    public static int[] offerUsage(ServerPlayer player, Civilization civ) {
        PlayerMythData d = MythData.get(player);
        return new int[]{d.relics().used(RelicState.OFFERED, civ), RelicSlots.offerSlots(d.civ(civ).age())};
    }
}
