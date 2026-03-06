package com.Team_Berry.Slay.Commands.Camera;

import com.Team_Berry.Slay.Camera.CameraInitializer;
import com.Team_Berry.Slay.Component.Data.PlayerPOVComponent;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ExportCameraCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> name;
    private final RequiredArg<String> packName;
    private final OptionalArg<Boolean> force;
    public ExportCameraCommand() {
        super("export", "Export your current camera settings as JSON");
        this.name = this.withRequiredArg("name", "Your camera name", ArgTypes.STRING);
        this.packName = this.withRequiredArg("name", "The name of you pack", ArgTypes.STRING);
        this.force = this.withOptionalArg("force", "Your camera name", ArgTypes.BOOLEAN);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        PlayerPOVComponent pPOV = store.getComponent(ref, PlayerPOVComponent.getComponentType());
        if (pPOV == null) {
            commandContext.sendMessage(Message.raw("You do not have any custom POV applied"));
            return;
        }
        ServerCameraSettings settings = pPOV.getCamSettings();
        String json = parse(settings);
        String name = this.name.get(commandContext);
        AssetPack mypack = AssetModule.get().getAssetPack(this.packName.get(commandContext));
        if  (mypack == null) {
            commandContext.sendMessage(Message.raw("Invalid Pack Name"));
            return;
        }
        Path path = instanceDir(mypack, name + ".json");
        if (!Files.exists(path.getParent(), new LinkOption[0])) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                commandContext.sendMessage(Message.raw("Error while creating directory: " + e.getMessage()));
                return;
            }
        }
        if (Files.exists(path, new LinkOption[0]) && !this.force.get(commandContext)) {
            commandContext.sendMessage(Message.raw("A POV with this name already exists, use <--force true> to overwrite"));
            return;
        }
        try {
            Files.deleteIfExists(path);
            Files.writeString(path, json);
        } catch (IOException e) {
            commandContext.sendMessage(Message.raw("Error while creating configuration: " + e.getMessage()));
        }
        CameraInitializer.set(name, settings);
        new CameraInitializer(name);
    }

    private Path instanceDir(AssetPack pack, String instanceName) {
        return pack.getRoot().resolve("Server").resolve("CameraSettings").resolve(instanceName);
    }

    private String parse(ServerCameraSettings settings) {
        Map<String, Object> data = new HashMap<>();
        data.put("positionLerpSpeed", settings.positionLerpSpeed);
        data.put("rotationLerpSpeed", settings.rotationLerpSpeed);
        data.put("distance", settings.distance);
        data.put("speedModifier", settings.speedModifier);
        data.put("allowPitchControls", settings.allowPitchControls);
        data.put("displayCursor", settings.displayCursor);
        data.put("displayReticle", settings.displayReticle);
        data.put("mouseInputTargetType", settings.mouseInputTargetType.toString());
        data.put("sendMouseMotion", settings.sendMouseMotion);
        data.put("skipCharacterPhysics", settings.skipCharacterPhysics);
        data.put("isFirstPerson", settings.isFirstPerson);
        data.put("movementForceRotationType", settings.movementForceRotationType.toString());

        Map<String, Object> movementForceRotation = new HashMap<>();
        movementForceRotation.put("pitch", settings.movementForceRotation != null ? settings.movementForceRotation.pitch : 0);
        movementForceRotation.put("yaw", settings.movementForceRotation != null ? settings.movementForceRotation.yaw : 0);
        movementForceRotation.put("roll", settings.movementForceRotation != null ? settings.movementForceRotation.roll : 0);
        data.put("movementForceRotation", movementForceRotation);

        data.put("attachedToType", settings.attachedToType.toString());
        data.put("attachedToEntityId", settings.attachedToEntityId);
        data.put("eyeOffset", settings.eyeOffset);
        data.put("positionDistanceOffsetType", settings.positionDistanceOffsetType.toString());

        Map<String, Object> positionOffset = new HashMap<>();
        positionOffset.put("x", settings.positionOffset != null ? settings.positionOffset.x : 0);
        positionOffset.put("y", settings.positionOffset != null ? settings.positionOffset.y : 0);
        positionOffset.put("z", settings.positionOffset != null ? settings.positionOffset.z : 0);
        data.put("positionOffset", positionOffset);

        Map<String, Object> rotationOffset = new HashMap<>();
        rotationOffset.put("pitch", settings.rotationOffset != null ? settings.rotationOffset.pitch : 0);
        rotationOffset.put("yaw", settings.rotationOffset != null ? settings.rotationOffset.yaw : 0);
        rotationOffset.put("roll", settings.rotationOffset != null ? settings.rotationOffset.roll : 0);
        data.put("rotationOffset", rotationOffset);

        data.put("positionType", settings.positionType.toString());
        Map<String, Object> position = new HashMap<>();
        position.put("x", settings.position != null ? settings.position.x : 0);
        position.put("y", settings.position != null ? settings.position.y : 0);
        position.put("z", settings.position != null ? settings.position.z : 0);
        data.put("position", position);

        data.put("rotationType", settings.rotationType.toString());
        Map<String, Object> rotation = new HashMap<>();
        rotation.put("pitch", settings.rotation != null ? settings.rotation.pitch : 0);
        rotation.put("yaw", settings.rotation != null ? settings.rotation.yaw : 0);
        rotation.put("roll", settings.rotation != null ? settings.rotation.roll : 0);
        data.put("rotation", rotation);

        data.put("canMoveType", settings.canMoveType.toString());
        data.put("applyMovementType", settings.applyMovementType.toString());

        Map<String, Object> movementMultiplier = new HashMap<>();
        movementMultiplier.put("x", settings.movementMultiplier != null ? settings.movementMultiplier.x : 0);
        movementMultiplier.put("y", settings.movementMultiplier != null ? settings.movementMultiplier.y : 0);
        movementMultiplier.put("z", settings.movementMultiplier != null ? settings.movementMultiplier.z : 0);
        data.put("movementMultiplier", movementMultiplier);

        data.put("applyLookType", settings.applyLookType.toString());
        Map<String, Object> lookMultiplier = new HashMap<>();
        lookMultiplier.put("x", settings.lookMultiplier != null ? settings.lookMultiplier.x : 0);
        lookMultiplier.put("y", settings.lookMultiplier != null ? settings.lookMultiplier.y : 0);
        data.put("lookMultiplier", lookMultiplier);

        data.put("mouseInputType", settings.mouseInputType.toString());

        Map<String, Object> planeNormal = new HashMap<>();
        planeNormal.put("x", settings.planeNormal != null ? settings.planeNormal.x : 0);
        planeNormal.put("y", settings.planeNormal != null ? settings.planeNormal.y : 0);
        planeNormal.put("z", settings.planeNormal != null ? settings.planeNormal.z : 0);
        data.put("planeNormal", planeNormal);

        return new GsonBuilder().setPrettyPrinting().create().toJson(data);
    }
}
