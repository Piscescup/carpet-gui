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

val versions = versionSettings["versions"] as List<*>

for (entry in versions) {
	val version = entry as String
	include(":$version")

	project(":$version").apply {
		projectDir = file("versions/$version")
		buildFileName = "../../common.gradle.kts"
	}
}