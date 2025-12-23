@file:Suppress("UnstableApiUsage")

import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// noinspection GradleDynamicVersion
plugins {
	// noinspection AndroidGradlePluginVersion
	id("com.android.kotlin.multiplatform.library") version "+" apply true
	kotlin("multiplatform") version "+" apply true
	`maven-publish`
	id("com.palantir.git-version") version "+"
}

group = "ren.shiror"
val versionDetails: Closure<VersionDetails> by extra
version = versionDetails().lastTag ?: "0.1"

kotlin {
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

	jvm()

	androidNativeArm64()
	androidNativeArm32()
	androidNativeX64()
	androidNativeX86()

	iosArm64()
	iosSimulatorArm64()
	iosX64()
	macosArm64()
	macosX64()
	tvosArm64()
	tvosSimulatorArm64()
	tvosX64()
}

afterEvaluate {
	publishing {
		publications {
			withType<MavenPublication> {
				groupId = "ren.shiror"
				artifactId = "fvv"
				version = versionDetails().lastTag ?: "0.1"

				pom {
					name = "FVV"
					description = "FVV Language for Kotlin"
					url = "https://github.com/OOM-WG/FVV"

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
						name = "O.O.M. W.G."
						url = "https://oom-wg.dev"
					}

					scm {
						connection = "scm:git:https://github.com/OOM-WG/FVV.git"
						developerConnection = "scm:git:https://github.com/OOM-WG/FVV.git"
						url = "https://github.com/OOM-WG/FVV.git"
					}
				}
			}
		}
		repositories {
			mavenLocal()
		}
	}
}