package com.Team_Berry.Camera.Commands.Cinematic;

import com.Team_Berry.Camera.Cinematic.CinematicManager;
import com.Team_Berry.Camera.Cinematic.CinematicPlayer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeDoublePosition;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

public class RunCinematicCommand extends AbstractPlayerCommand {
    @Nonnull private final RequiredArg<String> nameArg;
    private final OptionalArg<RelativeDoublePosition> positionArg;
    private final OptionalArg<Boolean> isDebugMode;
    public RunCinematicCommand() {
        super("run", "Run a cinematic");
        this.nameArg = this.withRequiredArg("Cinematic Name", "The name of the cinematic", ArgTypes.STRING);
        this.positionArg = this.withOptionalArg("origin", "Origin point of the cinematic", ArgTypes.RELATIVE_POSITION);
        this.isDebugMode = this.withOptionalArg("debug", "give keyframe feedback in the chatbox", ArgTypes.BOOLEAN);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        String cinemaName = this.nameArg.get(commandContext);
        CinematicPlayer c = CinematicManager.getCinematic(cinemaName);
        if (c == null)
            c = CinematicManager.get(cinemaName);
        if (c == null) {
            commandContext.sendMessage(Message.raw("Cinematic Player Not Found: " + this.nameArg.get(commandContext)));
            return;
        }
        Position p;
        if (this.positionArg.provided(commandContext)) {
            p = getPositionFromRelativeCoord(this.positionArg.get(commandContext), commandContext, world, store);
        }
        else {
            com.hypixel.hytale.math.vector.Vector3d t = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
            p = new Position(t.x, t.y, t.z);
        }
        c.runCinematic(playerRef, p, world, this.isDebugMode.get(commandContext) ? commandContext : null);
    }


    private Position getPositionFromRelativeCoord(RelativeDoublePosition pos, CommandContext commandContext, World world, ComponentAccessor<EntityStore> accessor) {
        com.hypixel.hytale.math.vector.Vector3d tmp = pos.getRelativePosition(commandContext, world, accessor);
        return new Position((float) tmp.x, (float) tmp.y, (float) tmp.z);
    }
}
