package com.Varrell.gamemodeAPI.Commands.Camera;

import com.Varrell.gamemodeAPI.Camera.CameraInitializer;
import com.Varrell.gamemodeAPI.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.builtin.teleport.commands.teleport.TeleportCommand;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

public class SetCameraCommand extends AbstractPlayerCommand {
    @Nonnull
    private final RequiredArg<String> nameArg;
    public SetCameraCommand() {
        super("set", "Set your camera with a camera setting");
        this.nameArg = this.withRequiredArg("cameraName", "The camera name already set in the system", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        String name = (String)commandContext.get(this.nameArg);
        CameraInitializer cam = CameraInitializer.get(name);
        if (cam == null) {
            commandContext.sendMessage(Message.raw(name + " is not a valid POV"));
            return;
        }
        PlayerPOVComponent pPOV = store.getComponent(ref, PlayerPOVComponent.getComponentType());
        if (pPOV != null)
            CameraInitializer.deletePOV(playerRef);
        if (!cam.isActive) {
            commandContext.sendMessage(Message.raw("The " + name + " POV is disabled"));
            return;
        }
        store.addComponent(ref, PlayerPOVComponent.getComponentType(), new PlayerPOVComponent(name));
    }
}
