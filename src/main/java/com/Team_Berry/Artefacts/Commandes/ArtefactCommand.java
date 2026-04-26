package com.Team_Berry.Artefacts.Commandes;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class ArtefactCommand extends AbstractCommandCollection {
    public ArtefactCommand() {
        super("Artefact", "Artefact command collection");
        addSubCommand(new GiveArtefactCommand());
        addSubCommand(new ShowArtefactSelectionCommand());
        addSubCommand(new ShowSkillSelectionCommand());
        addSubCommand(new ShowWeaponSelectionCommand());
        addSubCommand(new ClearArtefactsCommand());
    }
}
