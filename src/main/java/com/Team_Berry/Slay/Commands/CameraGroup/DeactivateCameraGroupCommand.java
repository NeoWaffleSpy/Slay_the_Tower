package com.Team_Berry.Slay.Commands.CameraGroup;

import com.Team_Berry.Slay.Camera.CameraInitializer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

public class DeactivateCameraGroupCommand extends CommandBase {
    @Nonnull
    private final RequiredArg<String> nameArg;
    public DeactivateCameraGroupCommand() {
        super("deactivate", "server.commands.worlds.desc");
        this.addAliases("d");
        this.nameArg = this.withRequiredArg("cameraName", "The camera name already set in the system", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@NonNull CommandContext commandContext) {
        String name = (String)commandContext.get(this.nameArg);
        CameraInitializer cam = CameraInitializer.get(name);
        if (cam == null) {
            commandContext.sendMessage(Message.raw(name + " is not a valid POV"));
            return;
        }
        if (!cam.isActive) {
            commandContext.sendMessage(Message.raw("POV " + name + " is already deactivated"));
            return;
        }
        cam.deactivate();
    }
}
