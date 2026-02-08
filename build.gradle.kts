@file:Suppress("UnstableApiUsage") @file:OptIn(ExperimentalWasmDsl::class)

import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// noinspection GradleDynamicVersion
plugins {
	// noinspection AndroidGradlePluginVersion
	id("com.android.kotlin.multiplatform.library") version "+"
	kotlin("multiplatform") version "+"
	kotlin("plugin.serialization") version "+"
	`maven-publish`
	id("com.palantir.git-version") version "+"
}

kotlin {
	applyDefaultHierarchyTemplate()
	withSourcesJar()

	androidLibrary {
		namespace = "ren.shiror.fvv"
		compileSdk = 36
		minSdk = 1
		buildToolsVersion = "36.1.0"

		compilerOptions.jvmTarget = JvmTarget.JVM_1_8

		optimization {
			consumerKeepRules.publish = true
			consumerKeepRules.files("consumer-rules.pro")
			minify = false
		}
	}

	jvm { compilerOptions.jvmTarget = JvmTarget.JVM_1_8 }

	androidNativeArm64()
	androidNativeArm32()
	androidNativeX64()
	androidNativeX86()

	iosArm64()
	iosX64()
	iosSimulatorArm64()
	macosArm64()
	macosX64()
	tvosArm64()
	tvosX64()
	tvosSimulatorArm64()
	watchosArm64()
	watchosArm32()
	watchosX64()
	watchosDeviceArm64()
	watchosSimulatorArm64()

	linuxArm64()
	linuxX64()

	mingwX64()

	js(IR) {
		browser()
		nodejs()
	}
	wasmJs {
		browser()
		nodejs()
	}

	// noinspection GradleDynamicVersion
	sourceSets.commonMain.dependencies {
		implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:+")
	}
}

afterEvaluate {
	publishing {
		publications {
			withType<MavenPublication> {
				groupId = "ren.shiror.fvv"
				val versionDetails: Closure<VersionDetails> by extra
				version = versionDetails().lastTag ?: "0.0"

				pom {
					name = "FVV"
					description = "FVV Language for Kotlin"
					url = "https://github.com/FVV-Lang/FVV.kt"

					licenses {
						license {
							name = "F2DLPRL"
							url = "https://license.fileto.download/LICENSE.txt"
							distribution = "repo"
						}
					}

					developers {
						developer {
							id = "shirorren"
							name = "ShIroRRen"
							email = "shiro@oom-wg.dev"
							url = "https://shiror.ren"
						}
					}

					organization {
						name = "OOM WG"
						url = "https://oom-wg.dev"
					}

					scm {
						connection = "scm:git:https://github.com/FVV-Lang/FVV.kt"
						developerConnection = "scm:git:https://github.com/FVV-Lang/FVV.kt"
						url = "https://github.com/FVV-Lang/FVV.kt"
					}
				}

				when (name) {
					"androidRelease" -> "releaseRuntimeClasspath"
					else             -> listOf(
						"${name}RuntimeClasspath", "${name}CompileKlibraries"
					).firstOrNull { project.configurations.findByName(it) != null }
				}?.let {
					versionMapping { allVariants { fromResolutionOf(it) } }
				}
			}
		}
		repositories { mavenLocal() }
	}
}