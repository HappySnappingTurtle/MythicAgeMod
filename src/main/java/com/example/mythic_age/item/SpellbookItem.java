package com.example.mythic_age.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellbookItem extends Item {

    private final int cooldownTicks;
    private final SpellType spellType;

    public enum SpellType {
        // 希腊神力
        LIGHTNING_STORM,
        RESTORATION,
        LURE,
        UNDERWORLD_ARMY,
        PESTILENCE,
        CURSE,
        EARTHQUAKE,
        CEASEFIRE,
        BRONZE,
        UNDERWORLD_PASSAGE,
        LIGHTNING_STORM_HERA,
        PLENTY,

        // 中国神力
        RAIN_PRAYER,
        EARTH_GOD_BLESSING,
        ENLIGHTENMENT,
        ABUNDANCE,
        FIVE_THUNDER,
        CRANE_BLESSING,
        FLOOD,
        CALL_TO_ARMS,
        TAIJI_SHIELD,
        RECREATION,

        // 北欧神力
        FENRIR_HOWL,
        ODIN_EYE,
        GREAT_HUNT,
        DWARVEN_MINE,
        THOR_WRATH,
        VALHALLA_CALL,
        FROST,
        WALKING_WOODS,
        RAGNAROK,
        FIMBULWINTER,

        // 埃及神力
        RA_RADIANCE,
        DESERT_STORM,
        SUN_RAIN,
        PROSPERITY,
        EYE_OF_HORUS,
        ANUBIS_JUDGMENT,
        LOCUST_SWARM,
        TORNADO,
        ISIS_RESURRECTION,
        METEOR,

        // 亚特兰蒂斯神力
        TIDAL_SURGE,
        ABYSSAL_CURRENT,
        CRYSTAL_BARRIER,
        PEARL_HARVEST,
        ORICHALCUM_FORGE,
        SIREN_SONG,
        LEVIATHAN_CALL,
        ABYSSAL_RIFT,
        CHRONO_STASIS,
        OCEANIC_WRATH,

        // 神殿生成
        GENERATE_EGYPTIAN_TEMPLE,
        GENERATE_CHINESE_TEMPLE,
        GENERATE_ATLANTEAN_TEMPLE,
        GENERATE_NORDIC_TEMPLE
    }

    public SpellbookItem(Settings settings, int cooldownTicks, SpellType spellType) {
        super(settings);
        this.cooldownTicks = cooldownTicks;
        this.spellType = spellType;
    }

    public SpellbookItem(Settings settings, SpellType spellType) {
        this(settings, 20, spellType);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        String descriptionKey = "mythic_age.spellbook." + spellType.name().toLowerCase() + ".description";
        tooltip.add(Text.translatable(descriptionKey).formatted(Formatting.GRAY));

        float cooldownSeconds = cooldownTicks / 20.0f;
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("mythic_age.spellbook.cooldown", String.format("%.1f", cooldownSeconds))
                .formatted(Formatting.BLUE));

        String spellTypeKey = "mythic_age.spellbook.type." + getSpellCategory();
        tooltip.add(Text.translatable(spellTypeKey).formatted(Formatting.YELLOW));

        super.appendTooltip(stack, world, tooltip, context);
    }

    private String getSpellCategory() {
        com.example.mythic_age.item.spell.Spell spell =
            com.example.mythic_age.item.spell.SpellRegistry.get(spellType);
        if (spell != null) {
            return spell.getCategory().getId();
        }
        return "unknown";
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (!world.isClient) {
            if (user.getItemCooldownManager().isCoolingDown(this)) {
                user.sendMessage(Text.translatable("item.mythic_age.spellbook.on_cooldown"), true);
                return TypedActionResult.fail(itemStack);
            }

            boolean success = castGodPower(world, user, spellType);

            if (success) {
                if (this.cooldownTicks > 0) {
                    user.getItemCooldownManager().set(this, this.cooldownTicks);
                }

                world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.PLAYERS, 0.8F, 1.05F);

                user.sendMessage(Text.translatable("item.mythic_age.spellbook.cast_success",
                    Text.translatable("spell." + spellType.name().toLowerCase())), false);

                return TypedActionResult.success(itemStack, world.isClient());
            } else {
                user.sendMessage(Text.translatable("item.mythic_age.spellbook.cast_failed"), true);
                return TypedActionResult.fail(itemStack);
            }
        }

        return TypedActionResult.pass(itemStack);
    }

    private boolean castGodPower(World world, PlayerEntity player, SpellType spellType) {
        com.example.mythic_age.item.spell.Spell spell =
            com.example.mythic_age.item.spell.SpellRegistry.get(spellType);
        if (spell != null && world instanceof ServerWorld serverWorld) {
            return spell.cast(serverWorld, player, getTargetPos(world, player));
        }
        return false;
    }

    private BlockPos getTargetPos(World world, PlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(50.0));

        BlockHitResult hitResult = world.raycast(new RaycastContext(
            start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }

        return player.getBlockPos().add(0, 0, 5);
    }

    public SpellType getSpellType() {
        return spellType;
    }
}
