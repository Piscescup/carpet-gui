import org.gradle.api.file.DuplicatesStrategy
import org.gradle.kotlin.dsl.withGroovyBuilder

plugins {
    `maven-publish`
    id("com.github.hierynomus.license") version "0.16.1" apply false
    id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
    id("com.replaymod.preprocess") version "7973f848a8"
    id("me.fallenbreath.yamlang") version "1.5.0" apply false
}

// Dynamic interoperability avoids depending on version-specific generated plugin accessors.
val preprocessExtension = extensions.getByName("preprocess")
preprocessExtension.withGroovyBuilder {
    setProperty("strictExtraMappings", false)

    fun node(name: String, version: Int, mappings: String?): Any =
        requireNotNull("createNode"(name, version, mappings))

    val mc114 = node("1.14.4", 1_14_04, "")
    val mc115 = node("1.15.2", 1_15_02, "")
    val mc116 = node("1.16.5", 1_16_05, "")
    val mc117 = node("1.17.1", 1_17_01, "")
    val mc118 = node("1.18.2", 1_18_02, "")
    val mc119 = node("1.19.4", 1_19_04, "")
    val mc1201 = node("1.20.1", 1_20_01, "")
    val mc1202 = node("1.20.2", 1_20_02, "")
    val mc1204 = node("1.20.4", 1_20_04, "")
    val mc1206 = node("1.20.6", 1_20_06, "")
    val mc1211 = node("1.21.1", 1_21_01, "")
    val mc1213 = node("1.21.3", 1_21_03, "")
    val mc1214 = node("1.21.4", 1_21_04, "")
    val mc1215 = node("1.21.5", 1_21_05, "")
    val mc1218 = node("1.21.8", 1_21_08, "")
    val mc12110 = node("1.21.10", 1_21_10, "")
    val mc12111 = node("1.21.11", 1_21_11, "")
    val mc2601 = node("26.1.2", 26_01_02, null)
    val mc2602 = node("26.2", 26_02_00, null)
    val mc2603 = node("26.3", 26_03_00, null)

    fun link(source: Any, destination: Any) {
        source.withGroovyBuilder { "link"(destination, null) }
    }

    link(mc115, mc114)
    link(mc115, mc116)
    link(mc116, mc117)
    link(mc117, mc118)
    link(mc118, mc119)
    link(mc119, mc1201)
    link(mc1201, mc1202)
    link(mc1202, mc1204)
    link(mc1204, mc1206)
    link(mc1206, mc1211)
    link(mc1211, mc1213)
    link(mc1213, mc1214)
    link(mc1214, mc1215)
    link(mc1215, mc1218)
    link(mc1218, mc12110)
    link(mc12110, mc12111)
    link(mc12111, mc2601)
    link(mc2601, mc2602)
    link(mc2602, mc2603)

    val nodes = "getNodes"() as Iterable<*>
    for (entry in nodes) {
        requireNotNull(entry).withGroovyBuilder {
            val projectPath = getProperty("project").toString()
            val version = getProperty("mcVersion")
            // settings.json can enable only a subset of the version graph.
            findProject(projectPath)?.extensions?.extraProperties?.set("mcVersion", version)
        }
    }
}

tasks.register("buildAndGather") {
    val projectsToGather = project.subprojects.toList()
    dependsOn(projectsToGather.map { "${it.path}:build" })

    doLast {
        println("Gathering builds")
        val destination = rootProject.layout.buildDirectory.dir("libs").get().asFile
        rootProject.delete(rootProject.fileTree(destination) { include("*") })

        for (subproject in projectsToGather) {
            rootProject.copy {
                from(subproject.layout.buildDirectory.dir("libs")) {
                    include("*.jar")
                    exclude("*-dev.jar", "*-sources.jar", "*-shadow.jar")
                }
                into(destination)
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
    }
}
