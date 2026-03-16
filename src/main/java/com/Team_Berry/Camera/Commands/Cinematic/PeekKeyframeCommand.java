package com.Team_Berry.Camera.Commands.Cinematic;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Cinematic.CinematicManager;
import com.Team_Berry.Camera.Cinematic.CinematicPlayer;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class PeekKeyframeCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> nameArg;
    private final RequiredArg<Integer> keyFrame;

    public PeekKeyframeCommand() {
        super("peek", "Set your camera to the desired keyframe of a cinematic for editing purposes");
        this.nameArg = this.withRequiredArg("name", "The name of the cinemaPlayer to edit", ArgTypes.STRING);
        this.keyFrame = this.withRequiredArg("keyframe", "the Keyframe to edit", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        CinematicPlayer c = CinematicManager.getCinematic(this.nameArg.get(commandContext));
        if (c == null) {
            commandContext.sendMessage(Message.raw("No cinematic player named " + nameArg.get(commandContext)).color(Color.RED));
            return;
        }
        ServerCameraSettings s = c.getCinemaPoint(this.keyFrame.get(commandContext)).clone();
        PlayerPOVComponent pPOV = store.getComponent(ref, PlayerPOVComponent.getComponentType());
        if (pPOV == null)
            CameraInitializer.setPlayerPov("custom", playerRef);
        s.position = addPosition(s.position, c.origin);
        CameraInitializer.editCameraSettings(playerRef, s);
    }

    private Position addPosition(Position a, Position b) {
        return new Position(a.x + b.x, a.y + b.y, a.z + b.z);
    }
}
