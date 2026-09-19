package org.abdat.inventory

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.abdat.inventory.database.JvmDatabaseDriverFactory
import org.abdat.inventory.database.createDatabase
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.database.seedInitialDataIfEmpty
import org.abdat.inventory.di.initDesktopKoin
import java.io.File

fun main(args: Array<String>) {
    val seedIndex = args.indexOfFirst { it == "--seed" || it == "-seed" || it.startsWith("--seed=") }
    if (seedIndex != -1) {
        val arg = args[seedIndex]
        val targetPath = when {
            arg.startsWith("--seed=") -> arg.substringAfter("--seed=")
            seedIndex + 1 < args.size && !args[seedIndex + 1].startsWith("-") -> args[seedIndex + 1]
            else -> {
                val userHome = System.getProperty("user.home")
                File(userHome, ".inventairemed/inventory.db").absolutePath
            }
        }

        println("==================================================")
        println("InventaireMed Database Seeder")
        println("Target database: $targetPath")
        println("==================================================")

        val factory = JvmDatabaseDriverFactory(targetPath)
        val db = createDatabase(factory)
        ensureCategoryTaxonomy(db)
        seedInitialDataIfEmpty(db)

        println("✓ Database initialized, taxonomy verified, and seeded successfully at: $targetPath")
        return
    }

    initDesktopKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "EPH InventaireMed",
        ) {
            App()
        }
    }
}