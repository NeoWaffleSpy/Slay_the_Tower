package com.Varrell.gamemodeAPI.Commands.Camera;

import com.Varrell.gamemodeAPI.Camera.CameraInitializer;
import com.Varrell.gamemodeAPI.Camera.MouseControl.DefaultMouseControl;
import com.Varrell.gamemodeAPI.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeDoublePosition;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

public class CustomCameraCommand extends AbstractPlayerCommand {
    private final OptionalArg<Float> positionLerpSpeed;
    private final OptionalArg<Float> rotationLerpSpeed;
    private final OptionalArg<Float> distance;
    private final OptionalArg<Float> speedModifier;
    private final OptionalArg<Boolean> allowPitchControls;
    private final OptionalArg<Boolean> displayCursor;
    private final OptionalArg<Boolean> displayReticle;
    private final OptionalArg<String> mouseInputTargetType;
    private final OptionalArg<Boolean> sendMouseMotion;
    private final OptionalArg<Boolean> skipCharacterPhysics;
    private final OptionalArg<Boolean> isFirstPerson;
    private final OptionalArg<String> movementForceRotationType;
    private final OptionalArg<com.hypixel.hytale.math.vector.Vector3f> movementForceRotation;
    private final OptionalArg<String> attachedToType;
    private final OptionalArg<Integer> attachedToEntityId;
    private final OptionalArg<Boolean> eyeOffset;
    private final OptionalArg<String> positionDistanceOffsetType;
    private final OptionalArg<com.hypixel.hytale.math.vector.Vector3f> positionOffset;
    private final OptionalArg<com.hypixel.hytale.math.vector.Vector3f> rotationOffset;
    private final OptionalArg<String> positionType;
    private final OptionalArg<RelativeDoublePosition> position;
    private final OptionalArg<String> rotationType;
    private final OptionalArg<com.hypixel.hytale.math.vector.Vector3f> rotation;
    private final OptionalArg<String> canMoveType;
    private final OptionalArg<String> applyMovementType;
    private final OptionalArg<com.hypixel.hytale.math.vector.Vector3f> movementMultiplier;
    private final OptionalArg<String> applyLookType;
    private final OptionalArg<Vector2i> lookMultiplier;
    private final OptionalArg<String> mouseInputType;
    private final OptionalArg<com.hypixel.hytale.math.vector.Vector3f> planeNormal;

    public CustomCameraCommand() {
        super("setCustom", "edit your custom camera");
        this.positionLerpSpeed = this.withOptionalArg("positionLerpSpeed", "Range: [0,1]\nTranslation Smoothening for the camera", ArgTypes.FLOAT);
        this.rotationLerpSpeed = this.withOptionalArg("rotationLerpSpeed", "Range: [0,1]\nRotation Smoothening for the camera", ArgTypes.FLOAT);
        this.distance = this.withOptionalArg("distance", "Range: No Limit\nDefine camera distance from the player", ArgTypes.FLOAT);
        /**/this.speedModifier = this.withOptionalArg("speedModifier", "Unsure of what it is used for", ArgTypes.FLOAT);
        this.allowPitchControls = this.withOptionalArg("allowPitchControls", "spaceship controls: forward allow to go toward look direction even if up in the air", ArgTypes.BOOLEAN);
        this.displayCursor = this.withOptionalArg("displayCursor", "Display the cursor outside of menues, [WARNING: NEED <--rotationType Custom> to be useable]", ArgTypes.BOOLEAN);
        /**/this.displayReticle = this.withOptionalArg("displayReticle", "Unsure of what it is used for", ArgTypes.BOOLEAN);
        this.mouseInputTargetType = this.withOptionalArg("mouseInputTargetType", "Values: " + Arrays.toString(MouseInputTargetType.VALUES) + "\n define valid target for the mouse", ArgTypes.STRING);
        /**/this.sendMouseMotion = this.withOptionalArg("sendMouseMotion", "Unsure of what it is used for", ArgTypes.BOOLEAN);
        this.skipCharacterPhysics = this.withOptionalArg("skipCharacterPhysics", "Ignore player collision and gravity", ArgTypes.BOOLEAN);
        this.isFirstPerson = this.withOptionalArg("isFirstPerson", "Hide the player model so that in FPS, it does not obstruct vision", ArgTypes.BOOLEAN);
        this.movementForceRotationType = this.withOptionalArg("movementForceRotationType", "Values: " + Arrays.toString(MovementForceRotationType.VALUES) + "\nAttachedToHead: Player move relative to head orientation.\nCameraRotation: Player move relative to the camera.\nCustom: Relative movement direction defined by movementForceRotationType", ArgTypes.STRING);
        this.movementForceRotation = this.withOptionalArg("movementForceRotation", "Range: [-360,360]\nDefine relative movement anchor if <--movementForceRotationType Custom>", ArgTypes.ROTATION);
        this.attachedToType = this.withOptionalArg("attachedToType", "Values: " + Arrays.toString(AttachedToType.VALUES) + "\nTODO", ArgTypes.STRING);
        this.attachedToEntityId = this.withOptionalArg("attachedToEntityId", "If <--attachedToType EntityId>, then TODO", ArgTypes.INTEGER);
        this.eyeOffset = this.withOptionalArg("eyeOffset", "Offset camera in third person to be centered on the head of the player instead of the feets", ArgTypes.BOOLEAN);
        this.positionDistanceOffsetType = this.withOptionalArg("positionDistanceOffsetType", "Values: " + Arrays.toString(PositionDistanceOffsetType.VALUES) + "\nDistanceOffset: Camera will phase through blocks to keep a constant offset\nDistanceOffsetRaycast: The camera will collide with blocks and dynamically change to never phase through blocks\nNone: disable third person POV", ArgTypes.STRING);
        this.positionOffset = this.withOptionalArg("positionOffset", "Offset camera position", ArgTypes.ROTATION);
        this.rotationOffset = this.withOptionalArg("rotationOffset", "Range: [-360,360]\nOffset camera rotation", ArgTypes.ROTATION);
        this.positionType = this.withOptionalArg("positionType", "Values: " + Arrays.toString(PositionType.VALUES) + "\n\nAttachedToPlusOffset: The camera will translate along with the player head\nCustom: set a fixed camera position defined by <--position x y z>", ArgTypes.STRING);
        this.position = this.withOptionalArg("position", "Set a fixed camera position when <--positionType Custom>", ArgTypes.RELATIVE_POSITION);
        this.rotationType = this.withOptionalArg("rotationType", "Values: " + Arrays.toString(RotationType.VALUES) + "\nAttachedToPlusOffset: The camera will rotate along with the player head\nCustom: set a fixed camera rotation defined by <--rotation x y z>", ArgTypes.STRING);
        this.rotation = this.withOptionalArg("rotation", "Range: [-360,360]\nSet a fixed camera rotation when <--rotationType Custom>", ArgTypes.ROTATION);
        /**/this.canMoveType = this.withOptionalArg("canMoveType", "Values: " + Arrays.toString(CanMoveType.VALUES) + "\nUnsure of what it is used for", ArgTypes.STRING);
        /**/this.applyMovementType = this.withOptionalArg("applyMovementType", "Values: " + Arrays.toString(ApplyMovementType.VALUES) + "\nUnsure of what it is used for", ArgTypes.STRING);
        this.movementMultiplier = this.withOptionalArg("movementMultiplier", "Multiplies movement vector, set to 0 to restrict a movement axis", ArgTypes.ROTATION);
        /**/this.applyLookType = this.withOptionalArg("applyLookType", "Values: " + Arrays.toString(ApplyLookType.VALUES) + "\nUnsure of what it is used for", ArgTypes.STRING);
        this.lookMultiplier = this.withOptionalArg("lookMultiplier", "Default value: <--lookMultiplier 100 100>\nChange mouse sensitivity", ArgTypes.VECTOR2I);
        this.mouseInputType = this.withOptionalArg("mouseInputType", "Values: " + Arrays.toString(MouseInputType.VALUES) + "\nDefine a valid raycast target for the mouse cursor", ArgTypes.STRING);
        this.planeNormal = this.withOptionalArg("planeNormal", "if <--mouseInputType LookAtPlane>\nDefine a plane centered on the player model as a valid raycast target (see sideView POV for reference)", ArgTypes.ROTATION);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        PlayerPOVComponent pPOV = store.getComponent(ref, PlayerPOVComponent.getComponentType());
        ServerCameraSettings settings = parseSettings(commandContext, pPOV, world, store);
        if (settings == null) { return; }
        if (pPOV == null)
            store.addComponent(ref, PlayerPOVComponent.getComponentType(), new PlayerPOVComponent("custom", settings, new DefaultMouseControl()));
        else
            CameraInitializer.editCameraSettings(playerRef, settings);
    }

    private ServerCameraSettings parseSettings(@NonNull CommandContext commandContext, PlayerPOVComponent pPOV, World world, ComponentAccessor<EntityStore> componentAccessor) {
        ServerCameraSettings settings;
        if (pPOV == null)
            settings = new ServerCameraSettings();
        else
            settings = pPOV.getCamSettings();

        MouseInputTargetType mitt = parseEnum((String)this.mouseInputTargetType.get(commandContext), MouseInputTargetType.class, settings.mouseInputTargetType, commandContext);
        if (mitt == null) return null;
        MovementForceRotationType mfrt = parseEnum((String)this.movementForceRotationType.get(commandContext), MovementForceRotationType.class, settings.movementForceRotationType, commandContext);
        if (mfrt == null) return null;
        AttachedToType att = parseEnum((String)this.attachedToType.get(commandContext), AttachedToType.class, settings.attachedToType, commandContext);
        if (att == null) return null;
        PositionDistanceOffsetType pdot = parseEnum((String)this.positionDistanceOffsetType.get(commandContext), PositionDistanceOffsetType.class, settings.positionDistanceOffsetType, commandContext);
        if (pdot == null) return null;
        PositionType posType = parseEnum((String)this.positionType.get(commandContext), PositionType.class, settings.positionType, commandContext);
        if (posType == null) return null;
        RotationType rotType = parseEnum((String)this.rotationType.get(commandContext), RotationType.class, settings.rotationType, commandContext);
        if (rotType == null) return null;
        CanMoveType cmt = parseEnum((String)this.canMoveType.get(commandContext), CanMoveType.class, settings.canMoveType, commandContext);
        if (cmt == null) return null;
        ApplyMovementType amt = parseEnum((String)this.applyMovementType.get(commandContext), ApplyMovementType.class, settings.applyMovementType, commandContext);
        if (amt == null) return null;
        ApplyLookType alt = parseEnum((String)this.applyLookType.get(commandContext), ApplyLookType.class, settings.applyLookType, commandContext);
        if (alt == null) return null;
        MouseInputType mit = parseEnum((String)this.mouseInputType.get(commandContext), MouseInputType.class, settings.mouseInputType, commandContext);
        if (mit == null) return null;

        settings.positionLerpSpeed = getVal(settings.positionLerpSpeed, this.positionLerpSpeed.get(commandContext));
        settings.rotationLerpSpeed = getVal(settings.rotationLerpSpeed, this.rotationLerpSpeed.get(commandContext));
        settings.distance = getVal(settings.distance, this.distance.get(commandContext));
        settings.speedModifier = getVal(settings.speedModifier, this.speedModifier.get(commandContext));
        settings.allowPitchControls = getVal(settings.allowPitchControls, this.allowPitchControls.get(commandContext));
        settings.displayCursor = getVal(settings.displayCursor, this.displayCursor.get(commandContext));
        settings.displayReticle = getVal(settings.displayReticle, this.displayReticle.get(commandContext));
        settings.mouseInputTargetType = mitt;
        settings.sendMouseMotion = getVal(settings.sendMouseMotion, this.sendMouseMotion.get(commandContext));
        settings.skipCharacterPhysics = getVal(settings.skipCharacterPhysics, this.skipCharacterPhysics.get(commandContext));
        settings.isFirstPerson = getVal(settings.isFirstPerson, this.isFirstPerson.get(commandContext));
        settings.movementForceRotationType = mfrt;
        settings.movementForceRotation = getDirectionFromCoord(settings.movementForceRotation, this.movementForceRotation.get(commandContext));
        settings.attachedToType = att;
        settings.attachedToEntityId = getVal(settings.attachedToEntityId, this.attachedToEntityId.get(commandContext));
        settings.eyeOffset = getVal(settings.eyeOffset, this.eyeOffset.get(commandContext));
        settings.positionDistanceOffsetType = pdot;
        settings.positionOffset = getPositionFromCoord(settings.positionOffset, this.positionOffset.get(commandContext));
        settings.rotationOffset = getDirectionFromCoord(settings.rotationOffset, this.rotationOffset.get(commandContext));
        settings.positionType = posType;
        settings.position = getPositionFromRelativeCoord(settings.position, this.position.get(commandContext), commandContext, world, componentAccessor);
        settings.rotationType = rotType;
        settings.rotation = getDirectionFromCoord(settings.rotation, this.rotation.get(commandContext));
        settings.canMoveType = cmt;
        settings.movementMultiplier = getVector3FromCoord(settings.movementMultiplier, this.movementMultiplier.get(commandContext));
        settings.applyLookType = alt;
        settings.lookMultiplier = getVector2FromVector2(settings.lookMultiplier, this.lookMultiplier.get(commandContext));
        settings.mouseInputType = mit;
        settings.planeNormal = getVector3FromCoord(settings.planeNormal, this.planeNormal.get(commandContext));

        return settings;
    }

    private Vector2f getVector2FromVector2(Vector2f toAssign, Vector2i vector) {
        if (vector == null)
            return toAssign;
        return new Vector2f(((float)vector.getX())/100, ((float)vector.getY())/100);
    }

    private Vector3f getVector3FromCoord(Vector3f toAssign, com.hypixel.hytale.math.vector.Vector3f pos) {
        if (pos == null)
            return toAssign;
        return new Vector3f(pos.x, pos.y, pos.z);
    }

    private Direction getDirectionFromCoord(Direction toAssign, com.hypixel.hytale.math.vector.Vector3f pos) {
        if (pos == null)
            return toAssign;
        return new Direction((float) Math.toRadians(pos.x), (float) Math.toRadians(pos.z), (float) Math.toRadians(pos.y));
    }

    private Position getPositionFromCoord(Position toAssign, com.hypixel.hytale.math.vector.Vector3f pos) {
        if (pos == null)
            return toAssign;
        return new Position(pos.x, pos.y, pos.z);
    }

    private Position getPositionFromRelativeCoord(Position toAssign, RelativeDoublePosition pos, CommandContext commandContext, World world, ComponentAccessor<EntityStore> accessor) {
        if (pos == null)
            return toAssign;
        Vector3d tmp = pos.getRelativePosition(commandContext, world, accessor);
        return new Position((float) tmp.x, (float) tmp.y, (float) tmp.z);
    }

    private <E> E getVal(E origin, E toAssign) {
        if (toAssign == null)
            return origin;
        return toAssign;
    }

    private <E extends Enum<E>> E parseEnum(String name, Class<E> enumClass, E defaultValue, @NonNull CommandContext commandContext) {
        E value;
        if (name != null) {
            value = getEnumFromString(name, enumClass);
            if (value == null) {
                commandContext.sendMessage(Message.raw(name + " is not a valid " + enumClass + " value"));
                return null;
            }
        }
        else
            return defaultValue;
        return value;
    }

    private <E extends Enum<E>> E getEnumFromString(String value, Class<E> enumClass) {
        if (value == null || enumClass == null) { return null; }

        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null; // or throw the exception depending on your needs
        }
    }
}
