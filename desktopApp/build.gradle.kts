import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)

    implementation(libs.koin.core)
    implementation(libs.koin.compose)
}

compose.desktop {
    application {
        mainClass = "org.abdat.inventory.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm
            )
            packageName = "org.abdat.inventory"
            packageVersion = providers.gradleProperty("packageVersion").getOrElse("1.0.0")
            modules("java.sql", "java.naming", "java.xml")
        }
    }
}

tasks.register<JavaExec>("seedDb") {
    group = "application"
    description = "Seeds the target SQLite database with mock data."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.abdat.inventory.MainKt")
    val dbPath = project.findProperty("dbPath") as? String
        ?: project.findProperty("seedDbPath") as? String
        ?: ""
    args = if (dbPath.isNotBlank()) listOf("--seed", dbPath) else listOf("--seed")
}

