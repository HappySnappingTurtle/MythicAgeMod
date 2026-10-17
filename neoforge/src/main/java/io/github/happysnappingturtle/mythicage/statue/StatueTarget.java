package io.github.happysnappingturtle.mythicage.statue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** 主神像道具上记的「哪个文明的哪位主神」（数据组件 {@code mythic_age:statue_target}）。 */
public record StatueTarget(Civilization civ, String patron) {
    public static final Codec<StatueTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(s -> Civilization.byId(s).orElse(Civilization.GREEK), Civilization::id)
                    .fieldOf("civilization").forGetter(StatueTarget::civ),
            Codec.STRING.fieldOf("patron").forGetter(StatueTarget::patron)).apply(i, StatueTarget::new));

    public static final StreamCodec<ByteBuf, StatueTarget> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(s -> Civilization.byId(s).orElse(Civilization.GREEK), Civilization::id),
            StatueTarget::civ, ByteBufCodecs.STRING_UTF8, StatueTarget::patron, StatueTarget::new);
}
