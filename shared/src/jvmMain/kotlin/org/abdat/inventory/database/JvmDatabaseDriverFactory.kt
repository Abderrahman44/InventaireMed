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
        }
        return driver
    }
}
