package com.Team_Berry.Camera.Commands.Camera;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.Camera.CameraTemplates;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.fusesource.jansi.Ansi;
import org.jspecify.annotations.NonNull;

import java.awt.*;


public class ListCameraCommand extends AbstractPlayerCommand {
    public ListCameraCommand() {
        super("list", "List all available cameras");
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        String s = CameraInitializer.getCameraList();
        CameraTemplates.getAssetMap().forEach(e -> {
            commandContext.sendMessage(Message.raw(e.getCameraSettings().toString()).color(Color.GREEN));
        });

        if (s == null)
            commandContext.sendMessage(Message.raw("You do not have any custom POV"));
        else
            commandContext.sendMessage(Message.raw(s));
    }
}

