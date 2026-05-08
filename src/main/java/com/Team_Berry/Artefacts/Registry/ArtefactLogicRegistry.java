package com.Team_Berry.Artefacts.Registry;

import com.Team_Berry.Artefacts.Artefact.IArtefactLogic;
import com.Team_Berry.Artefacts.Artefact.ShieldArtefact;

import java.util.HashMap;
import java.util.Map;

public class ArtefactLogicRegistry {
    private static final Map<String, IArtefactLogic> logicMap = new HashMap<>();

    public static void registerAll() {
        register("Shield_Artefact", new ShieldArtefact());
    }

    private static void register(String assetId, IArtefactLogic logic) {
        logicMap.put(assetId, logic);
    }

    public static IArtefactLogic getLogic(String assetId) {
        return logicMap.get(assetId);
    }
}