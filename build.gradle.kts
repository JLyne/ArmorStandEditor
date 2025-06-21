import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    java
    alias(libs.plugins.pluginYmlPaper)
}

group = "io.github.rypofalem.armorstandeditor"
version = "2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
	maven {
		url = uri("https://repo.papermc.io/repository/maven-public/")
	}
    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://repo.not-null.co.uk/snapshots/")
    }
    mavenLocal()
}

dependencies {
	compileOnly(libs.paperApi)
	compileOnly(libs.griefPrevention)
	compileOnly(libs.worldguard)
	compileOnly(libs.plotsquaredCore)
    compileOnly(libs.plotsquaredBukkit)
    implementation(platform(libs.intellectualsitesBom))
	compileOnly(libs.customItems)
}

paper {
    main = "io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin"
    apiVersion = libs.versions.paperApi.get().replace(Regex("\\-R\\d.\\d-SNAPSHOT"), "")
    authors = listOf("Jim (AnEnragedPigeon)", "Wolfstorm", "DreiFxn", "Pinnkk", "Kugge", "Marfjeh", "miknes123",
        "rypofalem", "sekwah41", "Sikatsu1997", "Cool_boy", "sumdream", "Amaury Carrade", "nicuch", "kotarobo",
        "prettydude", "Jumpy91", "Niasio", "Patbox", "Puremin0rez", "Prof-Bloodstone", "PlanetTeamSpeak")
    description = "Allows players to edit data of armorstands without any commands."
    foliaSupported = true

    permissions {
        register("asedit.basic") {
            description = "Allow use of /asedit and basic edit modes."
        }
        register("asedit.rename") {
            description = "Allow renaming armorstands"
        }
        register("asedit.equipment") {
            description = "Access armorstand equipment GUI"
        }
        register("asedit.disableSlots") {
            description = "Allows locking and unlocking the contents of an ArmorStand. When locked, armor and equipment can not be added or removed without unlocking it first."
        }
        register("asedit.give") {
            description = "Allows use of /ase give"
        }
        register("asedit.reload") {
            description = "Allows Reloading of the ASE Config."
        }
        register("asedit.movement") {
            description = "Changes whether the armor stand can be moved using the item for editing"
        }
        register("asedit.rotation") {
            description = "Allows player to rotate the ArmorStand"
        }
        register("asedit.copy") {
            description = "Allows the players to create copies of their ArmorStand Configurations."
        }
        register("asedit.paste") {
            description = "Allows the players to apply of a copy of their ArmorStand Configuration."
        }
        register("asedit.reset") {
            description = "Allows the reset of the ArmorStand back to Default values"
        }
        register("asedit.togglearmorstandvisibility") {
            description = "Toggles ArmorStand visibility."
        }
        register("asedit.toggleitemframevisibility") {
            description = "Allows setting of ItemFrame Visibility"
        }
        register("asedit.toggleInvulnerability") {
            description = "Allows players to toggle the vulnerability state of an ArmorStand."
        }
        register("asedit.togglebaseplate") {
            description = "Allows the toggling of the Baseplate of an ArmorStand."
        }
        register("asedit.togglearms") {
            description = "Allows the toggling of the Arms of an ArmorStand."
        }
        register("asedit.togglesize") {
            description = "Allows the toggling of the size of an ArmorStand."
        }
        register("asedit.togglegravity") {
            description = "Changes whether the armor stand has gravity"
        }
        register("asedit.togglearmorstandglow") {
            description = "Allows toggling of the Glowing State of an ArmorStand."
        }
        register("asedit.toggleitemframeglow") {
            description = "Allows toggling of the Glowing State of an ItemFrame."
        }

        register("asedit.ignoreProtection.griefProtection") {
            description = "Allows user to ignore GriefProtection's Protection Limitations."
            default = BukkitPluginDescription.Permission.Default.FALSE
        }
        register("asedit.ignoreProtection.plotSquared") {
            description = "Allows user to ignore PlotSquared's Protection Limitations."
            default = BukkitPluginDescription.Permission.Default.FALSE
        }
        register("asedit.ignoreProtection.worldGuard") {
            description = "Allows user to ignore WorldGuard's Protection Limitations."
            default = BukkitPluginDescription.Permission.Default.FALSE
        }

        register("asedit.permpack.basic") {
            default = BukkitPluginDescription.Permission.Default.TRUE
            children = listOf("asedit.basic", "asedit.equipment", "asedit.togglegravity", "asedit.movement",
                "asedit.disableSlots", "asedit.rename", "asedit.rotation", "asedit.copy", "asedit.paste",
                "asedit.reset", "asedit.toggleInvulnerability", "asedit.togglebaseplate", "asedit.togglearms",
                "asedit.togglesize", "asedit.togglearmorstandvisibility", "asedit.toggleitemframevisibility",
                "asedit.togglearmorstandglow", "asedit.toggleitemframeglow")
        }

        register("asedit.permpack.admin") {
            default = BukkitPluginDescription.Permission.Default.OP
            children = listOf("asedit.ignorePermissions.*", "asedit.permpack.basic", "asedit.reload", "asedit.give")
        }
    }

    serverDependencies {
        register("WorldGuard") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
        register("GriefPrevention") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
        register("PlotSquared") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
        register("CustomItems") {
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
            required = false
        }
    }
}

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
        options.encoding = "UTF-8"
    }
}
