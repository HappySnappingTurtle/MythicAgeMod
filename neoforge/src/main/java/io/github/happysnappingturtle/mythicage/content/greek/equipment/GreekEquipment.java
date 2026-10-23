package io.github.happysnappingturtle.mythicage.content.greek.equipment;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ItemModelSpec;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.equipment.EquipmentTable;
import io.github.happysnappingturtle.mythicage.equipment.DrawBowItem;
import io.github.happysnappingturtle.mythicage.equipment.EquipmentTables;
import io.github.happysnappingturtle.mythicage.equipment.EquipmentTags;
import io.github.happysnappingturtle.mythicage.equipment.MeleeWeaponItem;
import io.github.happysnappingturtle.mythicage.equipment.SetArmorItem;
import io.github.happysnappingturtle.mythicage.equipment.SetBonuses;
import io.github.happysnappingturtle.mythicage.equipment.ShieldWeaponItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊青铜装备（T17：EQ-GR-08～13，01_希腊 §8.2，古典时代解锁配方）。数值全部来自 {@code mythic_age/equipment/greek.json}。
 */
public final class GreekEquipment {
    public static final EquipmentTable TABLE = EquipmentTables.of("greek");
    private static final EquipmentTable.ArmorSet BRONZE = TABLE.armor().get("bronze");
    private static final TagKey<Item> BRONZE_INGOTS = ModTags.cItem("ingots/bronze");

    private static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MythicAgeIds.MOD_ID);

    /** 青铜护甲材料：2/7/5/3、韧性 1、附魔能力 12（D1、D2）；贴图层 bronze。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BRONZE_MATERIAL = MATERIALS.register("bronze", () -> {
        EnumMap<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
        def.put(ArmorItem.Type.HELMET, BRONZE.points()[0]);
        def.put(ArmorItem.Type.CHESTPLATE, BRONZE.points()[1]);
        def.put(ArmorItem.Type.LEGGINGS, BRONZE.points()[2]);
        def.put(ArmorItem.Type.BOOTS, BRONZE.points()[3]);
        def.put(ArmorItem.Type.BODY, BRONZE.points()[1]);
        return new ArmorMaterial(def, BRONZE.enchantability(), SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(BRONZE_INGOTS),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "bronze"))),
                (float) BRONZE.toughness(), 0);
    });

    public static final TagKey<Item> SET_BRONZE = EquipmentTags.set("bronze");
    private static final String SET_TIP = "item.mythic_age.bronze_armor.tip";

    public static final DeferredItem<SetArmorItem> BRONZE_HELMET = armor("bronze_helmet", ArmorItem.Type.HELMET, 0,
            "青铜头盔", "Bronze Helmet", "青銅の兜", ItemTags.HEAD_ARMOR, ItemTags.HEAD_ARMOR_ENCHANTABLE);
    public static final DeferredItem<SetArmorItem> BRONZE_CHESTPLATE = armor("bronze_chestplate", ArmorItem.Type.CHESTPLATE, 1,
            "青铜胸甲", "Bronze Chestplate", "青銅の胸当て", ItemTags.CHEST_ARMOR, ItemTags.CHEST_ARMOR_ENCHANTABLE);
    public static final DeferredItem<SetArmorItem> BRONZE_LEGGINGS = armor("bronze_leggings", ArmorItem.Type.LEGGINGS, 2,
            "青铜护腿", "Bronze Leggings", "青銅のすね当て", ItemTags.LEG_ARMOR, ItemTags.LEG_ARMOR_ENCHANTABLE);
    public static final DeferredItem<SetArmorItem> BRONZE_BOOTS = armor("bronze_boots", ArmorItem.Type.BOOTS, 3,
            "青铜靴", "Bronze Boots", "青銅のサンダル", ItemTags.FOOT_ARMOR, ItemTags.FOOT_ARMOR_ENCHANTABLE);

    /** EQ-GR-09 希腊短剑：伤害 6、攻速 1.8，不横扫（D4）。 */
    public static final DeferredItem<MeleeWeaponItem> XIPHOS = Content.item("xiphos", GREEK,
                    p -> new MeleeWeaponItem(p, TABLE.weapon("xiphos").orElseThrow(), BRONZE.enchantability(), BRONZE_INGOTS, null))
            .model(ItemModelSpec.HANDHELD)
            .tags(ItemTags.SWORDS, ItemTags.SWORD_ENCHANTABLE, ItemTags.SHARP_WEAPON_ENCHANTABLE, ItemTags.WEAPON_ENCHANTABLE,
                    ItemTags.FIRE_ASPECT_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE)
            .lang("希腊短剑", "Xiphos", "クシフォス")
            .register();
    /** EQ-GR-10 重装长矛：伤害 7、攻速 1.1、触及 +1；举盾时可刺击（×0.6）。 */
    public static final DeferredItem<MeleeWeaponItem> DORY = Content.item("dory", GREEK,
                    p -> new MeleeWeaponItem(p, TABLE.weapon("dory").orElseThrow(), BRONZE.enchantability(), BRONZE_INGOTS,
                            "item.mythic_age.dory.tip"))
            .model(ItemModelSpec.HANDHELD)
            .tags(EquipmentTags.SHIELD_THRUST, ItemTags.SHARP_WEAPON_ENCHANTABLE, ItemTags.WEAPON_ENCHANTABLE,
                    ItemTags.FIRE_ASPECT_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE)
            .lang("重装长矛", "Dory", "ドリュ")
            .register();
    /** EQ-GR-11 青铜圆盾：耐久 500；格挡后 10 刻内的下一次近战 +2。 */
    public static final DeferredItem<ShieldWeaponItem> ASPIS = Content.item("aspis", GREEK,
                    p -> new ShieldWeaponItem(p, TABLE.weapon("aspis").orElseThrow(), BRONZE.enchantability(), BRONZE_INGOTS,
                            "item.mythic_age.aspis.tip"))
            .tags(EquipmentTags.ASPIS_FAMILY, Tags.Items.TOOLS_SHIELD, ItemTags.DURABILITY_ENCHANTABLE)
            .lang("青铜圆盾", "Aspis", "アスピス")
            .register();
    /** EQ-GR-12 复合弓：满弓 18 刻（原版 20）。 */
    public static final DeferredItem<DrawBowItem> COMPOSITE_BOW = Content.item("composite_bow", GREEK,
                    p -> {
                        EquipmentTable.Weapon w = TABLE.weapon("composite_bow").orElseThrow();
                        return new DrawBowItem(p, w.durability(), w.drawTicks(), "item.mythic_age.composite_bow.tip");
                    })
            .model(ItemModelSpec.MANUAL)
            .tags(ItemTags.BOW_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE, Tags.Items.TOOLS_BOW)
            .lang("复合弓", "Composite Bow", "複合弓")
            .register();
    /** EQ-GR-13 橄榄木大棒：伤害 7、攻速 0.9，钝器，不横扫。 */
    public static final DeferredItem<MeleeWeaponItem> OLIVE_CLUB = Content.item("olive_club", GREEK,
                    p -> new MeleeWeaponItem(p, TABLE.weapon("olive_club").orElseThrow(), 5, ItemTags.PLANKS,
                            "item.mythic_age.olive_club.tip"))
            .model(ItemModelSpec.HANDHELD)
            .tags(EquipmentTags.BLUNT, ItemTags.SHARP_WEAPON_ENCHANTABLE, ItemTags.WEAPON_ENCHANTABLE,
                    ItemTags.FIRE_ASPECT_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE)
            .lang("橄榄木大棒", "Olive Club", "オリーブの棍棒")
            .register();

    private GreekEquipment() {
    }

    @SafeVarargs
    private static DeferredItem<SetArmorItem> armor(String id, ArmorItem.Type type, int slot, String zh, String en, String ja,
                                                    TagKey<Item>... tags) {
        return Content.item(id, GREEK, p -> new SetArmorItem(BRONZE_MATERIAL, type, p, BRONZE.durability()[slot], SET_TIP))
                .tags(tags)
                .tags(SET_BRONZE, ItemTags.ARMOR_ENCHANTABLE, ItemTags.EQUIPPABLE_ENCHANTABLE, ItemTags.DURABILITY_ENCHANTABLE)
                .lang(zh, en, ja)
                .register();
    }

    public static void init(IEventBus modBus) {
        MATERIALS.register(modBus);
        SetBonuses.register(new SetBonuses.Phalanx(TABLE.sets().get("phalanx")));
    }

    public static Holder<ArmorMaterial> bronzeMaterial() {
        return BRONZE_MATERIAL;
    }
}
