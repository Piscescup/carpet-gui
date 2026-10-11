import groovy.json.JsonSlurper

pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		maven {
			name = "Jitpack"
			url = uri("https://jitpack.io")
			content {
				includeGroupAndSubgroups("com.github")
			}
		}
		mavenCentral()
		gradlePluginPortal()
	}

	resolutionStrategy {
		eachPlugin {
			when (requested.id.id) {
				"com.replaymod.preprocess" ->
					useModule(
						"com.github.Fallen-Breath:preprocessor:${requested.version}"
					)
			}
		}
	}
}

val versionSettings = JsonSlurper()
	.parseText(file("settings.json").readText()) as Map<*, *>

val versions = (versionSettings["versions"] as? List<*>)
	?.map { requireNotNull(it as? String) { "Every settings.json version must be a string" } }
	?: error("settings.json must contain a versions array")

require(versions.isNotEmpty()) { "settings.json must enable at least one Minecraft version" }
require(versions.size == versions.distinct().size) { "settings.json contains duplicate Minecraft versions" }

// The main node owns src/main and generated resources. All other enabled
// projects receive their sources through ReplayMod Preprocess.
val mainProject = file("versions/mainProject").readText().trim()
require(mainProject in versions) {
	"versions/mainProject ($mainProject) must also be enabled in settings.json"
}

for (version in versions) {
	val versionDirectory = file("versions/$version")
	require(versionDirectory.isDirectory) { "Missing version directory: $versionDirectory" }
	require(file("$versionDirectory/gradle.properties").isFile) {
		"Missing version properties: $versionDirectory/gradle.properties"
	}
	include(":$version")

	project(":$version").apply {
		projectDir = versionDirectory
		buildFileName = "../../common.gradle.kts"
	}
}
