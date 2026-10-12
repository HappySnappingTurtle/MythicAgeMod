package com.example.mythic_age.item.spell.nordic;

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

public class GreatHuntSpell extends AbstractSpell {
    public GreatHuntSpell() { super(SpellType.GREAT_HUNT, MythologyType.NORDIC, SpellCategory.UTILITY, 120); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        BlockPos center = caster.getBlockPos();

        // Summon parrots as eagle substitutes
        for (int i = 0; i < 3; i++) {
            ParrotEntity eagle = new ParrotEntity(EntityType.PARROT, world);
            eagle.refreshPositionAfterTeleport(
                center.getX() + (world.random.nextDouble() - 0.5) * 6,
                center.getY() + 3,
                center.getZ() + (world.random.nextDouble() - 0.5) * 6
            );

            // Set parrot variant to red (eagle-like)
            eagle.setVariant(ParrotEntity.Variant.RED_BLUE);

            world.spawnEntity(eagle);
        }

        MythicParticleEffects.spawnMajor(world, ParticleTypes.CLOUD,
            center.getX() + 0.5, center.getY() + 2.8, center.getZ() + 0.5,
            36, 3.0, 1.5, 3.0, 0.08);

        // Grant hunting buffs
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6000, 2));

        world.playSound(null, center, SoundEvents.ENTITY_PARROT_AMBIENT, SoundCategory.PLAYERS, 1.5f, 0.8f);
        sendMessage(caster, "mythic_age.spellbook.message.great_hunt.success");

        return true;
    }
}
