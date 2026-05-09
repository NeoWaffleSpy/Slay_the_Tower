package com.Team_Berry.Game.Managers;

import com.Team_Berry.Artefacts.Codecs.ArtefactCodec;
import com.Team_Berry.Artefacts.Components.StatEffectComponent;
import com.Team_Berry.Artefacts.UI.ArtefactSelection;
import com.Team_Berry.Artefacts.UI.SkillSelection;
import com.Team_Berry.Artefacts.UI.WeaponSelection;
import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import java.util.*;

public class RewardManager {

    private static final String INVISIBLE_HP_ARTEFACT = "Invisible_Hp_Artefact";
    private final World world;
    private final Map<UUID, Integer> historicalSkillCounts = new HashMap<>();
    private final Map<UUID, String> playerClasses = new HashMap<>();
    private final Map<UUID, Set<String>> playerOwnedSkills = new HashMap<>();
    private final Map<UUID, List<String>> playerOwnedArtefacts = new HashMap<>();
    private final Map<UUID, Set<BlockPosition>> playerClaimedChests = new HashMap<>();
    private final Map<UUID, BlockPosition> pendingChestClaims = new HashMap<>();

    public RewardManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public void setPlayerClass(PlayerRef playerRef, String className) {
        playerClasses.put(playerRef.getUuid(), className);
        log("Assigned class '" + className + "' to " + playerRef.getUsername());
    }

    public boolean hasPlayerClass(PlayerRef playerRef) {
        return playerClasses.containsKey(playerRef.getUuid());
    }

    public boolean canPlayerPickSkill(PlayerRef playerRef, int globalMaxSkills) {
        int possessedSkills = historicalSkillCounts.getOrDefault(playerRef.getUuid(), 0);
        return possessedSkills < globalMaxSkills;
    }

    public void incrementPlayerSkillCount(PlayerRef playerRef) {
        int currentSkills = historicalSkillCounts.getOrDefault(playerRef.getUuid(), 0);
        historicalSkillCounts.put(playerRef.getUuid(), currentSkills + 1);
        log("Player " + playerRef.getUsername() + " incremented skills to: " + (currentSkills + 1));
    }

    public int getPlayerSkillCount(PlayerRef playerRef) {
        return historicalSkillCounts.getOrDefault(playerRef.getUuid(), 0);
    }

    public void grantSkillRewards(PlayerRef playerRef, int globalMaxSkills) {
        if (!hasPlayerClass(playerRef)) {
            log(playerRef.getUsername() + " interacted with the statue but has no weapon. Opening Weapon Selection UI.");

            WeaponSelection ui = new WeaponSelection(playerRef, world.getEntityStore().getStore());
            ui.buildPage();
            playerRef.sendMessage(Message.raw("Since you didn't talk to the kweebec back in prison, here's your weapon. Talk to the statue again to get something else!"));

            return;
        }

        if (!canPlayerPickSkill(playerRef, globalMaxSkills)) {
            log("Player " + playerRef.getUsername() + " attempted to claim a skill, but is already at their cap.");
            playerRef.sendMessage(Message.raw("You have already remembered all you can right now..."));
            return;
        }

        log("Granting skill rewards UI to : " + playerRef.getUsername());

        EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw("Choose a skill!"),
                Message.raw("The statue helps you remember training.."),
                false,
                null,
                2.0F, 0.5F, 0.5F
        );

        List<Item> skillsToOffer = generateSkillOptions(playerRef);

        SkillSelection ui = new SkillSelection(playerRef, world.getEntityStore().getStore());
        ui.buildPageWithList(skillsToOffer);
    }

    private List<Item> generateSkillOptions(PlayerRef playerRef) {
        DefaultAssetMap<String, Item> itemMap = Item.getAssetMap();

        String playerClass = playerClasses.getOrDefault(playerRef.getUuid(), "Dagger");
        String tagToSearch = "HotbarClass=" + playerClass;

        int tagIndex = AssetRegistry.getOrCreateTagIndex(tagToSearch);
        Set<String> validKeys = itemMap.getKeysForTag(tagIndex);
        if (validKeys == null || validKeys.isEmpty()) {
            log("WARNING: No skills found in Asset Store with tag: " + tagToSearch);
            return Collections.emptyList();
        }

        Set<String> ownedSkills = playerOwnedSkills.getOrDefault(playerRef.getUuid(), Collections.emptySet());

        List<Item> options = new ArrayList<>();
        for (String key : validKeys) {
            if (!ownedSkills.contains(key)) {
                Item item = itemMap.getAsset(key);
                if (item != null) {
                    options.add(item);
                }
            }
        }

        Collections.shuffle(options);

        if (options.size() > 3) {
            return options.subList(0, 3);
        }
        return options;
    }

    public void onPlayerClaimedSkillReward(PlayerRef playerRef, String claimedSkillId, int globalMaxSkills, PlayerModelManager playerModelManager) {
        if (!canPlayerPickSkill(playerRef, globalMaxSkills)) {
            log("Player " + playerRef.getUsername() + " attempted to claim a skill, but is already at their cap.");
            return;
        }

        incrementPlayerSkillCount(playerRef);

        playerOwnedSkills.computeIfAbsent(playerRef.getUuid(), k -> new HashSet<>()).add(claimedSkillId);

        playerRef.sendMessage(Message.raw("You remembered a part of yourself!"));
        log(playerRef.getUsername() + " successfully claimed a skill reward: " + claimedSkillId);
        int currentSkills = getPlayerSkillCount(playerRef);
        if (currentSkills >= 4) {
            playerModelManager.resetPlayerModel(playerRef);
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw("HUMANITY REGAINED !"),
                    Message.raw("Your true form has been restored."),
                    true,
                    null,
                    2.0F, 0.5F, 0.5F
            );
        }
    }

    public void onPlayerClaimedArtefactReward(PlayerRef playerRef, String artefactId) {
        playerOwnedArtefacts.computeIfAbsent(playerRef.getUuid(), k -> new ArrayList<>()).add(artefactId);

        BlockPosition pendingPos = pendingChestClaims.remove(playerRef.getUuid());
        if (pendingPos != null) {
            playerClaimedChests.computeIfAbsent(playerRef.getUuid(), k -> new HashSet<>()).add(pendingPos);
            log("Reward claimed by: " + playerRef.getUsername() + " (Artefact: " + artefactId + ") from chest.");
        }
    }

    public void playerChestClaim(BlockPosition pos, PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        Set<BlockPosition> claimedChests = playerClaimedChests.getOrDefault(playerId, Collections.emptySet());

        if (claimedChests.contains(pos)) {
            playerRef.sendMessage(Message.raw("You have already looted this chest!"));
            return;
        }

        pendingChestClaims.put(playerId, pos.clone());
        log(playerRef.getUsername() + " is viewing chest loot at " + pos.x + ", " + pos.y + ", " + pos.z);

        EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw("Chest Looted!"),
                Message.raw("Select your reward..."),
                false,
                null,
                2.0F, 0.5F, 0.5F
        );

        ArtefactSelection ui = new ArtefactSelection(playerRef, world.getEntityStore().getStore());
        ui.buildPage();
    }

    public void resetClaimedChests() {
        this.playerClaimedChests.clear();
        this.pendingChestClaims.clear();
        log("Reset chest claim and pending histories for the new room.");
    }

    public void grantStartingArtefact(PlayerRef playerRef) {
        if (!playerOwnedArtefacts.containsKey(playerRef.getUuid())) {
            playerOwnedArtefacts.put(playerRef.getUuid(), new ArrayList<>(List.of(INVISIBLE_HP_ARTEFACT)));
            log("Granted starting Invisible_Hp_Artefact to new participant: " + playerRef.getUsername());
        }
    }

    public void restorePlayerArtefacts(PlayerRef playerRef) {
        if (playerOwnedArtefacts.containsKey(playerRef.getUuid())) {
            StatEffectComponent statComp = StatEffectComponent.getPlayerStatComp(playerRef);
            if (statComp != null) {
                DefaultAssetMap<String, ArtefactCodec> artefactMap = ArtefactCodec.getAssetMap();
                statComp.addStackToArtifact(artefactMap.getAsset(INVISIBLE_HP_ARTEFACT), 0);
                List<String> savedArtefacts = playerOwnedArtefacts.get(playerRef.getUuid());

                for (String artefactId : savedArtefacts) {
                    ArtefactCodec artefact = artefactMap.getAsset(artefactId);
                    if (artefact != null) {
                        statComp.addArtifact(artefact);
                    }
                }
                log("Restored " + savedArtefacts.size() + " artefacts for returning player: " + playerRef.getUsername());
            }
        }
    }
}
