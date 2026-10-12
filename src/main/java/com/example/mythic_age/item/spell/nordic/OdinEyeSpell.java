package com.example.mythic_age.item.spell.nordic;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class OdinEyeSpell extends AbstractSpell {
    public OdinEyeSpell() { super(SpellType.ODIN_EYE, MythologyType.NORDIC, SpellCategory.UTILITY, 120); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Odin's Eye - clairvoyance and detection
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12000, 0));

        // Make all nearby entities glow
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(30), entity -> entity.isAlive() && entity != caster);

        for (LivingEntity entity : entities) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 6000, 0));
        }

        // Reveal nearby ores via particle effects
        for (int x = -20; x <= 20; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -20; z <= 20; z++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // Detect precious ores
                    if (state.getBlock() == Blocks.DIAMOND_ORE ||
                        state.getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE ||
                        state.getBlock() == Blocks.GOLD_ORE ||
                        state.getBlock() == Blocks.DEEPSLATE_GOLD_ORE ||
                        state.getBlock() == Blocks.IRON_ORE ||
                        state.getBlock() == Blocks.DEEPSLATE_IRON_ORE) {

                        MythicParticleEffects.spawnAmbient(world, ParticleTypes.END_ROD,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            1, 0, 0, 0, 0);
                    }
                }
            }
        }

        world.playSound(null, targetPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        sendMessage(caster, "mythic_age.spellbook.message.odin_eye.success");

        return true;
    }
}
