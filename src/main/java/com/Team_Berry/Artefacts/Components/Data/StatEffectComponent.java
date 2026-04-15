package com.Team_Berry.Artefacts.Components.Data;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.UI.ArtefactHud;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StatEffectComponent implements Component<EntityStore> {
    public Map<ArtefactCodec, Integer> artefactList = new HashMap<>();
    public ArrayList<ArtefactCodec> artefactUpdated = new ArrayList<>();
    public ArtefactHud artefactHud;

    public StatEffectComponent() {
        this.flush();
    }

    public void addArtifact(ArtefactCodec artefact) { addStackToArtifact(artefact, 1); }
    public void removeArtifact(ArtefactCodec artefact) { addStackToArtifact(artefact, -1); }

    public void addStackToArtifact(ArtefactCodec artefact, int amount) {
        if (artefactList.containsKey(artefact))
            artefactList.put(artefact, artefactList.get(artefact) + amount);
        else
            artefactList.put(artefact, amount);
        artefactUpdated.add(artefact);
        if (artefactHud != null)
            artefactHud.refresh();
    }

    public void setStackArtefact(ArtefactCodec artefact, int amount) {
            artefactList.put(artefact, amount);
        artefactUpdated.add(artefact);
        if (artefactHud != null)
            artefactHud.refresh();
    }

    public void flush() {
        ArtefactCodec.getAssetMap().getAssetMap().forEach((assetName, asset) -> artefactList.put(asset, 0));
        artefactUpdated.addAll(artefactList.keySet());
        if (artefactHud != null)
            artefactHud.refresh();
    }

    public int getAmount(ArtefactCodec artefact) {
        return artefactList.get(artefact);
    }

    public static @NonNull ComponentType<EntityStore, StatEffectComponent> getComponentType() {
        return ArtefactPlugin.get().getStatEffectComponentType();
    }

    public static StatEffectComponent getPlayerStatComp(PlayerRef player) {
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = ref.getStore();
        StatEffectComponent statComp = store.getComponent(ref, StatEffectComponent.getComponentType());
        if (statComp == null) {
            store.addComponent(ref, StatEffectComponent.getComponentType(), new StatEffectComponent());
            statComp = store.getComponent(ref, StatEffectComponent.getComponentType());
        }
        return statComp;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return null;
    }

}
