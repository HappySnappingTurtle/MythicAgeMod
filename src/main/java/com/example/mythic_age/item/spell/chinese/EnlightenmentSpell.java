package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class EnlightenmentSpell extends AbstractSpell {
    public EnlightenmentSpell() { super(SpellType.ENLIGHTENMENT, MythologyType.CHINESE, SpellCategory.UTILITY, 120); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 开悟 - 获得经验和智慧
        caster.addExperience(500);

        // 给予玩家智慧效果
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 12000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));

        // 在目标位置生成经验球
        for (int i = 0; i < 10; i++) {
            ExperienceOrbEntity orb = new ExperienceOrbEntity(world,
                targetPos.getX() + (world.random.nextDouble() - 0.5) * 4,
                targetPos.getY() + 1,
                targetPos.getZ() + (world.random.nextDouble() - 0.5) * 4,
                50);
            world.spawnEntity(orb);
        }

        MythicParticleEffects.spawnMajor(world, ParticleTypes.ENCHANT,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            50, 3.0, 3.0, 3.0, 0.1);

        world.playSound(null, targetPos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.5f, 1.2f);
        sendMessage(caster, "mythic_age.spellbook.message.enlightenment.success");

        return true;
    }
}
