package io.github.happysnappingturtle.mythicage.trade;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易界面的显示数据（T9 设计文档 §9）。全部由服务端算好，客户端只展示，不做判定。
 *
 * @param guardianEntityId 守护者实体 id；在祭坛兑换时为 −1
 * @param villagePatron    祭司所在村庄的主神；不在村庄或祭坛兑换时为空
 * @param patronGods       该主神名下的次神，格式「时代:次神 id」（对话页展示）
 */
public record TradeView(TradeOffer.Venue venue, int guardianEntityId, Dialogue dialogue, long favor, long cap, Age age,
                        List<OfferView> offers, String villagePatron, List<String> patronGods) {

    /** 对话页要显示的按钮。 */
    public enum Dialogue { NONE, FIRST, REISSUE }

    /**
     * @param favorPrice     恩宠价（已含折扣）；物品价商品为 0
     * @param remainingToday 今日剩余次数，不限购为 −1
     * @param needsMinorGod  因未供奉某位次神而锁定时为该次神 id，否则为空
     */
    public record OfferView(String id, TradeOffer.Category category, ItemStack result, long favorPrice,
                            List<ItemStack> itemPrice, Age minAge, boolean locked, int remainingToday,
                            boolean affordable, String needsMinorGod) {
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeView> STREAM_CODEC = StreamCodec.of(
            TradeView::write, TradeView::read);

    private static void write(RegistryFriendlyByteBuf buf, TradeView v) {
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
            buf.writeBoolean(o.locked);
            buf.writeVarInt(o.remainingToday);
            buf.writeBoolean(o.affordable);
            buf.writeUtf(o.needsMinorGod);
        }
        buf.writeUtf(v.villagePatron);
        buf.writeVarInt(v.patronGods.size());
        v.patronGods.forEach(buf::writeUtf);
    }

    private static TradeView read(RegistryFriendlyByteBuf buf) {
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
            offers.add(new OfferView(id, category, result, favorPrice, price, buf.readEnum(Age.class), buf.readBoolean(),
                    buf.readVarInt(), buf.readBoolean(), buf.readUtf()));
        }
        String patron = buf.readUtf();
        int g = buf.readVarInt();
        List<String> gods = new ArrayList<>(g);
        for (int i = 0; i < g; i++) {
            gods.add(buf.readUtf());
        }
        return new TradeView(venue, entityId, dialogue, favor, cap, age, offers, patron, gods);
    }
}
