package com.sniperoftdm.booktracker.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val books: List<BackupBook> = emptyList(),
    val sessions: List<BackupSession> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
    val settings: BackupSettings? = null
)

@Serializable
data class BackupBook(
    val exportId: String,
    val title: String,
    val author: String = "",
    val totalPages: Int,
    val startPage: Int = 0,
    val status: String = "READING",
    val goalDateEpochDay: Long? = null,
    val createdAt: Long
)

@Serializable
data class BackupSession(
    val exportId: String,
    val bookExportId: String,
    val timestamp: Long,
    val endPage: Int,
    val durationSeconds: Int? = null
)

@Serializable
data class BackupNote(
    val exportId: String,
    val bookExportId: String,
    val page: Int,
    val text: String,
    val createdAt: Long
)

@Serializable
data class BackupSettings(
    val countZeroDays: Boolean = true,
    val paceWindowDays: Int = 7,
    val yearlyGoalBooks: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0
)

data class ImportProblem(
    val bookTitle: String,
    val reason: String
)

data class ImportReport(
    val problems: List<ImportProblem> = emptyList()
)

sealed interface ImportResult {
    data class Success(val booksImported: Int, val sessionsImported: Int, val notesImported: Int) : ImportResult
    data class ValidationError(val report: ImportReport) : ImportResult
    data class Error(val message: String) : ImportResult
}

enum class ImportMode {
    REPLACE_ALL
}
