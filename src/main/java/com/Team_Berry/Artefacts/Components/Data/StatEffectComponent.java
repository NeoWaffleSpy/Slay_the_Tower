package com.Team_Berry.Artefacts.Components.Data;

import com.Team_Berry.Artefacts.Items.ArtefactTemplate;
import com.Team_Berry.Artefacts.Items.AttackArtefact;

import java.util.ArrayList;

public class StatEffectComponent {
    public ArrayList<ArtefactTemplate> artefactList = new ArrayList<>();

    public StatEffectComponent() {
        artefactList.add(new AttackArtefact("Attack"));
    }

    public ArrayList<ArtefactTemplate> getArtefactList() { return artefactList; }
    public void addStackToArtifact(String name) { addStackToArtifact(name, 1); }

    public void addStackToArtifact(String name, int amount) {
        artefactList.forEach(artefact -> {
            if (artefact.ArtefactName == name)
                artefact.addStackToArtifact(amount);
        });
    }
}
