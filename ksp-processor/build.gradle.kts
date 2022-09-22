plugins {
    kotlin("jvm")
}

group = "pl.ninebits"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":ksp-annotation"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.21-1.0.6")
    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
}
