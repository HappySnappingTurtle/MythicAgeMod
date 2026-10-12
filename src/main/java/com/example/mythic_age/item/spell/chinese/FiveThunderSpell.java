package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
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

public class FiveThunderSpell extends AbstractSpell {
    public FiveThunderSpell() { super(SpellType.FIVE_THUNDER, MythologyType.CHINESE, SpellCategory.OFFENSIVE, 200); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 五雷正法 - 召唤五道雷电攻击敌人
        BlockPos center = caster.getBlockPos();

        // 寻找附近的敌对生物
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(center).expand(20), entity -> entity.isAlive());

        int thunderCount = Math.min(5, enemies.size());
        if (thunderCount == 0) {
            sendMessage(caster, "mythic_age.spellbook.message.five_thunder.no_targets");
            return false;
        }

        // 对前5个敌人施放雷电
        for (int i = 0; i < thunderCount; i++) {
            HostileEntity enemy = enemies.get(i);
            BlockPos enemyPos = enemy.getBlockPos();

            // 创建雷电
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAfterTeleport(enemyPos.getX(), enemyPos.getY(), enemyPos.getZ());
            world.spawnEntity(lightning);
            MythicParticleEffects.spawnMajor(world, ParticleTypes.ELECTRIC_SPARK,
                enemyPos.getX() + 0.5, enemyPos.getY() + 1.0, enemyPos.getZ() + 0.5,
                18, 0.8, 1.2, 0.8, 0.2);

            // 额外伤害
            enemy.damage(world.getDamageSources().lightningBolt(), 15.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
        }

        // 给予玩家雷法加持
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));

        world.playSound(null, center, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.0f, 0.8f);
        sendMessage(caster, "mythic_age.spellbook.message.five_thunder.success");

        return true;
    }
}
