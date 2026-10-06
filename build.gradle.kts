plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
}

repositories {
	maven("https://api.modrinth.com/maven")
	maven("https://maven.terraformersmc.com/releases")
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

fabricApi {
	configureDataGeneration {
		client = true
		modId = "carpet-gui"
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	implementation("maven.modrinth:carpet:${providers.gradleProperty("carpet_version").get()}")
	implementation("com.terraformersmc:modmenu:${providers.gradleProperty("modmenu_version").get()}")
	implementation("maven.modrinth:tcdcommons:FwE7UAgA")
	implementation("maven.modrinth:7PCm6yD1:fB2DWKS1")
	// Optional PRY compatibility: compile against the supplied 26.3 jar, never bundle or require it at runtime.
	// Override with -Ppry_compile_jar=path/to/a/compatible/pry.jar when using another build.
	compileOnly(files(providers.gradleProperty("pry_compile_jar")
		.orElse("run/mods/carpet-pry-addition-v1.2.2-mc26.3+build.190.jar")))
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
