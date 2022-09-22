plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

group = "pl.ninebits"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

dependencies {
    implementation(project(":ksp-annotation"))
    ksp(project(":ksp-processor"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

ksp {
    arg("autogen-viewmodel", "pl.ninebits.mock.ViewModel")
    arg("autogen-viewmodel-provider-factory", "pl.ninebits.mock.ViewModelProvider.Factory")
}
