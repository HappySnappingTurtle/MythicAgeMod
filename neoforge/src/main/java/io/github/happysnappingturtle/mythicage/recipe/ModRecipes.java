package io.github.happysnappingturtle.mythicage.recipe;

import com.mojang.serialization.MapCodec;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** 本模组的配方序列化器。 */
public final class ModRecipes {
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AgeGatedRecipe>> AGE_GATED =
            ModRegistries.RECIPE_SERIALIZERS.register("age_gated", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<AgeGatedRecipe> codec() {
                    return AgeGatedRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, AgeGatedRecipe> streamCodec() {
                    return AgeGatedRecipe.STREAM_CODEC;
                }
            });

    public static final DeferredHolder<RecipeType<?>, RecipeType<PressingRecipe>> PRESSING =
            ModRegistries.RECIPE_TYPES.register("pressing", () -> RecipeType.simple(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            io.github.happysnappingturtle.mythicage.core.MythicAgeIds.MOD_ID, "pressing")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PressingRecipe>> PRESSING_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("pressing", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<PressingRecipe> codec() {
                    return PressingRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> streamCodec() {
                    return PressingRecipe.STREAM_CODEC;
                }
            });

    private ModRecipes() {
    }

    public static void init() {
    }

    /** 每个服务端 tick 开始时清除合成上下文，防止异常路径留下残留。 */
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        CraftingContext.exit();
    }
}
