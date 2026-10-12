package com.example.mythic_age.item.spell.nordic;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class FenrirHowlSpell extends AbstractSpell {
    public FenrirHowlSpell() { super(SpellType.FENRIR_HOWL, MythologyType.NORDIC, SpellCategory.SUMMONING, 120); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Summon wolf pack
        for (int i = 0; i < 5; i++) {
            double angle = (i * 72.0) * Math.PI / 180.0;
            double x = targetPos.getX() + Math.cos(angle) * 3;
            double z = targetPos.getZ() + Math.sin(angle) * 3;

            WolfEntity wolf = new WolfEntity(EntityType.WOLF, world);
            wolf.refreshPositionAfterTeleport(x, targetPos.getY(), z);
            wolf.setOwner(caster);
            wolf.setTamed(true);
            world.spawnEntity(wolf);
        }

        // Intimidate nearby hostile mobs
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(15), entity -> entity.isAlive());

        for (HostileEntity enemy : enemies) {
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 2));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 400, 1));
        }

        // Grant player wolf king buffs
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 6000, 0));

        MythicParticleEffects.spawnMajor(world, ParticleTypes.SOUL,
            targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(),
            50, 5.0, 2.0, 5.0, 0.1);

        world.playSound(null, targetPos, SoundEvents.ENTITY_WOLF_HOWL, SoundCategory.PLAYERS, 2.0f, 0.8f);
        sendMessage(caster, "mythic_age.spellbook.message.fenrir_howl.success");

        return true;
    }
}
