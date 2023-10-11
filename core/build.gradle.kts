plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
    id("com.android.library")
}

base {
    archivesBaseName = "kotlinx-datetime" // doesn't work
}

val serializationVersion: String = "1.5.1"
val roboVMVersion: String by project

// Attributes
val isForRoboVMAttribute = Attribute.of("is_for_robovm", String::class.java)

kotlin {
    explicitApi()

    jvm {
        attributes {
            attribute(isForRoboVMAttribute, "standard-jvm")
        }
    }
    jvm("robovm") {
        attributes {
            attribute(isForRoboVMAttribute, "robovm")
        }
    }
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Multiplatform module for getting date/time objects."
        homepage = "https://github.com/Kotlin/kotlinx-datetime"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "kotlinx-datetime"
        }
    }

    sourceSets.all {
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).lowercase().takeIf { it != "main" }
//        println("SOURCE_SET: $name")
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources" } ?: "resources"}")
        languageSettings {
            //            progressiveMode = true
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["test"].kotlinOptions {
            freeCompilerArgs += listOf("-trw")
        }
        if (konanTarget.family.isAppleFamily) {
            return@withType
        }
        compilations["main"].cinterops {
            create("date") {
                val cinteropDir = "$projectDir/native/cinterop"
                val dateLibDir = "${project(":").projectDir}/thirdparty/date"
                headers("$cinteropDir/public/cdate.h")
                defFile("native/cinterop/date.def")
                extraOpts("-Xsource-compiler-option", "-I$cinteropDir/public")
                extraOpts("-Xsource-compiler-option", "-DONLY_C_LOCALE=1")
                when {
                    konanTarget.family == org.jetbrains.kotlin.konan.target.Family.LINUX -> {
                        // needed for the date library so that it does not try to download the timezone database
                        extraOpts("-Xsource-compiler-option", "-DUSE_OS_TZDB=1")
                        /* using a more modern C++ version causes the date library to use features that are not
                    * present in the currently outdated GCC root shipped with Kotlin/Native for Linux. */
                        extraOpts("-Xsource-compiler-option", "-std=c++11")
                        // the date library and its headers
                        extraOpts("-Xcompile-source", "$dateLibDir/src/tz.cpp")
                        extraOpts("-Xsource-compiler-option", "-I$dateLibDir/include")
                        // the main source for the platform bindings.
                        extraOpts("-Xcompile-source", "$cinteropDir/cpp/cdate.cpp")
                    }
                    konanTarget.family == org.jetbrains.kotlin.konan.target.Family.MINGW -> {
                        // needed to be able to use std::shared_mutex to implement caching.
                        extraOpts("-Xsource-compiler-option", "-std=c++17")
                        // the date library headers, needed for some pure calculations.
                        extraOpts("-Xsource-compiler-option", "-I$dateLibDir/include")
                        // the main source for the platform bindings.
                        extraOpts("-Xcompile-source", "$cinteropDir/cpp/windows.cpp")
                    }
                    else -> {
                        throw IllegalArgumentException("Unknown native target ${this@withType}")
                    }
                }
            }
        }
        compilations["main"].defaultSourceSet {
            kotlin.srcDir("native/cinterop_actuals")
        }
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
                compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }

        val commonTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test")
            }
        }

        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }

        val jvmTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        val robovmMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib")
                api("com.mobidevelop.robovm:robovm-rt:$roboVMVersion")
                api("com.mobidevelop.robovm:robovm-cocoatouch:$roboVMVersion")
            }
        }

        val androidMain by getting {
            dependsOn(jvmMain)
        }
        val androidUnitTest by getting

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }

        val nativeTest by creating {
            dependsOn(commonTest)
        }

        val darwinMain by creating {
            dependsOn(nativeMain)
        }

        val darwinTest by creating {
            dependsOn(nativeTest)
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(darwinMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(darwinTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "kotlinx.datetime"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles.addAll(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), File("proguard-rules.pro")))
        }
        create("releaseTest") {
            initWith(buildTypes["release"])
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}