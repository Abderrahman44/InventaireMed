package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.DatabaseDriverFactory
import org.abdat.inventory.di.appModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicDesktopTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testSqlDelightDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)

        database.appDatabaseQueries.setMetadata("version", "1.0.0")
        val version = database.appDatabaseQueries.getMetadata("version").executeAsOne()
        assertEquals("1.0.0", version)
    }

    @Test
    fun testKoinDependencyInjection() {
        val testPlatformModule = module {
            single<DatabaseDriverFactory> {
                object : DatabaseDriverFactory {
                    override fun createDriver() = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
                        AppDatabase.Schema.create(it)
                    }
                }
            }
            single<kotlinx.coroutines.CoroutineDispatcher> { kotlinx.coroutines.Dispatchers.IO }
        }

        val koinApp = startKoin {
            modules(appModule, testPlatformModule)
        }

        val database = koinApp.koin.get<AppDatabase>()
        database.appDatabaseQueries.setMetadata("status", "ready")
        val status = database.appDatabaseQueries.getMetadata("status").executeAsOne()
        assertEquals("ready", status)

        val deliveriesVm = koinApp.koin.get<org.abdat.inventory.ui.deliveries.DeliveriesViewModel>()
        kotlin.test.assertNotNull(deliveriesVm)
        val personnelVm = koinApp.koin.get<org.abdat.inventory.ui.personnel.PersonnelViewModel>()
        kotlin.test.assertNotNull(personnelVm)
        val dechargeVm = koinApp.koin.get<org.abdat.inventory.ui.decharges.DechargeViewModel>()
        kotlin.test.assertNotNull(dechargeVm)
        val sortiesVm = koinApp.koin.get<org.abdat.inventory.ui.sorties.SortiesViewModel>()
        kotlin.test.assertNotNull(sortiesVm)
    }

    @Test
    fun testJvmDatabaseDriverFactoryAppliesMigrations() {
        val tempDb = java.io.File.createTempFile("test_migration", ".db")
        tempDb.deleteOnExit()

        // Create an older schema without convention.tva_rate and bon_de_livraison.service_id
        val initialDriver = JdbcSqliteDriver("jdbc:sqlite:${tempDb.absolutePath}")
        initialDriver.execute(null, """
            CREATE TABLE fournisseur (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL
            );
        """.trimIndent(), 0)
        initialDriver.execute(null, """
            CREATE TABLE convention (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                fournisseur_id INTEGER NOT NULL REFERENCES fournisseur(id) ON DELETE CASCADE,
                ord_number INTEGER NOT NULL,
                date TEXT NOT NULL,
                global_unit_price REAL NOT NULL DEFAULT 0.0,
                total_montant REAL NOT NULL DEFAULT 0.0
            );
        """.trimIndent(), 0)
        initialDriver.execute(null, """
            CREATE TABLE bon_de_livraison (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                reference_bon TEXT NOT NULL,
                fournisseur_id INTEGER NOT NULL,
                date_livraison TEXT NOT NULL,
                commande_id INTEGER
            );
        """.trimIndent(), 0)
        initialDriver.execute(null, """
            CREATE TABLE service (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                local_num TEXT NOT NULL DEFAULT ''
            );
        """.trimIndent(), 0)
        initialDriver.close()

        // Now run JvmDatabaseDriverFactory on existing DB
        val factory = org.abdat.inventory.database.JvmDatabaseDriverFactory(tempDb.absolutePath)
        val migratedDriver = factory.createDriver()

        // Verify that migrated columns exist and can be selected
        migratedDriver.executeQuery(null, "SELECT tva_rate FROM convention", {
            app.cash.sqldelight.db.QueryResult.Value(Unit)
        }, 0)
        migratedDriver.executeQuery(null, "SELECT service_id, beneficiaire_id FROM bon_de_livraison", {
            app.cash.sqldelight.db.QueryResult.Value(Unit)
        }, 0)
        migratedDriver.executeQuery(null, "SELECT chef_id FROM service", {
            app.cash.sqldelight.db.QueryResult.Value(Unit)
        }, 0)
        migratedDriver.close()
        tempDb.delete()
    }
}