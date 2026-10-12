package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class TaijiShieldSpell extends AbstractSpell {
    public TaijiShieldSpell() { super(SpellType.TAIJI_SHIELD, MythologyType.CHINESE, SpellCategory.BUFF, 400); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 太极护盾 - 强大的防护效果
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 3));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 12000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 1));

        BlockPos center = caster.getBlockPos();

        // 太极粒子效果
        for (int i = 0; i < 100; i++) {
            double angle = (i * 3.6) * Math.PI / 180.0;
            double radius = 3.0;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;

            MythicParticleEffects.spawnUltimate(world, ParticleTypes.END_ROD,
                x, center.getY() + 1.0, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        world.playSound(null, center, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 1.0f);
        sendMessage(caster, "mythic_age.spellbook.message.taiji_shield.success");

        return true;
    }
}
