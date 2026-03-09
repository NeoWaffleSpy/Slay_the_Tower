package com.Team_Berry.GearAffix.GearAffixes;

import com.hypixel.hytale.server.core.Message;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGearAffix {
    protected Map<GearAffixesEnum, Float> affixList = new HashMap<>();
    protected Map<String, String> effectList = new HashMap<>();

    public void setAffix(GearAffixesEnum key, float value) {
        affixList.put(key, value);
    }

    public void addAffix(GearAffixesEnum key, float value) {
        float newVal = value;
        if (!affixList.containsKey(key)) {
            newVal += affixList.get(key);
        }
        affixList.put(key, newVal);
    }

    public void removeAffix(GearAffixesEnum key) {
        affixList.remove(key);
    }

    public Map<GearAffixesEnum, Float> getAffixList() {
        return affixList;
    }

    public String getAffixString() {
        StringBuilder sb = new StringBuilder();
        for (GearAffixesEnum key : affixList.keySet()) {
            sb.append(
                    Message.translation("GearAffix.affix." + key))
                    .append(" = ")
                    .append(affixList.get(key) < 0 ? "" : "+")
                    .append(affixList.get(key).toString())
                    .append(key == GearAffixesEnum.PERCENT_ARMOR ? "%" : "")
                    .append("\n");
        }

        return sb.toString();
    }

    public void setEffects(String effectName, String effects) {
        effectList.put(effectName, effects);
    }

    public void removeEffects(String effectName) {
        effectList.remove(effectName);
    }

    public Map<String, String> getEffectList() {
        return effectList;
    }

    public String getEffectString() {
        StringBuilder sb = new StringBuilder();
        for (String key : effectList.keySet()) {
            sb.append(key).append(" = ").append(effectList.get(key)).append("\n");
        }
        return sb.toString();
    }
}
