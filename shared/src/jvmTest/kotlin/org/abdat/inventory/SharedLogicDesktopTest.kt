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
        }

        val koinApp = startKoin {
            modules(appModule, testPlatformModule)
        }

        val database = koinApp.koin.get<AppDatabase>()
        database.appDatabaseQueries.setMetadata("status", "ready")
        val status = database.appDatabaseQueries.getMetadata("status").executeAsOne()
        assertEquals("ready", status)
    }
}