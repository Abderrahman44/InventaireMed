package org.abdat.inventory.database

data class BackupMetadata(
    val lastBackupDate: String?,
    val lastBackupPath: String?,
    val lastRestoreDate: String?
)

sealed class BackupValidationResult {
    data class Valid(
        val tableCount: Int,
        val articleCount: Long,
        val details: String = ""
    ) : BackupValidationResult()

    data class Invalid(val reason: String) : BackupValidationResult()
}

data class BackupExportResult(
    val destinationPath: String,
    val timestamp: String,
    val fileSizeInBytes: Long
)

data class BackupRestoreResult(
    val sourcePath: String,
    val timestamp: String,
    val safetyBackupPath: String?
)

interface DatabaseBackupService {
    suspend fun getBackupMetadata(): BackupMetadata
    suspend fun exportBackup(destinationPath: String): Result<BackupExportResult>
    suspend fun validateBackupFile(sourcePath: String): BackupValidationResult
    suspend fun restoreBackup(sourcePath: String): Result<BackupRestoreResult>
}
