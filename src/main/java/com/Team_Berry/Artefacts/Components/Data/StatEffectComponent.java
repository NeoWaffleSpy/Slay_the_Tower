package com.Team_Berry.Artefacts.Components.Data;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;

import java.util.ArrayList;

public class StatEffectComponent {
    public ArrayList<ArtefactCodec> artefactList = new ArrayList<>();

    public StatEffectComponent() {
        artefactList.add(new ArtefactCodec("Attack"));
    }

    public ArrayList<ArtefactCodec> getArtefactList() { return artefactList; }
    public void addStackToArtifact(String name) { addStackToArtifact(name, 1); }

    public void addStackToArtifact(String name, int amount) {
        artefactList.forEach(artefact -> {
            return;
        });
    }
}
