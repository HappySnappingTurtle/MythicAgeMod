package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class AbundanceSpell extends AbstractSpell {
    public AbundanceSpell() { super(SpellType.ABUNDANCE, MythologyType.CHINESE, SpellCategory.RESOURCE, 200); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 丰收 - 催熟周围作物
        BlockPos center = caster.getBlockPos();

        for (int x = -8; x <= 8; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // 催熟作物
                    if (state.getBlock() instanceof CropBlock) {
                        CropBlock crop = (CropBlock) state.getBlock();
                        if (!crop.isMature(state)) {
                            world.setBlockState(pos, crop.withAge(crop.getMaxAge()));
                        }
                    }
                }
            }
        }

        // 给予玩家丰收祝福
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 12000, 1));

        // 粒子效果
        MythicParticleEffects.spawnNormal(world, ParticleTypes.COMPOSTER,
            center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
            50, 8.0, 3.0, 8.0, 0.2);

        world.playSound(null, center, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.PLAYERS, 1.5f, 1.0f);
        sendMessage(caster, "mythic_age.spellbook.message.abundance.success");

        return true;
    }
}
