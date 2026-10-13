package com.oritong.cloudstorage.misc;

import com.oritong.cloudstorage.CloudStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CSSoundRegistry {

    public static final DeferredRegister<SoundEvent> DEF_REG = DeferredRegister.create(Registries.SOUND_EVENT, CloudStorage.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BALLOON_HURT = createSoundEvent("balloon_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BALLOON_POP = createSoundEvent("balloon_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOVIATOR_IDLE = createSoundEvent("bloviator_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOVIATOR_HURT = createSoundEvent("bloviator_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOVIATOR_BLOW = createSoundEvent("bloviator_blow");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOVIATOR_LIGHTNING = createSoundEvent("bloviator_lightning");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOUD_CHEST_OPEN = createSoundEvent("cloud_chest_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOUD_CHEST_CLOSE = createSoundEvent("cloud_chest_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> STATIC_SHOCK = createSoundEvent("static_shock");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_DRIFT = createSoundEvent("music_disc_drift");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOUD_BLOWER = createSoundEvent("cloud_blower");

    private static DeferredHolder<SoundEvent, SoundEvent> createSoundEvent(final String soundName) {
        return DEF_REG.register(soundName, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CloudStorage.MODID, soundName)));
    }
}
