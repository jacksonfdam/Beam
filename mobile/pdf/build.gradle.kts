import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    jvm()

    androidLibrary {
        namespace = "com.jacksonfdam.beam.pdf"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Pure rendering output (ARGB pixels); no Compose/UI coupling here.
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.pdfbox)
        }
        // Android uses android.graphics.pdf.PdfRenderer (framework); iOS uses
        // CoreGraphics / CGPDF (platform) — no extra dependencies needed.
    }
}
