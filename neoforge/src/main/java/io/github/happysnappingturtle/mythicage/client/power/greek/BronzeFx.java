package io.github.happysnappingturtle.mythicage.client.power.greek;

import io.github.happysnappingturtle.mythicage.client.power.PowerFxRenderer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * 青铜的表现（03 号分镜 GP-GR-08）：瞄准时青铜描边圈；施放时每个目标胸口迸出青铜碎光。
 * 身上的青铜外壳由 Aura 叠层画（{@code AuraLayer}），随药水效果持续与褪色。
 */
public final class BronzeFx implements PowerFxRenderer {
    static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/bronze_ring.png");

    @Override
    public AimStyle aimStyle() {
        return new AimStyle(RING, 0xD9893C);
    }

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        if (p.phase() != PowerFxPayload.Phase.HIT || !"harden".equals(p.tag())) {
            return;
        }
        RandomSource r = RandomSource.create(p.seed());
        for (int id : p.targets()) {
            Entity e = level.getEntity(id);
            if (e == null) {
                continue;
            }
            double y = e.getY() + e.getBbHeight() * 0.65;
            for (int i = 0; i < 12; i++) {
                level.addParticle(PowerRegistries.BRONZE_FLAKE.get(), e.getX(), y, e.getZ(),
                        (r.nextDouble() - 0.5) * 0.25, r.nextDouble() * 0.15, (r.nextDouble() - 0.5) * 0.25);
            }
        }
    }
}
