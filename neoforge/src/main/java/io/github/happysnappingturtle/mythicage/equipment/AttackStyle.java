package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * 武器的攻击方式（T17 §8.2，五文明通用）：物品带 {@code #mythic_age:attack_style/<方式>} 标签就用对应的手持姿势与攻击动作。
 * swingTicks 是挥击动画的长度（原版 6 刻）；直刺更快，重砸更慢，与攻速的手感对应。
 */
public enum AttackStyle {
    /** 短剑：短促直刺。 */
    STAB(5),
    /** 长矛：长刺；举盾时越过盾上沿下刺。 */
    THRUST(8),
    /** 大棒：高举重砸。 */
    SMASH(11);

    private final int swingTicks;
    private final TagKey<Item> tag;

    AttackStyle(int swingTicks) {
        this.swingTicks = swingTicks;
        this.tag = ModTags.item("attack_style/" + name().toLowerCase(Locale.ROOT));
    }

    public int swingTicks() {
        return swingTicks;
    }

    public TagKey<Item> tag() {
        return tag;
    }

    public static Optional<AttackStyle> of(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (AttackStyle s : values()) {
            if (stack.is(s.tag)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
