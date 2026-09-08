package org.abdat.inventory.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

class JvmDatabaseDriverFactory(
    private val dbPath: String? = null
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val dbFile = if (dbPath != null) {
            File(dbPath)
        } else {
            val userHome = System.getProperty("user.home")
            val appDir = File(userHome, ".inventairemed")
            File(appDir, "inventory.db")
        }

        val isNew = !dbFile.exists()
        dbFile.parentFile?.mkdirs()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) {
            AppDatabase.Schema.create(driver)
        } else {
            val columns = listOf(
                "societe TEXT NOT NULL DEFAULT ''",
                "phone TEXT NOT NULL DEFAULT ''",
                "responsable_phone TEXT NOT NULL DEFAULT ''",
                "ville TEXT NOT NULL DEFAULT ''",
                "email TEXT NOT NULL DEFAULT ''",
                "other_links TEXT NOT NULL DEFAULT ''"
            )
            for (col in columns) {
                try {
                    driver.execute(null, "ALTER TABLE fournisseur ADD COLUMN $col", 0)
                } catch (_: Exception) {
                    // Column already exists or migration already applied
                }
            }
        }
        return driver
    }
}
