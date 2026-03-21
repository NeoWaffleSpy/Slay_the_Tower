package com.Team_Berry.Camera.Camera;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.protocol.*;

import java.util.Collection;

@SuppressWarnings("unchecked")
public class CameraTemplates implements JsonAssetWithMap<String, DefaultAssetMap<String, CameraTemplates>> {
    private static AssetStore<String, CameraTemplates, DefaultAssetMap<String, CameraTemplates>> ASSET_STORE;
    public static final BuilderCodec<CameraTemplates> CODEC;

    private String cameraName;
    private AssetExtraInfo.Data data;
    private ServerCameraSettings camSettings = new ServerCameraSettings();

    public CameraTemplates() {}
    public CameraTemplates(String cameraName) { this.cameraName = cameraName; }
    public CameraTemplates(String cameraName, AssetExtraInfo.Data data, ServerCameraSettings camSettings) {
        this.cameraName = cameraName;
        this.data = data;
        this.camSettings = camSettings;
    }

    public static AssetStore<String, CameraTemplates, DefaultAssetMap<String, CameraTemplates>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(CameraTemplates.class);
        }
        return ASSET_STORE;
    }

    public static Collection<CameraTemplates> getAssetMap() {
        return getAssetStore().getAssetMap().getAssetMap().values();
    }

    @Override
    public String getId() { return cameraName; }

    public static final BuilderCodec<Direction> DIR_CODEC = BuilderCodec.builder(Direction.class, () -> new Direction(0, 0, 0))
            .append(new KeyedCodec<>("Yaw", Codec.FLOAT), (h, v) -> h.yaw = v, h -> h.yaw).add()
            .append(new KeyedCodec<>("Pitch", Codec.FLOAT), (h, v) -> h.pitch = v, h -> h.pitch).add()
            .append(new KeyedCodec<>("Roll", Codec.FLOAT), (h, v) -> h.roll = v, h -> h.roll).add()
            .build();

    public static final BuilderCodec<Position> POS_CODEC = BuilderCodec.builder(Position.class, () -> new Position(0, 0, 0))
            .append(new KeyedCodec<>("X", Codec.DOUBLE), (h, v) -> h.x = v, h -> h.x).add()
            .append(new KeyedCodec<>("Y", Codec.DOUBLE), (h, v) -> h.y = v, h -> h.y).add()
            .append(new KeyedCodec<>("Z", Codec.DOUBLE), (h, v) -> h.z = v, h -> h.z).add()
            .build();

    public static final BuilderCodec<Vector3f> VEC3_CODEC = BuilderCodec.builder(Vector3f.class, () -> new Vector3f(0, 0, 0))
            .append(new KeyedCodec<>("X", Codec.FLOAT), (h, v) -> h.x = v, h -> h.x).add()
            .append(new KeyedCodec<>("Y", Codec.FLOAT), (h, v) -> h.y = v, h -> h.y).add()
            .append(new KeyedCodec<>("Z", Codec.FLOAT), (h, v) -> h.z = v, h -> h.z).add()
            .build();

    public static final BuilderCodec<Vector2f> VEC2_CODEC = BuilderCodec.builder(Vector2f.class, () -> new Vector2f(0, 0))
            .append(new KeyedCodec<>("X", Codec.FLOAT), (h, v) -> h.x = v, h -> h.x).add()
            .append(new KeyedCodec<>("Y", Codec.FLOAT), (h, v) -> h.y = v, h -> h.y).add()
            .build();

    static {
       CODEC = BuilderCodec.builder(CameraTemplates.class, CameraTemplates::new)
               .append(new KeyedCodec<>("PositionLerpSpeed", Codec.FLOAT),
                       (obj, val) -> obj.camSettings.positionLerpSpeed = val,
                       obj -> obj.camSettings.positionLerpSpeed).add()
               .append(new KeyedCodec<>("RotationLerpSpeed", Codec.FLOAT),
                       (obj, val) -> obj.camSettings.rotationLerpSpeed = val,
                       obj -> obj.camSettings.rotationLerpSpeed).add()
               .append(new KeyedCodec<>("Distance", Codec.FLOAT),
                       (obj, val) -> obj.camSettings.distance = val,
                       obj -> obj.camSettings.distance).add()
               .append(new KeyedCodec<>("SpeedModifier", Codec.FLOAT),
                       (obj, val) -> obj.camSettings.speedModifier = val,
                       obj -> obj.camSettings.speedModifier).add()
               .append(new KeyedCodec<>("AllowPitchControls", Codec.BOOLEAN),
                       (obj, val) -> obj.camSettings.allowPitchControls = val,
                       obj -> obj.camSettings.allowPitchControls).add()
               .append(new KeyedCodec<>("DisplayCursor", Codec.BOOLEAN),
                       (obj, val) -> obj.camSettings.displayCursor = val,
                       obj -> obj.camSettings.displayCursor).add()
               .append(new KeyedCodec<>("DisplayReticle", Codec.BOOLEAN),
                       (obj, val) -> obj.camSettings.displayReticle = val,
                       obj -> obj.camSettings.displayReticle).add()
               .append(new KeyedCodec<MouseInputTargetType>("MouseInputTargetType", new EnumCodec(MouseInputTargetType.class)),
                       (obj, val) -> obj.camSettings.mouseInputTargetType = val,
                       obj -> obj.camSettings.mouseInputTargetType).add()
               .append(new KeyedCodec<>("SendMouseMotion", Codec.BOOLEAN),
                       (obj, val) -> obj.camSettings.sendMouseMotion = val,
                       obj -> obj.camSettings.sendMouseMotion).add()
               .append(new KeyedCodec<>("skipCharacterPhysics", Codec.BOOLEAN),
                       (obj, val) -> obj.camSettings.skipCharacterPhysics = val,
                       obj -> obj.camSettings.skipCharacterPhysics).add()
               .append(new KeyedCodec<>("IsFirstPerson", Codec.BOOLEAN),
                       (obj, val) -> obj.camSettings.isFirstPerson = val,
                       obj -> obj.camSettings.isFirstPerson).add()
               .append(new KeyedCodec<MovementForceRotationType>("MovementForceRotationType", new EnumCodec(MovementForceRotationType.class)),
                       (obj, val) -> obj.camSettings.movementForceRotationType = val,
                       obj -> obj.camSettings.movementForceRotationType).add()
               .append(new KeyedCodec<>("MovementForceRotation", DIR_CODEC),
                       (obj, val) -> obj.camSettings.movementForceRotation = val,
                       obj -> obj.camSettings.movementForceRotation).add()
               .append(new KeyedCodec<AttachedToType>("AttachedToType", new EnumCodec(AttachedToType.class)),
                       (obj, val) -> obj.camSettings.attachedToType = val,
                       obj -> obj.camSettings.attachedToType).add()
               .append(new KeyedCodec<>("AttachedToEntityId", Codec.INTEGER),
                       (obj, val) -> obj.camSettings.attachedToEntityId = val,
                       obj -> obj.camSettings.attachedToEntityId).add()
               .append(new KeyedCodec<>("EyeOffset", Codec.BOOLEAN),
                       (obj, val) -> obj.camSettings.eyeOffset = val,
                       obj -> obj.camSettings.eyeOffset).add()
               .append(new KeyedCodec<PositionDistanceOffsetType>("PositionDistanceOffsetType", new EnumCodec(PositionDistanceOffsetType.class)),
                       (obj, val) -> obj.camSettings.positionDistanceOffsetType = val,
                       obj -> obj.camSettings.positionDistanceOffsetType).add()
               .append(new KeyedCodec<>("PositionOffset", POS_CODEC),
                       (obj, val) -> obj.camSettings.positionOffset = val,
                       obj -> obj.camSettings.positionOffset).add()
               .append(new KeyedCodec<>("RotationOffset", DIR_CODEC),
                       (obj, val) -> obj.camSettings.rotationOffset = val,
                       obj -> obj.camSettings.rotationOffset).add()
               .append(new KeyedCodec<PositionType>("PositionType", new EnumCodec(PositionType.class)),
                       (obj, val) -> obj.camSettings.positionType = val,
                       obj -> obj.camSettings.positionType).add()
               .append(new KeyedCodec<>("Position", POS_CODEC),
                       (obj, val) -> obj.camSettings.position = val,
                       obj -> obj.camSettings.position).add()
               .append(new KeyedCodec<RotationType>("RotationType", new EnumCodec(RotationType.class)),
                       (obj, val) -> obj.camSettings.rotationType = val,
                       obj -> obj.camSettings.rotationType).add()
               .append(new KeyedCodec<>("Rotation", DIR_CODEC),
                       (obj, val) -> obj.camSettings.rotation = val,
                       obj -> obj.camSettings.rotation).add()
               .append(new KeyedCodec<CanMoveType>("CanMoveType", new EnumCodec(CanMoveType.class)),
                       (obj, val) -> obj.camSettings.canMoveType = val,
                       obj -> obj.camSettings.canMoveType).add()
               .append(new KeyedCodec<ApplyMovementType>("ApplyMovementType", new EnumCodec(ApplyMovementType.class)),
                       (obj, val) -> obj.camSettings.applyMovementType = val,
                       obj -> obj.camSettings.applyMovementType).add()
               .append(new KeyedCodec<>("MovementMultiplier", VEC3_CODEC),
                       (obj, val) -> obj.camSettings.movementMultiplier = val,
                       obj -> obj.camSettings.movementMultiplier).add()
               .append(new KeyedCodec<ApplyLookType>("ApplyLookType", new EnumCodec(ApplyLookType.class)),
                       (obj, val) -> obj.camSettings.applyLookType = val,
                       obj -> obj.camSettings.applyLookType).add()
               .append(new KeyedCodec<>("LookMultiplier", VEC2_CODEC),
                       (obj, val) -> obj.camSettings.lookMultiplier = val,
                       obj -> obj.camSettings.lookMultiplier).add()
               .append(new KeyedCodec<MouseInputType>("MouseInputType", new EnumCodec(MouseInputType.class)),
                       (obj, val) -> obj.camSettings.mouseInputType = val,
                       obj -> obj.camSettings.mouseInputType).add()
               .append(new KeyedCodec<>("PlaneNormal", VEC3_CODEC),
                       (obj, val) -> obj.camSettings.planeNormal = val,
                       obj -> obj.camSettings.planeNormal).add()
               .build();
    }
}
