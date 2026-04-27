package com.Team_Berry.Game.Interactions;

import com.Team_Berry.Artefacts.Components.Systems.StatEffectSystem;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

public class SpawnAOEInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<SpawnAOEInteraction> CODEC;
    private ScheduledExecutorService scheduler;

    protected Integer radius = 5;
    protected Integer occurences = 20;
    protected Integer delay = 1000;
    protected String particlePreview;
    protected String particleExplosion;
    protected Integer damage = 20;
    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        Ref<EntityStore> ref = interactionContext.getOwningEntity();
        Store<EntityStore> store = ref.getStore();
        TransformComponent trans = store.getComponent(ref, TransformComponent.getComponentType());
        if (trans == null)
            return;
        Vector3d position = trans.getPosition();
        Vector3d[] points = randomPoints(position);
        for (Vector3d point : points) {
            ParticleUtil.spawnParticleEffect(particlePreview, point, commandBuffer);
        }

        float EXPLOSION_RADIUS = 5.0f;
        ExplosionConfig config = new StatEffectSystem.StatEffectDeathSystem.ArtefactExplosionConfig(EXPLOSION_RADIUS, damage);
        World world = store.getExternalData().getWorld();
        ComponentAccessor<ChunkStore> chunkStore = world.getChunkStore().getStore();
        if (scheduler == null)
            scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> world.execute(() -> {
            for (Vector3d point : points) {
                ParticleUtil.spawnParticleEffect(particleExplosion, point, commandBuffer);
                ExplosionUtils.performExplosion(new Damage.EntitySource(ref), point, config, ref, commandBuffer, chunkStore);
            }
        }), delay, TimeUnit.MILLISECONDS);
    }

    public Vector3d[] randomPoints(Vector3d origin) {
        var rng = RandomGenerator.getDefault();
        var points = new Vector3d[this.occurences];

        for (int i = 0; i < this.occurences; i++) {
            points[i] = new Vector3d(
                    origin.x + (rng.nextDouble() * 2 - 1) * this.radius,
                    origin.y,
                    origin.z + (rng.nextDouble() * 2 - 1) * this.radius
            );
        }

        return points;
    }

    static {
        CODEC = BuilderCodec.builder(SpawnAOEInteraction.class, SpawnAOEInteraction::new, SimpleInstantInteraction.CODEC)
                .documentation("Executes an interaction randomly around a given point (circle not sphere/square)")
                .append(new KeyedCodec<>("Radius", Codec.INTEGER),
                        (interaction, o) -> interaction.radius = o,
                        (interaction) -> interaction.radius)
                .documentation("Radius to randomly set interaction around").add()
                .append(new KeyedCodec<>("Occurences", Codec.INTEGER),
                        (interaction, o) -> interaction.occurences = o,
                        (interaction) -> interaction.occurences)
                .documentation("Radius to randomly set interaction around").add()
                .append(new KeyedCodec<>("Delay", Codec.INTEGER),
                        (interaction, o) -> interaction.delay = o,
                        (interaction) -> interaction.delay)
                .documentation("Radius to randomly set interaction around").add()
                .append(new KeyedCodec<>("Damage", Codec.INTEGER),
                        (interaction, o) -> interaction.damage = o,
                        (interaction) -> interaction.damage)
                .documentation("Radius to randomly set interaction around").add()
                .append(new KeyedCodec<>("Particle Preview", Codec.STRING),
                        (interaction, o) -> interaction.particlePreview = o,
                        (interaction) -> interaction.particlePreview)
                .addValidator(ParticleSystem.VALIDATOR_CACHE.getValidator())
                .documentation("Radius to randomly set interaction around").add()
                .append(new KeyedCodec<>("Particle Explosion", Codec.STRING),
                        (interaction, o) -> interaction.particleExplosion = o,
                        (interaction) -> interaction.particleExplosion)
                .addValidator(ParticleSystem.VALIDATOR_CACHE.getValidator())
                .documentation("Radius to randomly set interaction around").add()
                .build();
    }
}