package com.github.alexthe668.cloudstorage.client.particle;

import com.github.alexthe668.cloudstorage.CloudStorage;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CSParticleRegistry {

    public static final DeferredRegister<ParticleType<?>> DEF_REG = DeferredRegister.create(Registries.PARTICLE_TYPE, CloudStorage.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BALLOON_SHARD = DEF_REG.register("balloon_shard", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CLOUD_CHEST = DEF_REG.register("cloud_chest", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STATIC_LIGHTNING = DEF_REG.register("static_lightning", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOVIATOR_BREATH = DEF_REG.register("bloviator_breath", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STOP_SPAWN = DEF_REG.register("stop_spawn", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COOL = DEF_REG.register("cool", () -> new SimpleParticleType(false));

}
