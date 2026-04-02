package com.Team_Berry.Artefacts.Components.Data;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Camera.CameraPlugin;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StatEffectComponent implements Component<EntityStore> {
    public Map<ArtefactCodec, Integer> artefactList = new HashMap<>();
    public ArrayList<ArtefactCodec> artefactUpdated = new ArrayList<>();

    public StatEffectComponent() {}

    public Map<ArtefactCodec, Integer> getArtefactList() { return artefactList; }
    public void addArtifact(ArtefactCodec artefact) { addStackToArtifact(artefact, 1); }
    public void removeArtifact(ArtefactCodec artefact) { addStackToArtifact(artefact, -1); }

    public void addStackToArtifact(ArtefactCodec artefact, int amount) {
        if (artefactList.containsKey(artefact))
            artefactList.put(artefact, artefactList.get(artefact) + amount);
        else
            artefactList.put(artefact, amount);
        artefactUpdated.add(artefact);
    }

    public void setStackArtefact(ArtefactCodec artefact, int amount) {
            artefactList.put(artefact, amount);
        artefactUpdated.add(artefact);
    }

    public void flush() {
        artefactUpdated.addAll(artefactList.keySet());
        artefactList.clear();
    }

    public int getAmount(ArtefactCodec artefact) {
        return artefactList.get(artefact);
    }

    public static @NonNull ComponentType<EntityStore, StatEffectComponent> getComponentType() {
        return ArtefactPlugin.get().getStatEffectComponentType();
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return null;
    }

}
