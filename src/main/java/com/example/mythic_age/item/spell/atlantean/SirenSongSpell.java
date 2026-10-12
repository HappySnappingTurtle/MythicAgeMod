package com.example.mythic_age.item.spell.atlantean;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class SirenSongSpell extends AbstractSpell {
    public SirenSongSpell() { super(SpellType.SIREN_SONG, MythologyType.ATLANTEAN, SpellCategory.DEBUFF, 240); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(18.0, 6.0, 18.0), entity -> entity.isAlive());
        List<PlayerEntity> allies = world.getEntitiesByClass(PlayerEntity.class,
            new Box(targetPos).expand(12.0, 5.0, 12.0), entity -> entity.isAlive());

        for (HostileEntity enemy : enemies) {
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 420, 2));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 420, 1));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 240, 0));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 260, 0));
        }

        for (PlayerEntity ally : allies) {
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 220, 0));
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 600, 0));
        }

        MythicParticleEffects.spawnMajor(world, ParticleTypes.NOTE,
            targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5,
            70, 7.0, 2.2, 7.0, 0.04);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.BUBBLE_POP,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            65, 7.5, 2.0, 7.5, 0.08);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.ENCHANT,
            targetPos.getX() + 0.5, targetPos.getY() + 1.2, targetPos.getZ() + 0.5,
            55, 6.0, 1.4, 6.0, 0.04);

        world.playSound(null, targetPos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.6f, 1.45f);
        world.playSound(null, targetPos, SoundEvents.ENTITY_DROWNED_AMBIENT_WATER, SoundCategory.PLAYERS, 1.1f, 1.25f);
        sendMessage(caster, "mythic_age.spellbook.message.siren_song.success");
        return true;
    }
}
