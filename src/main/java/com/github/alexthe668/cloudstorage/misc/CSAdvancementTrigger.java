package com.github.alexthe668.cloudstorage.misc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;

public class CSAdvancementTrigger extends SimpleCriterionTrigger<CSAdvancementTrigger.Instance> {
    public final ResourceLocation resourceLocation;
    public CSAdvancementTrigger(ResourceLocation id) { resourceLocation = id; }
    @Override public Codec<Instance> codec() { return Instance.CODEC; }
    public void trigger(ServerPlayer player) { trigger(player, instance -> true); }
    public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)
        ).apply(instance, Instance::new));
    }
}
