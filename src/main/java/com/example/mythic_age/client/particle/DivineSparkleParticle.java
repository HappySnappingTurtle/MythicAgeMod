package com.example.mythic_age.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class DivineSparkleParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float baseScale;

    protected DivineSparkleParticle(
            ClientWorld world,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteProvider spriteProvider
    ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.baseScale = 0.08F + this.random.nextFloat() * 0.08F;
        this.scale = this.baseScale;
        this.maxAge = 24 + this.random.nextInt(14);
        this.gravityStrength = -0.004F;
        this.velocityMultiplier = 0.92F;
        this.velocityX = velocityX * 0.35D + (this.random.nextDouble() - 0.5D) * 0.028D;
        this.velocityY = velocityY * 0.35D + 0.018D + this.random.nextDouble() * 0.028D;
        this.velocityZ = velocityZ * 0.35D + (this.random.nextDouble() - 0.5D) * 0.028D;
        this.setBoundingBoxSpacing(0.04F, 0.04F);
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.dead) {
            float life = (float) this.age / (float) this.maxAge;
            float pulse = 0.72F + 0.28F * (float) Math.sin(this.age * 0.65F);
            this.scale = this.baseScale * pulse * (1.0F + life * 0.18F);
            this.setColor(0.96F, 0.94F, 0.78F + life * 0.14F);
            this.setAlpha(1.0F - life * 0.75F);
            this.setSpriteForAge(this.spriteProvider);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
                DefaultParticleType parameters,
                ClientWorld world,
                double x,
                double y,
                double z,
                double velocityX,
                double velocityY,
                double velocityZ
        ) {
            return new DivineSparkleParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
