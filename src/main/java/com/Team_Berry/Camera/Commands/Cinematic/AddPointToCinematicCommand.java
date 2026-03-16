package com.Team_Berry.Camera.Commands.Cinematic;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Cinematic.CinematicManager;
import com.Team_Berry.Camera.Cinematic.CinematicPlayer;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

public class AddPointToCinematicCommand extends AbstractPlayerCommand {
    @Nonnull private final RequiredArg<String> nameArg;
    @Nonnull private final RequiredArg<Integer> transitionTime;
    @Nonnull private final OptionalArg<Integer> insertPoint;

    public AddPointToCinematicCommand() {
        super("addPoint", "Add a keyframe to the cinematic");
        this.nameArg = this.withRequiredArg("Cinematic Name", "The name of the cinematic", ArgTypes.STRING);
        this.transitionTime = this.withRequiredArg("Transition Time", "length of time in milliseconds spent before starting the next keyframe", ArgTypes.INTEGER);
        this.insertPoint = this.withOptionalArg("keyframe", "Insert camera at keyframe value", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        CinematicPlayer c = CinematicManager.getCinematic(this.nameArg.get(commandContext));
        PlayerPOVComponent pPOV = store.getComponent(ref, PlayerPOVComponent.getComponentType());
        if (c == null) {
            commandContext.sendMessage(Message.raw("Cinematic Player Not Found: " + this.nameArg.get(commandContext)));
            return;
        }
        ServerCameraSettings cameraSettings;
        if (pPOV == null)
            cameraSettings = new ServerCameraSettings();
        else
            cameraSettings = pPOV.getCamSettings().clone();

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        if (cameraSettings.positionType != PositionType.Custom) {
            cameraSettings.positionType = PositionType.Custom;
            cameraSettings.position = getDeltaPosition(c.origin, transformComponent.getPosition());
        }
        if (cameraSettings.rotationType != RotationType.Custom) {
            cameraSettings.rotationType = RotationType.Custom;
            Vector3f v = headRotation.getRotation();
            cameraSettings.rotation = new Direction(v.y, v.x, v.z);
        }
        cameraSettings.isFirstPerson = false;
        cameraSettings.eyeOffset = true;
        if (this.insertPoint.provided(commandContext))
            c.addCameraPoint(cameraSettings, transitionTime.get(commandContext), this.insertPoint.get(commandContext));
        else
            c.addCameraPoint(cameraSettings, transitionTime.get(commandContext));
    }

    private Position getDeltaPosition(Position origin, Vector3d key) {
        return new Position(key.x - origin.x, (key.y + 2) - origin.y, key.z - origin.z);
    }
}
