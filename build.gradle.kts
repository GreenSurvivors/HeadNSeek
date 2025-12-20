plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18" // note: beta.19 is dependent on Gradle 9 and somehow that breaks library loading in runetime. Based on past experience Gradle 9 is horribly broken!
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("xyz.jpenilla.run-waterfall") version "3.0.2"
    id("xyz.jpenilla.run-velocity") version "3.0.2"
    id("net.raphimc.class-token-replacer") version "1.1.7" // allows us to replace placeholders with version strings in class files
}

group = "de.greensurvivors"
version = buildString {
    append(project.properties["plugin_version"])

    if ((project.properties["release"] as String).toBoolean().not()) {
        append("-Snapshot")
    }

    append("+${project.properties["minecraft_version"]}")
}

// don't reobfuscate, use mojang mapping.
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

java {
    // Configure the java toolchain. This allows Gradle to auto-provision JDK 21 on systems that only have JDK 17 installed for example.
    // If you need to compile to for example JVM 8 or 17 bytecode, adjust the 'release' option below and keep the toolchain at 21.
    toolchain.languageVersion = JavaLanguageVersion.of("${rootProject.properties["java_version"]}")
    sourceCompatibility = JavaVersion.toVersion(rootProject.properties["java_version"]!!)
}

repositories {
    mavenCentral()
    mavenLocal()

    //paper
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://maven.greensurvivors.de/releases/")
    }
}

dependencies {
    paperweight.paperDevBundle("${project.properties["minecraft_version"]}-R0.1-SNAPSHOT")
    compileOnly("io.github.waterfallmc:waterfall-api:${project.properties["waterfall_version"]}-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:${project.properties["velocity_version"]}")
    annotationProcessor("com.velocitypowered:velocity-api:${project.properties["velocity_version"]}")

    // plus means use the latest. This is a dangerous choice since you might never know what got changed in the dependency.
    // however since we ourselves control the plugin and want a fail fast behaviour - e.a. fail at compile time if the
    // lastest release on the server will break something, this is ok.
    api("de.greensurvivors:GreenSocket:+")
    compileOnly ("com.github.ben-manes.caffeine:caffeine:${project.properties["caffeine_version"]}") // caches
    compileOnly("net.kyori:adventure-platform-bungeecord:${project.properties["bungee_adventure_version"]}")

    // tests
    testImplementation(platform("org.junit:junit-bom:${project.properties["junit_version"]}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        classTokenReplacer {
            project.properties.entries
                .filter { it.value != null && (it.value is Number || it.value is String) }
                .associate {"\${${it.key}}" to it.value!! }
                .forEach {
                    property(it.key, it.value)
                }
        }
    }
}

tasks {
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        expand(project.properties)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = project.properties["java_version"].toString().toInt()
    }

    test {
        useJUnitPlatform()
    }

    runWaterfall {
        waterfallVersion(project.properties["waterfall_version"] as String)

        // get jars from dependency
        // note: this was done to profit from gradles resolving artefact algorithm.
        // doing it like this means we will use the same version as we compile against (latest)
        // and we can ignore classifiers we would to add if we would use downloadPlugins { url("") }
        pluginJars.from(
            configurations.runtimeClasspath.map { configuration ->
                configuration.files.filter { file -> file.name.startsWith("GreenSocket", ignoreCase = true)
                }
            }
        )
    }

    runVelocity {
        velocityVersion(project.properties["velocity_version"] as String)

        // get jars from dependency
        // note: this was done to profit from gradles resolving artefact algorithm.
        // doing it like this means we will use the same version as we compile against (latest)
        // and we can ignore classifiers we would to add if we would use downloadPlugins { url("") }
        pluginJars.from(
            configurations.runtimeClasspath.map { configuration ->
                configuration.files.filter { file -> file.name.startsWith("GreenSocket", ignoreCase = true)
                }
            }
        )
        // This is a dependency of GreenSocket on velocity we have to fulfill in order to load GreenSocket.
        //
        // god alternative: https://modrinth.com/plugin/mckotlin , it's made by a known guy (4drian3d) in the community.
        // I don't know if done by hand beats done by github action, but at point of writing the action failed to upload the latest
        // kotlin version to modrinth.
        // downside of mckotlin is that it has its own versioning system and multiple jars for the same kotlin version.
        // so just updating the kotlin version isn't that easy, where for kotlinmc - yes I know very similar names - it is
        // that's why im sticking with my original pick of kotlinmc, even though 4drian3d is somewhat well known.
        // (he is a velocity contributor himself).
        // if kotlinmc stays at kotlin version 2.2.20 and fails to further update it should be easy to just swap out.
        // maybe the way forward is to download kotlin ourselves and attaching it via the pluginmanager to the classloader
        // https://docs.papermc.io/velocity/dev/dependency-management/
        // witch is stupid, when dealing with mutlible kotlin plugins...
        downloadPlugins {
            modrinth("kotlinmc", project.properties["greensocket_kotlin_version"] as String)
        }
    }

    runServer {
        pluginJars.from(
            configurations.runtimeClasspath.map { configuration ->
                configuration.files.filter { file -> file.name.startsWith("GreenSocket", ignoreCase = true)
                }
            }
        )

        // disable bstats, as it isn't needed for dev environment
        doFirst { // this happens after downloading the plugins above, but before the server starts
            val cfg = runDirectory.get().asFile.resolve("plugins/bStats/config.yml")
            if (!cfg.exists()) {
                cfg.parentFile.mkdirs()
                cfg.createNewFile()
            }
            cfg.writeText("enabled: false\n")
        }
        // automatically agree to eula
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}