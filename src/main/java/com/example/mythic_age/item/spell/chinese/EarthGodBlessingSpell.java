package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class EarthGodBlessingSpell extends AbstractSpell {
    public EarthGodBlessingSpell() { super(SpellType.EARTH_GOD_BLESSING, MythologyType.CHINESE, SpellCategory.UTILITY, 120); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 土地神佑 - 增强周围土地肥沃度
        BlockPos center = caster.getBlockPos();

        // 在周围区域增强土地
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos pos = center.add(x, -1, z);
                BlockState state = world.getBlockState(pos);

                // 将泥土转换为耕地
                if (state.getBlock() == Blocks.DIRT || state.getBlock() == Blocks.GRASS_BLOCK) {
                    world.setBlockState(pos, Blocks.FARMLAND.getDefaultState());
                }
            }
        }

        // 给予玩家祝福效果
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 1200, 0));

        // 粒子效果
        MythicParticleEffects.spawnNormal(world, ParticleTypes.HAPPY_VILLAGER,
            center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
            30, 5.0, 2.0, 5.0, 0.1);

        world.playSound(null, center, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        sendMessage(caster, "mythic_age.spellbook.message.earth_god_blessing.success");

        return true;
    }
}
