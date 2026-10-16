package io.github.happysnappingturtle.mythicage.content.greek.guardian;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ItemModelSpec;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.guardian.GuardianTypes;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/** 希腊祭司的内容条目（T9）与祭司类型登记（T10b）。 */
public final class GreekGuardian {
    /** 刷怪蛋：白袍底色、紫边斑点（与 VI-GR-PRI-01 的配色一致）。 */
    public static final DeferredItem<DeferredSpawnEggItem> SPAWN_EGG = Content.item("greek_guardian_spawn_egg", GREEK,
                    p -> new DeferredSpawnEggItem(ModEntities.GREEK_GUARDIAN, 0xEDE6D6, 0x6B2A78, p))
            .model(ItemModelSpec.MANUAL)
            .lang("神庙祭司刷怪蛋", "Temple Priest Spawn Egg", "神殿の神官のスポーンエッグ")
            .register();

    private GreekGuardian() {
    }

    public static void init() {
        GuardianTypes.register(GREEK, ModEntities.GREEK_GUARDIAN);
    }
}
