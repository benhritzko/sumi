plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(17)

    // Desktop only for now. Add androidTarget(), iosArm64(), wasmJs() etc. later;
    // everything lives in commonMain so it carries over unchanged.
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "dev.sumi.resources"
}
