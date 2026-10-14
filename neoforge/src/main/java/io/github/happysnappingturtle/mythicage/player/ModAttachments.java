package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** 玩家数据附件：{@code mythic_age:player_data}，死亡后保留（copyOnDeath）。 */
public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MythicAgeIds.MOD_ID);

    public static final Supplier<AttachmentType<PlayerMythData>> PLAYER_DATA = ATTACHMENT_TYPES.register(
            "player_data", () -> AttachmentType.builder(PlayerMythData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, PlayerMythData>() {
                        @Override
                        public PlayerMythData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            return PlayerDataNbt.read(tag);
                        }

                        @Override
                        public CompoundTag write(PlayerMythData data, HolderLookup.Provider provider) {
                            return PlayerDataNbt.write(data);
                        }
                    })
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
