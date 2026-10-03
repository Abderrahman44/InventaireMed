package org.abdat.inventory.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder

class ConfigRepository(
    private val database: AppDatabase
) {
    private val _institutionalHeader = MutableStateFlow(loadInstitutionalHeaderSync())
    val institutionalHeader: StateFlow<InstitutionalHeader> = _institutionalHeader.asStateFlow()

    init {
        InstitutionalHeaderHolder.current = _institutionalHeader.value
    }

    fun loadInstitutionalHeaderSync(): InstitutionalHeader {
        val l1 = database.appDatabaseQueries.getMetadata("header_institution").executeAsOneOrNull()
        val l2 = database.appDatabaseQueries.getMetadata("header_location").executeAsOneOrNull()
        val l3 = database.appDatabaseQueries.getMetadata("header_department").executeAsOneOrNull()
        val l4 = database.appDatabaseQueries.getMetadata("header_service").executeAsOneOrNull()

        val header = InstitutionalHeader(
            line1 = l1 ?: InstitutionalHeader.DEFAULT_LINE_1,
            line2 = l2 ?: InstitutionalHeader.DEFAULT_LINE_2,
            line3 = l3 ?: InstitutionalHeader.DEFAULT_LINE_3,
            line4 = l4 ?: InstitutionalHeader.DEFAULT_LINE_4
        )
        InstitutionalHeaderHolder.current = header
        return header
    }

    fun saveInstitutionalHeaderSync(header: InstitutionalHeader): InstitutionalHeader {
        val sanitized = InstitutionalHeader(
            line1 = header.line1.trim().ifBlank { InstitutionalHeader.DEFAULT_LINE_1 },
            line2 = header.line2.trim().ifBlank { InstitutionalHeader.DEFAULT_LINE_2 },
            line3 = header.line3.trim().ifBlank { InstitutionalHeader.DEFAULT_LINE_3 },
            line4 = header.line4.trim().ifBlank { InstitutionalHeader.DEFAULT_LINE_4 }
        )
        database.transaction {
            database.appDatabaseQueries.setMetadata("header_institution", sanitized.line1)
            database.appDatabaseQueries.setMetadata("header_location", sanitized.line2)
            database.appDatabaseQueries.setMetadata("header_department", sanitized.line3)
            database.appDatabaseQueries.setMetadata("header_service", sanitized.line4)
        }
        InstitutionalHeaderHolder.current = sanitized
        _institutionalHeader.value = sanitized
        return sanitized
    }

    fun resetInstitutionalHeaderSync(): InstitutionalHeader {
        return saveInstitutionalHeaderSync(InstitutionalHeader.DEFAULT)
    }
}
