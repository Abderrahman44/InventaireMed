package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.database.seedInitialDataIfEmpty
import org.abdat.inventory.ui.services.ServicesViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServicesLocauxScreenRefactorTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        seedInitialDataIfEmpty(database)
        ensureCategoryTaxonomy(database)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun testDenseLocalRoomsDatasetForLazyColumn() {
        val viewModel = ServicesViewModel(database)
        val selectedService = viewModel.uiState.value.selectedService
        assertNotNull(selectedService, "Expected a default selected service")

        // Seed 30 additional local rooms to simulate dense data for the virtualized LazyColumn
        for (i in 1..30) {
            val localName = "Salle Spécialisée Dense #$i"
            val success = viewModel.addLocalSync(selectedService.id, localName)
            assertTrue(success, "Adding local room $i should succeed")
        }

        viewModel.selectService(selectedService)
        val state = viewModel.uiState.value
        val locals = state.localsForSelectedService

        // Verify dataset density
        assertTrue(locals.size >= 30, "Expected at least 30 locals in selected service")

        // Verify each local has unique stable key (id > 0)
        val uniqueIds = locals.map { it.id }.toSet()
        assertEquals(locals.size, uniqueIds.size, "Every local must have a unique stable ID for LazyColumn key")

        // Verify all locals belong to the selected service
        assertTrue(locals.all { it.serviceId == selectedService.id })
    }

    @Test
    fun testEmptyLocauxStateForNewlyCreatedService() {
        val viewModel = ServicesViewModel(database)
        val newServiceName = "Service Radiothérapie Spéciale"
        val success = viewModel.addServiceSync(newServiceName, "SRV-RADIO-01")
        assertTrue(success)

        val state = viewModel.uiState.value
        val createdService = state.services.firstOrNull { it.name == newServiceName }
        assertNotNull(createdService)

        viewModel.selectService(createdService)
        val updatedState = viewModel.uiState.value

        // When a service has 0 locals, locals list is empty
        assertEquals(0, updatedState.localsForSelectedService.size, "Newly created service should have 0 locals")
        assertEquals(createdService.id, updatedState.selectedService?.id)
    }
}
