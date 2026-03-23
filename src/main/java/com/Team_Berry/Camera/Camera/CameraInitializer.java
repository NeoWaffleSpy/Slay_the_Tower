package com.Team_Berry.Camera.Camera;

import com.Team_Berry.Camera.Camera.MouseControl.DefaultMouseControl;
import com.Team_Berry.Camera.Camera.MouseControl.AbstractMouseControl;
import com.Team_Berry.Camera.Camera.MouseControl.UltMouseControl;
import com.Team_Berry.Camera.CameraPlugin;
import com.Team_Berry.Camera.Component.Data.PlayerPOVComponent;
import com.Team_Berry.Utils.Files.FileUtils;
import com.Team_Berry.Utils.Files.JSONParser;
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
import com.hypixel.hytale.server.core.universe.world.World;
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
        this.mouseControl = null;
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
        ServerCameraSettings cameraSettings = new ServerCameraSettings();
        cameraSettings.eyeOffset = true;
        set("Custom", cameraSettings);
        new CameraInitializer("Custom");
        getCodecSetting();
        new CameraInitializer("TopDown", new DefaultMouseControl(), false, "TopDown");
        /*getJsonSettings("topDown");
        getJsonSettings("sideView");
        getJsonSettings("isometric");
        getJsonSettings("shoulder");
        new CameraInitializer("topDown", new DefaultMouseControl(), false, "topDown");
        new CameraInitializer("isometric2", new DefaultMouseControl(), false, "isometric");
        new CameraInitializer("ultCam", new UltMouseControl(), false, "topDown");*/
    }

    public static void editCameraSettings(PlayerRef playerRef, ServerCameraSettings newSettings) {
        PlayerPOVComponent pPOV = getPOV(playerRef);
        if (pPOV != null) {
            pPOV.setCamSettings(newSettings);
            playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, newSettings));
        }
    }

    public static void editMouseControl(PlayerRef playerRef, AbstractMouseControl newControl) {
        PlayerPOVComponent pPOV = getPOV(playerRef);
        if (pPOV != null)
            pPOV.setMouseControl(newControl);
    }

    public static void set(String key, ServerCameraSettings value) {
        templateDict.put(key, value);
    }
    public static ServerCameraSettings getTemplate (String key) {
        return templateDict.get(key);
    }

    public static void setPlayerPov(String povName, PlayerRef playerRef) {
        CameraInitializer cam = CameraInitializer.get(povName);
        PlayerPOVComponent pPOV = getPOV(playerRef);
        if (pPOV != null)
            CameraInitializer.deletePOV(playerRef);
        playerRef.getReference().getStore().addComponent(playerRef.getReference(), PlayerPOVComponent.getComponentType(), new PlayerPOVComponent(povName));
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

    public static void getCodecSetting() {
        CameraTemplates.getAssetMap().forEach(s -> set(s.getId(), s.getCameraSettings()));
    }

    public static void updateCodecSetting(String key) {
        CameraTemplates.getAssetMap().forEach(s -> {
            if (s.getId().equals(key)) {
                set(s.getId(), s.getCameraSettings());
                Universe.get().getPlayers().forEach((pRef) -> {
                    if (pRef.getWorldUuid() == null)
                        return;
                    Universe.get().getWorld(pRef.getWorldUuid()).execute(() -> {
                        PlayerPOVComponent pPOV = getPOV(pRef);
                        if (pPOV != null) {
                            String componentName = getPOV(pRef).getPOVName();
                            if (componentName.equals(key))
                                CameraInitializer.editCameraSettings(pRef, s.getCameraSettings());
                        }
                    });
                });
            }
            else
                set(s.getId(), s.getCameraSettings());
        });
    }

    public static ServerCameraSettings getJsonSettings(String key) {
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path p = FileUtils.instanceDirCamera(pack, key + ".json");
            if (Files.exists(p)) {
                try {
                    return loadCameraSettings(p, key);
                } catch (IOException | ParseException e) {
                    CameraPlugin.LOGGER.atSevere().log("Failed to load Camera settings " + key, e);
                }
            }
        }
        CameraPlugin.LOGGER.atSevere().log("Could not find matching camera settings " + key);
        return null;
    }

    public void activate() {
        if (!this.isActive) {
            this.eventRegistry.enable();
            this.isActive = true;
            //this.eventRegistry.register(PlayerConnectEvent.class, (event) -> this.setPOV(event.getPlayerRef()));
            this.eventRegistry.register(PlayerMouseButtonEvent.class, this::dispatchControl);
            this.eventRegistry.registerGlobal(PlayerInteractEvent.class, (event) -> { if (mouseControl != null) event.setCancelled(true); });
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
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, pPOV.getCamSettings()));
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

    public static void reload(PlayerRef playerRef) {
        PlayerPOVComponent pPOV = getPOV(playerRef);
        if (pPOV != null)
            CameraInitializer.setPlayerPov(pPOV.getPOVName(), playerRef);
        else
            CameraInitializer.deletePOV(playerRef);
    }

    public static void resetCamera(PlayerRef playerRef) {
        playerRef.getHiddenPlayersManager().showPlayer(playerRef.getUuid());
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.FirstPerson, false, null));
    }

    public void dispatchControl(PlayerMouseButtonEvent event) {
        AbstractMouseControl mouseControl = event.getPlayerRef().getStore().getComponent(event.getPlayerRef(), PlayerPOVComponent.getComponentType()).getMouseControl();
        if (mouseControl != null)
            mouseControl.onPlayerMouseButton(event);
    }

    private static PlayerPOVComponent getPOV(PlayerRef playerRef) {
        PlayerPOVComponent pPOV = null;
        try {
            pPOV = playerRef.getReference().getStore().getComponent(playerRef.getReference(), PlayerPOVComponent.getComponentType());
        } catch (NullPointerException npe) {
            try {
                pPOV = playerRef.getHolder().getComponent(PlayerPOVComponent.getComponentType());
            } catch (NullPointerException npe2) {
                CameraPlugin.LOGGER.atSevere().log("PlayerPOVComponent.getComponentType() is null for " + playerRef.getUsername());
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

    private static ServerCameraSettings loadCameraSettings(Path p, String name) throws IOException, ParseException {
        String json = Files.readString(p);
        Map<String, Object> data = JSONObjectUtils.parse(json);
        ServerCameraSettings camSettings = new ServerCameraSettings();
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

        set(name, camSettings);
        return camSettings;
    }
}
