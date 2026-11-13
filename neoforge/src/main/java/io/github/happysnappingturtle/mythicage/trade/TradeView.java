package io.github.happysnappingturtle.mythicage.trade;

import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.rite.RitesView;
import java.util.Optional;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易界面的显示数据（T9 设计文档 §9）。全部由服务端算好，客户端只展示，不做判定。
 *
 * @param civ              本次交易的文明（来自祭司或神庙，T10b）
 * @param labels           与文明有关的文本 key（来自文明档案；客户端没有档案数据）
 * @param guardianEntityId 祭司实体 id；在主神像处为 −1
 * @param villagePatron    本村主神 / 此像主神；无村庄的祭司为空
 * @param villageStage     本村阶段（T11a）；主神像处为 NONE
 * @param rites            晋升页数据（T11a）；只在有村庄的祭司处
 * @param villageNotes     祭司对话页上关于本村的提示（T11a 阶段 ③：「需要更多住居」）
 */
public record TradeView(Civilization civ, Labels labels, TradeOffer.Venue venue, int guardianEntityId, Dialogue dialogue,
                        long favor, long cap, Age age, List<OfferView> offers, String villagePatron, Age villageStage,
                        Optional<RitesView> rites, List<Component> villageNotes) {

    /** @param favor 「X 恩宠」；shrine 神庙名 */
    public record Labels(String favor, String shrine) {
    }

    /** 对话页要显示的按钮。 */
    /** REISSUE：补领神坛（玩家神坛的文明）；JOIN：在这里供奉（村庄神庙的文明，GA-13）。 */
    public enum Dialogue { NONE, FIRST, REISSUE, JOIN }

    /**
     * @param favorPrice     恩宠价（已含折扣）；物品价商品为 0
     * @param lock           列出但不能买的原因（时代、次神、本村阶段、次神祠、主神不符）；可买时为 OK
     * @param lockArg        原因的参数：次神 / 主神 id 或时代 id
     * @param remainingToday 今日剩余次数，不限购为 −1
     */
    public record OfferView(String id, TradeOffer.Category category, ItemStack result, long favorPrice,
                            List<ItemStack> itemPrice, Age minAge, TradeReason lock, String lockArg, int remainingToday,
                            boolean affordable) {
        public boolean locked() {
            return lock != TradeReason.OK;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeView> STREAM_CODEC = StreamCodec.of(
            TradeView::write, TradeView::read);

    private static void write(RegistryFriendlyByteBuf buf, TradeView v) {
        buf.writeEnum(v.civ);
        buf.writeUtf(v.labels.favor());
        buf.writeUtf(v.labels.shrine());
        buf.writeEnum(v.venue);
        buf.writeVarInt(v.guardianEntityId);
        buf.writeEnum(v.dialogue);
        buf.writeVarLong(v.favor);
        buf.writeVarLong(v.cap);
        buf.writeEnum(v.age);
        buf.writeVarInt(v.offers.size());
        for (OfferView o : v.offers) {
            buf.writeUtf(o.id);
            buf.writeEnum(o.category);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, o.result);
            buf.writeVarLong(o.favorPrice);
            buf.writeVarInt(o.itemPrice.size());
            for (ItemStack s : o.itemPrice) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, s);
            }
            buf.writeEnum(o.minAge);
            buf.writeEnum(o.lock);
            buf.writeUtf(o.lockArg);
            buf.writeVarInt(o.remainingToday);
            buf.writeBoolean(o.affordable);
        }
        buf.writeUtf(v.villagePatron);
        buf.writeEnum(v.villageStage);
        buf.writeBoolean(v.rites.isPresent());
        v.rites.ifPresent(r -> RitesView.STREAM_CODEC.encode(buf, r));
        buf.writeVarInt(v.villageNotes.size());
        v.villageNotes.forEach(c -> ComponentSerialization.STREAM_CODEC.encode(buf, c));
    }

    private static TradeView read(RegistryFriendlyByteBuf buf) {
        Civilization civ = buf.readEnum(Civilization.class);
        Labels labels = new Labels(buf.readUtf(), buf.readUtf());
        TradeOffer.Venue venue = buf.readEnum(TradeOffer.Venue.class);
        int entityId = buf.readVarInt();
        Dialogue dialogue = buf.readEnum(Dialogue.class);
        long favor = buf.readVarLong();
        long cap = buf.readVarLong();
        Age age = buf.readEnum(Age.class);
        int n = buf.readVarInt();
        List<OfferView> offers = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf();
            TradeOffer.Category category = buf.readEnum(TradeOffer.Category.class);
            ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            long favorPrice = buf.readVarLong();
            int m = buf.readVarInt();
            List<ItemStack> price = new ArrayList<>(m);
            for (int j = 0; j < m; j++) {
                price.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            }
            offers.add(new OfferView(id, category, result, favorPrice, price, buf.readEnum(Age.class),
                    buf.readEnum(TradeReason.class), buf.readUtf(), buf.readVarInt(), buf.readBoolean()));
        }
        String patron = buf.readUtf();
        Age stage = buf.readEnum(Age.class);
        Optional<RitesView> rites = buf.readBoolean() ? Optional.of(RitesView.STREAM_CODEC.decode(buf)) : Optional.empty();
        int k = buf.readVarInt();
        if (k > 16) {
            throw new IllegalArgumentException("村庄提示过多：" + k);
        }
        List<Component> notes = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            notes.add(ComponentSerialization.STREAM_CODEC.decode(buf));
        }
        return new TradeView(civ, labels, venue, entityId, dialogue, favor, cap, age, offers, patron, stage, rites, notes);
    }
}
