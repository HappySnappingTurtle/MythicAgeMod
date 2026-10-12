package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Blocks;
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

public class FloodSpell extends AbstractSpell {
    public FloodSpell() { super(SpellType.FLOOD, MythologyType.CHINESE, SpellCategory.OFFENSIVE, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 洪水 - 在目标区域创造水流
        // 在目标区域放置水
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y <= 2; y++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    if (world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, Blocks.WATER.getDefaultState());
                    }
                }
            }
        }

        // 对区域内的敌对生物造成伤害
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(8), entity -> entity.isAlive());

        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().drown(), 8.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 2));
        }

        // 给予玩家水中优势
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 6000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 6000, 1));

        MythicParticleEffects.spawnMajor(world, ParticleTypes.SPLASH,
            targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(),
            100, 8.0, 3.0, 8.0, 0.5);

        world.playSound(null, targetPos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 2.0f, 0.8f);
        sendMessage(caster, "mythic_age.spellbook.message.flood.success");

        return true;
    }
}
