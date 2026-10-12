package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class CraneBlessingSpell extends AbstractSpell {
    public CraneBlessingSpell() { super(SpellType.CRANE_BLESSING, MythologyType.CHINESE, SpellCategory.HEALING, 240); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 仙鹤祝福 - 飞行和治疗能力
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 12000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 12000, 3));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));

        // 召唤鹦鹉作为仙鹤
        for (int i = 0; i < 2; i++) {
            ParrotEntity crane = new ParrotEntity(EntityType.PARROT, world);
            crane.refreshPositionAfterTeleport(
                targetPos.getX() + (world.random.nextDouble() - 0.5) * 4,
                targetPos.getY() + 3,
                targetPos.getZ() + (world.random.nextDouble() - 0.5) * 4
            );
            crane.setVariant(ParrotEntity.Variant.GRAY);
            world.spawnEntity(crane);
        }

        MythicParticleEffects.spawnMajor(world, ParticleTypes.CLOUD,
            targetPos.getX() + 0.5, targetPos.getY() + 2.0, targetPos.getZ() + 0.5,
            30, 3.0, 2.0, 3.0, 0.1);

        world.playSound(null, targetPos, SoundEvents.ENTITY_PARROT_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.5f);
        sendMessage(caster, "mythic_age.spellbook.message.crane_blessing.success");

        return true;
    }
}
