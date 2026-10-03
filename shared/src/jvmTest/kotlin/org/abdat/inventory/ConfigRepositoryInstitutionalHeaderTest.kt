package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.data.repository.ConfigRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder
import org.abdat.inventory.ui.config.DynamicConfigViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigRepositoryInstitutionalHeaderTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var configRepository: ConfigRepository

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        configRepository = ConfigRepository(database)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun testDefaultInstitutionalHeaderOption1Values() {
        val header = configRepository.loadInstitutionalHeaderSync()

        assertEquals("Établissement Public Hospitalier", header.line1)
        assertEquals("El Attaf, Aïn Defla", header.line2)
        assertEquals("Département des Finances et des Moyens", header.line3)
        assertEquals("Service Magasin Central", header.line4)
        assertFalse(header.isBlank)
        assertEquals(InstitutionalHeader.DEFAULT, header)
        assertEquals(header, InstitutionalHeaderHolder.current)
    }

    @Test
    fun testSaveAndReloadCustomInstitutionalHeader() {
        val custom = InstitutionalHeader(
            line1 = "Centre Hospitalier Universitaire",
            line2 = "Blida",
            line3 = "Sous-Direction des Équipements",
            line4 = "Pharmacie Centrale"
        )

        val saved = configRepository.saveInstitutionalHeaderSync(custom)
        assertEquals(custom, saved)

        val reloaded = configRepository.loadInstitutionalHeaderSync()
        assertEquals(custom, reloaded)
        assertEquals(custom, InstitutionalHeaderHolder.current)
        assertEquals(custom, configRepository.institutionalHeader.value)
    }

    @Test
    fun testBlankLinesFallbackToDefaultsWhenSaved() {
        val blankHeader = InstitutionalHeader(
            line1 = "   ",
            line2 = "",
            line3 = "  ",
            line4 = ""
        )

        val saved = configRepository.saveInstitutionalHeaderSync(blankHeader)
        assertEquals(InstitutionalHeader.DEFAULT_LINE_1, saved.line1)
        assertEquals(InstitutionalHeader.DEFAULT_LINE_2, saved.line2)
        assertEquals(InstitutionalHeader.DEFAULT_LINE_3, saved.line3)
        assertEquals(InstitutionalHeader.DEFAULT_LINE_4, saved.line4)
    }

    @Test
    fun testResetInstitutionalHeaderRestoresDefaults() {
        val custom = InstitutionalHeader(
            line1 = "Hôpital Militaire",
            line2 = "Oran",
            line3 = "Direction Logistique",
            line4 = "Dépôt Central"
        )
        configRepository.saveInstitutionalHeaderSync(custom)
        assertEquals("Hôpital Militaire", configRepository.loadInstitutionalHeaderSync().line1)

        val reset = configRepository.resetInstitutionalHeaderSync()
        assertEquals(InstitutionalHeader.DEFAULT, reset)
        assertEquals(InstitutionalHeader.DEFAULT, configRepository.loadInstitutionalHeaderSync())
    }

    @Test
    fun testDynamicConfigViewModelIntegration() {
        val categoryRepo = CategoryRepository(database, kotlinx.coroutines.Dispatchers.IO)
        val viewModel = DynamicConfigViewModel(database, categoryRepo, null, configRepository)

        assertEquals(InstitutionalHeader.DEFAULT, viewModel.uiState.value.institutionalHeader)

        val updated = InstitutionalHeader(
            line1 = "EPH Sidi Bouzid",
            line2 = "Ain Defla",
            line3 = "Service Financier",
            line4 = "Magasin Central"
        )
        val success = viewModel.saveInstitutionalHeaderSync(updated)
        assertTrue(success)
        assertEquals(updated, viewModel.uiState.value.institutionalHeader)
        assertNotNull(viewModel.uiState.value.actionSuccessMessage)

        val reset = viewModel.resetInstitutionalHeaderSync()
        assertEquals(InstitutionalHeader.DEFAULT, reset)
        assertEquals(InstitutionalHeader.DEFAULT, viewModel.uiState.value.institutionalHeader)
    }
}
