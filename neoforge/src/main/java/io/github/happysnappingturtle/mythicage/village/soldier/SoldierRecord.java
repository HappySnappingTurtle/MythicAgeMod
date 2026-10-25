package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 名册里一名士兵的兵装与弹药（T20 设计文档 §4，存在村庄经济存档里，和名册的职业同步）。
 *
 * @param profession 兵种职业 id
 * @param tier       身上兵装的档（兵装从村库扣下后就记在这里，读档不会再扣一次）
 * @param wear       兵装被命中的次数
 * @param ammo       身上的弹药：物品 id → 支数（远程兵种；重启不补满）
 * @param order      转岗先后的序号（越大越晚，降回时先降最晚的）
 */
public record SoldierRecord(String profession, SoldierProfile.Tier tier, int wear, Map<String, Integer> ammo, long order) {
    public SoldierRecord {
        ammo = Map.copyOf(ammo);
    }

    public SoldierRecord withTier(SoldierProfile.Tier t, int newWear) {
        return new SoldierRecord(profession, t, newWear, ammo, order);
    }

    public SoldierRecord withWear(int w) {
        return new SoldierRecord(profession, tier, w, ammo, order);
    }

    public SoldierRecord withAmmo(Map<String, Integer> a) {
        Map<String, Integer> m = new LinkedHashMap<>();
        a.forEach((k, v) -> {
            if (v > 0) {
                m.put(k, v);
            }
        });
        return new SoldierRecord(profession, tier, wear, m, order);
    }

    /** 身上一共几支。 */
    public int ammoTotal() {
        return ammo.values().stream().mapToInt(Integer::intValue).sum();
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("profession", profession);
        t.putString("tier", tier.id());
        t.putInt("wear", wear);
        CompoundTag a = new CompoundTag();
        ammo.forEach(a::putInt);
        t.put("ammo", a);
        t.putLong("order", order);
        return t;
    }

    public static SoldierRecord load(CompoundTag t) {
        Map<String, Integer> ammo = new LinkedHashMap<>();
        CompoundTag a = t.getCompound("ammo");
        a.getAllKeys().forEach(k -> ammo.put(k, a.getInt(k)));
        return new SoldierRecord(t.getString("profession"), SoldierProfile.Tier.byId(t.getString("tier")), t.getInt("wear"),
                ammo, t.getLong("order"));
    }
}
