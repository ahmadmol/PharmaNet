import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        // Match Gradle’s default Java (avoids toolchain + JDK 11 install); :core is JVM-only glue
        jvmTarget.set(JvmTarget.JVM_21)
    }
}
