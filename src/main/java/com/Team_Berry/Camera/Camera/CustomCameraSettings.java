package com.Team_Berry.Camera.Camera;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.*;

import java.util.Collection;
import static com.Team_Berry.Utils.CodecUtils.*;

public class CustomCameraSettings implements JsonAssetWithMap<String, DefaultAssetMap<String, CustomCameraSettings>> {
    private static AssetStore<String, CustomCameraSettings, DefaultAssetMap<String, CustomCameraSettings>> ASSET_STORE;
    public static final AssetBuilderCodec<String, CustomCameraSettings> CODEC;

    private String cameraName = "Template";
    private AssetExtraInfo.Data data;
    private ServerCameraSettings camSettings = new ServerCameraSettings();

    public CustomCameraSettings() {}
    public CustomCameraSettings(String cameraName) { this.cameraName = cameraName;}
    public CustomCameraSettings(String cameraName, ServerCameraSettings camSettings) {
        this.cameraName = cameraName;
        this.camSettings = camSettings;
    }

    @Override
    public String getId() { return cameraName; }
    public ServerCameraSettings getCameraSettings() { return camSettings; }

    public static AssetStore<String, CustomCameraSettings, DefaultAssetMap<String, CustomCameraSettings>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(CustomCameraSettings.class);
        }
        return ASSET_STORE;
    }

    public static Collection<CustomCameraSettings> getAssetMap() {
        return getAssetStore().getAssetMap().getAssetMap().values();
    }

    static {
       CODEC = AssetBuilderCodec.builder(CustomCameraSettings.class, CustomCameraSettings::new, Codec.STRING,
                       (t, k) -> t.cameraName = k, (t) -> t.cameraName,
                       (asset, data) -> asset.data = data, (asset) -> asset.data)
               .append(new KeyedCodec<>("PositionLerpSpeed", Codec.FLOAT, true),
                       (obj, val) -> obj.camSettings.positionLerpSpeed = val,
                       obj -> obj.camSettings.positionLerpSpeed)
               .addValidator(Validators.min(0f)).addValidator(Validators.max(1f))
               .documentation("Translation Smoothening for the camera").add()
               .append(new KeyedCodec<>("RotationLerpSpeed", Codec.FLOAT, true),
                       (obj, val) -> obj.camSettings.rotationLerpSpeed = val,
                       obj -> obj.camSettings.rotationLerpSpeed)
               .addValidator(Validators.min(0f)).addValidator(Validators.max(1f))
               .documentation("Rotation Smoothening for the camera").add()
               .append(new KeyedCodec<>("Distance", Codec.FLOAT, true),
                       (obj, val) -> obj.camSettings.distance = val,
                       obj -> obj.camSettings.distance)
               .documentation("Define the camera distance from the player").add()
               .append(new KeyedCodec<>("SpeedModifier", Codec.FLOAT, true),
                       (obj, val) -> obj.camSettings.speedModifier = val,
                       obj -> obj.camSettings.speedModifier)
               .documentation("Unsure of what it is used for").add()
               .append(new KeyedCodec<>("AllowPitchControls", Codec.BOOLEAN, true),
                       (obj, val) -> obj.camSettings.allowPitchControls = val,
                       obj -> obj.camSettings.allowPitchControls)
               .documentation("spaceship controls: forward allow to go toward look direction even if up in the air").add()
               .append(new KeyedCodec<>("DisplayCursor", Codec.BOOLEAN, true),
                       (obj, val) -> obj.camSettings.displayCursor = val,
                       obj -> obj.camSettings.displayCursor)
               .documentation("Display the cursor outside of menues, [WARNING: NEED <RotationType Custom> to be useable]").add()
               .append(new KeyedCodec<>("DisplayReticle", Codec.BOOLEAN, true),
                       (obj, val) -> obj.camSettings.displayReticle = val,
                       obj -> obj.camSettings.displayReticle)
               .documentation("Unsure of what it is used for").add()
               .append(new KeyedCodec<MouseInputTargetType>("MouseInputTargetType", new EnumCodec(MouseInputTargetType.class), true),
                       (obj, val) -> obj.camSettings.mouseInputTargetType = val,
                       obj -> obj.camSettings.mouseInputTargetType)
               .documentation("Define valid target for the mouse").add()
               .append(new KeyedCodec<>("SendMouseMotion", Codec.BOOLEAN, true),
                       (obj, val) -> obj.camSettings.sendMouseMotion = val,
                       obj -> obj.camSettings.sendMouseMotion)
               .documentation("Unsure of what it is used for").add()
               .append(new KeyedCodec<>("SkipCharacterPhysics", Codec.BOOLEAN, true),
                       (obj, val) -> obj.camSettings.skipCharacterPhysics = val,
                       obj -> obj.camSettings.skipCharacterPhysics)
               .documentation("Ignore player collision and gravity").add()
               .append(new KeyedCodec<>("IsFirstPerson", Codec.BOOLEAN, true),
                       (obj, val) -> obj.camSettings.isFirstPerson = val,
                       obj -> obj.camSettings.isFirstPerson)
               .documentation("Show or hide the player model in TPS and player arms in FPS").add()
               .append(new KeyedCodec<MovementForceRotationType>("MovementForceRotationType", new EnumCodec(MovementForceRotationType.class), true),
                       (obj, val) -> obj.camSettings.movementForceRotationType = val,
                       obj -> obj.camSettings.movementForceRotationType)
               .documentation("AttachedToHead: Player move relative to head orientation.\nCameraRotation: Player move relative to the camera.\nCustom: Relative movement direction defined by movementForceRotationType").add()
               .append(new KeyedCodec<>("MovementForceRotation", DIR_CODEC, true),
                       (obj, val) -> obj.camSettings.movementForceRotation = val,
                       obj -> obj.camSettings.movementForceRotation)
               .documentation("Define relative movement anchor if <MovementForceRotationType Custom>").add()
               .append(new KeyedCodec<AttachedToType>("AttachedToType", new EnumCodec(AttachedToType.class), true),
                       (obj, val) -> obj.camSettings.attachedToType = val,
                       obj -> obj.camSettings.attachedToType)
               .documentation("TODO").add()
               .append(new KeyedCodec<>("AttachedToEntityId", Codec.INTEGER, true),
                       (obj, val) -> obj.camSettings.attachedToEntityId = val,
                       obj -> obj.camSettings.attachedToEntityId)
               .documentation("If <AttachedToType EntityId>, then TODO").add()
               .append(new KeyedCodec<>("EyeOffset", Codec.BOOLEAN, true),
                       (obj, val) -> obj.camSettings.eyeOffset = val,
                       obj -> obj.camSettings.eyeOffset)
               .documentation("Offset camera in third person to be centered on the head of the player instead of the feets").add()
               .append(new KeyedCodec<PositionDistanceOffsetType>("PositionDistanceOffsetType", new EnumCodec(PositionDistanceOffsetType.class), true),
                       (obj, val) -> obj.camSettings.positionDistanceOffsetType = val,
                       obj -> obj.camSettings.positionDistanceOffsetType)
               .documentation("DistanceOffset: Camera will phase through blocks to keep a constant offset\nDistanceOffsetRaycast: The camera will collide with blocks and dynamically change to never phase through blocks\nNone: disable third person POV").add()
               .append(new KeyedCodec<>("PositionOffset", POS_CODEC, true),
                       (obj, val) -> obj.camSettings.positionOffset = val,
                       obj -> obj.camSettings.positionOffset)
               .documentation("Offset camera position").add()
               .append(new KeyedCodec<>("RotationOffset", DIR_CODEC, true),
                       (obj, val) -> obj.camSettings.rotationOffset = val,
                       obj -> obj.camSettings.rotationOffset)
               .documentation("Offset camera rotation").add()
               .append(new KeyedCodec<PositionType>("PositionType", new EnumCodec(PositionType.class), true),
                       (obj, val) -> obj.camSettings.positionType = val,
                       obj -> obj.camSettings.positionType)
               .documentation("AttachedToPlusOffset: The camera will translate along with the player head\nCustom: set a fixed camera position defined by <Position x y z>").add()
               .append(new KeyedCodec<>("Position", POS_CODEC, true),
                       (obj, val) -> obj.camSettings.position = val,
                       obj -> obj.camSettings.position)
               .documentation("Set a fixed camera position when <PositionType Custom>").add()
               .append(new KeyedCodec<RotationType>("RotationType", new EnumCodec(RotationType.class), true),
                       (obj, val) -> obj.camSettings.rotationType = val,
                       obj -> obj.camSettings.rotationType)
               .documentation("AttachedToPlusOffset: The camera will rotate along with the player head\nCustom: set a fixed camera rotation defined by <--rotation x y z>").add()
               .append(new KeyedCodec<>("Rotation", DIR_CODEC, true),
                       (obj, val) -> obj.camSettings.rotation = val,
                       obj -> obj.camSettings.rotation)
               .documentation("Set a fixed camera rotation when <RotationType Custom>").add()
               .append(new KeyedCodec<CanMoveType>("CanMoveType", new EnumCodec(CanMoveType.class), true),
                       (obj, val) -> obj.camSettings.canMoveType = val,
                       obj -> obj.camSettings.canMoveType)
               .documentation("Unsure of what it is used for").add()
               .append(new KeyedCodec<ApplyMovementType>("ApplyMovementType", new EnumCodec(ApplyMovementType.class), true),
                       (obj, val) -> obj.camSettings.applyMovementType = val,
                       obj -> obj.camSettings.applyMovementType)
               .documentation("Unsure of what it is used for").add()
               .append(new KeyedCodec<>("MovementMultiplier", VEC3_CODEC, true),
                       (obj, val) -> obj.camSettings.movementMultiplier = val,
                       obj -> obj.camSettings.movementMultiplier)
               .documentation("Multiplies movement vector, set to 0 to restrict a movement axis").add()
               .append(new KeyedCodec<ApplyLookType>("ApplyLookType", new EnumCodec(ApplyLookType.class), true),
                       (obj, val) -> obj.camSettings.applyLookType = val,
                       obj -> obj.camSettings.applyLookType)
               .documentation("Unsure of what it is used for").add()
               .append(new KeyedCodec<>("LookMultiplier", VEC2_CODEC, true),
                       (obj, val) -> obj.camSettings.lookMultiplier = val,
                       obj -> obj.camSettings.lookMultiplier)
               .documentation("Change mouse sensitivity").add()
               .append(new KeyedCodec<MouseInputType>("MouseInputType", new EnumCodec(MouseInputType.class), true),
                       (obj, val) -> obj.camSettings.mouseInputType = val,
                       obj -> obj.camSettings.mouseInputType)
               .documentation("Define a valid raycast target for the mouse cursor").add()
               .append(new KeyedCodec<>("PlaneNormal", CLAMP_VEC3_CODEC, true),
                       (obj, val) -> obj.camSettings.planeNormal = val,
                       obj -> obj.camSettings.planeNormal)
               .documentation("Define a plane centered on the player model as a valid raycast if if <MouseInputType LookAtPlane>").add()
               .build();
    }
}
