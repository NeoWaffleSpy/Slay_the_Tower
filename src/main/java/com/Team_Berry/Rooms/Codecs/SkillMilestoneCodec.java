package com.Team_Berry.Rooms.Codecs;

import com.Team_Berry.Rooms.RoomPlugin;
import com.Team_Berry.Utils.Codecs.CustomArrayCodec;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import java.util.ArrayList;

public class SkillMilestoneCodec implements JsonAssetWithMap<String, DefaultAssetMap<String, SkillMilestoneCodec>> {
    public static final AssetBuilderCodec<String, SkillMilestoneCodec> CODEC;
    private static AssetStore<String, SkillMilestoneCodec, DefaultAssetMap<String, SkillMilestoneCodec>> ASSET_STORE;

    static {
        CODEC = AssetBuilderCodec.builder(SkillMilestoneCodec.class, SkillMilestoneCodec::new, Codec.STRING,
                        (t, k) -> t.id = k, (t) -> t.id,
                        (asset, data) -> asset.data = data, (asset) -> asset.data)
                .append(new KeyedCodec<>("Milestones", new CustomArrayCodec<>(MilestoneEntry.CODEC, ArrayList::new)),
                        (obj, val) -> obj.milestones = val,
                        obj -> obj.milestones)
                .documentation("A list of progressive milestones based on room clears.").add()
                .build();
    }

    public ArrayList<MilestoneEntry> milestones = new ArrayList<>();
    private String id;
    private AssetExtraInfo.Data data;

    public SkillMilestoneCodec() {
    }

    public SkillMilestoneCodec(String id) {
        this.id = id;
    }

    public static void register() {
        RoomPlugin plugin = RoomPlugin.get();
        plugin.getAssetRegistry()
                .register(HytaleAssetStore.builder(SkillMilestoneCodec.class, new DefaultAssetMap<>())
                        .setPath("Milestones")
                        .setCodec(SkillMilestoneCodec.CODEC)
                        .setKeyFunction(SkillMilestoneCodec::getId)
                        .setReplaceOnRemove(SkillMilestoneCodec::new)
                        .build());

        plugin.getEventRegistry().register(LoadedAssetsEvent.class, SkillMilestoneCodec.class, SkillMilestoneCodec::onLoaded);
        plugin.getEventRegistry().register(RemovedAssetsEvent.class, SkillMilestoneCodec.class, SkillMilestoneCodec::onRemoved);
    }

    // --- Inner Milestone Entry ---

    public static AssetStore<String, SkillMilestoneCodec, DefaultAssetMap<String, SkillMilestoneCodec>> getAssetStore() {
        if (ASSET_STORE == null) {
            ASSET_STORE = AssetRegistry.getAssetStore(SkillMilestoneCodec.class);
        }
        return ASSET_STORE;
    }

    // --- Registry Logic ---

    public static DefaultAssetMap<String, SkillMilestoneCodec> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public static void onLoaded(LoadedAssetsEvent<String, SkillMilestoneCodec, DefaultAssetMap<String, SkillMilestoneCodec>> event) {
    }

    public static void onRemoved(RemovedAssetsEvent<String, SkillMilestoneCodec, DefaultAssetMap<String, SkillMilestoneCodec>> event) {
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Gets a milestone by its index in the progression (0 = first, 1 = second, etc.)
     */
    public MilestoneEntry getMilestoneByIndex(int index) {
        if (index >= 0 && index < milestones.size()) {
            return milestones.get(index);
        }
        return null;
    }

    /**
     * Gets the total number of milestones defined in this skill tree.
     */
    public int getTotalMilestoneCount() {
        return milestones.size();
    }

    /**
     * Finds which milestone index a player is currently on based on their total cleared rooms.
     */
    public int getMilestoneIndexForRooms(int clearedRooms) {
        int currentIdx = -1;
        for (int i = 0; i < milestones.size(); i++) {
            if (clearedRooms >= milestones.get(i).roomCount) {
                currentIdx = i;
            } else {
                break; // Since the list is sorted, we can stop once we hit a count too high
            }
        }
        return currentIdx;
    }

    public static class MilestoneEntry {
        public static final BuilderCodec<MilestoneEntry> CODEC = BuilderCodec.builder(MilestoneEntry.class, MilestoneEntry::new)
                .append(new KeyedCodec<>("Name", Codec.STRING),
                        (obj, val) -> obj.name = val,
                        obj -> obj.name).add()
                .append(new KeyedCodec<>("RoomCount", Codec.INTEGER),
                        (obj, val) -> obj.roomCount = (val != null) ? val : 0,
                        obj -> obj.roomCount).add()
                .append(new KeyedCodec<>("Difficulty", Codec.INTEGER),
                        (obj, val) -> obj.difficulty = (val != null) ? val : 1,
                        obj -> obj.difficulty).add()
                .build();

        public String name = "Unnamed Milestone";
        public int roomCount = 0;
        public int difficulty = 1; // Default to base difficulty
    }
}