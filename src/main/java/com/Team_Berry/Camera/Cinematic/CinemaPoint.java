package com.Team_Berry.Camera.Cinematic;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.*;

import static com.Team_Berry.Utils.CodecUtils.*;

public class CinemaPoint extends ServerCameraSettings {
    public static final BuilderCodec<CinemaPoint> CODEC;
    public long transitionTime = 1000L;

    public CinemaPoint(ServerCameraSettings cameraSettings, long transitionTime) {
        super(cameraSettings);
        this.transitionTime = transitionTime;
    }

    public CinemaPoint(CinemaPoint cinemaPoint) {
        super(cinemaPoint);
        this.transitionTime = cinemaPoint.transitionTime;
    }

    public CinemaPoint() {
        super();
    }

    public CinemaPoint clone() {
        return new CinemaPoint(this);
    }

    static {
        CODEC = BuilderCodec.builder(CinemaPoint.class, CinemaPoint::new)
                .append(new KeyedCodec<>("TransitionTime", Codec.LONG, true),
                        (obj, val) -> obj.transitionTime = val,
                        obj -> obj.transitionTime)
                .addValidator(Validators.min(1L))
                .documentation("Time in milliseconds before swapping to the next keyframe").add()
                .append(new KeyedCodec<>("PositionLerpSpeed", Codec.FLOAT, true),
                        (obj, val) -> obj.positionLerpSpeed = val,
                        obj -> obj.positionLerpSpeed)
                .addValidator(Validators.min(0f)).addValidator(Validators.max(1f))
                .documentation("Translation Smoothening for the camera").add()
                .append(new KeyedCodec<>("RotationLerpSpeed", Codec.FLOAT, true),
                        (obj, val) -> obj.rotationLerpSpeed = val,
                        obj -> obj.rotationLerpSpeed)
                .addValidator(Validators.min(0f)).addValidator(Validators.max(1f))
                .documentation("Rotation Smoothening for the camera").add()
                .append(new KeyedCodec<>("Distance", Codec.FLOAT, true),
                        (obj, val) -> obj.distance = val,
                        obj -> obj.distance)
                .documentation("Define the camera distance from the player").add()
                .append(new KeyedCodec<>("SpeedModifier", Codec.FLOAT, true),
                        (obj, val) -> obj.speedModifier = val,
                        obj -> obj.speedModifier)
                .documentation("Unsure of what it is used for").add()
                .append(new KeyedCodec<>("AllowPitchControls", Codec.BOOLEAN, true),
                        (obj, val) -> obj.allowPitchControls = val,
                        obj -> obj.allowPitchControls)
                .documentation("spaceship controls: forward allow to go toward look direction even if up in the air").add()
                .append(new KeyedCodec<>("DisplayCursor", Codec.BOOLEAN, true),
                        (obj, val) -> obj.displayCursor = val,
                        obj -> obj.displayCursor)
                .documentation("Display the cursor outside of menues, [WARNING: NEED <RotationType Custom> to be useable]").add()
                .append(new KeyedCodec<>("DisplayReticle", Codec.BOOLEAN, true),
                        (obj, val) -> obj.displayReticle = val,
                        obj -> obj.displayReticle)
                .documentation("Unsure of what it is used for").add()
                .append(new KeyedCodec<MouseInputTargetType>("MouseInputTargetType", new EnumCodec(MouseInputTargetType.class), true),
                        (obj, val) -> obj.mouseInputTargetType = val,
                        obj -> obj.mouseInputTargetType)
                .documentation("Define valid target for the mouse").add()
                .append(new KeyedCodec<>("SendMouseMotion", Codec.BOOLEAN, true),
                        (obj, val) -> obj.sendMouseMotion = val,
                        obj -> obj.sendMouseMotion)
                .documentation("Unsure of what it is used for").add()
                .append(new KeyedCodec<>("SkipCharacterPhysics", Codec.BOOLEAN, true),
                        (obj, val) -> obj.skipCharacterPhysics = val,
                        obj -> obj.skipCharacterPhysics)
                .documentation("Ignore player collision and gravity").add()
                .append(new KeyedCodec<>("IsFirstPerson", Codec.BOOLEAN, true),
                        (obj, val) -> obj.isFirstPerson = val,
                        obj -> obj.isFirstPerson)
                .documentation("Show or hide the player model in TPS and player arms in FPS").add()
                .append(new KeyedCodec<MovementForceRotationType>("MovementForceRotationType", new EnumCodec(MovementForceRotationType.class), true),
                        (obj, val) -> obj.movementForceRotationType = val,
                        obj -> obj.movementForceRotationType)
                .documentation("AttachedToHead: Player move relative to head orientation.\nCameraRotation: Player move relative to the camera.\nCustom: Relative movement direction defined by movementForceRotationType").add()
                .append(new KeyedCodec<>("MovementForceRotation", DIR_CODEC, true),
                        (obj, val) -> obj.movementForceRotation = val,
                        obj -> obj.movementForceRotation)
                .documentation("Define relative movement anchor if <MovementForceRotationType Custom>").add()
                .append(new KeyedCodec<AttachedToType>("AttachedToType", new EnumCodec(AttachedToType.class), true),
                        (obj, val) -> obj.attachedToType = val,
                        obj -> obj.attachedToType)
                .documentation("TODO").add()
                .append(new KeyedCodec<>("AttachedToEntityId", Codec.INTEGER, true),
                        (obj, val) -> obj.attachedToEntityId = val,
                        obj -> obj.attachedToEntityId)
                .documentation("If <AttachedToType EntityId>, then TODO").add()
                .append(new KeyedCodec<>("EyeOffset", Codec.BOOLEAN, true),
                        (obj, val) -> obj.eyeOffset = val,
                        obj -> obj.eyeOffset)
                .documentation("Offset camera in third person to be centered on the head of the player instead of the feets").add()
                .append(new KeyedCodec<PositionDistanceOffsetType>("PositionDistanceOffsetType", new EnumCodec(PositionDistanceOffsetType.class), true),
                        (obj, val) -> obj.positionDistanceOffsetType = val,
                        obj -> obj.positionDistanceOffsetType)
                .documentation("DistanceOffset: Camera will phase through blocks to keep a constant offset\nDistanceOffsetRaycast: The camera will collide with blocks and dynamically change to never phase through blocks\nNone: disable third person POV").add()
                .append(new KeyedCodec<>("PositionOffset", POS_CODEC, true),
                        (obj, val) -> obj.positionOffset = val,
                        obj -> obj.positionOffset)
                .documentation("Offset camera position").add()
                .append(new KeyedCodec<>("RotationOffset", DIR_CODEC, true),
                        (obj, val) -> obj.rotationOffset = val,
                        obj -> obj.rotationOffset)
                .documentation("Offset camera rotation").add()
                .append(new KeyedCodec<PositionType>("PositionType", new EnumCodec(PositionType.class), true),
                        (obj, val) -> obj.positionType = val,
                        obj -> obj.positionType)
                .documentation("AttachedToPlusOffset: The camera will translate along with the player head\nCustom: set a fixed camera position defined by <Position x y z>").add()
                .append(new KeyedCodec<>("Position", POS_CODEC, true),
                        (obj, val) -> obj.position = val,
                        obj -> obj.position)
                .documentation("Set a fixed camera position when <PositionType Custom>").add()
                .append(new KeyedCodec<RotationType>("RotationType", new EnumCodec(RotationType.class), true),
                        (obj, val) -> obj.rotationType = val,
                        obj -> obj.rotationType)
                .documentation("AttachedToPlusOffset: The camera will rotate along with the player head\nCustom: set a fixed camera rotation defined by <--rotation x y z>").add()
                .append(new KeyedCodec<>("Rotation", DIR_CODEC, true),
                        (obj, val) -> obj.rotation = val,
                        obj -> obj.rotation)
                .documentation("Set a fixed camera rotation when <RotationType Custom>").add()
                .append(new KeyedCodec<CanMoveType>("CanMoveType", new EnumCodec(CanMoveType.class), true),
                        (obj, val) -> obj.canMoveType = val,
                        obj -> obj.canMoveType)
                .documentation("Unsure of what it is used for").add()
                .append(new KeyedCodec<ApplyMovementType>("ApplyMovementType", new EnumCodec(ApplyMovementType.class), true),
                        (obj, val) -> obj.applyMovementType = val,
                        obj -> obj.applyMovementType)
                .documentation("Unsure of what it is used for").add()
                .append(new KeyedCodec<>("MovementMultiplier", VEC3_CODEC, true),
                        (obj, val) -> obj.movementMultiplier = val,
                        obj -> obj.movementMultiplier)
                .documentation("Multiplies movement vector, set to 0 to restrict a movement axis").add()
                .append(new KeyedCodec<ApplyLookType>("ApplyLookType", new EnumCodec(ApplyLookType.class), true),
                        (obj, val) -> obj.applyLookType = val,
                        obj -> obj.applyLookType)
                .documentation("Unsure of what it is used for").add()
                .append(new KeyedCodec<>("LookMultiplier", VEC2_CODEC, true),
                        (obj, val) -> obj.lookMultiplier = val,
                        obj -> obj.lookMultiplier)
                .documentation("Change mouse sensitivity").add()
                .append(new KeyedCodec<MouseInputType>("MouseInputType", new EnumCodec(MouseInputType.class), true),
                        (obj, val) -> obj.mouseInputType = val,
                        obj -> obj.mouseInputType)
                .documentation("Define a valid raycast target for the mouse cursor").add()
                .append(new KeyedCodec<>("PlaneNormal", CLAMP_VEC3_CODEC, true),
                        (obj, val) -> obj.planeNormal = val,
                        obj -> obj.planeNormal)
                .documentation("Define a plane centered on the player model as a valid raycast if if <MouseInputType LookAtPlane>").add()
                .build();
    }
}
