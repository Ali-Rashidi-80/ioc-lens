import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    testImplementation(libs.kotlin.test)
}

compose.desktop {
    application {
        mainClass = "io.github.mrcoder20.ioclens.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "io.github.mrcoder20.ioclens"
            packageVersion = "1.0.0"
            description = "High-Speed IOC & Security Intelligence Hub"
            vendor = "mr-coder20"
            copyright = "© 2026 mr-coder20"

            modules("java.instrument", "java.management", "java.sql", "jdk.unsupported", "java.naming", "java.desktop", "jdk.security.auth")

            windows {
                iconFile.set(project.file("../docs/assets/logo/icon.ico"))
                upgradeUuid = "a8e5d7b2-4e8a-4d5e-b53f-9d1a0c7f3b4e"
                shortcut = true
                menu = true
                menuGroup = "IOC Lens"
                dirChooser = true
                jvmArgs("-Xmx2G", "-Dcompose.application.configure.stdio=true")
            }
            macOS {
                iconFile.set(project.file("../docs/assets/logo/icon.icns"))
                bundleID = "io.github.mrcoder20.ioclens"
                dockName = "IOC Lens"
                appCategory = "public.app-category.developer-tools"
            }
            linux {
                iconFile.set(project.file("../docs/assets/logo/icon-512.png"))
                shortcut = true
                menuGroup = "Utility"
            }
        }
    }
}
