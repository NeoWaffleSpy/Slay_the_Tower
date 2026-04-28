package com.Team_Berry.Game.Interactions;

import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.projectile.config.Projectile;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.projectile.config.BallisticData;
import com.hypixel.hytale.server.core.modules.projectile.config.BallisticDataProvider;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import java.util.UUID;
import java.util.random.RandomGenerator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ProjectileRainInteraction extends SimpleInstantInteraction implements BallisticDataProvider {
    @Nonnull
    public static final BuilderCodec<ProjectileRainInteraction> CODEC;
    protected String projectileId;
    protected int projectileCount = 0;
    protected float angleOffset = 0.1f;
    protected float minAngleOffset = 0.1f;

    private final float MAX_PITCH_ANGLE = 1.5607964f;

    public String getProjectileId() {
        return this.projectileId;
    }

    @Nullable
    public BallisticData getBallisticData() {
        return Projectile.getAssetMap().getAsset(this.projectileId);
    }

    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

        assert commandBuffer != null;

        World world = commandBuffer.getExternalData().getWorld();
        Ref<EntityStore> sourceRef = context.getEntity();
        Entity var8 = EntityUtils.getEntity(sourceRef, commandBuffer);
        if (var8 instanceof LivingEntity) {
            Transform lookVec = TargetUtil.getLook(sourceRef, commandBuffer);
            Vector3d lookPosition = lookVec.getPosition();
            UUIDComponent sourceUuidComponent = commandBuffer.getComponent(sourceRef, UUIDComponent.getComponentType());
            if (sourceUuidComponent != null) {
                UUID sourceUuid = sourceUuidComponent.getUuid();
                TimeResource timeResource = commandBuffer.getResource(TimeResource.getResourceType());

                for (Vector3f lookRotation : circleDirections()) {
                    Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(timeResource, this.projectileId, lookPosition, lookRotation);
                    ProjectileComponent projectileComponent = holder.getComponent(ProjectileComponent.getComponentType());

                    assert projectileComponent != null;

                    holder.ensureComponent(Intangible.getComponentType());
                    if (projectileComponent.getProjectile() == null) {
                        projectileComponent.initialize();
                        if (projectileComponent.getProjectile() == null) {
                            return;
                        }
                    }
                    projectileComponent.shoot(holder, sourceUuid, lookPosition.getX(), lookPosition.getY(), lookPosition.getZ(), lookRotation.getYaw(), lookRotation.getPitch());
                    commandBuffer.addEntity(holder, AddReason.SPAWN);
                }
            }
        }
    }

    public Vector3f[] circleDirections() {
        var rng = RandomGenerator.getDefault();
        var directions = new Vector3f[projectileCount];

        float pitch = MAX_PITCH_ANGLE - rng.nextFloat() * (angleOffset - minAngleOffset) + minAngleOffset;
        for (int i = 0; i < projectileCount; i++) {
            float yaw   = (float) (Math.PI - (2 * Math.PI * i / projectileCount));

            directions[i] = new Vector3f();
            directions[i].setPitch(pitch);
            directions[i].setYaw(yaw);
        }

        return directions;
    }

    protected void simulateFirstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
    }

    static {
        CODEC = BuilderCodec.builder(ProjectileRainInteraction.class, ProjectileRainInteraction::new, SimpleInstantInteraction.CODEC)
                .documentation("Launches a projectile.")
                .append(new KeyedCodec<>("ProjectileId", Codec.STRING),
                        (i, o) -> i.projectileId = o,
                        (i) -> i.projectileId)
                .addValidator(Validators.nonNull())
                .addValidator(Projectile.VALIDATOR_CACHE.getValidator().late()).add()
                .append(new KeyedCodec<>("Projectile Count", Codec.INTEGER),
                        (i, o) -> i.projectileCount = o,
                        (i) -> i.projectileCount).add()
                .append(new KeyedCodec<>("Maximum Angle Offset", Codec.FLOAT),
                        (i, o) -> i.angleOffset = o,
                        (i) -> i.angleOffset).add()
                .append(new KeyedCodec<>("Minimum Angle Offset", Codec.FLOAT),
                        (i, o) -> i.angleOffset = o,
                        (i) -> i.minAngleOffset).add()
                .build();
    }
}
