package com.sniperoftdm.booktracker.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CoverStorageUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storage: CoverStorage
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        storage = CoverStorage(filesDir)
    }

    @Test
    fun getFile_returnsFile_whenExists() {
        val coversDir = File(filesDir, "covers").apply { mkdirs() }
        val testFile = File(coversDir, "42.jpg").apply { writeText("mock image bytes") }

        val resolved = storage.getFile("covers/42.jpg")
        assertNotNull(resolved)
        assertEquals(testFile.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun getFile_returnsNull_whenMissingOrBlank() {
        assertNull(storage.getFile(null))
        assertNull(storage.getFile(""))
        assertNull(storage.getFile("covers/999.jpg"))
    }

    @Test
    fun delete_removesBookCoverFile() {
        val coversDir = File(filesDir, "covers").apply { mkdirs() }
        val testFile = File(coversDir, "10.jpg").apply { writeText("mock image") }
        assertTrue(testFile.exists())

        val result = storage.delete(10L)
        assertTrue(result)
        assertFalse(testFile.exists())
    }

    @Test
    fun delete_returnsTrue_whenFileDoesNotExist() {
        assertTrue(storage.delete(999L))
    }

    @Test
    fun clearAll_deletesAllCoverFiles() {
        val coversDir = File(filesDir, "covers").apply { mkdirs() }
        File(coversDir, "1.jpg").writeText("1")
        File(coversDir, "2.jpg").writeText("2")
        File(coversDir, "3.jpg").writeText("3")

        assertEquals(3, coversDir.listFiles()?.size)

        storage.clearAll()
        assertEquals(0, coversDir.listFiles()?.size)
    }
}
