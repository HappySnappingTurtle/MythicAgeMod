package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 希腊的恩宠来源：信徒祈祷（T10）。注册村民的祈祷记录附件、产出探针与事件。
 */
public final class GreekPrayer {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MythicAgeIds.MOD_ID);

    /** 村民的祈祷记录 {@code mythic_age:greek_prayer}。 */
    public static final Supplier<AttachmentType<PrayerRecord>> RECORD = ATTACHMENT_TYPES.register("greek_prayer",
            () -> AttachmentType.builder(PrayerRecord::new).serialize(PrayerRecord.SERIALIZER).build());

    private GreekPrayer() {
    }

    public static void init(IEventBus modBus, IEventBus game) {
        ATTACHMENT_TYPES.register(modBus);
        PrayerService.init();
        TemplePrayer.init();
        io.github.happysnappingturtle.mythicage.village.work.JobType.register(new ResidentPrayer());
        // F7：当天祈祷过的居民交易额度减半（T19 §1.8 I4）
        io.github.happysnappingturtle.mythicage.village.economy.ResidentTrade.setPrayedToday(
                (r, day) -> r.getData(RECORD).prayedOn(day));
        game.addListener(PrayerService::onServerTickPost);
        game.addListener(PrayerService::onServerStopped);
        game.addListener(TemplePrayer::onServerStopped);
        game.addListener(io.github.happysnappingturtle.mythicage.village.temple.VillageTemples::onServerStopped);
        game.addListener(BelieverEvents::onStartTracking);
        game.addListener(GreekPrayer::onServerStarted);
    }

    /** 包里有没有祈祷行为（Mixin 是否生效）。 */
    public static boolean injected() {
        return VillagerGoalPackages.getIdlePackage(VillagerProfession.NONE, 0.5F).stream()
                .anyMatch(p -> p.getSecond() instanceof PrayAtAltar)
                && VillagerGoalPackages.getWorkPackage(VillagerProfession.FARMER, 0.5F).stream()
                .anyMatch(p -> p.getSecond() instanceof PrayAtAltar);
    }

    /** 注入点设为非必需（兼容其他改村民 AI 的模组），所以启动时自检一次，失败只报错不崩溃。 */
    private static void onServerStarted(ServerStartedEvent event) {
        if (!injected()) {
            MythicAge.LOGGER.error("村民祈祷行为没有注入到 VillagerGoalPackages（可能与其他模组冲突）：希腊信徒不会去神坛祈祷");
        }
    }
}
