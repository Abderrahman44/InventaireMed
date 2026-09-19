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

            val conventionColumns = listOf(
                "tva_rate REAL NOT NULL DEFAULT 0.0"
            )
            for (col in conventionColumns) {
                try {
                    driver.execute(null, "ALTER TABLE convention ADD COLUMN $col", 0)
                } catch (_: Exception) {
                    // Column already exists or migration already applied
                }
            }

            val livraisonColumns = listOf(
                "service_id INTEGER",
                "beneficiaire_id INTEGER"
            )
            for (col in livraisonColumns) {
                try {
                    driver.execute(null, "ALTER TABLE bon_de_livraison ADD COLUMN $col", 0)
                } catch (_: Exception) {
                    // Column already exists or migration already applied
                }
            }

            val serviceColumns = listOf(
                "chef_id INTEGER"
            )
            for (col in serviceColumns) {
                try {
                    driver.execute(null, "ALTER TABLE service ADD COLUMN $col", 0)
                } catch (_: Exception) {
                    // Column already exists or migration already applied
                }
            }

            val sortieColumns = listOf(
                "demandeur TEXT NOT NULL DEFAULT ''",
                "recepteur_interne TEXT NOT NULL DEFAULT ''",
                "motif TEXT NOT NULL DEFAULT ''",
                "chef_service TEXT NOT NULL DEFAULT ''",
                "beneficiaire_ids TEXT NOT NULL DEFAULT ''"
            )
            for (col in sortieColumns) {
                try {
                    driver.execute(null, "ALTER TABLE bon_de_sortie ADD COLUMN $col", 0)
                } catch (_: Exception) {
                    // Column already exists or migration already applied
                }
            }

            // Ensure newer tables exist if migrating an older database file
            val newTables = listOf(
                """CREATE TABLE IF NOT EXISTS papier_decharge (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    reference_bon TEXT NOT NULL,
                    organization_name TEXT NOT NULL,
                    donor_name TEXT NOT NULL,
                    date TEXT NOT NULL,
                    notes TEXT NOT NULL
                );""".trimIndent(),
                """CREATE TABLE IF NOT EXISTS decharge_item (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    decharge_id INTEGER NOT NULL REFERENCES papier_decharge(id) ON DELETE CASCADE,
                    code_article TEXT NOT NULL,
                    designation TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    unit_price REAL NOT NULL DEFAULT 0.0
                );""".trimIndent(),
                """CREATE TABLE IF NOT EXISTS person (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    service_id INTEGER NOT NULL REFERENCES service(id) ON DELETE CASCADE,
                    full_name TEXT NOT NULL,
                    birth_date TEXT NOT NULL,
                    notes TEXT NOT NULL
                );""".trimIndent(),
                """CREATE TABLE IF NOT EXISTS equipment_assignment (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    person_id INTEGER NOT NULL REFERENCES person(id) ON DELETE CASCADE,
                    article_id INTEGER NOT NULL REFERENCES stock_article(id) ON DELETE CASCADE,
                    quantity REAL NOT NULL,
                    assigned_date TEXT NOT NULL,
                    return_date TEXT,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    notes TEXT NOT NULL
                );""".trimIndent(),
                """CREATE TABLE IF NOT EXISTS dynamic_config_item (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    category TEXT NOT NULL,
                    code TEXT NOT NULL,
                    label TEXT NOT NULL,
                    parent_code TEXT,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    sort_order INTEGER NOT NULL DEFAULT 0
                );""".trimIndent(),
                """CREATE TABLE IF NOT EXISTS user_profile (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    role TEXT NOT NULL,
                    service_id INTEGER REFERENCES service(id) ON DELETE SET NULL,
                    local_num TEXT NOT NULL DEFAULT ''
                );""".trimIndent()
            )
            for (stmt in newTables) {
                try {
                    driver.execute(null, stmt, 0)
                } catch (_: Exception) {
                    // Table already exists or migration already applied
                }
            }
        }
        return driver
    }
}
