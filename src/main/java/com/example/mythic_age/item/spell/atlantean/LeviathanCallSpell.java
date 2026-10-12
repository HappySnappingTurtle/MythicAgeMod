package com.example.mythic_age.item.spell.atlantean;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.particle.ModParticles;
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
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class LeviathanCallSpell extends AbstractSpell {
    public LeviathanCallSpell() { super(SpellType.LEVIATHAN_CALL, MythologyType.ATLANTEAN, SpellCategory.SUMMONING, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        Vec3d center = Vec3d.ofCenter(targetPos);
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(18.0, 7.0, 18.0), entity -> entity.isAlive());

        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().magic(), 9.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 360, 2));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 260, 0));
            Vec3d pull = center.subtract(enemy.getPos());
            if (pull.lengthSquared() > 0.01D) {
                enemy.setVelocity(enemy.getVelocity().add(pull.normalize().multiply(0.24D)).add(0.0D, 0.2D, 0.0D));
                enemy.velocityModified = true;
            }
        }

        MythicParticleEffects.spawnUltimate(world, ParticleTypes.NAUTILUS,
            targetPos.getX() + 0.5, targetPos.getY() + 2.0, targetPos.getZ() + 0.5,
            120, 7.0, 3.0, 7.0, 0.12);
        MythicParticleEffects.spawnMajor(world, ParticleTypes.SPLASH,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            130, 8.0, 2.8, 8.0, 0.2);
        MythicParticleEffects.spawnMajor(world, ModParticles.SHOCKWAVE,
            targetPos.getX() + 0.5, targetPos.getY() + 0.2, targetPos.getZ() + 0.5,
            32, 6.0, 0.1, 6.0, 0.02);
        world.playSound(null, targetPos, SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.PLAYERS, 1.8f, 0.75f);
        world.playSound(null, targetPos, SoundEvents.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 1.4f, 0.9f);
        sendMessage(caster, "mythic_age.spellbook.message.leviathan_call.success");
        return true;
    }
}
