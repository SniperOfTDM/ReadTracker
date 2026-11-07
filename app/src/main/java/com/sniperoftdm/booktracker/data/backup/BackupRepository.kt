package com.sniperoftdm.booktracker.data.backup

import java.io.InputStream
import java.io.OutputStream

interface BackupRepository {

    /** Serializes all books, sessions, notes, and user settings into an output JSON stream. */
    suspend fun export(outputStream: OutputStream): Result<Unit>

    /** Validates and imports data from an input JSON stream, updating database and preferences. */
    suspend fun import(inputStream: InputStream, mode: ImportMode = ImportMode.REPLACE_ALL): ImportResult
}
