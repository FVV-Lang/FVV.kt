@file:Suppress("UnstableApiUsage")

rootProject.name = "core"

pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		google()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		google()
		gradlePluginPortal()
		maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
	}
}