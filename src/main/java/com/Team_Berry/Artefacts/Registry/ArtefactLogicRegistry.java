package com.Team_Berry.Artefacts.Registry;

import com.Team_Berry.Artefacts.ArtefactLogic.*;
import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Interfaces.IArtefactLogic;

import java.util.EnumMap;
import java.util.Map;

public class ArtefactLogicRegistry {
    private static final Map<ArtefactLogicEnum, IArtefactLogic> logicMap = new EnumMap<>(ArtefactLogicEnum.class);

    public static void registerAll() {
        register(ArtefactLogicEnum.SHIELD, new ShieldArtefact());
        register(ArtefactLogicEnum.REFLECT, new ProjectileReflectArtefact());
        register(ArtefactLogicEnum.EXPLOSION_ON_KILL, new ExplosionOnKillArtefact());
        register(ArtefactLogicEnum.SPEED_ON_KILL, new SpeedOnKillArtefact());
        register(ArtefactLogicEnum.OVERHEAT, new OverheatArtefact());
        register(ArtefactLogicEnum.BLEED, new BleedArtefact());
        register(ArtefactLogicEnum.HOMING_MISSILE, new HomingMissileArtefact());
        register(ArtefactLogicEnum.STAMINA, new StaminaArtefact());
        register(ArtefactLogicEnum.SHOCK_ABSORBER, new ShockAbsorberArtefact());


    }

    private static void register(ArtefactLogicEnum logicId, IArtefactLogic logic) {
        logicMap.put(logicId, logic);
    }

    public static IArtefactLogic getLogic(ArtefactCodec codec) {
        if (codec.logicId != null && codec.logicId != ArtefactLogicEnum.NONE) {
            return logicMap.get(codec.logicId);
        }
        return null;
    }
}