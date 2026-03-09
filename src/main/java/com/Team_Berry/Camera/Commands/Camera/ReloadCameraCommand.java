package com.Team_Berry.Camera.Commands.Camera;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public class ReloadCameraCommand extends AbstractPlayerCommand {
    public ReloadCameraCommand() {
        super("reload", "Reload your current camera setting if changed in JSON");
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {

        PlayerPOVComponent pPOV = store.getComponent(ref, PlayerPOVComponent.getComponentType());
        if (pPOV == null) {
            commandContext.sendMessage(Message.raw("You do not have any POV applied"));
            return;
        }

        CameraInitializer.getJsonSettings(pPOV.getPOVName());
        CameraInitializer.editCameraSettings(playerRef, CameraInitializer.getTemplate(pPOV.getPOVName()));
    }
}
