package com.Team_Berry.Utils.Files;

import com.Team_Berry.SlayTheTower;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.asset.AssetModule;

import java.nio.file.Path;

public class FileUtils {
    public static AssetPack getBasePack() {
        return AssetModule.get().getAssetPack(SlayTheTower.instance.getName());
    }

    public static Path getBasePackRoot() {
        return getBasePack().getRoot();
    }

    public static Path instanceDirCamera(AssetPack pack, String instanceName) {
        return pack.getRoot().resolve("Common").resolve("CameraSettings").resolve(instanceName);
    }

    public static Path instanceDirCinema(AssetPack pack, String instanceName) {
        return pack.getRoot().resolve("Common").resolve("CinematicKeys").resolve(instanceName);
    }
}
