package com.Varrell.gamemodeAPI.Commands.CameraGroup;

import com.Varrell.gamemodeAPI.Commands.Camera.GetCameraCommand;
import com.Varrell.gamemodeAPI.Commands.Camera.ResetCameraCommand;
import com.Varrell.gamemodeAPI.Commands.Camera.SetCameraCommand;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CameraGroupCommand extends AbstractCommandCollection {
    public CameraGroupCommand() {
        super("cameraGroup", "activate and deactivate entire camera groups");
        this.addAliases("cameraGroupCommand", "cameragroupcommand");
        this.setPermissionGroup(GameMode.Creative);
        this.addSubCommand(new ActivateCameraGroupCommand());
        this.addSubCommand(new DeactivateCameraGroupCommand());
    }
}