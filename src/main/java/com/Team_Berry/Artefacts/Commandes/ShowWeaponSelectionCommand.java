package com.Team_Berry.Artefacts.Commandes;

import com.Team_Berry.Artefacts.UI.SkillSelection;
import com.Team_Berry.Artefacts.UI.WeaponSelection;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public class ShowWeaponSelectionCommand extends AbstractPlayerCommand {
    public ShowWeaponSelectionCommand() {
        super("weaponSelect", "Show the Skill Selection page");
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        new WeaponSelection(playerRef, store).buildPage();
    }
}
