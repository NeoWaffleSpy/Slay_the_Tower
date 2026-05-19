plugins {
    idea
    java
    `maven-publish`
    id("com.azuredoom.hytale-tools") version "1.0.28"
}

group = "com.Team_Berry"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.hytale-modding.info/releases") {
        name = "HytaleModdingReleases"
    }
    maven("https://www.cursemaven.com")
}

hytaleTools {
    hytaleVersion.set("2026.+")
    manifestGroup.set("com.Team_Berry")
    modCredits.set("Light06/Nalo/Varrell/Arc")
    patchline.set("release")
    modId.set("savethekweebecs")
    mainClass.set("com.Team_Berry.Save_The_Kweebec")
    modDescription.set("Save the Kweebecs rogue-lite mod")
    includesPack.set(true)

    subPlugin("Utils", "com.Team_Berry.Utils.UtilsPlugin")
    subPlugin("Game", "com.Team_Berry.Game.GamePlugin")
    subPlugin("Camera", "com.Team_Berry.Camera.CameraPlugin")
    subPlugin("GearAffix", "com.Team_Berry.GearAffix.GearAffixPlugin")
    subPlugin("WeaponInteraction", "com.Team_Berry.WeaponInteraction.WeaponInteractionPlugin")
    subPlugin("Artefact", "com.Team_Berry.Artefacts.ArtefactPlugin")
    subPlugin("Room", "com.Team_Berry.Rooms.RoomPlugin")

}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.jspecify)

    vineImplementation("curse.maven:hyui-1431415:7820303")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
