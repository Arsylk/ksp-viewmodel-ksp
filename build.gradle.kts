import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21" apply false
    id("com.google.devtools.ksp") version "1.6.21-1.0.6" apply false
}

group = "pl.ninebits"
version = "1.0"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}