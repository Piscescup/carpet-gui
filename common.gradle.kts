import java.util.*

// Convert Minecraft's dotted version to the integer format used by the
// preprocessor conditionals, e.g. 1.21.10 -> 12110 and 26.3 -> 260300.
fun minecraftVersionCode(version: String): Int {
    val parts = version.substringBefore('-').split('.')
    require(parts.size in 2..3) { "Unsupported Minecraft version: $version" }
    return parts[0].toInt() * 10_000 +
        parts[1].toInt() * 100 +
        parts.getOrElse(2) { "0" }.toInt()
}

val mcVersion = minecraftVersionCode(project.property("minecraft_version").toString())
val unobfuscated = mcVersion >= 26_00_00

apply(plugin = "maven-publish")
apply(plugin = "com.github.hierynomus.license")
apply(plugin = if (unobfuscated) "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap")
apply(plugin = "com.replaymod.preprocess")
apply(plugin = "me.fallenbreath.yamlang")

fun modProperty(name: String): String = project.property(name).toString()

repositories {
    maven {
        url = uri("https://maven.parchmentmc.org")
        content { includeGroup("org.parchmentmc.data") }
    }
    maven {
        url = uri("https://jitpack.io")
        content { includeGroupAndSubgroups("com.github") }
    }
    maven {
        url = uri("https://maven.fallenbreath.me/releases")
        content { includeGroup("me.fallenbreath") }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

val loomExtension = extensions.getByName("loom")

fun processDependency(dependency: Dependency): Dependency {
    // https://github.com/FabricMC/fabric-loader/issues/783
    if (dependency is ModuleDependency &&
        !(dependency.group == "net.fabricmc" && dependency.name == "fabric-loader")) {
        dependency.exclude(mapOf("group" to "net.fabricmc", "module" to "fabric-loader"))
    }
    return dependency
}

fun autoDependency(configuration: String, notation: Any): Dependency =
    processDependency(requireNotNull(dependencies.add(
        if (unobfuscated) configuration else "mod" + configuration.replaceFirstChar { it.uppercaseChar() },
        notation
    )))

fun autoImplementation(notation: Any) = autoDependency("implementation", notation)

fun autoRuntimeOnly(notation: Any) = autoDependency("runtimeOnly", notation)

fun autoLocalRuntime(notation: Any) = autoDependency("localRuntime", notation)

fun autoCompileOnly(notation: Any) = autoDependency("compileOnly", notation)

dependencies {
    add("minecraft", "com.mojang:minecraft:${modProperty("minecraft_version")}")
    if (!unobfuscated) {
        val layeredMappings = loomExtension.withGroovyBuilder {
            "layered" {
                "officialMojangMappings"()
                val parchmentVersion = modProperty("parchment_version")
                if (parchmentVersion.isNotEmpty()) {
                    "parchment"("org.parchmentmc.data:parchment-${modProperty("minecraft_version")}:$parchmentVersion@zip")
                }
            }
        }
        add("mappings", requireNotNull(layeredMappings))
    }
    autoImplementation("net.fabricmc.fabric-api:fabric-api:${modProperty("fabric_api_version")}")
    autoImplementation("net.fabricmc:fabric-loader:${modProperty("loader_version")}")

    autoImplementation("maven.modrinth:modmenu:${modProperty("modmenu_version")}")
    autoImplementation("maven.modrinth:tcdcommons:${modProperty("tcdcommons_version")}")
    autoImplementation("maven.modrinth:carpet:${modProperty("carpet_version")}")

    // TCDCommons 5 exports JSpecify annotations transitively, while the
    // legacy 3.x/4.x line does not.  Keep source annotations available on
    // every preprocess node without adding anything to the runtime jar.
    autoCompileOnly("org.jspecify:jspecify:1.0.0")

    add("testImplementation", platform("org.junit:junit-bom:${modProperty("junit_version")}"))
    add("testImplementation", "org.junit.jupiter:junit-jupiter")
    add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")

    // Optional runtime mods:
//     if (mcVersion < 11904) {
//         autoRuntimeOnly(if (mcVersion < 11900) "com.github.astei:lazydfu:0.1.2"
//             else "com.github.Fallen-Breath:lazydfu:a7cfc44c0c")
//     }
//     autoLocalRuntime("me.fallenbreath:mixin-auditor:0.3.0-${if (unobfuscated) "u" else "o"}")

    // Optional dependencies:
//     val fabricApiModules = mutableListOf<String>()
//     if (mcVersion >= 12105) fabricApiModules.add("fabric-api-base")
//     if (mcVersion < 12111) fabricApiModules.add("fabric-resource-loader-v0")
//     if (mcVersion >= 12109) fabricApiModules.add("fabric-resource-loader-v1")
//     val fabricApiExtension = extensions.getByName("fabricApi")
//     fabricApiModules.forEach { module ->
//         val notation = fabricApiExtension.withGroovyBuilder {
//             "module"(module, modProperty("fabric_api_version"))
//         }
//         add("include", autoImplementation(requireNotNull(notation)))
//     }
//     add("include", autoImplementation("me.fallenbreath:conditional-mixin-fabric:${modProperty("conditionalmixin_version")}"))
//     if (mcVersion < 12005) {
//         add("include", "io.github.llamalad7:mixinextras-fabric:${modProperty("mixinextras_version")}")
//     }
}

val mixinConfigPath = "carpet-gui.mixins.json"

val langDir = "assets/carpet-gui/lang"

val javaCompatibility = when {
    mcVersion >= 260000 -> JavaVersion.VERSION_25
    mcVersion >= 12005 -> JavaVersion.VERSION_21
    mcVersion >= 11800 -> JavaVersion.VERSION_17
    mcVersion >= 11700 -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
val mixinCompatibilityLevel = javaCompatibility

val commonVmArgs = listOf("-Dmixin.debug.export=true")

@Suppress("UNCHECKED_CAST")
val runConfigs = loomExtension.withGroovyBuilder { getProperty("runConfigs") }
    as NamedDomainObjectContainer<Any>
runConfigs.configureEach {
    withGroovyBuilder {
        setProperty("ideConfigGenerated", true)
        // Keep worlds, configs and logs isolated per Minecraft version:
        // versions/<version>/run instead of the root project's shared run/.
        "runDir"(layout.projectDirectory.dir("run").asFile.absolutePath)
        "vmArgs"(commonVmArgs)
    }
}

// Optional MIXIN_AUDITOR runs:
// val runs = loomExtension.withGroovyBuilder { getProperty("runs") } as NamedDomainObjectContainer<Any>
// runs.create("serverMixinAudit").withGroovyBuilder {
//     "server"()
//     "vmArgs"(commonVmArgs + "-DmixinAuditor.audit=true")
//     setProperty("ideConfigGenerated", false)
// }
// runs.create("clientMixinAudit").withGroovyBuilder {
//     "client"()
//     "vmArgs"(commonVmArgs + "-DmixinAuditor.audit=true")
//     setProperty("ideConfigGenerated", false)
// }

// Preserve the original Gradle 9.5 runClient/downloadAssets ordering workaround.
afterEvaluate {
    tasks.matching { it.name.startsWith("runClient") }.configureEach {
        val runTask = this
        project.parent?.subprojects?.filter { it != project }?.forEach { otherProject ->
            otherProject.tasks.findByName("downloadAssets")?.let { runTask.mustRunAfter(it) }
        }
    }
}

val modVersion = providers.gradleProperty("version").get()

// Local builds are snapshots by default.  Use -Prelease=true (or the legacy
// BUILD_RELEASE=true environment variable) to produce release jar names and
// release metadata without the -SNAPSHOT suffix.
val releaseBuild = providers.gradleProperty("release")
    .map { it.toBoolean() }
    .orElse(providers.environmentVariable("BUILD_RELEASE").map { it.toBoolean() })
    .orElse(false)
    .get()

val modVersionSuffix = if (releaseBuild) "" else
    System.getenv("BUILD_ID")?.let { "+build.$it" } ?: "-SNAPSHOT"

val artifactVersionSuffix = if (releaseBuild) "" else "-SNAPSHOT"

val fullModVersion = modVersion + modVersionSuffix

val minecraftVersion = modProperty("minecraft_version")

val jitpackBuild = System.getenv("JITPACK") == "true"

val archivesBaseName = modProperty("archives_base_name")

val fullProjectVersion = if (jitpackBuild) "v$fullModVersion" else "v$modVersion-mc$minecraftVersion$modVersionSuffix"

val fullArtifactVersion = if (jitpackBuild) "$modVersion$artifactVersionSuffix"
    else "$modVersion-mc$minecraftVersion$artifactVersionSuffix"

// Example version values:
//   project.mod_version     1.0.3                      (the base mod version)
//   modVersionSuffix        +build.88                  (use github action build number if possible)
//   artifactVersionSuffix   -SNAPSHOT
//   fullModVersion          1.0.3+build.88             (the actual mod version to use in the mod)
//   fullProjectVersion      v1.0.3-mc1.15.2+build.88   (in build output jar name)
//   fullArtifactVersion     1.0.3-mc1.15.2-SNAPSHOT    (maven artifact version)


group = providers.gradleProperty("group").get()

version = fullProjectVersion

val baseExtension = extensions.getByType<BasePluginExtension>()

baseExtension.archivesName.set(if (jitpackBuild) "$archivesBaseName-mc$minecraftVersion" else archivesBaseName)

tasks.named<ProcessResources>("processResources") {
    val modProperties = mapOf(
        "id" to modProperty("mod_id"),
        "name" to modProperty("mod_name"),
        "version" to fullModVersion,
        "minecraft_dependency" to modProperty("minecraft_dependency"),
        "java_dependency" to modProperty("java_dependency"),
        "carpet_dependency" to modProperty("carpet_dependency"),
        "tcdcommons_dependency" to modProperty("tcdcommons_dependency"),
        "modmenu_dependency" to modProperty("modmenu_dependency")
    )
    inputs.properties(modProperties)
    filesMatching("fabric.mod.json") { expand(modProperties) }
    filesMatching(mixinConfigPath) {
        filter { line: String ->
            val compatibilityLine = line.replace(
                "{{COMPATIBILITY_LEVEL}}",
                "JAVA_${mixinCompatibilityLevel.ordinal + 1}"
            )
            if (mcVersion < 260000 && (
                    compatibilityLine.contains("ClientPacketListenerMixin") ||
                    compatibilityLine.contains("GameRendererMixin")
                )) "" else compatibilityLine
        }
    }
}

extensions.getByName("yamlang").withGroovyBuilder {
    setProperty("targetSourceSets", listOf(extensions.getByType<SourceSetContainer>().getByName("main")))
    setProperty("inputDir", langDir)
}

extensions.configure<SourceSetContainer> {
    named("main") {
        // Workspace source selection is handled in-place by ReplayMod //#if blocks.
        if (mcVersion < 260000) {
            java.exclude("io/github/piscescup/fabricmc/carpetgui/integration/vanilla/VanillaRuleStore.java")
            java.exclude("io/github/piscescup/fabricmc/carpetgui/integration/vanilla/VanillaRuleView.java")
        }
        // The main preprocess node owns generated resources. Other nodes receive
        // them through preprocessResources, so adding the root directory again
        // would create duplicate lang entries.
        val mainProjectName = rootProject.file("versions/mainProject").readText().trim()
        if (project.name == mainProjectName) {
            resources.srcDir(rootProject.file("src/main/generated"))
        }
        resources.exclude(".cache/**")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    if (javaCompatibility <= JavaVersion.VERSION_1_8) options.compilerArgs.add("-Xlint:-options")
}

tasks.withType<Test>().configureEach {
    // The template currently contains a compile-only smoke-test class rather
    // than a JUnit test case. Gradle 9 otherwise fails the whole multi-version
    // build when no test methods are discovered.
    failOnNoDiscoveredTests.set(false)
}

extensions.configure<JavaPluginExtension> {
    sourceCompatibility = javaCompatibility
    targetCompatibility = javaCompatibility
    withSourcesJar()
}

tasks.named<Jar>("jar") {
    inputs.property("archives_base_name", archivesBaseName)
    from(rootProject.file("LICENSE")) {
        rename { fileName -> "${fileName}_$archivesBaseName" }
    }
}

val licenseExtension = extensions.getByName("license")
licenseExtension.withGroovyBuilder {
    setProperty("header", rootProject.file("HEADER.txt"))
    "include"("**/*.java")
    setProperty("skipExistingHeaders", true)
    "headerDefinitions" {
        requireNotNull("create"("SLASHSTAR_STYLE_NEWLINE")).withGroovyBuilder {
            setProperty("firstLine", "/*")
            setProperty("beforeEachLine", " * ")
            setProperty("endLine", " */" + System.lineSeparator())
            setProperty("afterEachLine", "")
            setProperty("skipLinePattern", null)
            setProperty("firstLineDetectionPattern", """(\s|\t)*/\*.*$""")
            setProperty("lastLineDetectionPattern", """.*\*/(\s|\t)*$""")
            setProperty("allowBlankLines", false)
            setProperty("isMultiline", true)
            setProperty("padLines", false)
        }
    }
    "mapping"("java", "SLASHSTAR_STYLE_NEWLINE")
}
(licenseExtension as ExtensionAware).extensions.extraProperties.apply {
    set("name", modProperty("mod_name"))
    set("author", "Fallen_Breath")
    set("year", Calendar.getInstance().get(Calendar.YEAR).toString())
}
tasks.named("classes") { dependsOn("licenseFormatMain") }
tasks.named("testClasses") { dependsOn("licenseFormatTest") }
tasks.named("licenseMain") { dependsOn("licenseFormatMain") }
tasks.named("licenseTest") { dependsOn("licenseFormatTest") }

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = baseExtension.archivesName.get()
            version = fullArtifactVersion
        }
    }
    repositories {
        // mavenLocal()
        // Optional FALLENS_MAVEN repository:
        // maven {
        //     url = uri(if (fullArtifactVersion.endsWith("SNAPSHOT"))
        //         "https://maven.fallenbreath.me/snapshots" else "https://maven.fallenbreath.me/releases")
        //     credentials(org.gradle.api.credentials.PasswordCredentials::class) {
        //         username = "fallen"
        //         password = System.getenv("FALLENS_MAVEN_TOKEN")
        //     }
        //     authentication { create<org.gradle.authentication.http.BasicAuthentication>("basic") }
        // }

        // Add repositories to retrieve artifacts from in here.
        // You should only use this when depending on other mods because
        // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
        // See https://docs.gradle.org/current/userguide/declaring_repositories.html
        // for more information about repositories.
        exclusiveContent {
            forRepository {
                maven {
                    name = "Modrinth"
                    url = uri("https://api.modrinth.com/maven")
                }
            }
            // forRepositories(fg.repository) // Uncomment when using ForgeGradle
            filter {
                includeGroup("maven.modrinth")
            }
        }
    }
}
