package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class CurseSpell extends AbstractSpell {
    public CurseSpell() { super(SpellType.CURSE, MythologyType.GREEK, SpellCategory.DEBUFF, 240); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
            caster.getBoundingBox().expand(10.0),
            entity -> entity != caster && !(entity instanceof PlayerEntity));

        int transformCount = 0;
        MythicParticleEffects.spawnNormal(world, ParticleTypes.WITCH,
            caster.getX(), caster.getY() + 1, caster.getZ(), 20, 1.0, 1.0, 1.0, 0.1);
        world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_WITCH_AMBIENT, SoundCategory.NEUTRAL, 1.0f, 1.2f);

        for (LivingEntity entity : nearbyEntities) {
            PigEntity pig = new PigEntity(net.minecraft.entity.EntityType.PIG, world);
            pig.refreshPositionAfterTeleport(entity.getX(), entity.getY(), entity.getZ());
            if (entity instanceof TameableEntity tameable && tameable.isTamed()) {
                pig.setCustomName(Text.translatable("mythic_age.spellbook.message.curse.named_entity", entity.getDisplayName()));
            } else {
                pig.setCustomName(Text.translatable("mythic_age.spellbook.message.curse.generic_entity"));
            }
            world.spawnEntity(pig);
            entity.discard();
            MythicParticleEffects.spawnNormal(world, ParticleTypes.POOF,
                pig.getX(), pig.getY() + 1, pig.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
            world.playSound(null, pig.getBlockPos(), SoundEvents.ENTITY_PIG_AMBIENT,
                SoundCategory.NEUTRAL, 0.8f, 1.0f + world.random.nextFloat() * 0.4f);
            transformCount++;
        }

        MythicParticleEffects.spawnNormal(world, ParticleTypes.ENCHANT,
            caster.getX(), caster.getY() + 1, caster.getZ(), 30, 3.0, 1.0, 3.0, 0.2);
        if (transformCount > 0) {
            world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_WITCH_CELEBRATE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            sendMessage(caster, "mythic_age.spellbook.message.curse.success", transformCount);
        } else {
            world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_WITCH_HURT, SoundCategory.NEUTRAL, 0.5f, 1.5f);
            sendMessage(caster, "mythic_age.spellbook.message.curse.no_targets");
        }
        return true;
    }
}
