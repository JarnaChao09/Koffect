plugins {
    kotlin("multiplatform") version "2.4.10"
    // application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// dependencies {
//     // testImplementation(kotlin("test"))
// }

// tasks.test {
//     useJUnitPlatform()
// }

kotlin {
    jvm()
    jvmToolchain(8)

    explicitApi()

    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val llvm by creating
            }
        }
        binaries {
            executable {
                entryPoint = "main"
                runTaskProvider?.configure {
                    standardInput = System.`in`
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {

        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {

        }

        nativeMain.dependencies {

        }
        all {
            compilerOptions {
                freeCompilerArgs.add("-Xcontext-parameters")
            }
        }
    }
}

// application {
//     mainClass.set("MainKt")
// }