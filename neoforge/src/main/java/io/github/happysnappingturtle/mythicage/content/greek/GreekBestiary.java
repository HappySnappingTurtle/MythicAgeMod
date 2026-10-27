package io.github.happysnappingturtle.mythicage.content.greek;

import java.util.List;

/**
 * 希腊图鉴条目（T24 §6.4）：与 {@code data/mythic_age/mythic_age/bestiary/greek.json} 的条目一一对应（GameTest 校验），
 * datagen 按它生成每个条目三级的隐藏进度，手札条目文件名也用它。
 */
public final class GreekBestiary {
    public static final List<String> ENTRIES = List.of("greek/centaur", "greek/stymphalian_bird", "greek/harpy", "greek/karkinos",
            "greek/hamadryad", "greek/ruin_sentinel", "greek/minotaur", "greek/pegasus");

    private GreekBestiary() {
    }
}
