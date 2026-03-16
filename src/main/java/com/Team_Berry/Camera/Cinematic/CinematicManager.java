package com.Team_Berry.Camera.Cinematic;

import com.Team_Berry.Camera.Camera.CameraInitializer;
import com.Team_Berry.Camera.CameraPlugin;
import com.Team_Berry.Utils.Files.FileUtils;
import com.Team_Berry.Utils.Files.JSONParser;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.nimbusds.jose.util.JSONObjectUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CinematicManager {
    private static final Map<String, CinematicPlayer> cinemaList = new HashMap<>();

    public CinematicManager() {}

    public static CinematicPlayer getCinematic(String name) {
        return cinemaList.get(name);
    }

    public static CinematicPlayer createCinematic(String name, Position origin) {
        CinematicPlayer cinematicPlayer = new CinematicPlayer(origin);
        cinemaList.put(name, cinematicPlayer);
        return cinematicPlayer;
    }

    private static CinematicPlayer loadCinemaSettings(Path p, String name) throws IOException, ParseException {
        String json = Files.readString(p);
        Map<String, Object> data = JSONObjectUtils.parse(json);
        Position origin = JSONParser.getPosition(data.get("origin"));
        CinematicPlayer cp = new CinematicPlayer(origin);
        ArrayList<Map<String, Object>> timeline = (ArrayList<Map<String, Object>>) data.get("timeline");
        timeline.forEach(dataPoint -> cp.addCameraPoint(getPoint(dataPoint)));
        cinemaList.put(name, cp);
        return cp;
    }

    private static CinemaPoint getPoint(Map<String, Object> data) {
        CinemaPoint camSettings = new CinemaPoint();
        camSettings.transitionTime = JSONParser.getLong(data.get("transitionTime"));
        camSettings.positionLerpSpeed = JSONParser.getFloat(data.get("positionLerpSpeed"));
        camSettings.rotationLerpSpeed = JSONParser.getFloat(data.get("rotationLerpSpeed"));
        camSettings.distance = JSONParser.getFloat(data.get("distance"));
        camSettings.speedModifier = JSONParser.getFloat(data.get("speedModifier"));
        camSettings.allowPitchControls = (boolean) data.get("allowPitchControls");
        camSettings.displayCursor = (boolean) data.get("displayCursor");
        camSettings.displayReticle = (boolean) data.get("displayReticle");
        camSettings.mouseInputTargetType = MouseInputTargetType.valueOf((String) data.get("mouseInputTargetType"));
        camSettings.sendMouseMotion = (boolean) data.get("sendMouseMotion");
        camSettings.skipCharacterPhysics = (boolean) data.get("skipCharacterPhysics");
        camSettings.isFirstPerson = (boolean) data.get("isFirstPerson");
        camSettings.movementForceRotationType = MovementForceRotationType.valueOf((String) data.get("movementForceRotationType"));
        camSettings.movementForceRotation = JSONParser.getDirection(data.get("movementForceRotation"));
        camSettings.attachedToType = AttachedToType.valueOf((String) data.get("attachedToType"));
        camSettings.attachedToEntityId = JSONParser.getInt(data.get("attachedToEntityId"));
        camSettings.eyeOffset = (boolean) data.get("eyeOffset");
        camSettings.positionDistanceOffsetType = PositionDistanceOffsetType.valueOf((String) data.get("positionDistanceOffsetType"));
        camSettings.positionOffset = JSONParser.getPosition(data.get("positionOffset"));
        camSettings.rotationOffset = JSONParser.getDirection(data.get("rotationOffset"));
        camSettings.positionType = PositionType.valueOf((String) data.get("positionType"));
        camSettings.position = JSONParser.getPosition(data.get("position"));
        camSettings.rotationType = RotationType.valueOf((String) data.get("rotationType"));
        camSettings.rotation = JSONParser.getDirection(data.get("rotation"));
        camSettings.canMoveType = CanMoveType.valueOf((String) data.get("canMoveType"));
        camSettings.applyMovementType = ApplyMovementType.valueOf((String) data.get("applyMovementType"));
        camSettings.movementMultiplier = JSONParser.getVector(data.get("movementMultiplier"));
        camSettings.applyLookType = ApplyLookType.valueOf((String) data.get("applyLookType"));
        camSettings.lookMultiplier = JSONParser.getVector2(data.get("lookMultiplier"));
        camSettings.mouseInputType = MouseInputType.valueOf((String) data.get("mouseInputType"));
        camSettings.planeNormal = JSONParser.getVector(data.get("planeNormal"));

        return camSettings;
    }

    public static CinematicPlayer getJsonSettings(String key) {
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path p = FileUtils.instanceDirCinema(pack, key + ".json");
            if (Files.exists(p)) {
                try {
                    return loadCinemaSettings(p, key);
                } catch (IOException | ParseException e) {
                    CameraPlugin.LOGGER.atSevere().log("Failed to load Cinema settings " + key, e);
                }
            }
        }
        CameraPlugin.LOGGER.atSevere().log("Could not find matching Cinema settings " + key);
        return null;
    }

    public static CinematicPlayer get(String key) {
        CinematicPlayer cam = cinemaList.get(key);
        if (cam == null)
            cam = getJsonSettings(key);
        return cam;
    }
}
