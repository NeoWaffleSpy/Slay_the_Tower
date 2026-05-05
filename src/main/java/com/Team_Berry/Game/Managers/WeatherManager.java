package com.Team_Berry.Game.Managers;

import com.Team_Berry.Game.GamePlugin;
import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeatherManager {
    private static final List<String> RANDOM_WEATHERS = Arrays.asList(
            "Weather_Red", "Weather_Blue", "Weather_Purple", "Weather_Green"
    );
    private final World world;

    public WeatherManager(World world) {
        this.world = world;
    }

    private void log(String message) {
        GamePlugin.LOGGER.atInfo().log(String.format("[SLAY THE TOWER] - [%s] - %s", world.getName(), message));
    }

    public void setForcedWeather(@Nullable String forcedWeather) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        WeatherResource weatherResource = (WeatherResource) store.getResource(WeatherResource.getResourceType());
        weatherResource.setForcedWeather(forcedWeather);
        WorldConfig config = this.world.getWorldConfig();
        config.setForcedWeather(forcedWeather);
        config.markChanged();
    }

    public void setRandomRoomWeather() {
        String randomWeather = RANDOM_WEATHERS.get(ThreadLocalRandom.current().nextInt(RANDOM_WEATHERS.size()));
        setForcedWeather(randomWeather);
        log("Weather changed to random state: " + randomWeather);
    }

    public void setLobbyWeather() {
        setForcedWeather("Weather_Tree");
        log("Weather changed to Lobby.");
    }

    public void setPrisonWeather() {
        setForcedWeather("Weather_Prison");
        log("Weather changed to Prison.");
    }

    public void setBossWeather() {
        setForcedWeather("Weather_Boss");
        log("Weather changed to Boss.");
    }

    public void setTransitionWeather() {
        setForcedWeather("Weather_Transition");
    }
}
