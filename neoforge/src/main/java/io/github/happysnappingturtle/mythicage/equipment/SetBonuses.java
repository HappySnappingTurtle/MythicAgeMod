package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.equipment.EquipmentTable;
import io.github.happysnappingturtle.mythicage.core.equipment.PhalanxRules;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 套装效果（T17 设计文档 §4.1）：每名玩家每 20 刻刷新一次（按 UUID 错开），结果写成固定 id 的临时属性修饰，
 * 重复刷新不叠加；换装、死亡、重进都在下一次刷新时纠正。各文明的套装在这里登记一条 {@link Entry}。
 */
public final class SetBonuses {
    public static final int PERIOD = 20;

    /** 一条套装效果：算出某属性修饰的值（0 = 移除）。 */
    public interface Entry {
        ResourceLocation id();

        double value(ServerPlayer player);

        net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute();
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private SetBonuses() {
    }

    public static synchronized void register(Entry e) {
        ENTRIES.add(e);
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Math.floorMod(p.getUUID().hashCode() + tick, PERIOD) == 0) {
                refresh(p);
            }
        }
    }

    public static void refresh(ServerPlayer player) {
        for (Entry e : ENTRIES) {
            AttributeInstance attr = player.getAttribute(e.attribute());
            if (attr == null) {
                continue;
            }
            double v = e.value(player);
            AttributeModifier old = attr.getModifier(e.id());
            if (v <= 0) {
                if (old != null) {
                    attr.removeModifier(e.id());
                }
            } else if (old == null || old.amount() != v) {
                attr.addOrUpdateTransientModifier(new AttributeModifier(e.id(), v, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    /** 四个护甲槽里带某套装标签的件数。 */
    public static int pieces(LivingEntity e, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> setTag) {
        int n = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (e.getItemBySlot(s).is(setTag)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 方阵（EQ-GR-08）：穿满青铜套装、主手或副手拿着圆盾一族时，半径内每名友方 +1 护甲，最多 +3。
     * 友方 = 同队玩家、自己的召唤单位、铁傀儡；村民与驯服生物不算（合同原文）。
     */
    public record Phalanx(EquipmentTable.SetBonus def) implements Entry {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "set/phalanx");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute() {
            return Attributes.ARMOR;
        }

        @Override
        public double value(ServerPlayer p) {
            if (pieces(p, EquipmentTags.set(def.armor())) < def.pieces() || !holdsAspis(p)) {
                return 0;
            }
            double r = def.radius();
            int allies = p.level().getEntities(p, p.getBoundingBox().inflate(r),
                    e -> e.isAlive() && e.distanceToSqr(p) <= r * r && isAlly(p, e)).size();
            return PhalanxRules.bonus(allies, def.perAlly(), def.max());
        }

        static boolean holdsAspis(Player p) {
            ItemStack main = p.getMainHandItem();
            ItemStack off = p.getOffhandItem();
            return main.is(EquipmentTags.ASPIS_FAMILY) || off.is(EquipmentTags.ASPIS_FAMILY);
        }

        public static boolean isAlly(Player p, Entity e) {
            if (e instanceof Player other) {
                return p.isAlliedTo(other);
            }
            if (e instanceof MythSummon s) {
                return p.getUUID().equals(s.link().owner);
            }
            return e instanceof IronGolem;
        }
    }
}
