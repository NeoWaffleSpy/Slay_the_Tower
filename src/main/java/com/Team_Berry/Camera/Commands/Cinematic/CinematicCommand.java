package com.Team_Berry.Camera.Commands.Cinematic;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CinematicCommand extends AbstractCommandCollection {
    public CinematicCommand() {
        super("cinematic", "Command collection for cinematic creation");
        addSubCommand(new CreateCinematicCommand());
        addSubCommand(new AddPointToCinematicCommand());
        addSubCommand(new RunCinematicCommand());
        addSubCommand(new EditCinemaKeyframeCommand());
        addSubCommand(new ExportCinematic());
        addSubCommand(new PeekKeyframeCommand());
        addSubCommand(new StopCinematicCommand());
        addSubCommand(new DeletePointToCinematicCommand());
    }
}
