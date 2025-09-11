import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	// noinspection AndroidGradlePluginVersion
	id("com.android.library") version "8.11.0" apply true
	// noinspection GradleDynamicVersion
	kotlin("multiplatform") version "+" apply true
	`maven-publish`
}

group = "ren.shiror"
version = "-SNAPSHOT"

kotlin {
	withSourcesJar()

	androidTarget {
		compilerOptions.jvmTarget = JvmTarget.JVM_1_8
	}

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

	jvm()
}

android {
	namespace = "ren.shiror.fvv"
	compileSdk = 36

	defaultConfig {
		minSdk = 1
		consumerProguardFiles("consumer-rules.pro")
	}
	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
}

afterEvaluate {
	publishing {
		publications {
			withType<MavenPublication> {
				groupId = "ren.shiror"
				artifactId = "FVV"
				version = "-SNAPSHOT"

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