import io.papermc.paperweight.patcher.*
import io.papermc.paperweight.util.*
import io.papermc.paperweight.util.constants.PAPERCLIP_CONFIG

plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("io.papermc.paperweight.patcher") version "1.5.3"
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/") {
        content {
            onlyForConfigurations(PAPERCLIP_CONFIG)
        }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.8.6:fat")
    decompiler("net.minecraftforge:forgeflower:2.0.605.2")
    paperclip("io.papermc:paperclip:3.0.3")
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }

    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    tasks.withType<Test> {
        minHeapSize = "2g"
        maxHeapSize = "2g"
    }

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://ci.emc.gs/nexus/content/groups/aikar/")
        maven("https://repo.aikar.co/content/groups/aikar")
        maven("https://repo.md-5.net/content/repositories/releases/")
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }

}

paperweight {
    serverProject.set(project(":wave-server"))

    remapRepo.set("https://maven.fabricmc.net/")
    decompileRepo.set("https://files.minecraftforge.net/maven/")

    useStandardUpstream("plazma") {
        url.set(github("PlazmaMC", "Plazma"))
        ref.set(providers.gradleProperty("plazmaRef"))

        withStandardPatcher {
            baseName("Plazma")

            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
            apiOutputDir.set(layout.projectDirectory.dir("wave-api"))

            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
            serverOutputDir.set(layout.projectDirectory.dir("wave-server"))
        }
    }
}

tasks.register("updateUpstream") {
    dependsOn("updateUpstreamCommit")
    dependsOn("applyPatches")
}

tasks.register("updateUpstreamCommit") {
    // Update the purpurRef in gradle.properties to be the latest commit.
    val tempDir = layout.cacheDir("plazmaRefLatest");
    val file = "gradle.properties";

    doFirst {
        data class GithubCommit(
            val sha: String
        )

        val plazmaLatestCommitJson = layout.cache.resolve("plazmaLatestCommit.json");
        download.get().download("https://api.github.com/repos/PlazmaMC/Plazma/commits/master", plazmaLatestCommitJson);
        val plazmaLatestCommit = gson.fromJson<paper.libs.com.google.gson.JsonObject>(plazmaLatestCommitJson)["sha"].asString;

        copy {
            from(file)
            into(tempDir)
            filter { line: String ->
                line.replace("plazmaCommit = .*".toRegex(), "plazmaCommit = $plazmaLatestCommit")
            }
        }
    }

    doLast {
        copy {
            from(tempDir.file("gradle.properties"))
            into(project.file(file).parent)
        }
    }
}
