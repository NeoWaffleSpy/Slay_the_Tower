package com.Varrell.gamemodeAPI.Camera;

import com.Varrell.gamemodeAPI.Camera.MouseControl.DefaultMouseControl;
import com.Varrell.gamemodeAPI.Camera.MouseControl.AbstractMouseControl;
import com.Varrell.gamemodeAPI.Component.Data.PlayerPOVComponent;
import com.Varrell.gamemodeAPI.GamemodeAPI;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.nimbusds.jose.util.JSONObjectUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CameraInitializer {
    private static Dictionary<String, CameraInitializer> camDict = new Hashtable<>();
    private static Dictionary<String, ServerCameraSettings> templateDict = new Hashtable<>();
    private final EventRegistry eventRegistry = new EventRegistry(new CopyOnWriteArrayList<>(), () -> this.isActive, "CameraDemo is not active!", HytaleServer.get().getEventBus());
    private final ServerCameraSettings cameraSettings;
    private final AbstractMouseControl mouseControl;
    public String cameraName;
    public boolean isActive;
    public boolean isPlayerHidden;

    public CameraInitializer(String cameraName) {
        this.cameraSettings = templateDict.get(cameraName);
        this.mouseControl = new DefaultMouseControl();
        this.isPlayerHidden = false;
        camDict.put(cameraName, this);
        activate();
    }

    public CameraInitializer(String cameraName, AbstractMouseControl mouseControl, boolean isPlayerHidden, String templateName) {
        this.cameraSettings = templateDict.get(templateName);
        this.mouseControl = mouseControl;
        this.isPlayerHidden = isPlayerHidden;
        this.cameraName = cameraName;
        camDict.put(cameraName, this);
        activate();
    }

    public static void init() {
        set("custom", new ServerCameraSettings());
        new CameraInitializer("custom");
        get("topDown");
        get("sideView");
        get("isometric");
        get("shoulder");
        new CameraInitializer("isometric2", new DefaultMouseControl(), false, "isometric");
    }

    public static void editCameraSettings(PlayerRef playerRef, ServerCameraSettings newSettings) {
        PlayerPOVComponent pPOV = getPOV(playerRef);
        if (pPOV != null) {
            pPOV.setCamSettings(newSettings);
            playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, newSettings));
        }
    }

    public static void editMouseControl(PlayerRef playerRef, AbstractMouseControl newControl) {
        PlayerPOVComponent pPOV = getPOV(playerRef);
        if (pPOV != null)
            pPOV.setMouseControl(newControl);
    }

    public static void set (String key, ServerCameraSettings value) {
        templateDict.put(key, value);
    }

    public static CameraInitializer get(String key) {
        CameraInitializer cam =  camDict.get(key);
        if (cam == null) {
            getJsonSettings(key);
            if (templateDict.get(key) == null)
                return null;
            cam = new CameraInitializer(key);
        }
        return cam;
    }

    public static ServerCameraSettings getJsonSettings(String key) {
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path p = instanceDir(pack, key + ".json");
            if (Files.exists(p)) {
                try {
                    return loadCameraSettings(p, key);
                } catch (IOException | ParseException e) {
                    GamemodeAPI.LOGGER.atSevere().log("Failed to load Camera settings " + key, e);
                }
            }
        }
        GamemodeAPI.LOGGER.atSevere().log("Could not find matching camera settings " + key);
        return null;
    }

    public void activate() {
        if (!this.isActive) {
            this.eventRegistry.enable();
            this.isActive = true;
            //this.eventRegistry.register(PlayerConnectEvent.class, (event) -> this.setPOV(event.getPlayerRef()));
            this.eventRegistry.register(PlayerMouseButtonEvent.class, this::dispatchControl);
            this.eventRegistry.registerGlobal(PlayerInteractEvent.class, (event) -> event.setCancelled(true));
            Universe.get().getPlayers().forEach((pRef) -> {
                PlayerPOVComponent pPOV = getPOV(pRef);
                if (pPOV != null) {
                    String componentName = getPOV(pRef).getPOVName();
                    if (componentName.equals(cameraName))
                        this.setPOV(pRef);
                }
            });
        }
    }

    public void setPOV(@Nonnull PlayerRef playerRef) {
        if (!isActive)
            return;
        if (isPlayerHidden)
            playerRef.getHiddenPlayersManager().hidePlayer(playerRef.getUuid());
        else
            playerRef.getHiddenPlayersManager().showPlayer(playerRef.getUuid());
        PlayerPOVComponent pPOV = getPOV(playerRef);
        if (pPOV == null)
            return;
        if (pPOV.getMouseControl() == null)
            pPOV.setMouseControl(mouseControl);
        if (pPOV.getCamSettings() == null)
            pPOV.setCamSettings(cameraSettings);
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, pPOV.getCamSettings()));
    }

    public void deactivate() {
        if (this.isActive) {
            this.eventRegistry.shutdown();
            Universe.get().getPlayers().forEach((PlayerRef playerRef) -> {
                PlayerPOVComponent pPOV = getPOV(playerRef);
                if (pPOV != null && Objects.equals(pPOV.getPOVName(), cameraName)) {
                    CameraInitializer.resetCamera(playerRef);
                }
            });
            this.isActive = false;
        }
    }

    public static void resetCamera(PlayerRef playerRef) {
        playerRef.getHiddenPlayersManager().showPlayer(playerRef.getUuid());
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
    }

    public void dispatchControl(PlayerMouseButtonEvent event) {
        event.getPlayerRef().getStore().getComponent(event.getPlayerRef(), PlayerPOVComponent.getComponentType()).getMouseControl().onPlayerMouseButton(event);
    }

    private static PlayerPOVComponent getPOV(PlayerRef playerRef) {
        PlayerPOVComponent pPOV = null;
        try {
            pPOV = playerRef.getReference().getStore().getComponent(playerRef.getReference(), PlayerPOVComponent.getComponentType());
        } catch (NullPointerException npe) {
            try {
                pPOV = playerRef.getHolder().getComponent(PlayerPOVComponent.getComponentType());
            } catch (NullPointerException npe2) {
                GamemodeAPI.LOGGER.atSevere().log("PlayerPOVComponent.getComponentType() is null for " + playerRef.getUsername());
                return null;
            }
        }
        return pPOV;
    }

    public static void deletePOV(PlayerRef playerRef) {
        try {
            playerRef.getReference().getStore().removeComponent(playerRef.getReference(), PlayerPOVComponent.getComponentType());
        } catch (NullPointerException npe) {
            try {
                playerRef.getHolder().removeComponent(PlayerPOVComponent.getComponentType());
            } catch (NullPointerException npe2) {
                return;
            }
        }
    }

    public static String getCameraList() {
        if (camDict.isEmpty())
            return null;
        return String.join(", ", Collections.list(camDict.keys()));
    }

    private static Path instanceDir(AssetPack pack, String instanceName) {
        return pack.getRoot().resolve("Server").resolve("CameraSettings").resolve(instanceName);
    }

    private static ServerCameraSettings loadCameraSettings(Path p, String name) throws IOException, ParseException {
        String json = Files.readString(p);
        Map<String, Object> data = JSONObjectUtils.parse(json);
        ServerCameraSettings camSettings = new ServerCameraSettings();
        camSettings.positionLerpSpeed = get(data.get("positionLerpSpeed"));
        camSettings.rotationLerpSpeed = get(data.get("rotationLerpSpeed"));
        camSettings.distance = get(data.get("distance"));
        camSettings.speedModifier = get(data.get("speedModifier"));
        camSettings.allowPitchControls = (boolean) data.get("allowPitchControls");
        camSettings.displayCursor = (boolean) data.get("displayCursor");
        camSettings.displayReticle = (boolean) data.get("displayReticle");
        camSettings.mouseInputTargetType = MouseInputTargetType.valueOf((String) data.get("mouseInputTargetType"));
        camSettings.sendMouseMotion = (boolean) data.get("sendMouseMotion");
        camSettings.skipCharacterPhysics = (boolean) data.get("skipCharacterPhysics");
        camSettings.isFirstPerson = (boolean) data.get("isFirstPerson");
        camSettings.movementForceRotationType = MovementForceRotationType.valueOf((String) data.get("movementForceRotationType"));
        camSettings.movementForceRotation = getDirection(data.get("movementForceRotation"));
        camSettings.attachedToType = AttachedToType.valueOf((String) data.get("attachedToType"));
        camSettings.attachedToEntityId = getInt(data.get("attachedToEntityId"));
        camSettings.eyeOffset = (boolean) data.get("eyeOffset");
        camSettings.positionDistanceOffsetType = PositionDistanceOffsetType.valueOf((String) data.get("positionDistanceOffsetType"));
        camSettings.positionOffset = getPosition(data.get("positionOffset"));
        camSettings.rotationOffset = getDirection(data.get("rotationOffset"));
        camSettings.positionType = PositionType.valueOf((String) data.get("positionType"));
        camSettings.position = getPosition(data.get("position"));
        camSettings.rotationType = RotationType.valueOf((String) data.get("rotationType"));
        camSettings.rotation = getDirection(data.get("rotation"));
        camSettings.canMoveType = CanMoveType.valueOf((String) data.get("canMoveType"));
        camSettings.applyMovementType = ApplyMovementType.valueOf((String) data.get("applyMovementType"));
        camSettings.movementMultiplier = getVector(data.get("movementMultiplier"));
        camSettings.applyLookType = ApplyLookType.valueOf((String) data.get("applyLookType"));
        camSettings.lookMultiplier = getVector2(data.get("lookMultiplier"));
        camSettings.mouseInputType = MouseInputType.valueOf((String) data.get("mouseInputType"));
        camSettings.planeNormal = getVector(data.get("planeNormal"));

        set(name, camSettings);
        return camSettings;
    }

    private static Vector2f getVector2(Object data) {
        Map<String, Object> myData = (Map<String, Object>) data;
        return new Vector2f(get(myData.get("x")), get(myData.get("y")));
    }

    private static Vector3f getVector(Object data) {
        Map<String, Object> myData = (Map<String, Object>) data;
        return new Vector3f(get(myData.get("x")), get(myData.get("y")), get(myData.get("z")));
    }

    private static Position getPosition(Object data) {
        Map<String, Object> myData = (Map<String, Object>) data;
        return new Position(get(myData.get("x")), get(myData.get("y")), get(myData.get("z")));
    }

    private static Direction getDirection(Object data) {
        Map<String, Object> myData = (Map<String, Object>) data;
        return new Direction(get(myData.get("yaw")), get(myData.get("pitch")), get(myData.get("roll")));
    }

    private static float get(Object n) {
        return ((Number) n).floatValue();
    }

    private static Integer getInt(Object n) {
        return ((Number) n).intValue();
    }
}
