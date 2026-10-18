package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 神力共用的注册项（T13 设计文档 §1.2–1.3）：音效事件、粒子类型、药水效果。
 * 音效事件名照 05 号各卡「音效事件」；声音文件先在 {@code sounds.json} 里指向原版音效（D3）。
 */
public final class PowerRegistries {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MythicAgeIds.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, MythicAgeIds.MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MythicAgeIds.MOD_ID);

    /** 全部音效事件：id → 注册项。 */
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SOUND_EVENTS = new LinkedHashMap<>();

    static {
        for (String id : new String[]{
                "gp.gr.bolt.lock", "gp.gr.bolt.charge", "gp.gr.bolt.strike",
                "gp.gr.lure.place", "gp.gr.lure.pulse", "gp.gr.lure.chicken", "gp.gr.lure.sink",
                "gp.gr.sentinel.rise", "gp.gr.sentinel.draw", "gp.gr.sentinel.shoot", "gp.gr.sentinel.crumble",
                "gp.gr.restoration.cast", "gp.gr.restoration.pulse", "gp.gr.restoration.repair",
                "gp.gr.pestilence.shatter", "gp.gr.pestilence.spawner_seal", "gp.gr.pestilence.end",
                "gp.gr.bronze.cast", "gp.gr.bronze.harden", "gp.gr.bronze.fade",
                "gp.gr.curse.impact", "gp.gr.curse.transform", "gp.gr.curse.immune"}) {
            SOUND_EVENTS.put(id, SOUNDS.register(id,
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, id))));
        }
    }

    // 粒子（贴图在 textures/particle/，由 tools/texgen/fxgen.py 生成）
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK = particle("spark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STORM_CLOUD = particle("storm_cloud");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HEAL_MOTE = particle("heal_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OLIVE_LEAF = particle("olive_leaf");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BRONZE_FLAKE = particle("bronze_flake");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CLEANSE = particle("cleanse");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> REPAIR = particle("repair");

    private PowerRegistries() {
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> particle(String id) {
        return PARTICLES.register(id, () -> new SimpleParticleType(false));
    }

    public static SoundEvent sound(String id) {
        DeferredHolder<SoundEvent, SoundEvent> h = SOUND_EVENTS.get(id);
        if (h == null) {
            throw new IllegalArgumentException("未登记的神力音效：" + id);
        }
        return h.get();
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        EFFECTS.register(modBus);
    }
}
