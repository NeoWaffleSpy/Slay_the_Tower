package com.Team_Berry.Slay.Commands.Camera;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CameraCommand extends AbstractCommandCollection {
    public CameraCommand() {
        super("camera", "See and edit camera settings of the player");
        this.addAliases("cameraCommand", "cameracommand");
        this.setPermissionGroup(GameMode.Creative);
        this.addSubCommand(new GetCameraCommand());
        this.addSubCommand(new SetCameraCommand());
        this.addSubCommand(new ResetCameraCommand());
        this.addSubCommand(new ListCameraCommand());
        this.addSubCommand(new CustomCameraCommand());
        this.addSubCommand(new ExportCameraCommand());
    }
}