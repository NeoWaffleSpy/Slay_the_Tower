package com.Team_Berry.Artefacts.Components;

import com.Team_Berry.Artefacts.ArtefactPlugin;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Interfaces.IArtefactLogic;
import com.Team_Berry.Artefacts.Registry.ArtefactLogicRegistry;
import com.Team_Berry.Artefacts.UI.ArtefactHud;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class StatEffectComponent implements Component<EntityStore> {
    public Map<ArtefactCodec, Integer> artefactList = new HashMap<>();
    public ArrayList<ArtefactCodec> artefactUpdated = new ArrayList<>();
    public ArtefactHud artefactHud;
    public Map<ArtefactCodec, Long> artefactCooldowns = new HashMap<>();
    public Map<ArtefactCodec, Long> lastNotifiedReady = new HashMap<>();
    public Map<String, Object> customArtefactData = new HashMap<>();

    public Map<Class<? extends IArtefactLogic>, List<ArtefactCodec>> categorizedArtefacts = new HashMap<>();

    public StatEffectComponent() {
        this.flush();
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

    public void addArtifact(ArtefactCodec artefact) {
        addStackToArtifact(artefact, 1);
    }

    public void removeArtifact(ArtefactCodec artefact) {
        addStackToArtifact(artefact, -1);
    }

    public void addStackToArtifact(ArtefactCodec artefact, int amount) {
        int currentAmount = artefactList.getOrDefault(artefact, 0);
        int newAmount = currentAmount + amount;

        if (currentAmount == 0 && newAmount > 0) {
            categorizeArtefact(artefact);
        } else if (currentAmount > 0 && newAmount <= 0) {
            uncategorizeArtefact(artefact);
        }

        artefactList.put(artefact, newAmount);
        artefactUpdated.add(artefact);
        if (artefactHud != null)
            artefactHud.refresh();
    }

    public void setStackArtefact(ArtefactCodec artefact, int amount) {
        int currentAmount = artefactList.getOrDefault(artefact, 0);

        if (currentAmount == 0 && amount > 0) {
            categorizeArtefact(artefact);
        } else if (currentAmount > 0 && amount <= 0) {
            uncategorizeArtefact(artefact);
        }

        artefactList.put(artefact, amount);
        artefactUpdated.add(artefact);
        if (artefactHud != null)
            artefactHud.refresh();
    }

    public void flush() {
        ArtefactCodec.getAssetMap().getAssetMap().forEach((assetName, asset) -> artefactList.put(asset, 0));
        categorizedArtefacts.clear();
        artefactUpdated.addAll(artefactList.keySet());
        if (artefactHud != null)
            artefactHud.refresh();
        customArtefactData.clear();
    }

    public int getAmount(ArtefactCodec artefact) {
        return artefactList.get(artefact);
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return null;
    }

    public <T extends IArtefactLogic> void triggerLogic(Class<T> logicClass, BiConsumer<ArtefactCodec, T> action) {
        List<ArtefactCodec> relevantArtefacts = this.categorizedArtefacts.get(logicClass);

        if (relevantArtefacts == null || relevantArtefacts.isEmpty()) {
            return;
        }

        for (ArtefactCodec artefact : relevantArtefacts) {
            IArtefactLogic logic = ArtefactLogicRegistry.getLogic(artefact);
            action.accept(artefact, logicClass.cast(logic));
        }
    }

    private void categorizeArtefact(ArtefactCodec artefact) {
        IArtefactLogic logic = ArtefactLogicRegistry.getLogic(artefact);
        if (logic == null) return;

        for (Class<?> iface : logic.getClass().getInterfaces()) {
            if (IArtefactLogic.class.isAssignableFrom(iface)) {
                Class<? extends IArtefactLogic> logicClass = (Class<? extends IArtefactLogic>) iface;
                categorizedArtefacts.computeIfAbsent(logicClass, k -> new ArrayList<>()).add(artefact);
            }
        }
    }

    private void uncategorizeArtefact(ArtefactCodec artefact) {
        IArtefactLogic logic = ArtefactLogicRegistry.getLogic(artefact);
        if (logic == null) return;

        for (Class<?> iface : logic.getClass().getInterfaces()) {
            if (IArtefactLogic.class.isAssignableFrom(iface)) {
                List<ArtefactCodec> list = categorizedArtefacts.get(iface);
                if (list != null) {
                    list.remove(artefact);
                }
            }
        }
    }


}
