package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class CallToArmsSpell extends AbstractSpell {
    public CallToArmsSpell() { super(SpellType.CALL_TO_ARMS, MythologyType.CHINESE, SpellCategory.SUMMONING, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 召唤守护者 - 召唤铁傀儡守护
        BlockPos center = caster.getBlockPos();

        // 召唤3个铁傀儡
        for (int i = 0; i < 3; i++) {
            double angle = (i * 120.0) * Math.PI / 180.0;
            double x = center.getX() + Math.cos(angle) * 3;
            double z = center.getZ() + Math.sin(angle) * 3;

            IronGolemEntity golem = new IronGolemEntity(EntityType.IRON_GOLEM, world);
            golem.refreshPositionAfterTeleport(x, center.getY(), z);

            // 设置为玩家创造的
            golem.setPlayerCreated(true);

            world.spawnEntity(golem);
        }

        // 给予玩家领导力加持
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 12000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 1));

        // 粒子效果
        MythicParticleEffects.spawnMajor(world, ParticleTypes.TOTEM_OF_UNDYING,
            center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
            30, 3.0, 2.0, 3.0, 0.1);

        world.playSound(null, center, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        sendMessage(caster, "mythic_age.spellbook.message.call_to_arms.success");

        return true;
    }
}
