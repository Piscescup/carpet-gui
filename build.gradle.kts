import org.gradle.api.file.DuplicatesStrategy
import org.gradle.kotlin.dsl.withGroovyBuilder

plugins {
    base
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

    // This is the full compatibility graph. settings.json selects the nodes
    // that Gradle actually configures and builds; inactive nodes may remain in
    // the graph so a future version can be enabled without rebuilding links.
    val configuredNodes = linkedSetOf<String>()
    fun node(name: String, version: Int, mappings: String?): Any {
        require(configuredNodes.add(name)) { "Duplicate preprocess node: $name" }
        require(rootProject.file("versions/$name/gradle.properties").isFile) {
            "Missing versions/$name/gradle.properties"
        }
        return requireNotNull("createNode"(name, version, mappings))
    }

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

    // Link adjacent compatible versions. extraMappings is only needed when
    // names/packages cannot be inferred across that particular version jump.
    fun link(source: Any, destination: Any, extraMappings: String? = null) {
        if (extraMappings != null) {
            require(rootProject.file(extraMappings).isFile) { "Missing preprocess mapping file: $extraMappings" }
        }
        source.withGroovyBuilder {
            "link"(destination, extraMappings?.let(rootProject::file))
        }
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
    // No extra mapping file is needed for these transitions; null means that
    // the preprocessor should rely on its normal name mapping.
    link(mc1211, mc1213)
    link(mc1213, mc1214)
    link(mc1214, mc1215, "versions/mapping-1.21.4-1.21.5.txt")
    link(mc1215, mc1218)
    link(mc1218, mc12110, "versions/mapping-1.21.8-1.21.10.txt")
    link(mc12110, mc12111, "versions/mapping-1.21.10-1.21.11.txt")
    link(mc12111, mc2601, "versions/mapping-1.21.11-26.1.2.txt")
    link(mc2601, mc2602, "versions/mapping-26.1.2-26.2.txt")
    link(mc2602, mc2603, "versions/mapping-26.2-26.3.txt")

    val enabledVersions = subprojects.map { it.name }.toSet()
    require(configuredNodes.containsAll(enabledVersions)) {
        "Enabled versions without preprocess nodes: ${enabledVersions - configuredNodes}"
    }

    // common.gradle.kts uses this value for version-specific dependency and
    // plugin configuration, while settings.json decides which nodes exist.
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

tasks.register("preprocessAllVersions") {
    group = "build"
    description = "Preprocess Java and resources for every enabled Minecraft version"
    dependsOn(subprojects.flatMap { subproject ->
        listOf("${subproject.path}:preprocessCode", "${subproject.path}:preprocessResources")
    })
}

tasks.register("checkAllVersions") {
    group = "verification"
    description = "Run verification tasks for every enabled Minecraft version"
    dependsOn(subprojects.map { "${it.path}:check" })
}

tasks.register("buildAndGather") {
    group = "build"
    description = "Build jars, and collect to ./build/release"
    val projectsToGather = project.subprojects.toList()
    dependsOn(projectsToGather.map { "${it.path}:build" })

    doLast {
        println("Gathering builds")
        val destination = rootProject.layout.buildDirectory.dir("release").get().asFile
        // Recreate the collection directory so jars left by a previous
        // SNAPSHOT/release mode cannot leak into the current result.
        rootProject.delete(destination)

        for (subproject in projectsToGather) {
            rootProject.copy {
                from(subproject.layout.buildDirectory.dir("libs")) {
                    // Each libs directory can contain artifacts from older
                    // invocations. Select only this invocation's version.
                    include("*-${subproject.version}.jar")
                    exclude("*-dev.jar", "*-sources.jar", "*-shadow.jar")
                }
                into(destination)
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
    }
}

tasks.register("cleanPreprocessSources") {
    group = "build"
    description = "Delete generated preprocess source trees for every enabled Minecraft version"
    doLast {
        subprojects.forEach { subproject ->
            val generated = subproject.layout.buildDirectory.dir("preprocessed").get().asFile
            if (generated.isDirectory) delete(generated)
        }
    }
}
